package com.qaframework.payments.automation.db;

import com.qaframework.payments.automation.api.model.CreateTransactionRequest;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Database checks used by the tests. Each check reads one row, attaches the query and result
 * to the Allure report, and asserts on it. Every check is keyed by a transaction ID or receipt,
 * so checks stay correct when tests run in parallel or the database holds earlier runs.
 */
public final class DbAssertions {
    private DbAssertions() {
    }

    @Step("DB: transaction {transactionId} is {expectedStatus}")
    public static void transactionStatus(String transactionId, String expectedStatus) {
        Map<String, Object> row = read(
                "SELECT status, created_at, updated_at FROM transactions WHERE transaction_id = ?", transactionId);
        assertThat(row).as("transactions row for %s", transactionId).containsEntry("status", expectedStatus);
        assertThat(row.get("created_at")).as("created_at").isNotNull();
        assertThat(row.get("updated_at")).as("updated_at").isNotNull();
    }

    @Step("DB: transaction {transactionId} matches the create request")
    public static void transactionMatches(String transactionId, String requestId, CreateTransactionRequest request) {
        Map<String, Object> row = read(
                "SELECT merchant_id, provider_id, amount, currency, subscription, duration, is_renewable, request_id"
                        + " FROM transactions WHERE transaction_id = ?",
                transactionId);
        assertThat(row).as("transactions row for %s", transactionId)
                .containsEntry("merchant_id", request.merchantId())
                .containsEntry("provider_id", request.providerId())
                .containsEntry("currency", request.currency())
                .containsEntry("request_id", requestId);
        assertThat((BigDecimal) row.get("amount")).isEqualByComparingTo(request.amount());
        assertThat(((Number) row.get("duration")).intValue()).isEqualTo(request.duration());
        assertThat(asBoolean(row.get("subscription"))).isEqualTo(request.subscription());
        assertThat(asBoolean(row.get("is_renewable"))).isEqualTo(request.isRenewable());
    }

    @Step("DB: subscription for {transactionId} was renewed and cancelled")
    public static void subscriptionRenewedAndCancelled(String transactionId) {
        Map<String, Object> row = read(
                "SELECT renewal_count, next_renewal_date, status FROM subscriptions WHERE transaction_id = ?",
                transactionId);
        assertThat(row).as("subscriptions row for %s", transactionId).containsEntry("status", "CANCELLED");
        assertThat(((Number) row.get("renewal_count")).intValue()).isGreaterThanOrEqualTo(1);
        assertThat(row.get("next_renewal_date")).isNotNull();
    }

    /** Asserts a SUCCESS audit entry exists for {@code apiName} whose response payload contains {@code payloadValue}. */
    @Step("DB: audit log has a successful {apiName} entry for {payloadValue}")
    public static void auditLogged(String apiName, String payloadValue) {
        Map<String, Object> row = read(
                "SELECT COUNT(*) AS total FROM audit_logs"
                        + " WHERE api_name = ? AND status = 'SUCCESS' AND INSTR(response_payload, ?) > 0",
                apiName, payloadValue);
        assertThat(((Number) row.get("total")).longValue())
                .as("audit entries for %s containing %s", apiName, payloadValue)
                .isPositive();
    }

    private static Map<String, Object> read(String sql, Object... params) {
        Map<String, Object> row = Database.queryOne(sql, params);
        Allure.addAttachment("SQL", "text/plain",
                sql + "\nparams: " + Arrays.toString(params) + "\nresult: " + row, ".txt");
        return row;
    }

    private static boolean asBoolean(Object value) {
        return value instanceof Boolean flag ? flag : ((Number) value).intValue() != 0;
    }
}
