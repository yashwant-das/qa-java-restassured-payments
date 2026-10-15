package io.github.yashwantdas.payments.automation.api;

import io.github.yashwantdas.payments.automation.config.TestConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Signs every request the way the API requires: {@code X-Api-Key}, an epoch-millisecond {@code X-Timestamp}
 * and an HMAC-SHA256 {@code X-Signature} over {@code METHOD\nPATH\nQUERY\nTIMESTAMP}. Also logs each call's timing.
 */
public final class SigningFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(SigningFilter.class);

    // The API rejects a repeated timestamp+signature as a replay, and parallel tests can sign in the
    // same millisecond, so timestamps are kept strictly increasing across threads.
    private static final AtomicLong LAST_TIMESTAMP = new AtomicLong();

    @Override
    public Response filter(FilterableRequestSpecification request,
                           FilterableResponseSpecification response,
                           FilterContext ctx) {
        URI uri = URI.create(request.getURI());
        String timestamp = nextTimestamp();
        request.header("X-Api-Key", TestConfig.apiKey());
        request.header("X-Timestamp", timestamp);
        request.header("X-Signature", sign(request.getMethod(), uri.getRawPath(), uri.getRawQuery(), timestamp));

        long started = System.nanoTime();
        Response result = ctx.next(request, response);
        log.info("{} {} -> {} in {} ms", request.getMethod(), uri, result.statusCode(),
                (System.nanoTime() - started) / 1_000_000);
        return result;
    }

    /** A timestamp later than every one handed out before, so no two signed requests share one. */
    public static String nextTimestamp() {
        return Long.toString(LAST_TIMESTAMP.updateAndGet(last -> Math.max(last + 1, Instant.now().toEpochMilli())));
    }

    /** HMAC-SHA256 signature, hex encoded, for the given request parts. {@code query} may be null. */
    public static String sign(String method, String path, String query, String timestamp) {
        String canonical = method + "\n" + path + "\n" + (query == null ? "" : query) + "\n" + timestamp;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(TestConfig.signingSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Cannot sign request", ex);
        }
    }
}
