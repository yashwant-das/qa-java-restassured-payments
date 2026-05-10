package com.payflowx.automation.clients;

import com.payflowx.automation.constants.ApiPaths;
import com.payflowx.automation.dto.CreateTransactionRequest;
import com.payflowx.automation.dto.FinalizeTransactionRequest;
import com.payflowx.automation.dto.ValidateTransactionDataRequest;
import io.qameta.allure.Step;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class PaymentApiClient {
    @Step("Get active providers for merchant {merchantId}")
    public Response getProviders(String merchantId) {
        return given(RestClientFactory.requestSpec())
                .queryParam("merchantId", merchantId)
                .get(ApiPaths.PROVIDERS);
    }

    @Step("Create payment transaction")
    public Response createTransaction(CreateTransactionRequest request) {
        return given(RestClientFactory.requestSpec())
                .body(request)
                .post(ApiPaths.TRANSACTIONS);
    }

    @Step("Get transaction status {transactionId}")
    public Response getTransaction(String transactionId) {
        return given(RestClientFactory.requestSpec())
                .get(ApiPaths.TRANSACTIONS + "/{transactionId}", transactionId);
    }

    @Step("Validate provider authorization data")
    public Response validateTransactionData(ValidateTransactionDataRequest request) {
        return given(RestClientFactory.requestSpec())
                .body(request)
                .post(ApiPaths.VALIDATE_TRANSACTION_DATA);
    }

    @Step("Finalise transaction {transactionId}")
    public Response finalise(String transactionId, String receipt) {
        return given(RestClientFactory.requestSpec())
                .body(new FinalizeTransactionRequest(receipt))
                .post(ApiPaths.TRANSACTIONS + "/{transactionId}/finalise", transactionId);
    }

    @Step("Charge transaction {transactionId}")
    public Response charge(String transactionId) {
        return given(RestClientFactory.requestSpec())
                .post(ApiPaths.TRANSACTIONS + "/{transactionId}/charge", transactionId);
    }

    @Step("Renew transaction {transactionId}")
    public Response renew(String transactionId) {
        return given(RestClientFactory.requestSpec())
                .post(ApiPaths.TRANSACTIONS + "/{transactionId}/renew", transactionId);
    }

    @Step("Cancel transaction {transactionId}")
    public Response cancel(String transactionId) {
        return given(RestClientFactory.requestSpec())
                .delete(ApiPaths.TRANSACTIONS + "/{transactionId}/cancel", transactionId);
    }
}
