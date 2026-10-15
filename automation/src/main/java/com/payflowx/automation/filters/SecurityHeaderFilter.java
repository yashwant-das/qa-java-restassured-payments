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
import java.util.concurrent.atomic.AtomicLong;

public class SecurityHeaderFilter implements Filter {
    // The backend rejects a repeated timestamp+signature as a replay, and parallel tests can sign in the same millisecond.
    private static final AtomicLong LAST_TIMESTAMP = new AtomicLong();

    private final FrameworkConfig config = FrameworkConfig.get();

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {
        URI uri = URI.create(requestSpec.getURI());
        String timestamp = Long.toString(
                LAST_TIMESTAMP.updateAndGet(last -> Math.max(last + 1, Instant.now().toEpochMilli())));
        String signature = SignatureGenerator.sign(
                config.signingSecret(),
                requestSpec.getMethod(),
                uri.getPath(),
                uri.getQuery(),
                timestamp);
        requestSpec.header("X-Api-Key", config.accessToken());
        requestSpec.header("X-Timestamp", timestamp);
        requestSpec.header("X-Signature", signature);
        return ctx.next(requestSpec, responseSpec);
    }
}
