# conveyor-persistence-core Instructions

## Purpose
- Persistence SPI, `PersistentConveyor`, converters, archive strategies, and persistence utilities.

## Read First
- `../../doc/architecture-notes.md`
- `./doc/project-context.md`
- `../../../conveyor.wiki/3.-Consumers.md`

## Local Rules
- Keep this module database-neutral; JDBC-specific behavior belongs in `conveyor-persistence-jdbc`.
- Converter behavior is compatibility-sensitive. Avoid silent format changes without clear migration intent.
- `Persistence<K>` is a downstream contract for JDBC and configurator code.

## Validation
- `mvn -pl conveyor-persistence/conveyor-persistence-core -am test`
