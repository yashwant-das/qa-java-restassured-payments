package com.qaframework.payments.automation.api.model;

/** Body of {@code POST /transactions/{id}/finalise}. */
public record FinaliseRequest(String receipt) {
}
