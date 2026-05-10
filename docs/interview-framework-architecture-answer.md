# PayFlowX Automation Framework Architecture - Interview Answer

Use this document when an interviewer asks:

> Can you explain your automation framework architecture?

## 1. Strong Opening Answer

My framework is a Java-based API automation framework built for a payment aggregator backend. It uses REST Assured for API automation, TestNG for test execution and grouping, AssertJ for assertions, JSON schema validation for contract testing, HikariCP with MySQL for database validation, and Allure for reporting.

The framework follows a layered architecture. The test layer contains only the test intent, the workflow layer handles end-to-end business flows, the API client layer wraps REST Assured calls, the request factory centralizes common REST configuration, filters handle security and reporting, validators handle API and database assertions, and configuration is environment-driven.

The key strength is that the framework does not only validate API responses. It validates the complete payment lifecycle, HMAC-secured API calls, JSON contracts, database state, subscription state, and audit logs.

## 2. Architecture Diagram

```text
TestNG Test Classes
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
        |       - access token
        |       - timestamp
        |       - HMAC signature
        |
        +--> Allure Reporting Filter
        |       - request
        |       - response
        |       - elapsed time
        |
        v
Spring Boot Payment APIs
        |
        v
MySQL Database
        ^
        |
Database Validator
```

## 3. How I Explain The Layers

### Test Layer

The test layer is written using TestNG. Test classes are kept thin and readable. They do not contain repeated REST Assured code.

Examples:

- `ProviderDiscoverySmokeTest`
- `PaymentLifecycleWorkflowTest`
- `NegativePaymentApiTest`
- `BaseApiTest`

`BaseApiTest` performs suite-level setup. It configures REST Assured logging and checks database connectivity before the suite starts.

What I would say:

> My test classes focus on business intent. For example, the workflow test says that a payment lifecycle should complete successfully, but the detailed API steps are delegated to reusable workflow and client classes.

### Workflow Layer

The workflow layer represents complete business journeys. In this framework, `PaymentWorkflow` handles the full payment lifecycle:

1. Get active providers.
2. Create a transaction.
3. Extract transaction ID and redirect URL.
4. Parse provider hash from the redirect URL.
5. Validate provider authorization data.
6. Get receipt.
7. Finalise the transaction.
8. Charge the transaction.
9. Renew the subscription.
10. Cancel the transaction.
11. Validate database state and audit logs.

Runtime data is stored in `PaymentWorkflowContext`.

What I would say:

> I created a workflow layer because payment testing is not just one API call. It is a sequence of dependent business actions. The workflow object owns that sequence and returns a context object containing generated values like transaction ID, request ID, provider hash, and receipt.

### API Client Layer

The API client layer wraps REST Assured calls into reusable methods.

Main class:

```text
PaymentApiClient
```

It has methods such as:

- `getProviders`
- `createTransaction`
- `validateTransactionData`
- `finalise`
- `charge`
- `renew`
- `cancel`

What I would say:

> The API client layer hides raw REST Assured syntax from the tests. If an endpoint path or request format changes, I update the client class instead of changing every test.

### REST Client Factory

`RestClientFactory` centralizes REST Assured setup.

It configures:

- Base URI
- JSON content type
- JSON accept header
- Security filter
- Allure reporting filter
- Common response specifications

What I would say:

> I use a REST client factory so common request configuration is defined once. Every API call automatically gets the same base URI, content type, security handling, and reporting behavior.

### Security Layer

The backend APIs are protected using custom headers:

- `x-access-token`
- `x-dp-timestamp`
- `x-dp-signature`

The framework adds these headers through `SecurityHeaderFilter`.

The signature is generated using HMAC-SHA256:

```text
METHOD + PATH + QUERY_STRING + TIMESTAMP
```

What I would say:

> Security is implemented as a REST Assured filter. That means tests do not manually add headers. Every request is automatically signed with a fresh timestamp and HMAC signature. This makes the framework realistic for secured enterprise APIs.

### Configuration Layer

Configuration is handled by `FrameworkConfig`.

It loads values from environment-specific property files and also supports environment variable overrides.

Examples:

- `PAYFLOWX_BASE_URI`
- `PAYFLOWX_ACCESS_TOKEN`
- `PAYFLOWX_SIGNING_SECRET`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

What I would say:

> The framework is environment-ready. Local values come from properties files, but CI or staging environments can override them using environment variables.

### Test Data Layer

The framework uses DTOs and builder classes instead of hardcoding JSON everywhere.

Example:

```text
TransactionPayloadBuilder
```

It creates valid transaction payloads using merchant ID, provider ID, amount, currency, subscription flag, and renewal flag.

What I would say:

> I use builders and DTOs for test data so payload creation is reusable, type-safe, and easy to modify. Tests do not need to manually build large JSON strings.

### Validation Layer

Validation is separated into focused classes:

| Validator | Responsibility |
|---|---|
| `ResponseValidator` | Status code, JSON field, and JSON schema checks |
| `DatabaseValidator` | Transaction, subscription, and audit log validation |
| `StateTransitionValidator` | Valid payment status movement |

What I would say:

> I separated validations so assertions are reusable and consistent. API response validation, database validation, and business rule validation are not mixed directly inside test methods.

### Database Layer

The framework uses `DatabaseManager` with HikariCP connection pooling.

It supports database validation through reusable query methods.

The framework validates:

- Transaction status
- Subscription renewal state
- Cancellation state
- Audit log entries
- Created and updated timestamps

What I would say:

> Since this is a payment system, response validation alone is not enough. I also validate MySQL state directly to confirm that the backend persisted the correct transaction, subscription, and audit data.

### Reporting Layer

Reporting is handled by Allure.

The report includes:

- Test metadata
- Epic and feature names
- Severity
- API request details
- API response body
- Response time
- Database validation attachments

What I would say:

> Allure reports are useful for debugging because every failed test includes the request, response, elapsed time, and database validation evidence.

## 4. Complete Request Flow

When a test executes, this is what happens:

1. TestNG starts the test.
2. Test calls a workflow or API client method.
3. API client builds a REST Assured request from `RestClientFactory`.
4. Security filter adds token, timestamp, and HMAC signature.
5. Allure filter captures request and response details.
6. Request is sent to the Spring Boot backend.
7. Backend validates security and processes payment logic.
8. Backend writes to MySQL.
9. Test validates API response.
10. Test validates database state.
11. Allure report stores execution evidence.

## 5. Main Scenario I Would Highlight

The strongest scenario in the framework is the full payment lifecycle:

```text
Provider Discovery
    -> Create Transaction
    -> Redirect Authorization
    -> Validate Provider Data
    -> Finalise Transaction
    -> Charge
    -> Renew Subscription
    -> Cancel
    -> Verify DB State
    -> Verify Audit Logs
```

What I would say:

> This is the best example of framework maturity because it validates a real business journey instead of only isolated API endpoints.

## 6. Short 60-Second Interview Answer

> My automation framework is built using Java, REST Assured, TestNG, AssertJ, MySQL, and Allure. It follows a layered architecture. Test classes contain the test intent, workflow classes handle end-to-end payment flows, API client classes wrap REST Assured calls, and a REST client factory centralizes base URI, JSON configuration, filters, and response specs.
>
> Security is handled through a REST Assured filter that dynamically adds access token, timestamp, and HMAC-SHA256 signature headers to every request. For validation, I use response validators for status codes, JSON fields, and schemas; database validators for transaction, subscription, and audit log checks; and state transition validators for business rules.
>
> The strongest part of the framework is that it validates the full payment lifecycle, from provider discovery to transaction creation, authorization, finalization, charge, renewal, cancellation, database reconciliation, and audit logging. All execution evidence is attached to Allure reports.

## 7. Detailed 2-Minute Interview Answer

> The framework is designed as a reusable API automation framework for a payment aggregator backend. It uses REST Assured for API calls and TestNG for test execution, grouping, and parallel runs.
>
> Architecturally, I divided it into layers. The test layer is intentionally thin and readable. The workflow layer contains business flows like the complete payment lifecycle. The API client layer wraps endpoint calls such as get providers, create transaction, finalise, charge, renew, and cancel. The REST client factory creates common request specifications with base URI, content type, security filter, and Allure filter.
>
> One important part is the security filter. The backend requires custom headers, so the framework automatically generates `x-access-token`, `x-dp-timestamp`, and `x-dp-signature` for every request. The signature is created using HMAC-SHA256, which makes the automation close to real enterprise API behavior.
>
> For validation, I separated responsibilities. `ResponseValidator` checks status codes, JSON fields, and schemas. `DatabaseValidator` checks MySQL state like transaction status, subscription renewal, cancellation, and audit logs. `StateTransitionValidator` verifies that payment statuses move through allowed states.
>
> Reporting is handled by Allure. The custom reporting filter attaches request, response, elapsed time, and database validation details. So if a test fails, it is easy to debug from the report.
>
> Overall, the framework is maintainable because endpoint logic, security, config, payload creation, validation, database checks, and reporting are all separated.

## 8. Interviewer Follow-Up Questions And Answers

### Why did you create a separate API client layer?

Because tests should not repeat REST Assured code. The client layer makes endpoint calls reusable and easier to maintain. If an endpoint changes, I update one method instead of many tests.

### Why did you use a workflow layer?

Payment flows are multi-step. A transaction depends on provider discovery, authorization, receipt generation, finalization, charge, renewal, and cancellation. The workflow layer keeps that business sequence in one reusable place.

### Why do you validate the database?

For payment systems, an API response can be correct while the database state is wrong. Database validation confirms that the transaction status, subscription state, audit logs, and timestamps were persisted correctly.

### How do you handle authentication?

Authentication is handled by a REST Assured filter. It automatically adds the access token, timestamp, and HMAC signature to every request.

### How is the framework environment-independent?

Configuration comes from property files and can be overridden using environment variables. This allows the same tests to run locally, in Docker, or in CI.

### How do you support reporting?

The framework uses Allure with TestNG. A custom REST Assured filter attaches request and response details, while database validators attach SQL validation evidence.

### How do you support parallel execution?

TestNG is configured for method-level parallel execution. Runtime data is stored in local workflow context objects instead of static shared variables. Faker is stored in `ThreadLocal`.

### What are the strongest design principles in this framework?

Separation of concerns, reusability, environment-driven configuration, realistic security handling, business workflow coverage, and strong validation beyond HTTP status codes.

## 9. Important Files To Mention

```text
automation/src/test/java/com/payflowx/automation/tests/BaseApiTest.java
automation/src/test/java/com/payflowx/automation/workflows/PaymentLifecycleWorkflowTest.java
automation/src/test/java/com/payflowx/automation/regression/NegativePaymentApiTest.java

automation/src/main/java/com/payflowx/automation/workflows/PaymentWorkflow.java
automation/src/main/java/com/payflowx/automation/models/PaymentWorkflowContext.java

automation/src/main/java/com/payflowx/automation/clients/PaymentApiClient.java
automation/src/main/java/com/payflowx/automation/clients/RestClientFactory.java

automation/src/main/java/com/payflowx/automation/filters/SecurityHeaderFilter.java
automation/src/main/java/com/payflowx/automation/filters/AllureRestAssuredFilter.java
automation/src/main/java/com/payflowx/automation/auth/SignatureGenerator.java

automation/src/main/java/com/payflowx/automation/validators/ResponseValidator.java
automation/src/main/java/com/payflowx/automation/validators/DatabaseValidator.java
automation/src/main/java/com/payflowx/automation/validators/StateTransitionValidator.java

automation/src/main/java/com/payflowx/automation/db/DatabaseManager.java
automation/src/main/java/com/payflowx/automation/config/FrameworkConfig.java
automation/src/test/resources/testng-suite.xml
```

## 10. Keywords To Use In Interview

Use these naturally:

- Layered architecture
- Separation of concerns
- Reusable API clients
- Business workflow abstraction
- Request specification
- REST Assured filters
- Dynamic HMAC signing
- JSON schema validation
- Database reconciliation
- Audit log validation
- Environment-driven configuration
- TestNG groups
- Parallel execution
- Allure reporting
- CI-ready execution

## 11. Do Not Say This

Avoid weak answers like:

> I used REST Assured to hit APIs and check status codes.

Say this instead:

> I built a layered API automation framework that validates secured payment workflows end to end, including API contracts, business state transitions, database persistence, and audit logs.

## 12. Final Polished Answer To Memorize

> This framework is a layered REST Assured and TestNG automation framework for a payment aggregator backend. The design separates test intent, workflow orchestration, API clients, request configuration, security, validation, database access, and reporting.
>
> Tests are kept thin. Business journeys are handled in workflow classes. API calls are wrapped in client methods. `RestClientFactory` builds common REST Assured specifications. Security is added through a filter that generates access token, timestamp, and HMAC-SHA256 signature headers for every request. Validation is handled through reusable validators for response status, JSON fields, JSON schemas, database state, audit logs, and allowed transaction status transitions.
>
> The main workflow validates the complete payment lifecycle: provider discovery, transaction creation, redirect authorization, provider data validation, finalization, charge, renewal, cancellation, database reconciliation, and audit logging. All request, response, timing, and database evidence is captured in Allure reports.
>
> The framework is maintainable because each responsibility has a clear layer, and it is scalable because configuration, test data, security, reporting, and execution are reusable across smoke, regression, security, and workflow tests.

