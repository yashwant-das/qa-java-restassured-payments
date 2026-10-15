package io.github.yashwantdas.payments.automation.api.model;

import java.math.BigDecimal;

/** Body of {@code POST /transactions}. */
public record CreateTransactionRequest(
        String itemDescription,
        BigDecimal amount,
        String currency,
        String merchantId,
        String providerId,
        Boolean subscription,
        Integer duration,
        Boolean isRenewable
) {
}
