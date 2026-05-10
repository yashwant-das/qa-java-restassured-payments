package com.payflowx.backend.dto;

import com.payflowx.backend.enums.ProviderStatus;

public record ProviderResponse(String providerId, String providerName, String providerType, ProviderStatus status) {
}
