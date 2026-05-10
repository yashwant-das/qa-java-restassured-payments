package com.payflowx.backend.dto;

import com.payflowx.backend.enums.TransactionStatus;

public record CreateTransactionResponse(
        String transactionId,
        String requestId,
        String redirectURL,
        TransactionStatus status
) {
}
