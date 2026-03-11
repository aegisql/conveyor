# conveyor-persistence-jdbc-derby Instructions

## Purpose
- Derby helper module for JDBC persistence.
- Owns Derby-specific engine classes and Derby driver dependencies so the base JDBC module stays less vendor-coupled.

## Read First
- `../conveyor-persistence-jdbc/AGENTS.md`
- `../conveyor-persistence-jdbc/doc/known-invariants.md`
- Derby tests under `src/test/java`

## Local Rules
- Keep Derby-specific URL handling and compatibility behavior here, not in the base JDBC module.
- Builder behavior is still defined by `JdbcPersistenceBuilder` in the base module; this module supplies the optional engine implementation behind the `derby`, `derby-client`, and `derby-memory` engine types.
- Keep Derby tests here when they depend on Derby-only behavior or Derby driver availability.

## Validation
- `mvn -pl conveyor-persistence/conveyor-persistence-jdbc-derby -am test`
- If changing the builder handoff, also validate the base JDBC, configurator, and service modules.
