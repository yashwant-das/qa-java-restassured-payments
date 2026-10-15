package io.github.yashwantdas.payments.backend.controller;

import io.github.yashwantdas.payments.backend.dto.CreateTransactionRequest;
import io.github.yashwantdas.payments.backend.dto.CreateTransactionResponse;
import io.github.yashwantdas.payments.backend.dto.FinalizeTransactionRequest;
import io.github.yashwantdas.payments.backend.dto.ProviderResponse;
import io.github.yashwantdas.payments.backend.dto.TransactionStatusResponse;
import io.github.yashwantdas.payments.backend.dto.ValidateTransactionDataRequest;
import io.github.yashwantdas.payments.backend.dto.ValidateTransactionDataResponse;
import io.github.yashwantdas.payments.backend.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/providers")
    public List<ProviderResponse> providers(@RequestParam("merchantId") String merchantId) {
        return paymentService.providers(merchantId);
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateTransactionResponse create(@Valid @RequestBody CreateTransactionRequest request) {
        return paymentService.create(request);
    }

    @GetMapping("/transactions/{transactionId}")
    public TransactionStatusResponse status(@PathVariable("transactionId") String transactionId) {
        return paymentService.status(transactionId);
    }

    @PostMapping("/transactions/{transactionId}/finalise")
    public TransactionStatusResponse finalise(@PathVariable("transactionId") String transactionId, @Valid @RequestBody FinalizeTransactionRequest request) {
        return paymentService.finalise(transactionId, request);
    }

    @PostMapping("/transactions/{transactionId}/charge")
    public TransactionStatusResponse charge(@PathVariable("transactionId") String transactionId) {
        return paymentService.charge(transactionId);
    }

    @PostMapping("/transactions/{transactionId}/renew")
    public TransactionStatusResponse renew(@PathVariable("transactionId") String transactionId) {
        return paymentService.renew(transactionId);
    }

    @DeleteMapping("/transactions/{transactionId}/cancel")
    public TransactionStatusResponse cancel(@PathVariable("transactionId") String transactionId) {
        return paymentService.cancel(transactionId);
    }

    @PostMapping("/validateTransactionData")
    public ValidateTransactionDataResponse validateTransactionData(@Valid @RequestBody ValidateTransactionDataRequest request) {
        return paymentService.validateProviderData(request);
    }
}
