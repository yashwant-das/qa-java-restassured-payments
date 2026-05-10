package com.payflowx.automation.clients;

import com.payflowx.automation.config.FrameworkConfig;
import com.payflowx.automation.filters.AllureRestAssuredFilter;
import com.payflowx.automation.filters.SecurityHeaderFilter;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

public final class RestClientFactory {
    private RestClientFactory() {
    }

    public static RequestSpecification requestSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(FrameworkConfig.get().baseUri())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new SecurityHeaderFilter())
                .addFilter(new AllureRestAssuredFilter())
                .build();
    }

    public static ResponseSpecification ok() {
        return new ResponseSpecBuilder().expectStatusCode(200).build();
    }

    public static ResponseSpecification created() {
        return new ResponseSpecBuilder().expectStatusCode(201).build();
    }

    public static void configure() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}
