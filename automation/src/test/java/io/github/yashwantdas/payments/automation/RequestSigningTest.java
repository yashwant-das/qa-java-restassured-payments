package io.github.yashwantdas.payments.automation;

import io.github.yashwantdas.payments.automation.api.PaymentApi;
import io.github.yashwantdas.payments.automation.api.SigningFilter;
import io.github.yashwantdas.payments.automation.config.TestConfig;
import io.github.yashwantdas.payments.automation.support.TestData;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/** Sends hand-built auth headers to {@code GET /providers} and checks each rejection. */
@Epic("Payments API")
@Feature("Request signing")
public class RequestSigningTest extends BaseApiTest {

    @Test(groups = "regression")
    public void wrongApiKeyIsRejected() {
        getProviders(Map.of("X-Api-Key", "wrong-key", "X-Timestamp", now(), "X-Signature", "unused")).then()
                .statusCode(401)
                .body("code", equalTo("INVALID_TOKEN"));
    }

    @Test(groups = "regression")
    public void missingSignatureHeadersAreRejected() {
        getProviders(Map.of("X-Api-Key", TestConfig.apiKey())).then()
                .statusCode(401)
                .body("code", equalTo("MISSING_SECURITY_HEADERS"));
    }

    @Test(groups = "regression")
    public void nonNumericTimestampIsRejected() {
        getProviders(Map.of("X-Api-Key", TestConfig.apiKey(), "X-Timestamp", "yesterday", "X-Signature", "unused")).then()
                .statusCode(401)
                .body("code", equalTo("INVALID_TIMESTAMP"));
    }

    @Test(groups = "regression")
    public void expiredTimestampIsRejected() {
        String hourAgo = Long.toString(Instant.now().minusSeconds(3600).toEpochMilli());
        getProviders(Map.of("X-Api-Key", TestConfig.apiKey(), "X-Timestamp", hourAgo, "X-Signature", "unused")).then()
                .statusCode(401)
                .body("code", equalTo("EXPIRED_TIMESTAMP"));
    }

    @Test(groups = "regression")
    public void wrongSignatureIsRejected() {
        getProviders(Map.of("X-Api-Key", TestConfig.apiKey(), "X-Timestamp", now(), "X-Signature", "wrong-signature")).then()
                .statusCode(401)
                .body("code", equalTo("INVALID_SIGNATURE"));
    }

    @Test(groups = "regression")
    public void replayedRequestIsRejected() {
        String timestamp = SigningFilter.nextTimestamp();
        String signature = SigningFilter.sign("GET", "/providers", "merchantId=" + TestData.MERCHANT, timestamp);
        Map<String, String> headers = Map.of(
                "X-Api-Key", TestConfig.apiKey(), "X-Timestamp", timestamp, "X-Signature", signature);

        getProviders(headers).then().statusCode(200);
        getProviders(headers).then()
                .statusCode(409)
                .body("code", equalTo("REPLAY_ATTACK_DETECTED"));
    }

    private static Response getProviders(Map<String, String> headers) {
        return given(PaymentApi.unsigned())
                .headers(headers)
                .queryParam("merchantId", TestData.MERCHANT)
                .get("/providers");
    }

    private static String now() {
        return Long.toString(Instant.now().toEpochMilli());
    }
}
