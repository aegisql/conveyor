# conveyor-persistence-jdbc-mysql Instructions

## Purpose
- MySQL helper module for JDBC persistence.
- Owns the MySQL-specific engine class and MySQL driver dependency so the base JDBC module stays less vendor-coupled.

## Read First
- `../conveyor-persistence-jdbc/AGENTS.md`
- `../conveyor-persistence-jdbc/doc/known-invariants.md`
- MySQL tests under `src/test/java`

## Local Rules
- Keep MySQL-specific URL handling and driver-dependent tests here, not in the base JDBC module.
- Builder behavior is still defined by `JdbcPersistenceBuilder` in the base module; this module supplies the optional engine implementation behind the `mysql` engine type.
- Keep MySQL tests here when they depend on the MySQL driver or a live MySQL instance.

## Validation
- `mvn -pl conveyor-persistence/conveyor-persistence-jdbc-mysql -am test`
- If changing the builder handoff, also validate the base JDBC, configurator, and service modules.
