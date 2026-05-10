package com.payflowx.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record FinalizeTransactionRequest(@NotBlank String receipt) {
}
