# Framework guide

How the automation framework is built, how request signing works, and how to add a test.

## Design choices

- **Four small packages.** Framework code is split by job: `config` reads settings, `api` talks HTTP, `db` reads MySQL, `support` builds test data. There are no base layers or wrappers beyond these.
- **Framework code in `src/main`.** The framework is a library the tests use, so it lives in `automation/src/main/java`. That also puts it under the SpotBugs check in CI, which skips test classes.
- **Tests read as scripts.** `TransactionLifecycleTest` lists the lifecycle steps in order; each step is an API call, a response check and a DB check. Nothing is hidden in a workflow class.
- **Explicit test data.** IDs come from the seed data and requests from small factories in `TestData`. The server generates transaction IDs, so tests need no random data and failures are easy to reproduce.
- **Raw responses.** `PaymentApi` returns REST Assured `Response` objects and tests assert with `then()`, so each test shows exactly what it checks.

## Packages

| Package | Classes | Job |
| --- | --- | --- |
| [`config`](../automation/src/main/java/io/github/yashwantdas/payments/automation/config) | `TestConfig` | Reads settings: system property, then environment variable, then `config.properties`. |
| [`api`](../automation/src/main/java/io/github/yashwantdas/payments/automation/api) | `PaymentApi`, `SigningFilter`, `model/*` | One method per endpoint; signing; request bodies as records. |
| [`db`](../automation/src/main/java/io/github/yashwantdas/payments/automation/db) | `Database`, `DbAssertions` | Connection pool; database checks that attach their SQL to the report. |
| [`support`](../automation/src/main/java/io/github/yashwantdas/payments/automation/support) | `TestData`, `Transactions` | Seeded IDs and request factories; setup that creates an authorised transaction. |

Tests live in [`automation/src/test/java`](../automation/src/test/java/io/github/yashwantdas/payments/automation) and extend `BaseApiTest`, which checks the database is reachable before the suite starts.

## Request signing

Every request except `/actuator/**` must carry three headers:

| Header | Value |
| --- | --- |
| `X-Api-Key` | The shared API key |
| `X-Timestamp` | Current time in epoch milliseconds; must be within 300 seconds of the server clock |
| `X-Signature` | Hex HMAC-SHA256, keyed with the signing secret, of `METHOD\nPATH\nQUERY\nTIMESTAMP` |

`PATH` and `QUERY` are the raw, still-encoded parts of the URL; `QUERY` is empty when there is none. For `GET /providers?merchantId=merchant_001` the signed string is:

```text
GET
/providers
merchantId=merchant_001
1760000000000
```

The API remembers each `timestamp:signature` pair and rejects a repeat with `409 REPLAY_ATTACK_DETECTED`. Because four tests run at once, two threads could sign in the same millisecond, so `SigningFilter.nextTimestamp()` always returns a value larger than the last one it gave out.

`RequestSigningTest` builds headers by hand on the unsigned spec (`PaymentApi.unsigned()`) to check each rejection, and signs one request with a fixed timestamp to send it twice for the replay check.

## Database checks

`TransactionLifecycleTest` checks MySQL after every call:

| After | Checked |
| --- | --- |
| Create | Status `CREATED`; merchant, provider, amount, currency, subscription, duration, renewable flag and request ID match the request |
| Provider validation | Status `PROCESSING` |
| Finalise, charge, renew, cancel | Status `SUCCESS`, `CHARGED`, `RENEWED`, `CANCELLED` |
| Each of the above | A `SUCCESS` audit-log entry for that endpoint |
| Cancel | Subscription `CANCELLED`, renewed at least once, next renewal date set |

The audit table has no transaction column, so `DbAssertions.auditLogged` matches the endpoint name and looks for the transaction ID (or, for provider validation, the receipt) in the logged response. Every check is keyed this way, which keeps parallel and repeated runs independent.

## Reporting

Allure records, for every test:

- each `PaymentApi` and `DbAssertions` call as a named step (through the AspectJ agent configured in `automation/pom.xml`),
- the full request and response, including the signed headers (the official `AllureRestAssured` filter, added after `SigningFilter`),
- each SQL query, its parameters and the row it returned.

Open the latest report locally with `mvn -pl automation allure:serve` after a run. CI attaches the report to every run and publishes the one from `main` to GitHub Pages.

## Adding a test

Suppose the API gains `GET /transactions/{transactionId}/receipt`.

1. Add the endpoint to `PaymentApi`:

   ```java
   @Step("Get receipt for transaction {transactionId}")
   public Response getReceipt(String transactionId) {
       return given(signed()).get(TRANSACTION + "/receipt", transactionId);
   }
   ```

2. Write the test, reusing the shared setup:

   ```java
   @Test(groups = "regression")
   public void finalisedTransactionHasReceipt() {
       Transactions.Authorised txn = Transactions.createAndAuthorise(api);
       api.finalise(txn.transactionId(), txn.receipt()).then().statusCode(200);

       api.getReceipt(txn.transactionId()).then()
               .statusCode(200)
               .body("receipt", equalTo(txn.receipt()));
       DbAssertions.transactionStatus(txn.transactionId(), "SUCCESS");
   }
   ```

3. If the test needs a new DB check, add a `@Step` method to `DbAssertions` that reads one row with `read(...)` and asserts on it, keyed by the transaction ID.

## API reference

| Method | Path | Success | Purpose |
| --- | --- | --- | --- |
| GET | `/providers?merchantId=` | 200 | Active providers for an active merchant |
| POST | `/transactions` | 201 | Create a transaction; returns a redirect URL with `providerHash` |
| GET | `/transactions/{id}` | 200 | Current transaction state |
| POST | `/validateTransactionData` | 200 | Provider authorises with `providerHash` and PIN; returns a receipt |
| POST | `/transactions/{id}/finalise` | 200 | Confirm with the receipt → `SUCCESS` |
| POST | `/transactions/{id}/charge` | 200 | → `CHARGED` |
| POST | `/transactions/{id}/renew` | 200 | → `RENEWED` |
| DELETE | `/transactions/{id}/cancel` | 200 | → `CANCELLED` |

Error responses have the shape `{"code": "...", "message": "...", "timestamp": "..."}`. The codes the suite checks are listed in the README's test coverage table.

## Running without Docker

You need Java 21, Maven 3.9+ and MySQL 8.

1. Create the database:

   ```sql
   CREATE DATABASE payments;
   CREATE USER 'payments'@'localhost' IDENTIFIED BY 'payments';
   GRANT ALL ON payments.* TO 'payments'@'localhost';
   ```

2. Start the API. It creates the tables and seed data on startup:

   ```bash
   mvn -pl backend -am -DskipTests package
   java -jar backend/target/payments-api-1.0.0.jar
   ```

3. Run the tests: `mvn -pl automation test`.

To run tests from IntelliJ IDEA, start MySQL and the API (with Docker: `docker compose up --build -d --wait mysql backend`), then run `automation/src/test/resources/testng-suite.xml` or any test class. Add the AspectJ `-javaagent` argument from `automation/pom.xml` to the run configuration if you want steps in the Allure results.

To run only the build and SpotBugs: `mvn -B -DskipTests verify`.

### Configuration

| Setting | Environment variable | Default |
| --- | --- | --- |
| `base.uri` | `PAYMENTS_BASE_URI` | `http://localhost:8080` |
| `api.key` | `PAYMENTS_API_KEY` | `local-test-api-key` |
| `signing.secret` | `PAYMENTS_SIGNING_SECRET` | `local-test-signing-secret` |
| `provider.pin` | `PAYMENTS_PROVIDER_PIN` | `4321` |
| `db.url` | `DB_URL` | local MySQL `payments` |
| `db.username`, `db.password` | `DB_USERNAME`, `DB_PASSWORD` | `payments` |

The API reads the same environment variables (see `backend/src/main/resources/application.yml`). These defaults are for local use only; never reuse them anywhere else.
