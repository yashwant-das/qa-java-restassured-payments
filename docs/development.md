# Development

Run commands from the repository root unless stated otherwise. The backend is a disposable test target: its startup SQL drops and recreates the `payments` tables.

## Docker setup

Install Docker with Compose v2 or later. Reserve ports 3306 and 8080, then run:

```bash
docker compose up --build -d --wait mysql backend
docker compose --profile test up --build --abort-on-container-exit --exit-code-from tests tests
```

The second command exits with the test container's status. MySQL is health-checked; the backend check waits for its TCP listener, not an authenticated business request. Actual endpoint behavior is verified by the test suite. Output mounts into `automation/target/`.

To check isolation against existing data, confirm MySQL/backend are still running and run the test container without restarting dependencies:

```bash
docker compose ps
docker compose --profile test run --rm --no-deps tests
```

The repeat command requires healthy existing services and a built test image. `docker compose --profile test down` stops/removes this stack's containers and network. A new backend start recreates the schema; there is no persistent named MySQL volume in this Compose file.

## Running without Docker

Install Java 21 and MySQL 8.4. The committed Maven wrapper pins the Maven distribution and checksum; no global Maven installation is required. Its first run needs network access, as do uncached dependencies. On Windows use `mvnw.cmd` in place of `./mvnw`.

Create a dedicated disposable database and local user with a MySQL administrative account:

```sql
CREATE DATABASE payments;
CREATE USER 'payments'@'localhost' IDENTIFIED BY 'payments';
GRANT ALL ON payments.* TO 'payments'@'localhost';
```

Build and start the API:

```bash
./mvnw -pl backend -am -DskipTests package
java -jar backend/target/payments-api-1.0.0.jar
```

In a second terminal at the repository root, run:

```bash
./mvnw -pl automation test
```

Alternatively, start only MySQL with `docker compose up -d --wait mysql`, then run the API and tests on the host. Do not run the container backend and host backend on port 8080 simultaneously.

### IDE execution

Start MySQL and the API, then run `automation/src/test/resources/testng-suite.xml` or a concrete test class from IntelliJ IDEA using JDK 21. For named Allure steps, add the AspectJ `-javaagent` argument from [automation/pom.xml](../automation/pom.xml) to the IDE test run configuration, resolving the property values to the actual jar in your Maven cache. Surefire supplies it automatically for Maven runs. IDE execution is a manual workflow, separate from the automated CI suite.

## Configuration

The automation's [TestConfig](../automation/src/main/java/com/qaframework/payments/automation/config/TestConfig.java) reads a Java system property first, then the matching environment variable, then [config.properties](../automation/src/test/resources/config.properties). Empty overrides are rejected. The backend reads Spring configuration and environment variables through [application.yml](../backend/src/main/resources/application.yml); test-client `-D` keys do not configure the server.

| Automation property | Environment variable | Local default |
| --- | --- | --- |
| `base.uri` | `PAYMENTS_BASE_URI` | `http://localhost:8080` |
| `api.key` | `PAYMENTS_API_KEY` | `local-test-api-key` |
| `signing.secret` | `PAYMENTS_SIGNING_SECRET` | `local-test-signing-secret` |
| `provider.pin` | `PAYMENTS_PROVIDER_PIN` | `4321` |
| `db.url` | `DB_URL` | `jdbc:mysql://localhost:3306/payments?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `db.username` | `DB_USERNAME` | `payments` |
| `db.password` | `DB_PASSWORD` | `payments` |

The backend uses the same key/secret/PIN/database environment names. It also accepts `PAYMENTS_TIMESTAMP_SKEW_SECONDS` (default `300`); this has no automation property. Compose sets the client URL to `http://backend:8080` and database host to `mysql`. Compose's MySQL root password is `root`; the application and tests use the `payments` user.

Example host-side override with a running matching backend:

```bash
./mvnw -pl automation test -Dbase.uri=http://localhost:8080
```

Defaults are only for local demo use. HTTP reports include signed headers, and backend startup resets tables; review both before pointing at another environment.

## Adding a test

Use an implemented endpoint as the starting point. This example finalises an authorised transaction, then checks its persisted state:

```java
@Test(groups = "regression")
public void finalisedTransactionHasSuccessfulStatus() {
    Transactions.Authorised txn = Transactions.createAndAuthorise(api);
    api.finalise(txn.transactionId(), txn.receipt()).then().statusCode(200);
    api.getTransaction(txn.transactionId()).then()
            .statusCode(200)
            .body("status", equalTo("SUCCESS"));
    DbAssertions.transactionStatus(txn.transactionId(), "SUCCESS");
}
```

Place the method in a test class extending `BaseApiTest`, with a `PaymentApi api` field and imports as in [TransactionValidationTest](../automation/src/test/java/com/qaframework/payments/automation/TransactionValidationTest.java). This is an extension example, not an additional committed test or a new endpoint. For a new API endpoint, add the controller/service contract first, then a named `@Step` method in `PaymentApi` returning a raw REST Assured response. For a new SQL check, add a `@Step` method in `DbAssertions`, use its `read(...)` helper and scope the query by transaction ID or receipt. Keep assertions visible in tests rather than hiding the lifecycle in a generic workflow abstraction.

The suite discovers the `com.qaframework.payments.automation` package. All 14 current methods belong to `regression`; provider discovery and lifecycle also belong to `smoke`. Run the full committed suite for required verification; selective IDE execution does not replace it.

## Reports

After a test run, generate a static report or open an interactive report server:

```bash
./mvnw -B -pl automation allure:report
./mvnw -pl automation allure:serve
```

| Output | Path |
| --- | --- |
| TestNG/Surefire XML | `automation/target/surefire-reports/` |
| Allure raw results and HTTP/SQL attachments | `automation/target/allure-results/` |
| Static Allure report | `automation/target/site/allure-maven-plugin/` |
| Automation log | `automation/target/logs/automation.log` |

Allure appends result files across runs. For a report of one run only, clear that run's generated Allure results before testing; retain useful evidence elsewhere first. CI starts from a clean checkout. The committed [overview](assets/allure-report-overview.png) and [suite view](assets/allure-report-suites.png) were provided by the user from the report dated 2026-10-29. They show 14 passed tests and the current `com.qaframework.payments.automation` namespace; the overview appears first in the README.

## Quality gates

Install `pre-commit==4.7.0` in your preferred isolated Python environment, then:

```bash
pre-commit install
pre-commit run --all-files
./mvnw -B -DskipTests verify
docker compose config --quiet
```

Hooks use pinned revisions and a pinned Node 24 runtime for Markdown tooling. Initial installation requires network access. Subsequent staged checks do not require MySQL or the API. Review and restage hook fixes before committing; do not bypass a failed hook.

| Gate | Scope and limits |
| --- | --- |
| Text/config hooks | Staged whitespace, final newline, merge conflicts, YAML, JSON and XML syntax; preserve Markdown hard breaks; XML parser does not fetch TestNG's DTD |
| Markdown | `markdownlint-cli2`, defaults with only `MD013` disabled, shared local/CI config; excludes generated output and historical plans |
| Build and SpotBugs | Both production modules; skips integration execution; no claim of unit coverage is implied |
| API tests | 14 TestNG cases against controlled MySQL/backend; HTTP, JSON schema, persistence and rejection assertions |
| CodeQL | GitHub-hosted Java security/quality analysis; cannot be proven by a local compile |
| Dependency review | GitHub PR dependency-diff vulnerability gate; not an audit of every existing dependency |
| Manual review | Links, command accuracy, Mermaid rendering, report content and preservation of useful original guidance |

CI runs all file hooks independently of staged state, builds both modules, and executes the Docker suite. All workflows pin actions to full commit SHAs and checkout without persisted credentials. Reports/log artifacts expire after three days. Only a successful `main` push with successful quality, build and API jobs may deploy Pages; failures retain diagnostic artifacts. Maven/Actions/Docker Dependabot updates run weekly. No new coverage threshold is invented.

## Troubleshooting

| Symptom | Action |
| --- | --- |
| Port 3306 or 8080 allocated | Stop the conflicting local service or previous stack before starting this one |
| Docker socket unavailable | Start Docker Desktop and confirm `docker info` succeeds |
| Java runtime missing or wrong release | Set `JAVA_HOME` to JDK 21 and confirm `java -version`; the wrapper downloads Maven, not Java |
| Changes absent from container tests | Use `--build`; stop the prior stack with `docker compose --profile test down` if needed |
| DB connection fails before tests | Check MySQL readiness and the client DB URL; host tests use `localhost`, container tests use `mysql` |
| Signature/expired timestamp failures | Match client/server key and secret; check host/container clocks and the backend skew setting |
| Allure steps missing in IDE | Configure the AspectJ agent, or run via Maven Surefire |
| Report has old or duplicate cases | Allure raw results accumulate; archive useful output and clear generated results before the next run |
| Existing rows disappear on restart | Expected: startup runs the destructive demo schema; use only disposable data |

## Dependency decisions

MySQL is pinned to the reviewed `8.4.13` patch in [Compose](../docker-compose.yml). The Docker dependency-download goal uses the pinned Maven Dependency Plugin in the parent POM.

The [parent POM](../pom.xml) and [wrapper properties](../.mvn/wrapper/maven-wrapper.properties) are the version sources of truth. Java 21 remains the deliberate baseline. Official [Maven Central metadata](https://repo.maven.apache.org/maven2/), [Spring Boot requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html), and [REST Assured documentation](https://github.com/rest-assured/rest-assured/wiki/GettingStarted) were reviewed on 2026-10-28. Preview releases are excluded.

The retained compatibility set uses Spring Boot 3.5, Jackson 2, REST Assured 5 and Allure 2. New major releases exist; Boot 4/Jackson 3 and Allure 3 require a separate migration of serialization/reporting contracts. Keep the latest reviewed compatible stable releases in these lines, align Jackson through its BOM and Logback core/classic explicitly, and verify the entire set with the integration suite. Surefire stays at 3.5.6 ([TestNG execution changed in 3.6](https://maven.apache.org/surefire/maven-surefire-plugin/examples/testng.html)): an executed 3.6.1 run rejected `suiteXmlFiles` because 3.6 moved TestNG to JUnit Platform. Retaining 3.5 preserves the committed four-thread TestNG XML suite. Maven stays in the 3.9 line to match the container toolchain. These are compatibility constraints, not claims that newer major releases do not exist. Owner: Yashwant Das. Review trigger: a security advisory affecting this set, an upstream support change or a planned major migration.

## Verification and exceptions

Standards profile: `complete`, pinned to [a429dd2f2da55a4c3e1c73c7c0d1111f9cd0b9a9](https://github.com/yashwant-das/repo-standards/tree/a429dd2f2da55a4c3e1c73c7c0d1111f9cd0b9a9). Baseline: `31ac98adb4bdbb8224cbf0fc37602a7592a52883`, pulled with `git pull --ff-only` before changes.

| Check | Adoption evidence (2026-10-28–29) |
| --- | --- |
| Baseline Java build/static analysis | Passed with Homebrew OpenJDK 21.0.13: both modules, zero SpotBugs findings |
| Baseline controlled Docker suite | Passed: 14 tests, zero failures/errors/skips |
| Host Java suite | Passed twice against an isolated schema on the task MySQL container: 14 tests each, zero failures/errors/skips |
| File/config/Markdown gates | All seven pre-commit checks passed with pre-commit 4.7.0 and Node 24.22.1 |
| Workflow validation | actionlint 1.7.12 passed; full action SHAs, checkout credentials and deployment dependencies reviewed |
| Allure raw evidence | 14 passed results across four classes; lifecycle has 22 named steps and 14 SQL attachments; signed headers present |
| Static report generation | Passed: Allure summary contains 14 passed, zero failed/broken/skipped |
| Final Java build/static analysis | Passed after final plugin pins: both modules, zero SpotBugs findings |
| Final Docker verification | Fresh MySQL 8.4.13 quickstart passed 14/14; two `run --rm --no-deps` repeats also passed 14/14, no failures/errors/skips |
| Repeat-run isolation | Transaction rows increased 3 → 6 → 9; the backend start timestamp was unchanged across both repeats |
| Source/content preservation | All 46 Java files match the baseline after namespace substitution; maintained file/image links and anchors resolve |
| Visual review | User-provided report overview and suite screenshots inspected: 14 passed tests and the current namespace. Interactive browser and Mermaid rendering remain unverified after automatic approval failed at its usage limit; IDE interaction was not exercised |
| Existing ruleset | Active `default-branch-protection` prevents deletion and non-fast-forward updates; it has no required-status-check rule |
| Scanning settings | CodeQL workflow exists; secret scanning and push protection reported disabled during read-only inspection |
| Hosted checks and deployment | Review the PR checks for actual CI, CodeQL and dependency-review outcomes; Pages deployment requires a successful checked push to `main` after merge |

Required-check enforcement and optional secret scanning need repository-settings follow-up; this change does not weaken or silently change those settings. Proposed required jobs: `Repository quality`, `Build and static analysis`, `API tests`, `Analyze Java`, and `dependency-review`. Owner: Yashwant Das; review trigger: PR review before merge or the next security-settings review.

Java staged formatting is deferred: there is no established formatter configuration, and introducing a formatter across both modules would obscure this namespace/documentation adoption. Compile and SpotBugs remain mandatory; whitespace/config checks apply locally. Owner: Yashwant Das; review trigger: the next Java style change. This is a scoped practical exception, not a claim that a formatter runs.

The original framework guide's design, package overview, signing example, DB assertions, reporting details and API reference now live in [architecture](architecture.md). Docker-free/IDE setup, all configuration defaults and extension guidance live here. The hypothetical receipt endpoint example was replaced with an implemented status endpoint. README coverage and source evidence are preserved; the old Allure image was replaced with the user-provided overview and suite screenshots; no active rollout material was removed.

The [adoption plan](superpowers/plans/2026-10-28-repo-standards-adoption.md) is a planning record. Current commands are maintained in this guide and the README.
