package io.github.yashwantdas.payments.backend.dto;

import io.github.yashwantdas.payments.backend.enums.TransactionStatus;

public record CreateTransactionResponse(
        String transactionId,
        String requestId,
        String redirectURL,
        TransactionStatus status
) {
}
