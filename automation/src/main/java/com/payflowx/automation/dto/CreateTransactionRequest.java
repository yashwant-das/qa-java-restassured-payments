package com.payflowx.automation.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreateTransactionRequest(
        String itemDescription,
        BigDecimal amount,
        String currency,
        String merchantId,
        String providerId,
        Boolean subscription,
        Integer duration,
        Boolean isRenewable
) {
}
