package com.payflowx.backend.service;

import com.payflowx.backend.config.SecurityProperties;
import com.payflowx.backend.dto.CreateTransactionRequest;
import com.payflowx.backend.dto.CreateTransactionResponse;
import com.payflowx.backend.dto.FinalizeTransactionRequest;
import com.payflowx.backend.dto.ProviderResponse;
import com.payflowx.backend.dto.TransactionStatusResponse;
import com.payflowx.backend.dto.ValidateTransactionDataRequest;
import com.payflowx.backend.dto.ValidateTransactionDataResponse;
import com.payflowx.backend.entity.ProviderEntity;
import com.payflowx.backend.entity.SubscriptionEntity;
import com.payflowx.backend.entity.TransactionEntity;
import com.payflowx.backend.enums.ProviderStatus;
import com.payflowx.backend.enums.SubscriptionStatus;
import com.payflowx.backend.enums.TransactionStatus;
import com.payflowx.backend.exception.ApiException;
import com.payflowx.backend.repository.MerchantRepository;
import com.payflowx.backend.repository.ProviderRepository;
import com.payflowx.backend.repository.SubscriptionRepository;
import com.payflowx.backend.repository.TransactionRepository;
import com.payflowx.backend.util.CryptoUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {
    private static final List<String> SUPPORTED_CURRENCIES = List.of("USD", "EUR", "INR", "GBP", "SGD");

    private final MerchantRepository merchantRepository;
    private final ProviderRepository providerRepository;
    private final TransactionRepository transactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SecurityProperties securityProperties;
    private final AuditService auditService;

    public PaymentService(MerchantRepository merchantRepository,
                          ProviderRepository providerRepository,
                          TransactionRepository transactionRepository,
                          SubscriptionRepository subscriptionRepository,
                          SecurityProperties securityProperties,
                          AuditService auditService) {
        this.merchantRepository = merchantRepository;
        this.providerRepository = providerRepository;
        this.transactionRepository = transactionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.securityProperties = securityProperties;
        this.auditService = auditService;
    }

    public List<ProviderResponse> providers(String merchantId) {
        merchantRepository.findById(merchantId)
                .filter(merchant -> "ACTIVE".equals(merchant.getStatus()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVALID_MERCHANT", "Merchant is not active or does not exist"));
        List<ProviderResponse> response = providerRepository.findByStatus(ProviderStatus.ACTIVE).stream()
                .map(provider -> new ProviderResponse(provider.getProviderId(), provider.getProviderName(), provider.getProviderType(), provider.getStatus()))
                .toList();
        auditService.log("GET /providers", merchantId, response, "SUCCESS");
        return response;
    }

    @Transactional
    public CreateTransactionResponse create(CreateTransactionRequest request) {
        validateCreateRequest(request);
        ProviderEntity provider = providerRepository.findById(request.providerId())
                .filter(p -> p.getStatus() == ProviderStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVALID_PROVIDER", "Provider is not active or does not exist"));

        String transactionId = "txn_" + UUID.randomUUID().toString().replace("-", "");
        String requestId = "req_" + UUID.randomUUID().toString().replace("-", "");
        TransactionEntity entity = new TransactionEntity();
        entity.setTransactionId(transactionId);
        entity.setRequestId(requestId);
        entity.setMerchantId(request.merchantId());
        entity.setProviderId(request.providerId());
        entity.setAmount(request.amount());
        entity.setCurrency(request.currency().toUpperCase());
        entity.setStatus(TransactionStatus.CREATED);
        entity.setSubscription(Boolean.TRUE.equals(request.subscription()));
        entity.setDuration(request.duration() == null ? 1 : request.duration());
        entity.setRenewable(Boolean.TRUE.equals(request.isRenewable()));
        transactionRepository.save(entity);

        if (Boolean.TRUE.equals(entity.getSubscription())) {
            SubscriptionEntity subscription = new SubscriptionEntity();
            subscription.setSubscriptionId("sub_" + UUID.randomUUID().toString().replace("-", ""));
            subscription.setTransactionId(transactionId);
            subscription.setRenewalCount(0);
            subscription.setNextRenewalDate(LocalDate.now().plusMonths(entity.getDuration()));
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setCreatedAt(Instant.now());
            subscriptionRepository.save(subscription);
        }

        String providerHash = providerHash(entity, provider);
        CreateTransactionResponse response = new CreateTransactionResponse(
                transactionId,
                requestId,
                "https://checkout.payflowx.local/authorize?providerHash=" + URLEncoder.encode(providerHash, StandardCharsets.UTF_8),
                TransactionStatus.CREATED);
        auditService.log("POST /transactions", request, response, "SUCCESS");
        return response;
    }

    public TransactionStatusResponse status(String transactionId) {
        TransactionEntity entity = findTransaction(transactionId);
        TransactionStatusResponse response = toResponse(entity);
        auditService.log("GET /transactions/{transactionId}", transactionId, response, "SUCCESS");
        return response;
    }

    @Transactional
    public TransactionStatusResponse finalise(String transactionId, FinalizeTransactionRequest request) {
        TransactionEntity entity = findTransaction(transactionId);
        if (entity.getStatus() == TransactionStatus.SUCCESS || entity.getStatus() == TransactionStatus.CHARGED || entity.getStatus() == TransactionStatus.RENEWED) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_FINALIZATION", "Transaction has already been finalized");
        }
        if (entity.getStatus() == TransactionStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "TRANSACTION_CANCELLED", "Cancelled transaction cannot be finalized");
        }
        String expectedReceipt = receipt(entity);
        if (!expectedReceipt.equals(request.receipt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RECEIPT", "Receipt validation failed");
        }
        entity.setStatus(TransactionStatus.SUCCESS);
        entity.setReceiptHash(CryptoUtil.sha256(request.receipt()));
        transactionRepository.save(entity);
        TransactionStatusResponse response = toResponse(entity);
        auditService.log("POST /transactions/{transactionId}/finalise", request, response, "SUCCESS");
        return response;
    }

    @Transactional
    public TransactionStatusResponse charge(String transactionId) {
        TransactionEntity entity = findTransaction(transactionId);
        if (entity.getStatus() != TransactionStatus.SUCCESS && entity.getStatus() != TransactionStatus.RENEWED) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", "Only successful or renewed transactions can be charged");
        }
        entity.setStatus(TransactionStatus.CHARGED);
        transactionRepository.save(entity);
        TransactionStatusResponse response = toResponse(entity);
        auditService.log("POST /transactions/{transactionId}/charge", transactionId, response, "SUCCESS");
        return response;
    }

    @Transactional
    public TransactionStatusResponse renew(String transactionId) {
        TransactionEntity entity = findTransaction(transactionId);
        if (!Boolean.TRUE.equals(entity.getSubscription()) || !Boolean.TRUE.equals(entity.getRenewable())) {
            throw new ApiException(HttpStatus.CONFLICT, "NOT_RENEWABLE", "Transaction is not renewable");
        }
        if (entity.getStatus() != TransactionStatus.CHARGED && entity.getStatus() != TransactionStatus.SUCCESS) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", "Only successful or charged subscriptions can renew");
        }
        SubscriptionEntity subscription = subscriptionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND", "Subscription record not found"));
        subscription.setRenewalCount(subscription.getRenewalCount() + 1);
        subscription.setNextRenewalDate(subscription.getNextRenewalDate().plusMonths(entity.getDuration()));
        subscriptionRepository.save(subscription);
        entity.setStatus(TransactionStatus.RENEWED);
        transactionRepository.save(entity);
        TransactionStatusResponse response = toResponse(entity);
        auditService.log("POST /transactions/{transactionId}/renew", transactionId, response, "SUCCESS");
        return response;
    }

    @Transactional
    public TransactionStatusResponse cancel(String transactionId) {
        TransactionEntity entity = findTransaction(transactionId);
        entity.setStatus(TransactionStatus.CANCELLED);
        transactionRepository.save(entity);
        subscriptionRepository.findByTransactionId(transactionId).ifPresent(subscription -> {
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscriptionRepository.save(subscription);
        });
        TransactionStatusResponse response = toResponse(entity);
        auditService.log("DELETE /transactions/{transactionId}/cancel", transactionId, response, "SUCCESS");
        return response;
    }

    public ValidateTransactionDataResponse validateProviderData(ValidateTransactionDataRequest request) {
        if (!securityProperties.providerPin().equals(request.password())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_PROVIDER_CREDENTIAL", "Provider credential is invalid");
        }
        String decoded = CryptoUtil.base64UrlDecode(request.providerHash());
        String[] parts = decoded.split("\\|");
        if (parts.length != 7) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PROVIDER_HASH", "Provider hash is malformed");
        }
        TransactionEntity transaction = findTransaction(parts[0]);
        ProviderEntity provider = providerRepository.findById(transaction.getProviderId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVALID_PROVIDER", "Provider does not exist"));
        if (!providerHash(transaction, provider).equals(request.providerHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PROVIDER_HASH", "Provider hash signature mismatch");
        }
        transaction.setStatus(TransactionStatus.PROCESSING);
        transactionRepository.save(transaction);
        ValidateTransactionDataResponse response = new ValidateTransactionDataResponse(receipt(transaction), "AUTHORIZED");
        auditService.log("POST /validateTransactionData", request, response, "SUCCESS");
        return response;
    }

    private void validateCreateRequest(CreateTransactionRequest request) {
        merchantRepository.findById(request.merchantId())
                .filter(merchant -> "ACTIVE".equals(merchant.getStatus()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVALID_MERCHANT", "Merchant is not active or does not exist"));
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount must be greater than zero");
        }
        if (!SUPPORTED_CURRENCIES.contains(request.currency().toUpperCase())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURRENCY", "Currency is not supported");
        }
    }

    private TransactionEntity findTransaction(String transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", "Transaction does not exist"));
    }

    private String providerHash(TransactionEntity entity, ProviderEntity provider) {
        String payload = String.join("|",
                entity.getTransactionId(),
                entity.getMerchantId(),
                entity.getProviderId(),
                entity.getAmount().toPlainString(),
                entity.getCurrency(),
                provider.getProviderType());
        String signature = CryptoUtil.hmacSha256(securityProperties.signingSecret(), payload);
        return CryptoUtil.base64UrlEncode(payload + "|" + signature);
    }

    private String receipt(TransactionEntity entity) {
        return "rcpt_" + CryptoUtil.hmacSha256(
                securityProperties.signingSecret(),
                entity.getTransactionId() + "|" + entity.getRequestId() + "|" + entity.getAmount().toPlainString());
    }

    private TransactionStatusResponse toResponse(TransactionEntity entity) {
        return new TransactionStatusResponse(
                entity.getTransactionId(),
                entity.getMerchantId(),
                entity.getProviderId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getSubscription(),
                entity.getRenewable());
    }
}
