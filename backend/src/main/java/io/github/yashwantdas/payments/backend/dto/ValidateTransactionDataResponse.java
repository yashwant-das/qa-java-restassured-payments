package io.github.yashwantdas.payments.backend.dto;

public record ValidateTransactionDataResponse(String receipt, String authorizationStatus) {
}
