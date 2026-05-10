package com.payflowx.backend.dto;

import com.payflowx.backend.enums.TransactionStatus;

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
