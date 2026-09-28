# API and test coverage

What the PayFlowX backend exposes and what the automation suite checks against it.

## Payment APIs

All APIs require:

- `x-access-token`
- `x-dp-signature`
- `x-dp-timestamp`

Implemented endpoints:

- `GET /providers?merchantId=...`
- `POST /transactions`
- `GET /transactions/{transactionId}`
- `POST /transactions/{transactionId}/finalise`
- `POST /transactions/{transactionId}/charge`
- `POST /transactions/{transactionId}/renew`
- `DELETE /transactions/{transactionId}/cancel`
- `POST /validateTransactionData`

Transaction statuses:

`CREATED`, `PENDING`, `PROCESSING`, `SUCCESS`, `FAILED`, `CANCELLED`, `CHARGED`, `RENEWED`

## Security model

Requests are signed using:

```text
METHOD + "\n" + PATH + "\n" + QUERY_STRING + "\n" + EPOCH_MILLISECONDS
```

The backend validates token, HMAC-SHA256 signature, timestamp skew, and replayed signatures. The automation framework injects these headers dynamically through `SecurityHeaderFilter`.

## End-to-end workflow coverage

The primary workflow test executes:

1. Get providers
2. Create transaction
3. Extract `transactionId`
4. Extract `redirectURL`
5. Extract encoded `providerHash`
6. Validate provider data
7. Get receipt
8. Finalise transaction
9. Verify `SUCCESS`
10. Charge transaction
11. Renew subscription
12. Cancel transaction
13. Verify final DB state, subscription state, audit logs, and timestamps

Main workflow test:

`automation/src/test/java/com/payflowx/automation/workflows/PaymentLifecycleWorkflowTest.java`

## Negative coverage

Implemented negative scenarios include:

- invalid merchant
- invalid provider
- invalid amount
- invalid currency
- invalid token
- invalid signature
- expired timestamp
- duplicate finalisation (enforced by the backend)
- invalid receipt (enforced by the backend)

## Database

Tables:

- `merchants`
- `providers`
- `transactions`
- `subscriptions`
- `audit_logs`

SQL scripts:

- `docker/mysql/schema.sql`
- `docker/mysql/seed-data.sql`
- `docker/mysql/cleanup.sql`
- `backend/src/main/resources/db/schema.sql`
- `backend/src/main/resources/db/seed-data.sql`

## Local test credentials

Local-only defaults for the Docker and CI environments; never reuse them elsewhere. Override them through environment variables.

```text
PAYFLOWX_ACCESS_TOKEN=payflowx-enterprise-token
PAYFLOWX_SIGNING_SECRET=payflowx-super-secret-signing-key
PAYFLOWX_PROVIDER_PIN=4321
```

## Framework packages (automation module)

- `clients`: REST Assured API clients and reusable specifications
- `filters`: security header injection, logging, Allure attachments
- `auth`: HMAC signing
- `builders`: dynamic payment test data
- `db`: HikariCP connection management
- `validators`: response, schema, state transition, and DB validators
- `workflows`: business process orchestration
- `tests`, `smoke`, `regression`, `workflows`: categorized TestNG coverage
