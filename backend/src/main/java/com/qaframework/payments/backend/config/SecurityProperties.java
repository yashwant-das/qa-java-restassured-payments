package com.qaframework.payments.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payments.security")
public record SecurityProperties(
        String apiKey,
        String signingSecret,
        long timestampSkewSeconds,
        String providerPin
) {
}
