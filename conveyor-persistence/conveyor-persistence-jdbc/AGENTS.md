# conveyor-persistence-jdbc Instructions

## Purpose
- JDBC-backed persistence implementation, SQL engines, statement executors, and JDBC archivers.

## Read First
- `./doc/project-context.md`
- `./doc/known-invariants.md`
- Engine-specific tests under `src/test/java`

## Local Rules
- Engine tests are the source of truth for observable DB behavior.
- Keep pooled/shared connection semantics explicit; do not collapse them into one ambiguous path.
- Keep generated test databases inside `test-dbs`.
- Derby-specific compatibility behavior must be preserved unless corresponding tests are updated intentionally.

## Validation
- `mvn -pl conveyor-persistence/conveyor-persistence-jdbc -am test`
- For DB-specific changes, run the affected engine tests directly.
