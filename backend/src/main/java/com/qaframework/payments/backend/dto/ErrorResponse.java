package com.qaframework.payments.backend.dto;

import java.time.Instant;

public record ErrorResponse(String code, String message, Instant timestamp) {
}
