package com.payflowx.automation.validators;

import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;

import static org.assertj.core.api.Assertions.assertThat;

public final class ResponseValidator {
    private ResponseValidator() {
    }

    public static void assertStatus(Response response, int expected) {
        assertThat(response.statusCode()).isEqualTo(expected);
    }

    public static void assertField(Response response, String path, Object expected) {
        Object actual = response.jsonPath().get(path);
        assertThat(actual).isEqualTo(expected);
    }

    public static void assertSchema(Response response, String schemaPath) {
        response.then().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(schemaPath));
    }
}
