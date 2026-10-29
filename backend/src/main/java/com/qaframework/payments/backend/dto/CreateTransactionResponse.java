package com.qaframework.payments.backend.dto;

import com.qaframework.payments.backend.enums.TransactionStatus;

public record CreateTransactionResponse(
        String transactionId,
        String requestId,
        String redirectURL,
        TransactionStatus status
) {
}
