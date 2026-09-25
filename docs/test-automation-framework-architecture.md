# PayFlowX Test Automation Framework Architecture

This document explains the architecture of the PayFlowX API automation framework in interview-friendly language. Use it to describe what the framework tests, how it is layered, why each component exists, and how a test request flows through the system.

## 1. Framework Purpose

The automation framework validates the PayFlowX payment aggregator backend through API-level tests. It is not limited to checking HTTP status codes. It validates the complete payment lifecycle, security headers, JSON contracts, database state, audit logs, and reporting output.

The framework is built around:

- Java
- Maven
- REST Assured
- TestNG
- AssertJ
- JSON Schema Validator
- HikariCP and MySQL
- Allure reporting
- Docker-based local and CI execution

In an interview, you can describe it as:

> This is a layered API automation framework for a payment aggregator system. It uses REST Assured and TestNG for API execution, dynamic HMAC signing for secured requests, workflow classes for end-to-end business flows, validators for API and database assertions, and Allure for reporting.

## 2. High-Level Architecture

```text
TestNG Tests
    |
    v
Workflow Layer
    |
    v
API Client Layer
    |
    v
REST Assured Request Factory
    |
    +--> Security Header Filter
    +--> Allure Reporting Filter
    |
    v
Spring Boot Backend APIs
    |
    v
MySQL Database
    ^
    |
Database Validators
```

The automation module talks to two external surfaces:

- Backend APIs through signed HTTP requests.
- MySQL database through read-only validation queries.

This gives the tests stronger coverage because they verify both the API response and the persisted backend state.

## 3. Repository Structure

```text
qa-java-restassured-payments
├── backend
│   └── Spring Boot payment API service
├── automation
│   └── REST Assured and TestNG automation framework
├── docker
│   └── MySQL schema, seed data, and cleanup scripts
├── docker-compose.yml
└── README.md
```

The automation framework lives mainly under:

```text
automation/src/main/java/com/payflowx/automation
automation/src/test/java/com/payflowx/automation
automation/src/test/resources
```

## 4. Main Framework Layers

### 4.1 Test Layer

Location:

```text
automation/src/test/java/com/payflowx/automation
```

This layer contains TestNG test classes. The tests are grouped by purpose:

- `smoke`: fast checks such as provider discovery.
- `regression`: negative scenarios and security validation.
- `workflow`: full payment lifecycle validation.

Important classes:

- `ProviderDiscoverySmokeTest`
- `NegativePaymentApiTest`
- `PaymentLifecycleWorkflowTest`
- `BaseApiTest`

`BaseApiTest` runs suite-level setup:

- Configures REST Assured failure logging.
- Performs a database health check using `SELECT 1`.

Interview explanation:

> The test layer stays thin. It focuses on test intent and delegates reusable API operations, workflow steps, and validations to framework classes.

### 4.2 Workflow Layer

Location:

```text
automation/src/main/java/com/payflowx/automation/workflows
```

Main class:

```text
PaymentWorkflow
```

The workflow layer models business journeys instead of individual API calls. For example, the main payment lifecycle test performs:

1. Get active providers for a merchant.
2. Create a transaction.
3. Extract `transactionId`, `requestId`, and `redirectURL`.
4. Parse `providerHash` from the redirect URL.
5. Validate provider authorization data.
6. Extract receipt.
7. Finalise the transaction.
8. Charge the transaction.
9. Renew the subscription.
10. Cancel the transaction.
11. Validate database state and audit logs.

The workflow stores runtime values in `PaymentWorkflowContext`.

Interview explanation:

> The workflow layer improves readability and reuse. Instead of repeating multi-step API logic in tests, the framework has a business workflow object that owns the lifecycle and returns a context object containing generated IDs and important runtime data.

### 4.3 API Client Layer

Location:

```text
automation/src/main/java/com/payflowx/automation/clients
```

Main class:

```text
PaymentApiClient
```

This layer wraps REST Assured calls behind meaningful methods:

- `getProviders`
- `createTransaction`
- `getTransaction`
- `validateTransactionData`
- `finalise`
- `charge`
- `renew`
- `cancel`

The client uses constants from `ApiPaths` so endpoint paths are not duplicated across tests.

Interview explanation:

> The API client layer hides REST Assured syntax from tests. If an endpoint path, request style, or common behavior changes, the update is made in one place.

### 4.4 REST Client Factory

Location:

```text
automation/src/main/java/com/payflowx/automation/clients/RestClientFactory.java
```

`RestClientFactory` creates reusable REST Assured request and response specifications.

It sets:

- Base URI from framework configuration.
- JSON content type.
- JSON accept header.
- Security filter.
- Allure reporting filter.

It also defines common response specs such as:

- `ok()` for HTTP 200.
- `created()` for HTTP 201.

Interview explanation:

> The request factory centralizes REST Assured configuration. This avoids duplicating base URI, content type, filters, and common response expectations in every test.

### 4.5 Security Layer

Locations:

```text
automation/src/main/java/com/payflowx/automation/filters/SecurityHeaderFilter.java
automation/src/main/java/com/payflowx/automation/auth/SignatureGenerator.java
```

The backend requires every request to include:

- `x-access-token`
- `x-dp-timestamp`
- `x-dp-signature`

The framework generates these dynamically for every request through `SecurityHeaderFilter`.

The signature is generated using HMAC-SHA256 from this canonical string:

```text
METHOD
PATH
QUERY_STRING
EPOCH_MILLISECONDS
```

Interview explanation:

> Security is handled as a REST Assured filter, so individual tests do not need to manually add authentication headers. The filter calculates a fresh timestamp and HMAC signature for every request, which also supports replay-prevention testing.

### 4.6 Configuration Layer

Location:

```text
automation/src/main/java/com/payflowx/automation/config/FrameworkConfig.java
automation/src/test/resources/config/local.properties
```

`FrameworkConfig` loads environment-specific properties. By default it uses:

```text
config/local.properties
```

It supports overrides through environment variables and system properties.

Examples:

- `PAYFLOWX_BASE_URI`
- `PAYFLOWX_ACCESS_TOKEN`
- `PAYFLOWX_SIGNING_SECRET`
- `PAYFLOWX_PROVIDER_PIN`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Interview explanation:

> The framework supports environment-based configuration. Local defaults are stored in properties files, while CI and secure environments can override values through environment variables.

### 4.7 Test Data and Payload Builder Layer

Locations:

```text
automation/src/main/java/com/payflowx/automation/builders
automation/src/main/java/com/payflowx/automation/dto
automation/src/test/resources/payloads
automation/src/test/resources/testdata
```

The framework uses DTOs and builders for request payloads. For example, `TransactionPayloadBuilder.renewableSubscription` creates a valid transaction request with:

- Merchant ID
- Provider ID
- Amount
- Currency
- Subscription flag
- Renewal flag
- Dynamic item description using Faker

Interview explanation:

> Payload creation is abstracted through builders and DTOs. This makes tests easier to read and prevents repeated manual JSON construction.

### 4.8 Validator Layer

Location:

```text
automation/src/main/java/com/payflowx/automation/validators
```

Validators are split by responsibility:

- `ResponseValidator`: status code, JSON field, and JSON schema checks.
- `DatabaseValidator`: database state and audit log checks.
- `StateTransitionValidator`: allowed payment status transitions.

Examples of validations:

- Provider response matches JSON schema.
- Create transaction response matches JSON schema.
- Transaction status is persisted as `CREATED`, `PROCESSING`, `SUCCESS`, `CHARGED`, `RENEWED`, or `CANCELLED`.
- Subscription renewal count is updated.
- Audit log entries exist for important APIs.

Interview explanation:

> The framework uses separate validators so assertions are reusable and consistent. API validation, database validation, and business state transition validation are kept independent.

### 4.9 Database Layer

Location:

```text
automation/src/main/java/com/payflowx/automation/db/DatabaseManager.java
```

`DatabaseManager` uses HikariCP for connection pooling. It provides reusable methods:

- `queryOne`
- `update`

The database validator uses this layer to verify backend persistence.

Interview explanation:

> API tests validate more than responses. The framework queries MySQL directly to confirm that API actions produced the expected database state, which is important for payment workflows.

### 4.10 Reporting Layer

Locations:

```text
automation/src/main/java/com/payflowx/automation/filters/AllureRestAssuredFilter.java
automation/src/test/resources/testng-suite.xml
```

Allure integration is implemented through:

- `AllureTestNg` listener in TestNG suite XML.
- `@Epic`, `@Feature`, `@Owner`, and `@Severity` annotations.
- Custom REST Assured filter attaching API request and response details.
- Database validator attachments for SQL validation output.

Interview explanation:

> The reporting layer captures API request and response details, elapsed time, test metadata, and database validation evidence. This helps debugging and makes reports useful for stakeholders.

## 5. End-to-End Request Flow

This is the flow when a test calls an API:

1. Test method calls a workflow or API client method.
2. API client creates a REST Assured request using `RestClientFactory.requestSpec()`.
3. `SecurityHeaderFilter` adds token, timestamp, and HMAC signature.
4. `AllureRestAssuredFilter` captures request and response details.
5. Request is sent to the Spring Boot backend.
6. Backend validates security headers and processes payment logic.
7. Backend writes transaction, subscription, and audit data to MySQL.
8. Test validates HTTP response using `ResponseValidator`.
9. Test validates database state using `DatabaseValidator`.
10. Allure report contains steps, API evidence, and DB validation evidence.

## 6. TestNG Execution Model

Suite file:

```text
automation/src/test/resources/testng-suite.xml
```

The suite runs:

- `smoke`
- `workflow`
- `regression`

It is configured with:

```text
parallel="methods"
thread-count="4"
```

This means TestNG can run test methods in parallel. The framework supports this by avoiding shared mutable test data where possible and using thread-safe patterns such as `ThreadLocal<Faker>`.

Interview explanation:

> TestNG groups allow selective execution, and parallel method execution improves feedback time. The framework design avoids putting runtime transaction data in static shared variables.

## 7. Main Scenarios Covered

### Smoke Coverage

Provider discovery:

- Calls `GET /providers`.
- Verifies response status is 200.
- Verifies at least one provider is returned.
- Verifies returned provider is active.

### Workflow Coverage

Full distributed payment lifecycle:

- Provider discovery.
- Transaction creation.
- Redirect URL parsing.
- Provider authorization.
- Transaction finalization.
- Charge.
- Renew.
- Cancel.
- Database and audit validation.

### Negative Coverage

Examples:

- Invalid merchant.
- Invalid provider.
- Invalid amount.
- Invalid currency.
- Invalid receipt.
- Duplicate finalization.
- Invalid token.
- Expired timestamp.
- Invalid signature.

## 8. Why This Architecture Is Useful

The architecture is useful because it separates responsibilities clearly:

| Concern | Component |
|---|---|
| Test intent | TestNG test classes |
| Business flow | Workflow classes |
| API communication | API client classes |
| Common REST setup | RestClientFactory |
| Security headers | SecurityHeaderFilter |
| Payload creation | Builders and DTOs |
| API assertions | ResponseValidator |
| DB assertions | DatabaseValidator |
| Business rules | StateTransitionValidator |
| Config management | FrameworkConfig |
| Reporting | Allure filters and annotations |

This makes the framework easier to maintain. For example:

- If the signature algorithm changes, update `SignatureGenerator`.
- If a header changes, update `SecurityHeaderFilter`.
- If an endpoint path changes, update `ApiPaths` or `PaymentApiClient`.
- If response contracts change, update JSON schema files.
- If database checks change, update `DatabaseValidator`.

## 9. Interview Talking Points

Use these points when explaining the framework:

- The framework uses a layered design to keep tests readable and reusable.
- REST Assured handles API execution, while TestNG handles test organization, grouping, and parallel execution.
- Security is implemented through a REST Assured filter that dynamically signs every request.
- Workflows model business journeys, not just endpoint-level checks.
- DTOs and builders are used for clean payload creation.
- JSON schema validation protects API contracts.
- Direct database validation verifies persisted backend state.
- Allure reports include API requests, responses, timings, and DB validation evidence.
- Config is environment-driven and can be overridden in CI.
- Negative tests cover both business validation and API security failures.

## 10. Sample Interview Answer

If asked, "Can you explain your automation framework architecture?", you can answer:

> The framework is a Java-based REST Assured and TestNG API automation framework for a payment aggregator backend. It follows a layered architecture. Test classes contain only the test intent, workflow classes model complete business flows, API client classes wrap REST Assured calls, and a request factory centralizes base URI, JSON settings, security, and reporting filters. Every API request is signed dynamically using an HMAC-SHA256 security filter. The framework validates HTTP responses, JSON schemas, database state, payment state transitions, subscriptions, and audit logs. Configuration is environment-driven, and reports are generated through Allure with request, response, and SQL validation attachments.

## 11. How To Run

Start services with Docker:

```bash
docker compose up --build mysql backend
```

Run automation through Docker:

```bash
docker compose --profile test up --build --abort-on-container-exit tests
```

Run automation locally:

```bash
mvn -pl automation test
```

Generate Allure report:

```bash
mvn -pl automation allure:report
```

Serve Allure report:

```bash
mvn -pl automation allure:serve
```

## 12. Files To Remember For Interview

```text
automation/src/test/java/com/payflowx/automation/workflows/PaymentLifecycleWorkflowTest.java
automation/src/main/java/com/payflowx/automation/workflows/PaymentWorkflow.java
automation/src/main/java/com/payflowx/automation/clients/PaymentApiClient.java
automation/src/main/java/com/payflowx/automation/clients/RestClientFactory.java
automation/src/main/java/com/payflowx/automation/filters/SecurityHeaderFilter.java
automation/src/main/java/com/payflowx/automation/auth/SignatureGenerator.java
automation/src/main/java/com/payflowx/automation/validators/ResponseValidator.java
automation/src/main/java/com/payflowx/automation/validators/DatabaseValidator.java
automation/src/main/java/com/payflowx/automation/db/DatabaseManager.java
automation/src/test/resources/testng-suite.xml
```

## 13. Strengths To Highlight

- Clear separation of test, client, workflow, validation, config, and reporting layers.
- Realistic secured API testing with dynamic HMAC signing.
- End-to-end workflow coverage instead of isolated CRUD tests.
- API response validation combined with database reconciliation.
- TestNG grouping and parallel execution support.
- Allure reporting with strong debugging evidence.
- Docker and CI-friendly execution model.

## 14. Possible Improvements To Mention

If an interviewer asks how you would improve it, good answers are:

- Add data cleanup hooks per test to make parallel execution even more isolated.
- Add retry or polling utilities for eventually consistent backend operations.
- Add contract tests for all endpoints using JSON schemas.
- Add more environment profiles such as `qa`, `stage`, and `ci`.
- Add test data factory support for more payment scenarios.
- Mask sensitive headers in Allure attachments.
- Add tagging strategy for smoke, sanity, regression, security, and database tests.

