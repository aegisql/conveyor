# conveyor-persistence-jdbc-mariadb Instructions

## Scope
- This module provides the MariaDB-specific JDBC helper extracted from the base JDBC persistence module.
- It owns the MariaDB engine implementation and MariaDB-only tests.

## Read First
- `/Users/mike/work/conveyor/conveyor-persistence/AGENTS.md`
- `/Users/mike/work/conveyor/conveyor-persistence/conveyor-persistence-jdbc/AGENTS.md`

## Local Rules
- Keep this module focused on MariaDB-specific behavior only.
- Shared JDBC builder, connectivity, and persistence contracts remain in `conveyor-persistence-jdbc`.
- Do not reintroduce direct MariaDB engine code back into the base JDBC module.

## Validation
- `mvn -pl conveyor-persistence/conveyor-persistence-jdbc-mariadb -am test`
