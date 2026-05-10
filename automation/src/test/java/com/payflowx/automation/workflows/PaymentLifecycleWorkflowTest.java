package com.payflowx.automation.workflows;

import com.payflowx.automation.models.PaymentWorkflowContext;
import com.payflowx.automation.tests.BaseApiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Payment Orchestration")
@Feature("Distributed Transaction Lifecycle")
@Owner("PayFlowX SDET Platform")
public class PaymentLifecycleWorkflowTest extends BaseApiTest {
    @Test(groups = {"smoke", "regression", "workflow"})
    @Severity(SeverityLevel.CRITICAL)
    public void shouldCompleteProviderRedirectFinaliseChargeRenewCancelWorkflow() {
        PaymentWorkflowContext context = new PaymentWorkflow()
                .executeSuccessfulRenewableSubscription("mrc_enterprise_001");

        assertThat(context.getTransactionId()).startsWith("txn_");
        assertThat(context.getRequestId()).startsWith("req_");
        assertThat(context.getReceipt()).startsWith("rcpt_");
    }
}
