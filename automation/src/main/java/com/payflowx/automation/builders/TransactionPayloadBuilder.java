package com.payflowx.automation.builders;

import com.github.javafaker.Faker;
import com.payflowx.automation.dto.CreateTransactionRequest;

import java.math.BigDecimal;

public final class TransactionPayloadBuilder {
    private static final ThreadLocal<Faker> FAKER = ThreadLocal.withInitial(Faker::new);

    private TransactionPayloadBuilder() {
    }

    public static CreateTransactionRequest renewableSubscription(String merchantId, String providerId) {
        return CreateTransactionRequest.builder()
                .itemDescription("Subscription for " + FAKER.get().commerce().productName())
                .amount(BigDecimal.valueOf(49.99))
                .currency("USD")
                .merchantId(merchantId)
                .providerId(providerId)
                .subscription(true)
                .duration(1)
                .isRenewable(true)
                .build();
    }
}
