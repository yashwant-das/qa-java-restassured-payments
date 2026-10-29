package com.qaframework.payments.automation;

import com.qaframework.payments.automation.api.PaymentApi;
import com.qaframework.payments.automation.support.TestData;
import com.qaframework.payments.automation.support.Transactions;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.equalTo;

@Epic("Payments API")
@Feature("Business-rule validation")
public class TransactionValidationTest extends BaseApiTest {
    private final PaymentApi api = new PaymentApi();

    @Test(groups = "regression")
    public void unknownMerchantIsRejected() {
        api.getProviders(TestData.UNKNOWN_MERCHANT).then()
                .statusCode(404)
                .body("code", equalTo("INVALID_MERCHANT"));
    }

    @Test(groups = "regression")
    public void unknownProviderIsRejected() {
        api.createTransaction(TestData.renewableSubscription(TestData.UNKNOWN_PROVIDER)).then()
                .statusCode(404)
                .body("code", equalTo("INVALID_PROVIDER"));
    }

    @Test(groups = "regression")
    public void zeroAmountIsRejected() {
        api.createTransaction(TestData.oneOffPayment(BigDecimal.ZERO, "USD")).then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test(groups = "regression")
    public void unsupportedCurrencyIsRejected() {
        api.createTransaction(TestData.oneOffPayment(BigDecimal.TEN, "BTC")).then()
                .statusCode(400)
                .body("code", equalTo("INVALID_CURRENCY"));
    }

    @Test(groups = "regression")
    public void invalidReceiptIsRejected() {
        Transactions.Authorised txn = Transactions.createAndAuthorise(api);
        api.finalise(txn.transactionId(), "rcpt_invalid").then()
                .statusCode(400)
                .body("code", equalTo("INVALID_RECEIPT"));
    }

    @Test(groups = "regression")
    public void secondFinalisationIsRejected() {
        Transactions.Authorised txn = Transactions.createAndAuthorise(api);
        api.finalise(txn.transactionId(), txn.receipt()).then().statusCode(200);
        api.finalise(txn.transactionId(), txn.receipt()).then()
                .statusCode(409)
                .body("code", equalTo("DUPLICATE_FINALIZATION"));
    }
}
