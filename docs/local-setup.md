# Running without Docker

Prerequisites: Java 21, Maven 3.9+ and MySQL 8.

Start MySQL 8 and initialize:

```bash
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS payflowx;"
mysql -uroot -proot payflowx < docker/mysql/schema.sql
mysql -uroot -proot payflowx < docker/mysql/seed-data.sql
```

Build and run the backend:

```bash
DB_URL='jdbc:mysql://localhost:3306/payflowx?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
DB_USERNAME=payflowx \
DB_PASSWORD=payflowx \
DB_DRIVER=com.mysql.cj.jdbc.Driver \
mvn -pl backend -am spring-boot:run
```

Run tests:

```bash
mvn -pl automation test
```

Build and open the Allure report after a run:

```bash
mvn -pl automation allure:report
mvn -pl automation allure:serve
```

Run the static analysis gate (SpotBugs) without tests:

```bash
mvn -DskipTests verify
```

SpotBugs suppressions live in [`spotbugs-exclude.xml`](../spotbugs-exclude.xml).
