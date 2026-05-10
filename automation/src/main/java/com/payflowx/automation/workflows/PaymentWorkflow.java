package com.payflowx.automation.workflows;

import com.payflowx.automation.builders.TransactionPayloadBuilder;
import com.payflowx.automation.clients.PaymentApiClient;
import com.payflowx.automation.config.FrameworkConfig;
import com.payflowx.automation.dto.CreateTransactionRequest;
import com.payflowx.automation.dto.ValidateTransactionDataRequest;
import com.payflowx.automation.models.PaymentWorkflowContext;
import com.payflowx.automation.utilities.RedirectUrlParser;
import com.payflowx.automation.validators.DatabaseValidator;
import com.payflowx.automation.validators.ResponseValidator;
import com.payflowx.automation.validators.StateTransitionValidator;
import io.qameta.allure.Step;
import io.restassured.response.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentWorkflow {
    private final PaymentApiClient client = new PaymentApiClient();
    private final DatabaseValidator databaseValidator = new DatabaseValidator();

    @Step("Execute full distributed payment lifecycle")
    public PaymentWorkflowContext executeSuccessfulRenewableSubscription(String merchantId) {
        PaymentWorkflowContext context = new PaymentWorkflowContext();
        context.setMerchantId(merchantId);

        Response providers = client.getProviders(merchantId);
        ResponseValidator.assertStatus(providers, 200);
        ResponseValidator.assertSchema(providers, "schemas/providers.schema.json");
        String providerId = providers.jsonPath().getString("[0].providerId");
        context.setProviderId(providerId);

        CreateTransactionRequest createRequest = TransactionPayloadBuilder.renewableSubscription(merchantId, providerId);
        Response create = client.createTransaction(createRequest);
        ResponseValidator.assertStatus(create, 201);
        ResponseValidator.assertSchema(create, "schemas/create-transaction.schema.json");
        context.setTransactionId(create.jsonPath().getString("transactionId"));
        context.setRequestId(create.jsonPath().getString("requestId"));
        context.setRedirectUrl(create.jsonPath().getString("redirectURL"));
        context.setProviderHash(RedirectUrlParser.providerHash(context.getRedirectUrl()));
        assertThat(context.getRedirectUrl()).contains("providerHash=");
        databaseValidator.assertTransactionStatus(context.getTransactionId(), "CREATED");

        Response providerAuth = client.validateTransactionData(new ValidateTransactionDataRequest(
                context.getProviderHash(),
                FrameworkConfig.get().providerPin()));
        ResponseValidator.assertStatus(providerAuth, 200);
        context.setReceipt(providerAuth.jsonPath().getString("receipt"));
        databaseValidator.assertTransactionStatus(context.getTransactionId(), "PROCESSING");

        StateTransitionValidator.assertAllowed("PROCESSING", "SUCCESS");
        Response finalise = client.finalise(context.getTransactionId(), context.getReceipt());
        ResponseValidator.assertStatus(finalise, 200);
        ResponseValidator.assertField(finalise, "status", "SUCCESS");
        databaseValidator.assertTransactionStatus(context.getTransactionId(), "SUCCESS");

        StateTransitionValidator.assertAllowed("SUCCESS", "CHARGED");
        Response charge = client.charge(context.getTransactionId());
        ResponseValidator.assertStatus(charge, 200);
        ResponseValidator.assertField(charge, "status", "CHARGED");
        databaseValidator.assertTransactionStatus(context.getTransactionId(), "CHARGED");

        StateTransitionValidator.assertAllowed("CHARGED", "RENEWED");
        Response renew = client.renew(context.getTransactionId());
        ResponseValidator.assertStatus(renew, 200);
        ResponseValidator.assertField(renew, "status", "RENEWED");
        databaseValidator.assertTransactionStatus(context.getTransactionId(), "RENEWED");

        StateTransitionValidator.assertAllowed("RENEWED", "CANCELLED");
        Response cancel = client.cancel(context.getTransactionId());
        ResponseValidator.assertStatus(cancel, 200);
        ResponseValidator.assertField(cancel, "status", "CANCELLED");
        databaseValidator.assertTransactionStatus(context.getTransactionId(), "CANCELLED");
        databaseValidator.assertSubscriptionRenewedAndCancelled(context.getTransactionId());
        databaseValidator.assertAuditLogExists("POST /transactions");
        databaseValidator.assertAuditLogExists("POST /validateTransactionData");
        return context;
    }
}
