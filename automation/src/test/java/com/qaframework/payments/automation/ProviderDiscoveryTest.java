package com.qaframework.payments.automation;

import com.qaframework.payments.automation.api.PaymentApi;
import com.qaframework.payments.automation.support.TestData;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

@Epic("Payments API")
@Feature("Provider discovery")
public class ProviderDiscoveryTest extends BaseApiTest {
    private final PaymentApi api = new PaymentApi();

    @Test(groups = {"smoke", "regression"})
    public void activeMerchantSeesActiveProviders() {
        api.getProviders(TestData.MERCHANT).then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/providers.schema.json"))
                .body("size()", greaterThan(0))
                .body("[0].status", equalTo("ACTIVE"));
    }
}
