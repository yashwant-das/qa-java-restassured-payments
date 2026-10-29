package com.qaframework.payments.automation.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.qaframework.payments.automation.config.TestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shared MySQL connection pool and a single-row query helper for database checks. */
public final class Database {
    private static final Logger log = LoggerFactory.getLogger(Database.class);
    private static final HikariDataSource DATA_SOURCE = createDataSource();

    private Database() {
    }

    /** Runs a parameterised query and returns its first row (column label to value), or an empty map. */
    public static Map<String, Object> queryOne(String sql, Object... params) {
        log.info("sql={} params={}", sql, Arrays.toString(params));
        try (Connection connection = DATA_SOURCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Map.of();
                }
                ResultSetMetaData meta = resultSet.getMetaData();
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    row.put(meta.getColumnLabel(i), resultSet.getObject(i));
                }
                return row;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Query failed: " + sql, ex);
        }
    }

    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(TestConfig.dbUrl());
        config.setUsername(TestConfig.dbUsername());
        config.setPassword(TestConfig.dbPassword());
        config.setMaximumPoolSize(4);
        config.setPoolName("automation-db");
        return new HikariDataSource(config);
    }
}
