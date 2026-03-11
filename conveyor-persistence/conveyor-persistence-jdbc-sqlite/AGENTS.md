# conveyor-persistence-jdbc-sqlite Instructions

## Purpose
- SQLite helper module for JDBC persistence.
- Owns SQLite-specific engine classes and the SQLite driver dependency so the base JDBC module stays less vendor-coupled.

## Read First
- `../conveyor-persistence-jdbc/AGENTS.md`
- `../conveyor-persistence-jdbc/doc/known-invariants.md`
- SQLite tests under `src/test/java`

## Local Rules
- Keep SQLite-specific connection tuning and URL handling here, not in the base JDBC module.
- Builder behavior is still defined by `JdbcPersistenceBuilder` in the base module; this module supplies the optional engine implementation behind the `sqlite` and `sqlite-memory` engine types.
- Keep SQLite tests here when they depend on `org.sqlite` types or SQLite-only behavior.

## Validation
- `mvn -pl conveyor-persistence/conveyor-persistence-jdbc-sqlite -am test`
- If changing the builder handoff, also validate the base JDBC and configurator modules.
