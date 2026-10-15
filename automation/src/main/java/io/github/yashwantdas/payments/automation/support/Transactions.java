package io.github.yashwantdas.payments.automation.support;

import io.github.yashwantdas.payments.automation.api.PaymentApi;
import io.github.yashwantdas.payments.automation.api.model.ValidateRequest;
import io.github.yashwantdas.payments.automation.config.TestConfig;
import io.qameta.allure.Step;
import io.restassured.response.Response;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Setup shared by tests that need a transaction in a given state. */
public final class Transactions {
    private Transactions() {
    }

    /** A transaction the provider has authorised, ready to finalise with {@code receipt}. */
    public record Authorised(String transactionId, String receipt) {
    }

    @Step("Create a transaction and authorise it with the provider")
    public static Authorised createAndAuthorise(PaymentApi api) {
        Response created = api.createTransaction(TestData.renewableSubscription(TestData.PROVIDER)).then()
                .statusCode(201)
                .extract().response();
        String receipt = api.validateProviderData(
                        new ValidateRequest(providerHash(created.path("redirectURL")), TestConfig.providerPin()))
                .then().statusCode(200)
                .extract().path("receipt");
        return new Authorised(created.path("transactionId"), receipt);
    }

    /** Extracts the {@code providerHash} query parameter from the checkout redirect URL. */
    public static String providerHash(String redirectUrl) {
        String query = URI.create(redirectUrl).getRawQuery();
        return Arrays.stream(query == null ? new String[0] : query.split("&"))
                .map(pair -> pair.split("=", 2))
                .filter(parts -> parts.length == 2 && parts[0].equals("providerHash"))
                .map(parts -> URLDecoder.decode(parts[1], StandardCharsets.UTF_8))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("providerHash missing from " + redirectUrl));
    }
}
