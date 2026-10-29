package com.qaframework.payments.backend.dto;

public record ValidateTransactionDataResponse(String receipt, String authorizationStatus) {
}
