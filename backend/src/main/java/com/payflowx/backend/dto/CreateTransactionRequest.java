package com.payflowx.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTransactionRequest(
        @NotBlank String itemDescription,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String merchantId,
        @NotBlank String providerId,
        Boolean subscription,
        Integer duration,
        Boolean isRenewable
) {
}
