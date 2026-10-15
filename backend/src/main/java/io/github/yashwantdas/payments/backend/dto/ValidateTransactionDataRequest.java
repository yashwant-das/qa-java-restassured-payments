package io.github.yashwantdas.payments.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateTransactionDataRequest(@NotBlank String providerHash, @NotBlank String password) {
}
