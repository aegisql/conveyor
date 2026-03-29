# JDBC Initialization

This module now supports two complementary initialization paths:

- runtime initialization through `JdbcPersistenceBuilder.init()`
- offline SQL generation through `JdbcPersistenceBuilder.initializationScript(...)`

The generated script follows the same step order as builder `init()`:

1. create database
2. create schema
3. create parts table
4. create parts-table index
5. create completed-log table
6. create configured unique indexes

This is intended to improve the handoff from development to controlled environments where database administrators prefer to review and extend SQL before it is applied.

## Builder API

Generate a script directly from the builder:

```java
JdbcPersistenceBuilder<Long> builder =
    JdbcPersistenceBuilder.presetInitializer("postgres", Long.class)
        .database("conveyor_db")
        .schema("conveyor_db")
        .partTable("PART")
        .completedLogTable("COMPLETED_LOG")
        .addField(Long.class, "TRANSACTION_ID")
        .addUniqueFields("TRANSACTION_ID");

String script = builder.initializationScript(
    new JdbcInitializationScriptOptions(true, "conveyor-init.sql")
);
```

Notes:

- The script is engine-aware and uses the current engine DDL methods.
- The cleanup section is optional and always commented out.
- Some engines cannot express every runtime creation step as portable SQL.
  Example:
  - Derby database creation is driven by the JDBC URL
  - Oracle service/schema creation is intentionally left to DBA-managed setup

## Standalone App

Use:

- `com.aegisql.conveyor.persistence.jdbc.InitConveyorPersistence`

The app supports built-in JDBC engine types only:

- `derby`
- `derby-client`
- `derby-memory`
- `mysql`
- `mariadb`
- `oracle`
- `sqlserver`
- `postgres`
- `sqlite`
- `sqlite-memory`

Modes:

- `script`
  - generate SQL only
  - stdout by default
  - write to file if `--output` is provided
- `init`
  - execute initialization only

## Runtime Options

```text
InitConveyorPersistence --mode script --type postgres --key-class java.lang.Long \
  --database conveyor_db --schema conveyor_db \
  --part-table PART --completed-log-table COMPLETED_LOG \
  --field java.lang.Long,TRANSACTION_ID \
  --unique-field TRANSACTION_ID \
  --include-cleanup
```

## YAML Configuration

Example file:

- `conveyor-persistence/conveyor-persistence-jdbc/doc/examples/init/postgres-init.yml`

Engine-specific helper-module examples:

- `conveyor-persistence/conveyor-persistence-jdbc-derby/doc/examples/init/derby-init.yml`
- `conveyor-persistence/conveyor-persistence-jdbc-derby/doc/examples/init/derby-init.sql`
- `conveyor-persistence/conveyor-persistence-jdbc-mariadb/doc/examples/init/mariadb-init.yml`
- `conveyor-persistence/conveyor-persistence-jdbc-mariadb/doc/examples/init/mariadb-init.sql`
- `conveyor-persistence/conveyor-persistence-jdbc-mysql/doc/examples/init/mysql-init.yml`
- `conveyor-persistence/conveyor-persistence-jdbc-mysql/doc/examples/init/mysql-init.sql`
- `conveyor-persistence/conveyor-persistence-jdbc-oracle/doc/examples/init/oracle-init.yml`
- `conveyor-persistence/conveyor-persistence-jdbc-oracle/doc/examples/init/oracle-init.sql`
- `conveyor-persistence/conveyor-persistence-jdbc-postgres/doc/examples/init/postgres-init.yml`
- `conveyor-persistence/conveyor-persistence-jdbc-postgres/doc/examples/init/postgres-init.sql`
- `conveyor-persistence/conveyor-persistence-jdbc-sqlite/doc/examples/init/sqlite-init.yml`
- `conveyor-persistence/conveyor-persistence-jdbc-sqlite/doc/examples/init/sqlite-init.sql`
- `conveyor-persistence/conveyor-persistence-jdbc-sqlserver/doc/examples/init/sqlserver-init.yml`
- `conveyor-persistence/conveyor-persistence-jdbc-sqlserver/doc/examples/init/sqlserver-init.sql`

The YAML file covers the initialization inputs that materially affect schema generation:

- engine type
- key class
- host / port / database / schema
- table names
- user / password
- JDBC properties
- additional fields
- unique indexes
- script mode and output settings

`uniqueFields` is intentionally a list of index definitions, where each definition is itself a list of field names.

Examples:

```yaml
uniqueFields:
  - [TRANSACTION_ID]
  - [ACCOUNT_ID, EXTERNAL_ID]
```

This means:

- one single-column unique index on `TRANSACTION_ID`
- one composite unique index on `(ACCOUNT_ID, EXTERNAL_ID)`

## Typical Flows

### Generate Script To Stdout

If `--output` is omitted, the SQL script is written to standard output.
This is the right mode when you want to:

- review the script in the terminal
- pipe it into another tool
- redirect it yourself

```bash
java -cp ... com.aegisql.conveyor.persistence.jdbc.InitConveyorPersistence \
  --config conveyor-persistence/conveyor-persistence-jdbc-postgres/doc/examples/init/postgres-init.yml
```

Examples:

```bash
java -cp ... com.aegisql.conveyor.persistence.jdbc.InitConveyorPersistence \
  --config conveyor-persistence/conveyor-persistence-jdbc-postgres/doc/examples/init/postgres-init.yml \
  | less
```

```bash
java -cp ... com.aegisql.conveyor.persistence.jdbc.InitConveyorPersistence \
  --config conveyor-persistence/conveyor-persistence-jdbc-postgres/doc/examples/init/postgres-init.yml \
  > conveyor-init.sql
```

### Generate Script To File

Use `--output` when you want the tool itself to write the file.
In this mode the SQL goes to the named file and the tool prints only a short confirmation message to standard output.

```bash
java -cp ... com.aegisql.conveyor.persistence.jdbc.InitConveyorPersistence \
  --config conveyor-persistence/conveyor-persistence-jdbc-postgres/doc/examples/init/postgres-init.yml \
  --output ./conveyor-init.sql
```

### Execute Initialization Directly

```bash
java -cp ... com.aegisql.conveyor.persistence.jdbc.InitConveyorPersistence \
  --mode init \
  --type derby-memory \
  --key-class java.lang.Long \
  --schema conveyor_db
```

## Admin Extension Points

The generated script is intended as a starting point for controlled environments.
DBAs can extend it with:

- grants and roles
- tablespace or storage directives
- index tuning
- engine-specific maintenance options
- environment-specific cleanup or migration notes

## Current Boundaries

- The standalone app is limited to built-in engine types.
- The app does not execute the optional cleanup section.
- The generated script mirrors the builder `init()` order, but does not promise full metadata-driven `IF NOT EXISTS` behavior on every engine.
- Oracle service and user/schema provisioning remain outside Conveyor-managed SQL.
