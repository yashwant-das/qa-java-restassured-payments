package com.qaframework.payments.backend.dto;

import com.qaframework.payments.backend.enums.ProviderStatus;

public record ProviderResponse(String providerId, String providerName, String providerType, ProviderStatus status) {
}
