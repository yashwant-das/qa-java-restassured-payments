package com.qaframework.payments.automation.support;

import com.qaframework.payments.automation.api.model.CreateTransactionRequest;

import java.math.BigDecimal;

/** Seeded IDs (see backend/src/main/resources/db/seed-data.sql) and request factories. */
public final class TestData {
    public static final String MERCHANT = "merchant_001";
    public static final String PROVIDER = "provider_card_us";
    public static final String UNKNOWN_MERCHANT = "merchant_missing";
    public static final String UNKNOWN_PROVIDER = "provider_missing";

    private TestData() {
    }

    /** A one-month renewable subscription for {@link #MERCHANT}. */
    public static CreateTransactionRequest renewableSubscription(String providerId) {
        return new CreateTransactionRequest(
                "Monthly plan", new BigDecimal("49.99"), "USD", MERCHANT, providerId, true, 1, true);
    }

    /** A single payment for {@link #MERCHANT} through {@link #PROVIDER}. */
    public static CreateTransactionRequest oneOffPayment(BigDecimal amount, String currency) {
        return new CreateTransactionRequest(
                "One-off payment", amount, currency, MERCHANT, PROVIDER, false, 1, false);
    }
}
