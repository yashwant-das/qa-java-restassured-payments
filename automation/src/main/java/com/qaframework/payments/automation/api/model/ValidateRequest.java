package com.qaframework.payments.automation.api.model;

/** Body of {@code POST /validateTransactionData}: the provider hash from the redirect URL and the provider PIN. */
public record ValidateRequest(String providerHash, String password) {
}
