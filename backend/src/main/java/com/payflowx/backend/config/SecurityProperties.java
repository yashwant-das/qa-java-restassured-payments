package com.payflowx.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payflowx.security")
public record SecurityProperties(
        String accessToken,
        String signingSecret,
        long timestampSkewSeconds,
        String providerPin
) {
}
