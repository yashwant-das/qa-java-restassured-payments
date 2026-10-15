# Framework simplification: design

Date: 2026-10-15
Status: approved in conversation, awaiting written-spec review

## Goal

This repository is a flagship portfolio project. A reviewer (hiring manager, senior SDET) should open it and see a small, well-organised REST Assured framework that clearly shows what it can do. The current version has too many moving parts, invented branding ("PayFlowX Enterprise") and docs that describe more than the code does.

Success means:

- Every core capability below still works and is still tested.
- Fewer packages, classes, dependencies and docs, with each piece having one clear job.
- No invented brand, no inflated wording, no real company names in test data.
- Every claim in the README is backed by a test or file it links to.

### Core capabilities that must be preserved

- HMAC-SHA256 signing of every request (token, timestamp, signature headers).
- The full transaction lifecycle in one test: providers, create, validate provider data, finalise, charge, renew, cancel.
- Database checks after each lifecycle step (transactions, subscriptions, audit logs).
- Negative and security tests.
- Allure evidence: request/response and SQL attachments per test.
- One-command Docker Compose run.
- CI: build plus SpotBugs, API tests, JUnit summary, Allure report on GitHub Pages, CodeQL, dependency review, Dependabot.

## Decisions

| Topic | Decision |
| --- | --- |
| Branding | Remove "PayFlowX". The backend is described as "a demo payment API (system under test)". |
| Base package | `io.github.yashwantdas.payments`: `.backend` for the API, `.automation` for the framework. |
| Auth headers | `X-Api-Key`, `X-Timestamp`, `X-Signature` (were `x-access-token`, `x-dp-timestamp`, `x-dp-signature`). |
| Env vars | `PAYMENTS_*` (were `PAYFLOWX_*`). Server port uses Spring's standard `SERVER_PORT`. |
| Test data | Explicit seeded IDs, no JavaFaker, no real brands. |
| Allure labels | Honest `@Epic`/`@Feature`/`@Severity`; no `@Owner`. |
| Backend scope | Rename and deduplicate only. Endpoints, status codes, error codes and behaviour stay identical. |
| Framework shape | Approach A: flat layered framework (chosen over a fluent step DSL and over pruning in place). |
| Docs | README plus one framework guide; the three current docs are removed. |
| CI | The test job runs the same `docker compose` commands as the quickstart. |

## Section 1: Automation module

### Structure

Framework code stays in `src/main/java` so SpotBugs analyses it (SpotBugs skips test classes by default) and because that is the conventional home for a reusable test library.

```
automation/src/main/java/io/github/yashwantdas/payments/automation/
├── config/TestConfig.java          # properties file + env/system-property overrides
├── api/
│   ├── PaymentApi.java             # one @Step method per endpoint; signed and unsigned request specs
│   ├── SigningFilter.java          # adds auth headers; public static sign(...); monotonic timestamp; logs elapsed time
│   └── model/                      # records: CreateTransactionRequest, FinaliseRequest, ValidateRequest
├── db/
│   ├── Database.java               # Hikari pool, queryOne
│   └── DbAssertions.java           # transaction, subscription and audit-log checks with Allure attachments
└── support/
    ├── TestData.java               # seeded IDs, provider PIN, request factories
    └── Transactions.java           # "create + authorise" setup, providerHash extraction

automation/src/test/java/io/github/yashwantdas/payments/automation/
├── BaseApiTest.java
├── ProviderDiscoveryTest.java      # smoke
├── TransactionLifecycleTest.java   # full lifecycle as a readable script
├── TransactionValidationTest.java  # business-rule negatives
└── RequestSigningTest.java         # security negatives
```

### Mapping from current code

| Current | New |
| --- | --- |
| `FrameworkConfig` | `config/TestConfig` |
| `PaymentApiClient`, `ApiPaths`, `RestClientFactory` | `api/PaymentApi` |
| `SecurityHeaderFilter`, `SignatureGenerator` | `api/SigningFilter` |
| `dto/*` | `api/model/*` (records, no Lombok) |
| `DatabaseManager` | `db/Database` (unused `update()` dropped) |
| `DatabaseValidator` | `db/DbAssertions` |
| `TransactionPayloadBuilder`, `testdata/payment-testdata.json` | `support/TestData` |
| `RedirectUrlParser`, `NegativePaymentApiTest.createAuthorizedTransaction` | `support/Transactions` |
| `PaymentWorkflow`, `PaymentWorkflowContext` | inlined into `TransactionLifecycleTest` |
| `AllureRestAssuredFilter` | official `io.qameta.allure:allure-rest-assured` `AllureRestAssured` filter |
| `StateTransitionValidator` | removed (it asserted its own hardcoded map, not system behaviour) |
| `ResponseValidator` | removed (use REST Assured `then().statusCode(...)` and schema matchers) |
| `RestClientFactory.ok()/created()` | removed (unused) |
| `enums`, `interceptors`, `assertions`, `integration`, `runners` packages | removed (empty) |
| `payloads/create-transaction-template.json`, `sql/cleanup.sql`, `environment.properties` | removed (unused) |

### Request signing

- `SigningFilter` builds the canonical string `METHOD\nPATH\nQUERY\nTIMESTAMP`, signs it with HMAC-SHA256 and sets the three headers.
- `sign(method, path, query, timestamp)` is `public static` so `RequestSigningTest` can build a validly signed request with a fixed timestamp.
- The `AtomicLong` monotonic timestamp and its comment are kept. The backend rejects a repeated `timestamp:signature` pair as a replay, and parallel tests can sign in the same millisecond.
- `SigningFilter` logs method, URI, status and elapsed milliseconds per call, replacing the timing the custom Allure filter used to provide.

### Request specs and filter order

- `PaymentApi` exposes a signed spec (`SigningFilter`, then `AllureRestAssured`) used by all endpoint methods.
- It also exposes an unsigned spec (`AllureRestAssured` only) for security tests that set headers by hand, so those tests get Allure attachments too.
- Filter order matters: `SigningFilter` is added before `AllureRestAssured` so attachments show the signed headers.
- `getTransaction` is kept and used once in the lifecycle test to compare the API status with the DB row.

### Test data

Seed IDs are renamed one-to-one, keeping every row and its status and type:

| Current | New | Name |
| --- | --- | --- |
| `mrc_enterprise_001` | `merchant_001` | Demo Store |
| `mrc_telco_001` | `merchant_002` | Demo Subscriptions |
| `mrc_inactive_001` | `merchant_inactive` | Inactive Merchant |
| `prv_stripe_us` | `provider_card_us` | Card Gateway US |
| `prv_adyen_eu` | `provider_card_eu` | Card Gateway EU |
| `prv_razorpay_in` | `provider_upi_in` | UPI Gateway India |
| `prv_paypal_global` | `provider_wallet` | Digital Wallet |
| `prv_legacy_disabled` | `provider_disabled` | Disabled Gateway |

`TestData` holds these IDs as constants plus factory methods, for example `renewableSubscription(merchantId, providerId)` with a fixed description and amount. Transaction IDs are generated by the server, so the tests need no randomness.

### Tests

Tests run with TestNG `parallel="methods" thread-count="4"`. `testng-suite.xml` points at `io.github.yashwantdas.payments.automation.*` and drops the group filter that currently filters nothing. Groups (`smoke`, `regression`) stay on the test methods for selective runs.

**ProviderDiscoveryTest** (smoke, 1 test)
- Active providers for `merchant_001`: status 200, schema match, non-empty list, first provider `ACTIVE`.

**TransactionLifecycleTest** (1 test)

The method reads as the lifecycle script: eight steps, each one `PaymentApi` call plus one `DbAssertions` call, about 30 lines. It must keep every current assertion:

- providers: 200 and schema
- create: 201 and schema, `redirectURL` contains `providerHash`, DB row `CREATED`, every DB field matches the request (merchant, provider, amount, currency, subscription, duration, renewable, request ID)
- validate provider data: 200, receipt returned, DB `PROCESSING`
- finalise, charge, renew, cancel: 200, response `status` equals `SUCCESS`, `CHARGED`, `RENEWED`, `CANCELLED`, and the DB matches after each
- `getTransaction` status matches the DB at one step
- subscription: `renewal_count >= 1`, status `CANCELLED`, `next_renewal_date` not null
- audit logs: a `SUCCESS` row exists for create, validate, finalise, charge, renew and cancel **for this transaction**. `audit_logs` has no `transaction_id` column, so rows are matched on `api_name` (the backend logs templated names such as `POST /transactions/{transactionId}/charge`) plus `response_payload` containing the transaction ID. The validate response holds only `receipt` and `authorizationStatus`, so its row is matched on the receipt instead. Today's check counts rows globally, so a parallel test can satisfy it.
- ID prefixes: `txn_`, `req_`, `rcpt_`

**TransactionValidationTest** (6 tests, unchanged scenarios)
- invalid merchant: 404 `INVALID_MERCHANT`
- invalid provider: 404 `INVALID_PROVIDER`
- invalid amount: 400
- invalid currency: 400 `INVALID_CURRENCY`
- invalid receipt: 400 `INVALID_RECEIPT`
- duplicate finalisation: 409 `DUPLICATE_FINALIZATION`

**RequestSigningTest** (6 tests: 3 existing, 3 new)
- invalid token: 401 `INVALID_TOKEN` (the code assertion is new; today only the status is checked)
- invalid signature: 401 `INVALID_SIGNATURE`
- expired timestamp: 401 `EXPIRED_TIMESTAMP`
- new: missing security headers: 401 `MISSING_SECURITY_HEADERS`
- new: non-numeric timestamp: 401 `INVALID_TIMESTAMP`
- new: replay, the same validly signed request sent twice with a fixed timestamp; the second returns 409 `REPLAY_ATTACK_DETECTED`

Exact status codes for the two new header tests are taken from `SecurityFilter` during implementation.

Total: 14 tests (was 11).

### JSON schemas

`providers.schema.json` and `create-transaction.schema.json` declare draft-04, the draft the REST Assured validator supports. They currently declare draft 2020-12.

### Dependencies (automation module)

Kept: rest-assured, json-schema-validator, testng, assertj-core, allure-testng, allure-rest-assured (new), HikariCP, mysql-connector-j, slf4j-api, logback-classic, jackson-databind (REST Assured serialisation).

Removed: lombok, javafaker, awaitility, commons-lang3, logback-core (comes with logback-classic).

## Section 2: Documentation

Principles: every claim links to the test or file that proves it; plain wording; each fact lives in one place.

### README.md (about 120 lines)

1. Title and one-line pitch; badges for CI, CodeQL, Allure report, license, Java.
2. "What this project demonstrates": five bullets linking to code (signing filter, lifecycle test, DB checks, security tests, CI with Allure).
3. Architecture: a Mermaid diagram (tests to signed HTTP to API to MySQL; tests also read MySQL directly).
4. Allure report screenshot at `docs/images/allure-report.png`, taken from a real run of the new suite.
5. Quickstart: the Docker commands, the expected `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0` line, a short troubleshooting table.
6. Test coverage table: one row per test class, what it proves, endpoints and error codes.
7. Project layout and tech stack, shortened.
8. Link to the framework guide.

### docs/framework-guide.md (about 180 lines)

1. Design choices: flat layers, framework code in `src/main` for SpotBugs, explicit test data, lifecycle test as a script.
2. The layers: one paragraph each for `config`, `api`, `db`, `support`, with file links.
3. Request signing: canonical string, headers, monotonic timestamp, how replay is tested.
4. Database checks: what is checked at each step; per-transaction audit-log matching.
5. Reporting: what Allure attaches; opening a report locally.
6. How to add a test: worked example (endpoint method, test, DB assertion).
7. API reference: endpoints, state machine (states and allowed transitions), error codes.
8. Running without Docker: local MySQL, backend, IntelliJ, SpotBugs, configuration table, local-only credentials note.

### In the code

- Short class-level Javadoc (one or two sentences) on each public framework class. No method-level boilerplate.
- Plain names and descriptions in every `pom.xml`.

### Removed

- `docs/test-automation-framework-architecture.md`, `docs/api-and-test-coverage.md`, `docs/local-setup.md` (useful content moves into the README and guide first).
- The "Possible improvements" section.

## Section 3: Backend, build, Docker and CI

### Backend

- Package `io.github.yashwantdas.payments.backend`; artifact `payments-api`; application class `PaymentsApiApplication`.
- Headers, property prefix (`payments.security.*`), env vars (`PAYMENTS_*`), Hikari pool name and logger config lose the brand.
- Seed data renamed per the table in Section 1.
- `backend/src/main/resources/db/schema.sql` and `seed-data.sql` are the only SQL sources. The backend already runs them at startup (`spring.sql.init.mode: always`, with `DROP TABLE IF EXISTS`), so the copies in `docker/mysql/` are deleted. `updated_at` keeps `ON UPDATE CURRENT_TIMESTAMP`.
- The H2 dependency and H2 defaults are removed; the default `DB_URL` points at local MySQL, matching `local.properties`. The tests always need MySQL, so one database path is enough.
- Lombok stays in the backend (idiomatic for JPA entities; internals are out of scope).
- Endpoints, request/response shapes, status codes and error codes do not change, apart from the header names.

### Build

- Root POM `payments-parent` with a plain description.
- Every dependency and plugin version is a property in the root POM. The module POMs reference properties only.
- SpotBugs configuration unchanged.

### Docker

- `Dockerfile.backend` and `Dockerfile.tests` copy the POMs first and run `mvn dependency:go-offline` to get a cached dependency layer.
- The backend runtime image runs as a non-root user.
- `docker-compose.yml`:
  - shared DB and security settings in one `x-` anchor
  - no SQL init mounts
  - a backend healthcheck that fails when the backend is down (any HTTP response, including 401, counts as up)
  - the `tests` service waits on `service_healthy`
  - container names without the brand

### CI

- `payflowx-ci.yml` becomes `ci.yml`; README badges updated.
- The static-analysis job is unchanged (`mvn -B -DskipTests verify`).
- The API test job runs the quickstart's `docker compose` commands, then publishes the JUnit summary, generates the Allure report, uploads artifacts and deploys Pages on `main` as today. The SQL init step and duplicated env blocks go away.
- CodeQL, dependency review and Dependabot unchanged.

## Verification

- `mvn -B verify` passes (build and SpotBugs).
- From a clean clone, the README quickstart ends with `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`.
- `grep -ri payflowx` across the tracked files returns nothing (outside git history).
- Every relative link and file path in the README and framework guide resolves.
- CI is green on the pull request, and the Allure report shows signed headers in request attachments.

## Out of scope

- Refactoring backend internals (`PaymentService`, entities).
- Changing endpoint paths or response shapes.
- A fluent step DSL or other new framework layers.
- A markdown link-check job in CI.

## Housekeeping

This spec and the implementation plan are working documents. They are removed in the final pull request so the repository keeps only the README and the framework guide as documentation.
