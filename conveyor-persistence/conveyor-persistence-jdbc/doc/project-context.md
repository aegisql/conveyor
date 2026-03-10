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
- Support multiple databases: Derby, SQLite, MySQL, PostgreSQL, MariaDB (inferred from dependencies and tests).
- Manage archive operations, completed logs, and restore ordering.
- Provide engine-specific DDL/DML and statement execution strategies.

## Key Tests
- `DerbyEngineTest`
- `SqliteEngineTest`
- `DerbyPersistenceTest`
- `SqlitePersistenceTest`
- `ConnectionFactoryTest`
- `FileArchiverTest`
