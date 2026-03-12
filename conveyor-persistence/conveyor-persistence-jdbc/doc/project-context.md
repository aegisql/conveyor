# conveyor-persistence-jdbc Context

## Purpose
- JDBC-backed implementation of the persistence SPI, including database-engine adapters and JDBC archivers.

## Main Entry Points
- `JdbcPersistence<K>`
- `JdbcPersistenceBuilder`
- engine types under `engine/*`
- connectivity helpers under `engine/connectivity`
- archivers under `archive/*`

## Responsibilities
- Translate carts and metadata into relational tables.
- Support JDBC persistence through the shared builder/runtime; Derby, SQLite, MySQL, MariaDB, Oracle, PostgreSQL, and Microsoft SQL Server support are provided through companion helper modules (inferred from current module structure and tests).
- Manage archive operations, completed logs, and restore ordering.
- Provide engine-specific DDL/DML and statement execution strategies.

## Key Tests
- `ConnectionFactoryTest`
- `FileArchiverTest`
