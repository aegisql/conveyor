# JDBC Initialization

JDBC persistence initialization is now supported through:

- runtime initialization via `JdbcPersistenceBuilder.init()`
- offline SQL generation via `JdbcPersistenceBuilder.initializationScript(...)`
- the persistence UI, which exposes both SQL and equivalent Java initialization previews

The supported user-facing initialization path is now the persistence UI.

The generated SQL follows the same step order as builder `init()`:

1. create database
2. create schema
3. create parts table
4. create parts-table index
5. create completed-log table
6. create configured unique indexes

This keeps the runtime path and the reviewable SQL path aligned for controlled environments.

## Builder API

Generate SQL directly from the builder:

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

- the script is engine-aware and uses the current engine DDL methods
- the cleanup section is optional and always commented out
- some engines cannot express every runtime creation step as portable SQL
  - Derby database creation is driven by the JDBC URL
  - Oracle service/schema creation remains DBA-managed

## Persistence UI

The persistence UI now owns the interactive initialization workflow.

For JDBC profiles, the UI exposes:

- generated SQL
- equivalent Java builder code

See:

- `conveyor-persistence/conveyor-persistence-ui/doc/project-context.md`
- `conveyor-persistence/conveyor-persistence-ui/doc/user-manual.md`

## Example Generated SQL

Engine-specific generated SQL examples remain available in the helper modules:

- `conveyor-persistence/conveyor-persistence-jdbc-derby/doc/examples/init/derby-init.sql`
- `conveyor-persistence/conveyor-persistence-jdbc-mariadb/doc/examples/init/mariadb-init.sql`
- `conveyor-persistence/conveyor-persistence-jdbc-mysql/doc/examples/init/mysql-init.sql`
- `conveyor-persistence/conveyor-persistence-jdbc-oracle/doc/examples/init/oracle-init.sql`
- `conveyor-persistence/conveyor-persistence-jdbc-postgres/doc/examples/init/postgres-init.sql`
- `conveyor-persistence/conveyor-persistence-jdbc-sqlite/doc/examples/init/sqlite-init.sql`
- `conveyor-persistence/conveyor-persistence-jdbc-sqlserver/doc/examples/init/sqlserver-init.sql`

## Admin Extension Points

The generated script is intended as a starting point for controlled environments.
DBAs can extend it with:

- grants and roles
- tablespace or storage directives
- index tuning
- engine-specific maintenance options
- environment-specific cleanup or migration notes

## Current Boundaries

- initialization script generation is limited to the built-in engine support in the JDBC module
- the optional cleanup section is generated as comments only
- the script mirrors builder `init()` order, but does not promise uniform `IF NOT EXISTS` behavior on every engine
- Oracle service and user/schema provisioning remain outside Conveyor-managed SQL
