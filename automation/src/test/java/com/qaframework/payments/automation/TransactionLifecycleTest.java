package com.qaframework.payments.automation;

import com.qaframework.payments.automation.api.PaymentApi;
import com.qaframework.payments.automation.api.model.CreateTransactionRequest;
import com.qaframework.payments.automation.api.model.ValidateRequest;
import com.qaframework.payments.automation.config.TestConfig;
import com.qaframework.payments.automation.db.DbAssertions;
import com.qaframework.payments.automation.support.TestData;
import com.qaframework.payments.automation.support.Transactions;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@Epic("Payments API")
@Feature("Transaction lifecycle")
public class TransactionLifecycleTest extends BaseApiTest {
    private final PaymentApi api = new PaymentApi();

    @Test(groups = {"smoke", "regression"})
    @Severity(SeverityLevel.CRITICAL)
    public void renewableSubscriptionCompletesFullLifecycle() {
        // 1. Discover a provider
        String providerId = api.getProviders(TestData.MERCHANT).then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/providers.schema.json"))
                .extract().path("[0].providerId");

        // 2. Create the transaction
        CreateTransactionRequest request = TestData.renewableSubscription(providerId);
        Response created = api.createTransaction(request).then()
                .statusCode(201)
                .body(matchesJsonSchemaInClasspath("schemas/create-transaction.schema.json"))
                .body("redirectURL", containsString("providerHash="))
                .extract().response();
        String transactionId = created.path("transactionId");
        String requestId = created.path("requestId");
        DbAssertions.transactionStatus(transactionId, "CREATED");
        DbAssertions.transactionMatches(transactionId, requestId, request);
        DbAssertions.auditLogged("POST /transactions", transactionId);

        // 3. Provider authorises it
        String providerHash = Transactions.providerHash(created.path("redirectURL"));
        String receipt = api.validateProviderData(new ValidateRequest(providerHash, TestConfig.providerPin())).then()
                .statusCode(200)
                .extract().path("receipt");
        DbAssertions.transactionStatus(transactionId, "PROCESSING");
        DbAssertions.auditLogged("POST /validateTransactionData", receipt);

        // 4. Finalise
        api.finalise(transactionId, receipt).then().statusCode(200).body("status", equalTo("SUCCESS"));
        DbAssertions.transactionStatus(transactionId, "SUCCESS");
        DbAssertions.auditLogged("POST /transactions/{transactionId}/finalise", transactionId);
        api.getTransaction(transactionId).then().statusCode(200).body("status", equalTo("SUCCESS"));

        // 5. Charge
        api.charge(transactionId).then().statusCode(200).body("status", equalTo("CHARGED"));
        DbAssertions.transactionStatus(transactionId, "CHARGED");
        DbAssertions.auditLogged("POST /transactions/{transactionId}/charge", transactionId);

        // 6. Renew
        api.renew(transactionId).then().statusCode(200).body("status", equalTo("RENEWED"));
        DbAssertions.transactionStatus(transactionId, "RENEWED");
        DbAssertions.auditLogged("POST /transactions/{transactionId}/renew", transactionId);

        // 7. Cancel
        api.cancel(transactionId).then().statusCode(200).body("status", equalTo("CANCELLED"));
        DbAssertions.transactionStatus(transactionId, "CANCELLED");
        DbAssertions.auditLogged("DELETE /transactions/{transactionId}/cancel", transactionId);

        // 8. Final state
        DbAssertions.subscriptionRenewedAndCancelled(transactionId);
        assertThat(transactionId).startsWith("txn_");
        assertThat(requestId).startsWith("req_");
        assertThat(receipt).startsWith("rcpt_");
    }
}
