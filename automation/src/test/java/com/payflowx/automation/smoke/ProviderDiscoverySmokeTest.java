package com.payflowx.automation.smoke;

import com.payflowx.automation.clients.PaymentApiClient;
import com.payflowx.automation.tests.BaseApiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Payment Orchestration")
@Feature("Provider Discovery")
public class ProviderDiscoverySmokeTest extends BaseApiTest {
    @Test(groups = "smoke")
    public void shouldReturnActiveProvidersForMerchant() {
        var response = new PaymentApiClient().getProviders("mrc_enterprise_001");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("$")).isNotEmpty();
        assertThat(response.jsonPath().getString("[0].status")).isEqualTo("ACTIVE");
    }
}
