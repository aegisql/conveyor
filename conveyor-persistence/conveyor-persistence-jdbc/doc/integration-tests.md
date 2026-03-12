# JDBC Persistence Integration Tests

## Purpose
- This manual describes how to run the JDBC persistence integration tests against live database instances.
- The source of truth for connection defaults is the shared test harness in `src/test/java/com/aegisql/conveyor/persistence/jdbc/harness/Tester.java`.
- The database-specific tests live in helper modules:
  - `conveyor-persistence-jdbc-mysql`
  - `conveyor-persistence-jdbc-mariadb`
  - `conveyor-persistence-jdbc-postgres`
  - `conveyor-persistence-jdbc-oracle`
  - `conveyor-persistence-jdbc-sqlserver`

## How The Tests Behave
- Run the tests from the repository root so Maven can build required sibling modules:

```bash
mvn -pl conveyor-persistence/conveyor-persistence-jdbc-mysql -am test
```

- Each database family has its own helper-module test suite.
- Tests skip themselves when the target database is not reachable.
- Most integration tests use `JdbcPersistenceBuilder.autoInit(true)`, so schema objects are created automatically by the builder.
- MySQL, MariaDB, PostgreSQL, and SQL Server tests create or drop test databases as needed.
- Oracle tests reuse the configured service and clean up tables inside that service instead of creating a separate database.

## Shared Test Controls
- `PERF_TEST_SIZE`
  - Controls the number of records used by performance tests.
  - Default: `10000`
- `conveyor.persistence.test.db.dir`
  - Controls where local file-based test artifacts are written.
  - This matters mainly for Derby and SQLite, not for the server databases documented here.

## DB Access Environment Variables
- Resolution order in the test harness is:
  - environment variable
  - matching JVM system property
  - harness default
- If `*_URL` is set, it is used directly.
- If `*_URL` is not set, the tests use the corresponding host, port, and credentials.

### Equivalent JVM System Properties
- The same names can be passed as JVM system properties instead of environment variables.
- Example:

```bash
mvn -pl conveyor-persistence/conveyor-persistence-jdbc-postgres -am test \
  -DPOSTGRES_HOST=db.example.internal \
  -DPOSTGRES_PORT=5433 \
  -DPOSTGRES_USER=postgres \
  -DPOSTGRES_PASSWORD=root
```

| Database | URL variable | Host variable | Port variable | User variable | Password variable | Extra variables | Defaults used by tests |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MySQL | `MYSQL_URL` | `MYSQL_HOST` | `MYSQL_PORT` | `MYSQL_USER` | `MYSQL_PASSWORD` | none | `jdbc:mysql://localhost:3306/`, user `tester`, password unset |
| MariaDB | `MARIADB_URL` | `MARIADB_HOST` | `MARIADB_PORT` | `MARIADB_USER` | `MARIADB_PASSWORD` | none | `jdbc:mariadb://localhost:3306/`, user `tester`, password unset |
| PostgreSQL | `POSTGRES_URL` | `POSTGRES_HOST` | `POSTGRES_PORT` | `POSTGRES_USER` | `POSTGRES_PASSWORD` | none | `jdbc:postgresql://localhost:5432/`, user `postgres`, password `root` |
| Oracle | `ORACLE_URL` | `ORACLE_HOST` | `ORACLE_PORT` | `ORACLE_USER` | `ORACLE_PASSWORD` | `ORACLE_SERVICE`, `ORACLE_SCHEMA` | `jdbc:oracle:thin:@//localhost:1521/FREEPDB1`, user `system`, password `root`, service `FREEPDB1`, schema `SYSTEM` |
| SQL Server | `SQLSERVER_URL` | `SQLSERVER_HOST` | `SQLSERVER_PORT` | `SQLSERVER_USER` | `SQLSERVER_PASSWORD` | none | `jdbc:sqlserver://localhost:1433;databaseName=master;encrypt=false;trustServerCertificate=true`, user `sa`, password `root2026!` |

## MySQL
### Docker
- The MySQL tests need an account that can create and drop databases.
- The current test defaults are `MYSQL_USER=tester` and no password, but the simplest Docker path is to run MySQL with a root password and override the test credentials.

```bash
docker pull mysql:8.4
```

```bash
docker run -d --name conveyor-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  mysql:8.4
```

### Test Access
- Recommended test credentials for the Docker container above:
  - user `root`
  - password `root`
- Common test database name created by the preset initializer:
  - `conveyor_db`

### Run The Tests
```bash
MYSQL_USER=root MYSQL_PASSWORD=root \
mvn -pl conveyor-persistence/conveyor-persistence-jdbc-mysql -am test
```

### Alternate Host Or Port
```bash
MYSQL_HOST=db.example.internal MYSQL_PORT=3307 MYSQL_USER=root MYSQL_PASSWORD=root \
mvn -pl conveyor-persistence/conveyor-persistence-jdbc-mysql -am test
```

### Direct URL Override
```bash
MYSQL_URL='jdbc:mysql://db.example.internal:3307/' MYSQL_USER=root MYSQL_PASSWORD=root \
mvn -pl conveyor-persistence/conveyor-persistence-jdbc-mysql -am test
```

## MariaDB
### Docker
- The MariaDB tests also need database-creation privileges.
- As with MySQL, the easiest Docker path is to run with a root password and override the test credentials.

```bash
docker pull mariadb:11
```

```bash
docker run -d --name conveyor-mariadb \
  -p 3306:3306 \
  -e MARIADB_ROOT_PASSWORD=root \
  mariadb:11
```

### Test Access
- Recommended test credentials for the Docker container above:
  - user `root`
  - password `root`
- The main integration tests create:
  - `conveyor_maria_db`

### Run The Tests
```bash
MARIADB_USER=root MARIADB_PASSWORD=root \
mvn -pl conveyor-persistence/conveyor-persistence-jdbc-mariadb -am test
```

### Alternate Host Or Port
```bash
MARIADB_HOST=db.example.internal MARIADB_PORT=3307 MARIADB_USER=root MARIADB_PASSWORD=root \
mvn -pl conveyor-persistence/conveyor-persistence-jdbc-mariadb -am test
```

### Direct URL Override
```bash
MARIADB_URL='jdbc:mariadb://db.example.internal:3307/' MARIADB_USER=root MARIADB_PASSWORD=root \
mvn -pl conveyor-persistence/conveyor-persistence-jdbc-mariadb -am test
```

## PostgreSQL
### Docker
- The PostgreSQL tests expect:
  - user `postgres`
  - password `root`
- They create and drop their own databases, including `conveyor_db_test`.

```bash
docker pull postgres:17
```

```bash
docker run -d --name conveyor-postgres \
  -p 5432:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=root \
  postgres:17
```

### Run The Tests
```bash
mvn -pl conveyor-persistence/conveyor-persistence-jdbc-postgres -am test
```

### Alternate Host Or Port
```bash
POSTGRES_HOST=db.example.internal POSTGRES_PORT=5433 POSTGRES_USER=postgres POSTGRES_PASSWORD=root \
mvn -pl conveyor-persistence/conveyor-persistence-jdbc-postgres -am test
```

### Direct URL Override
```bash
POSTGRES_URL='jdbc:postgresql://db.example.internal:5433/' POSTGRES_USER=postgres POSTGRES_PASSWORD=root \
mvn -pl conveyor-persistence/conveyor-persistence-jdbc-postgres -am test
```

## Oracle
### Docker
- The current Oracle tests expect:
  - host `localhost`
  - port `1521`
  - service `FREEPDB1`
  - user `system`
  - password `root`
- The helper-module tests clean up tables in the configured service rather than creating a separate database.

```bash
docker pull gvenzl/oracle-free
```

```bash
docker run -d --name conveyor-oracle \
  -p 1521:1521 \
  -e ORACLE_PASSWORD=root \
  gvenzl/oracle-free
```

### Run The Tests
```bash
mvn -pl conveyor-persistence/conveyor-persistence-jdbc-oracle -am test
```

### Alternate Host, Port, Service, Or Credentials
```bash
ORACLE_HOST=db.example.internal ORACLE_PORT=1522 ORACLE_SERVICE=FREEPDB1 ORACLE_USER=system ORACLE_PASSWORD=root \
mvn -pl conveyor-persistence/conveyor-persistence-jdbc-oracle -am test
```

### Direct URL Override
```bash
ORACLE_URL='jdbc:oracle:thin:@//db.example.internal:1522/FREEPDB1' ORACLE_USER=system ORACLE_PASSWORD=root \
mvn -pl conveyor-persistence/conveyor-persistence-jdbc-oracle -am test
```

### Optional Schema Override
```bash
ORACLE_SCHEMA=SYSTEM \
mvn -pl conveyor-persistence/conveyor-persistence-jdbc-oracle -am test
```

## SQL Server
### Docker
- The current SQL Server tests expect:
  - host `localhost`
  - port `1433`
  - user `sa`
  - password `root2026!`
- The default SQL Server URL in the harness is:
  - `jdbc:sqlserver://localhost:1433;databaseName=master;encrypt=false;trustServerCertificate=true`

```bash
docker pull mcr.microsoft.com/mssql/server:2022-latest
```

```bash
docker run -d --name conveyor-sqlserver \
  -p 1433:1433 \
  -e ACCEPT_EULA=Y \
  -e MSSQL_SA_PASSWORD='root2026!' \
  mcr.microsoft.com/mssql/server:2022-latest
```

### Run The Tests
```bash
mvn -pl conveyor-persistence/conveyor-persistence-jdbc-sqlserver -am test
```

### Alternate Host Or Port
```bash
SQLSERVER_HOST=db.example.internal SQLSERVER_PORT=14330 SQLSERVER_USER=sa SQLSERVER_PASSWORD='root2026!' \
mvn -pl conveyor-persistence/conveyor-persistence-jdbc-sqlserver -am test
```

### Direct URL Override
```bash
SQLSERVER_URL='jdbc:sqlserver://db.example.internal:14330;databaseName=master;encrypt=false;trustServerCertificate=true' SQLSERVER_USER=sa SQLSERVER_PASSWORD='root2026!' \
mvn -pl conveyor-persistence/conveyor-persistence-jdbc-sqlserver -am test
```

## Notes
- If Maven previously cached a failed snapshot lookup, rerun with `-U`.
- When a container has just started, wait until it is ready before launching the tests.
- If you run one helper module from inside its own directory, install the current snapshots first or run from the repository root with `-am`.
