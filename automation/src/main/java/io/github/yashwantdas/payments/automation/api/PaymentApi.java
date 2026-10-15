package io.github.yashwantdas.payments.automation.api;

import io.github.yashwantdas.payments.automation.api.model.CreateTransactionRequest;
import io.github.yashwantdas.payments.automation.api.model.FinaliseRequest;
import io.github.yashwantdas.payments.automation.api.model.ValidateRequest;
import io.github.yashwantdas.payments.automation.config.TestConfig;
import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/**
 * One method per endpoint of the payment API. Requests are signed by {@link SigningFilter} and
 * attached to the Allure report. Methods return the raw response so tests make their own assertions.
 */
public class PaymentApi {
    private static final String TRANSACTION = "/transactions/{transactionId}";

    /** Base spec for normal calls. SigningFilter runs before AllureRestAssured so attachments show the signed headers. */
    public static RequestSpecification signed() {
        return base().addFilter(new SigningFilter()).addFilter(new AllureRestAssured()).build();
    }

    /** Base spec without auth headers, for security tests that set them by hand. */
    public static RequestSpecification unsigned() {
        return base().addFilter(new AllureRestAssured()).build();
    }

    private static RequestSpecBuilder base() {
        return new RequestSpecBuilder()
                .setBaseUri(TestConfig.baseUri())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON);
    }

    @Step("Get active providers for merchant {merchantId}")
    public Response getProviders(String merchantId) {
        return given(signed()).queryParam("merchantId", merchantId).get("/providers");
    }

    @Step("Create transaction")
    public Response createTransaction(CreateTransactionRequest request) {
        return given(signed()).body(request).post("/transactions");
    }

    @Step("Get transaction {transactionId}")
    public Response getTransaction(String transactionId) {
        return given(signed()).get(TRANSACTION, transactionId);
    }

    @Step("Validate provider authorisation data")
    public Response validateProviderData(ValidateRequest request) {
        return given(signed()).body(request).post("/validateTransactionData");
    }

    @Step("Finalise transaction {transactionId}")
    public Response finalise(String transactionId, String receipt) {
        return given(signed()).body(new FinaliseRequest(receipt)).post(TRANSACTION + "/finalise", transactionId);
    }

    @Step("Charge transaction {transactionId}")
    public Response charge(String transactionId) {
        return given(signed()).post(TRANSACTION + "/charge", transactionId);
    }

    @Step("Renew transaction {transactionId}")
    public Response renew(String transactionId) {
        return given(signed()).post(TRANSACTION + "/renew", transactionId);
    }

    @Step("Cancel transaction {transactionId}")
    public Response cancel(String transactionId) {
        return given(signed()).delete(TRANSACTION + "/cancel", transactionId);
    }
}
