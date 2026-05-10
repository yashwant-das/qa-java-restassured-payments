package com.payflowx.automation.validators;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public final class StateTransitionValidator {
    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "CREATED", Set.of("PROCESSING", "CANCELLED"),
            "PROCESSING", Set.of("SUCCESS", "FAILED", "CANCELLED"),
            "SUCCESS", Set.of("CHARGED", "RENEWED", "CANCELLED"),
            "CHARGED", Set.of("RENEWED", "CANCELLED"),
            "RENEWED", Set.of("CHARGED", "CANCELLED")
    );

    private StateTransitionValidator() {
    }

    public static void assertAllowed(String from, String to) {
        assertThat(ALLOWED.getOrDefault(from, Set.of())).contains(to);
    }
}
