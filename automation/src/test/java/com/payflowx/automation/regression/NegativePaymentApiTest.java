package com.payflowx.automation.regression;

import com.payflowx.automation.builders.TransactionPayloadBuilder;
import com.payflowx.automation.clients.PaymentApiClient;
import com.payflowx.automation.clients.RestClientFactory;
import com.payflowx.automation.config.FrameworkConfig;
import com.payflowx.automation.dto.CreateTransactionRequest;
import com.payflowx.automation.dto.ValidateTransactionDataRequest;
import com.payflowx.automation.models.PaymentWorkflowContext;
import com.payflowx.automation.tests.BaseApiTest;
import com.payflowx.automation.utilities.RedirectUrlParser;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Payment Orchestration")
@Feature("Negative API and Security Validation")
public class NegativePaymentApiTest extends BaseApiTest {
    private final PaymentApiClient client = new PaymentApiClient();

    @Test(groups = "regression")
    public void shouldRejectInvalidMerchant() {
        Response response = client.getProviders("mrc_missing");
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.jsonPath().getString("code")).isEqualTo("INVALID_MERCHANT");
    }

    @Test(groups = "regression")
    public void shouldRejectInvalidProvider() {
        CreateTransactionRequest request = TransactionPayloadBuilder.renewableSubscription("mrc_enterprise_001", "prv_missing");
        Response response = client.createTransaction(request);
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.jsonPath().getString("code")).isEqualTo("INVALID_PROVIDER");
    }

    @Test(groups = "regression")
    public void shouldRejectInvalidAmount() {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .itemDescription("Invalid amount scenario")
                .amount(BigDecimal.ZERO)
                .currency("USD")
                .merchantId("mrc_enterprise_001")
                .providerId("prv_stripe_us")
                .subscription(false)
                .duration(1)
                .isRenewable(false)
                .build();
        Response response = client.createTransaction(request);
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test(groups = "regression")
    public void shouldRejectInvalidCurrency() {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .itemDescription("Invalid currency scenario")
                .amount(BigDecimal.TEN)
                .currency("BTC")
                .merchantId("mrc_enterprise_001")
                .providerId("prv_stripe_us")
                .subscription(false)
                .duration(1)
                .isRenewable(false)
                .build();
        Response response = client.createTransaction(request);
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("code")).isEqualTo("INVALID_CURRENCY");
    }

    @Test(groups = "regression")
    public void shouldRejectInvalidReceipt() {
        PaymentWorkflowContext context = createAuthorizedTransaction();
        Response response = client.finalise(context.getTransactionId(), "rcpt_invalid");
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("code")).isEqualTo("INVALID_RECEIPT");
    }

    @Test(groups = "regression")
    public void shouldRejectDuplicateFinalization() {
        PaymentWorkflowContext context = createAuthorizedTransaction();
        Response firstFinalise = client.finalise(context.getTransactionId(), context.getReceipt());
        assertThat(firstFinalise.statusCode()).isEqualTo(200);

        Response duplicate = client.finalise(context.getTransactionId(), context.getReceipt());
        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(duplicate.jsonPath().getString("code")).isEqualTo("DUPLICATE_FINALIZATION");
    }

    @Test(groups = "regression")
    public void shouldRejectInvalidToken() {
        Response response = given()
                .baseUri(com.payflowx.automation.config.FrameworkConfig.get().baseUri())
                .header("x-access-token", "bad-token")
                .header("x-dp-timestamp", Long.toString(Instant.now().toEpochMilli()))
                .header("x-dp-signature", "bad-signature")
                .queryParam("merchantId", "mrc_enterprise_001")
                .get("/providers");
        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test(groups = "regression")
    public void shouldRejectExpiredTimestamp() {
        String expired = Long.toString(Instant.now().minusSeconds(3600).toEpochMilli());
        Response response = given()
                .baseUri(com.payflowx.automation.config.FrameworkConfig.get().baseUri())
                .header("x-access-token", com.payflowx.automation.config.FrameworkConfig.get().accessToken())
                .header("x-dp-timestamp", expired)
                .header("x-dp-signature", "expired")
                .queryParam("merchantId", "mrc_enterprise_001")
                .get("/providers");
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.jsonPath().getString("code")).isEqualTo("EXPIRED_TIMESTAMP");
    }

    @Test(groups = "regression")
    public void shouldRejectInvalidSignature() {
        String now = Long.toString(Instant.now().toEpochMilli());
        Response response = given()
                .baseUri(com.payflowx.automation.config.FrameworkConfig.get().baseUri())
                .header("x-access-token", com.payflowx.automation.config.FrameworkConfig.get().accessToken())
                .header("x-dp-timestamp", now)
                .header("x-dp-signature", "invalid-signature")
                .queryParam("merchantId", "mrc_enterprise_001")
                .get("/providers");
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.jsonPath().getString("code")).isEqualTo("INVALID_SIGNATURE");
    }

    private PaymentWorkflowContext createAuthorizedTransaction() {
        PaymentWorkflowContext context = new PaymentWorkflowContext();
        Response providers = client.getProviders("mrc_enterprise_001");
        String providerId = providers.jsonPath().getString("[0].providerId");
        Response create = client.createTransaction(TransactionPayloadBuilder.renewableSubscription("mrc_enterprise_001", providerId));
        context.setTransactionId(create.jsonPath().getString("transactionId"));
        context.setRedirectUrl(create.jsonPath().getString("redirectURL"));
        context.setProviderHash(RedirectUrlParser.providerHash(context.getRedirectUrl()));
        Response providerAuth = client.validateTransactionData(new ValidateTransactionDataRequest(
                context.getProviderHash(),
                FrameworkConfig.get().providerPin()));
        context.setReceipt(providerAuth.jsonPath().getString("receipt"));
        return context;
    }
}
