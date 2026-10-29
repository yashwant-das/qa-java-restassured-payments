package com.qaframework.payments.backend.dto;

import com.qaframework.payments.backend.enums.TransactionStatus;

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
