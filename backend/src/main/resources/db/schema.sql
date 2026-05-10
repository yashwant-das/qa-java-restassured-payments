DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS subscriptions;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS providers;
DROP TABLE IF EXISTS merchants;

CREATE TABLE merchants (
    merchant_id VARCHAR(64) PRIMARY KEY,
    merchant_name VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE providers (
    provider_id VARCHAR(64) PRIMARY KEY,
    provider_name VARCHAR(160) NOT NULL,
    provider_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL
);

CREATE TABLE transactions (
    transaction_id VARCHAR(96) PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL,
    provider_id VARCHAR(64) NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    status VARCHAR(32) NOT NULL,
    subscription BOOLEAN NOT NULL,
    duration INT NOT NULL,
    is_renewable BOOLEAN NOT NULL,
    receipt_hash VARCHAR(128),
    request_id VARCHAR(96),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transactions_merchants FOREIGN KEY (merchant_id) REFERENCES merchants(merchant_id),
    CONSTRAINT fk_transactions_providers FOREIGN KEY (provider_id) REFERENCES providers(provider_id)
);

CREATE TABLE subscriptions (
    subscription_id VARCHAR(96) PRIMARY KEY,
    transaction_id VARCHAR(96) NOT NULL,
    renewal_count INT NOT NULL,
    next_renewal_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_subscriptions_transactions FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id)
);

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_payload TEXT,
    response_payload TEXT,
    api_name VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
