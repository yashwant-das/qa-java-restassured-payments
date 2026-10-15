package io.github.yashwantdas.payments.backend.dto;

import io.github.yashwantdas.payments.backend.enums.ProviderStatus;

public record ProviderResponse(String providerId, String providerName, String providerType, ProviderStatus status) {
}
