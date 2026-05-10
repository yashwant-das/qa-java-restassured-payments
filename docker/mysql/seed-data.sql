INSERT INTO merchants (merchant_id, merchant_name, status, created_at) VALUES
('mrc_enterprise_001', 'Acme Digital Commerce', 'ACTIVE', CURRENT_TIMESTAMP),
('mrc_telco_001', 'Northstar Telecom Billing', 'ACTIVE', CURRENT_TIMESTAMP),
('mrc_inactive_001', 'Inactive Merchant Ltd', 'INACTIVE', CURRENT_TIMESTAMP);

INSERT INTO providers (provider_id, provider_name, provider_type, status) VALUES
('prv_stripe_us', 'Stripe US Gateway', 'CARD', 'ACTIVE'),
('prv_adyen_eu', 'Adyen EU Acquiring', 'CARD', 'ACTIVE'),
('prv_razorpay_in', 'Razorpay India UPI', 'UPI', 'ACTIVE'),
('prv_paypal_global', 'PayPal Global Wallet', 'WALLET', 'ACTIVE'),
('prv_legacy_disabled', 'Legacy Disabled Gateway', 'CARD', 'INACTIVE');
