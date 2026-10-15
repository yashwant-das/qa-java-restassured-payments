package io.github.yashwantdas.payments.backend.dto;

import io.github.yashwantdas.payments.backend.enums.TransactionStatus;

import java.math.BigDecimal;

public record TransactionStatusResponse(
        String transactionId,
        String merchantId,
        String providerId,
        BigDecimal amount,
        String currency,
        TransactionStatus status,
        Boolean subscription,
        Boolean isRenewable
) {
}
