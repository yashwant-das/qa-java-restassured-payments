package com.payflowx.automation.filters;

import com.payflowx.automation.auth.SignatureGenerator;
import com.payflowx.automation.config.FrameworkConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.net.URI;
import java.time.Instant;

public class SecurityHeaderFilter implements Filter {
    private final FrameworkConfig config = FrameworkConfig.get();

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {
        URI uri = URI.create(requestSpec.getURI());
        String timestamp = Long.toString(Instant.now().toEpochMilli());
        String signature = SignatureGenerator.sign(
                config.signingSecret(),
                requestSpec.getMethod(),
                uri.getPath(),
                uri.getQuery(),
                timestamp);
        requestSpec.header("x-access-token", config.accessToken());
        requestSpec.header("x-dp-timestamp", timestamp);
        requestSpec.header("x-dp-signature", signature);
        return ctx.next(requestSpec, responseSpec);
    }
}
