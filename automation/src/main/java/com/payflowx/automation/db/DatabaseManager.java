package com.payflowx.automation.db;

import com.payflowx.automation.config.FrameworkConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DatabaseManager {
    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static final HikariDataSource DATA_SOURCE = createDataSource();

    private DatabaseManager() {
    }

    public static Map<String, Object> queryOne(String sql, Object... params) {
        try (Connection connection = DATA_SOURCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            log.info("sql_query sql={} params={}", sql, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Map.of();
                }
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                    row.put(resultSet.getMetaData().getColumnLabel(i), resultSet.getObject(i));
                }
                return row;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Database query failed: " + sql, ex);
        }
    }

    public static int update(String sql, Object... params) {
        try (Connection connection = DATA_SOURCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            log.info("sql_update sql={} params={}", sql, params);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Database update failed: " + sql, ex);
        }
    }

    private static void bind(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }

    private static HikariDataSource createDataSource() {
        FrameworkConfig config = FrameworkConfig.get();
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.dbUrl());
        hikariConfig.setUsername(config.dbUsername());
        hikariConfig.setPassword(config.dbPassword());
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setPoolName("PayFlowXAutomationPool");
        return new HikariDataSource(hikariConfig);
    }
}
