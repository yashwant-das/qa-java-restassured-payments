# Running without Docker

Use this if you want to run the backend and tests directly on your machine, for example to debug in an IDE.

## Prerequisites

- Java 21
- Maven 3.9+
- MySQL 8 running on `localhost:3306` with a `root` password of `root` (adjust the commands below if yours differs)

## 1. Create the database

```bash
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS payflowx; CREATE USER IF NOT EXISTS 'payflowx'@'%' IDENTIFIED BY 'payflowx'; GRANT ALL ON payflowx.* TO 'payflowx'@'%';"
mysql -uroot -proot payflowx < docker/mysql/schema.sql
mysql -uroot -proot payflowx < docker/mysql/seed-data.sql
```

## 2. Start the backend

Run from the repository root and leave it running:

```bash
DB_URL='jdbc:mysql://localhost:3306/payflowx?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
DB_USERNAME=payflowx \
DB_PASSWORD=payflowx \
DB_DRIVER=com.mysql.cj.jdbc.Driver \
mvn -pl backend spring-boot:run
```

The backend listens on `http://localhost:8080`. It re-applies the schema and seed data on every start.

## 3. Run the tests

In a second terminal:

```bash
mvn -pl automation test
```

The defaults in `automation/src/test/resources/config/local.properties` already point at `localhost`. See the README for the environment variables that override them.

## Running tests from an IDE (IntelliJ IDEA)

The simplest setup is to run MySQL and the backend in Docker and run the test classes from the IDE.

1. Start MySQL and the backend:

   ```bash
   docker compose up --build -d mysql backend
   ```

2. Confirm the backend is ready. Any HTTP response means it is listening; `401 INVALID_TOKEN` is expected for a bad token:

   ```bash
   curl -i -H "x-access-token: bad" http://localhost:8080/providers
   ```

   If you get "connection refused", wait a few seconds or check `docker compose logs backend`.

3. In IntelliJ, open the repository root `pom.xml` as a Maven project and set the project SDK to JDK 21.
4. Open a test class under `automation/src/test/java`, for example `PaymentLifecycleWorkflowTest`, and click the green run icon. The defaults in `local.properties` already point at `localhost`, so no extra configuration is needed.
5. To run the whole suite as Maven does, run `automation/src/test/resources/testng-suite.xml` from the IDE.
6. Stop the containers when you are done so ports 3306 and 8080 are freed:

   ```bash
   docker compose down
   ```

To run the backend itself from the IDE, start `PayFlowXBackendApplication` with the `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and `DB_DRIVER` environment variables shown above, and start only MySQL with `docker compose up -d mysql`.

## Allure report

```bash
mvn -pl automation allure:serve
```

Use `allure:report` instead to generate static HTML under `automation/target/site/allure-maven-plugin`.

## Static analysis

Compile both modules and run SpotBugs without the tests:

```bash
mvn -DskipTests verify
```

Suppressions live in [`spotbugs-exclude.xml`](../spotbugs-exclude.xml).
