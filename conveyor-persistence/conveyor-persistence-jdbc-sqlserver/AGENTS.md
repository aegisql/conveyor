# conveyor-persistence-jdbc-sqlserver Instructions

## Scope
- This module provides the Microsoft SQL Server-specific JDBC helper extracted from the base JDBC persistence module.
- It owns the SQL Server engine implementation and SQL Server-only tests.

## Read First
- `../AGENTS.md`
- `../conveyor-persistence-jdbc/AGENTS.md`

## Local Rules
- Keep this module focused on SQL Server-specific behavior only.
- Shared JDBC builder, connectivity, and persistence contracts remain in `conveyor-persistence-jdbc`.
- Do not reintroduce direct SQL Server engine code back into the base JDBC module.
- Current implementation is database-oriented and does not claim full custom-schema routing support beyond the login's default schema.

## Validation
- `mvn -pl conveyor-persistence/conveyor-persistence-jdbc-sqlserver -am test`
- If changing builder handoff, also validate `conveyor-configurator` and `conveyor-service`.
