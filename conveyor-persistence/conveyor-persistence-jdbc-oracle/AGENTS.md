# conveyor-persistence-jdbc-oracle Instructions

## Scope
- This module provides the Oracle-specific JDBC helper extracted from the base JDBC persistence module.
- It owns the Oracle engine implementation and Oracle-only tests.

## Read First
- `../AGENTS.md`
- `../conveyor-persistence-jdbc/AGENTS.md`

## Local Rules
- Keep this module focused on Oracle-specific behavior only.
- Shared JDBC builder, connectivity, and persistence contracts remain in `conveyor-persistence-jdbc`.
- Do not reintroduce direct Oracle engine code back into the base JDBC module.
- Oracle currently assumes an existing service and current-user schema. Do not assume the builder can create Oracle services or users automatically.

## Validation
- `mvn -pl conveyor-persistence/conveyor-persistence-jdbc-oracle -am test`
- If changing builder handoff, also validate `conveyor-configurator` and `conveyor-service`.
