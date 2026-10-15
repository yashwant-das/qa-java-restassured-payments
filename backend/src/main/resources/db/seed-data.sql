INSERT INTO merchants (merchant_id, merchant_name, status, created_at) VALUES
('merchant_001', 'Demo Store', 'ACTIVE', CURRENT_TIMESTAMP),
('merchant_002', 'Demo Subscriptions', 'ACTIVE', CURRENT_TIMESTAMP),
('merchant_inactive', 'Inactive Merchant', 'INACTIVE', CURRENT_TIMESTAMP);

INSERT INTO providers (provider_id, provider_name, provider_type, status) VALUES
('provider_card_us', 'Card Gateway US', 'CARD', 'ACTIVE'),
('provider_card_eu', 'Card Gateway EU', 'CARD', 'ACTIVE'),
('provider_upi_in', 'UPI Gateway India', 'UPI', 'ACTIVE'),
('provider_wallet', 'Digital Wallet', 'WALLET', 'ACTIVE'),
('provider_disabled', 'Disabled Gateway', 'CARD', 'INACTIVE');
