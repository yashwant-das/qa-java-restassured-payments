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
mvn -pl backend -am spring-boot:run
```

The backend listens on `http://localhost:8080`. It re-applies the schema and seed data on every start.

## 3. Run the tests

In a second terminal:

```bash
mvn -pl automation test
```

The defaults in `automation/src/test/resources/config/local.properties` already point at `localhost`. See the README for the environment variables that override them.

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
