package com.payflowx.automation.validators;

import com.payflowx.automation.db.DatabaseManager;
import io.qameta.allure.Allure;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseValidator {
    public void assertTransactionStatus(String transactionId, String expectedStatus) {
        Map<String, Object> row = DatabaseManager.queryOne(
                "SELECT transaction_id, status, created_at, updated_at FROM transactions WHERE transaction_id = ?",
                transactionId);
        attach("Transaction DB State", row);
        assertThat(row).containsEntry("status", expectedStatus);
        assertThat(row.get("created_at")).isNotNull();
        assertThat(row.get("updated_at")).isNotNull();
    }

    public void assertSubscriptionRenewedAndCancelled(String transactionId) {
        Map<String, Object> row = DatabaseManager.queryOne(
                "SELECT transaction_id, renewal_count, next_renewal_date, status FROM subscriptions WHERE transaction_id = ?",
                transactionId);
        attach("Subscription DB State", row);
        assertThat(((Number) row.get("renewal_count")).intValue()).isGreaterThanOrEqualTo(1);
        assertThat(row).containsEntry("status", "CANCELLED");
        assertThat(row.get("next_renewal_date")).isNotNull();
    }

    public void assertAuditLogExists(String apiName) {
        Map<String, Object> row = DatabaseManager.queryOne(
                "SELECT COUNT(*) AS total FROM audit_logs WHERE api_name = ? AND status = 'SUCCESS'",
                apiName);
        attach("Audit Log Validation", row);
        assertThat(((Number) row.get("total")).longValue()).isPositive();
    }

    private void attach(String name, Map<String, Object> row) {
        Allure.addAttachment(name, "text/plain", row.toString(), StandardCharsets.UTF_8.name());
    }
}
