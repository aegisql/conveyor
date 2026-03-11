# conveyor-persistence-jdbc-postgres Instructions

## Scope
- This module provides the PostgreSQL-specific JDBC helper extracted from the base JDBC persistence module.
- It owns the PostgreSQL engine implementation and PostgreSQL-only tests.

## Read First
- `../AGENTS.md`
- `../conveyor-persistence-jdbc/AGENTS.md`

## Local Rules
- Keep this module focused on PostgreSQL-specific behavior only.
- Shared JDBC builder, connectivity, and persistence contracts remain in `conveyor-persistence-jdbc`.
- Do not reintroduce direct PostgreSQL engine code back into the base JDBC module.

## Validation
- `mvn -pl conveyor-persistence/conveyor-persistence-jdbc-postgres -am test`
- If changing builder handoff, also validate `conveyor-configurator` and `conveyor-service`.
