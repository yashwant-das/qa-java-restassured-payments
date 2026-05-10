package com.payflowx.automation.filters;

import io.qameta.allure.Allure;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class AllureRestAssuredFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(AllureRestAssuredFilter.class);

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {
        long started = System.nanoTime();
        Response response = ctx.next(requestSpec, responseSpec);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;
        String request = requestSpec.getMethod() + " " + requestSpec.getURI() + "\nHeaders: " + requestSpec.getHeaders() + "\nBody: " + requestSpec.getBody();
        String responseText = "Status: " + response.statusCode() + "\nElapsedMs: " + elapsedMs + "\nBody: " + response.asPrettyString();
        log.info("api_call method={} uri={} status={} elapsedMs={}", requestSpec.getMethod(), requestSpec.getURI(), response.statusCode(), elapsedMs);
        Allure.addAttachment("API Request", "text/plain", request, StandardCharsets.UTF_8.name());
        Allure.addAttachment("API Response", "application/json", responseText, StandardCharsets.UTF_8.name());
        return response;
    }
}
