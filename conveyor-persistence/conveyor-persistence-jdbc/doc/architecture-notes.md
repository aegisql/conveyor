# conveyor-persistence-jdbc Architecture Notes

## Internal Shape
- `JdbcPersistence` is the main persistence implementation.
- `JdbcPersistenceBuilder` configures engine, schema/database, batching, archiving, converters, and filters.
- `EngineDepo`/engine classes isolate DB-specific SQL behavior.
- Connectivity helpers wrap driver-manager and external/pool data sources.
- Statement executors split caching and non-caching execution paths.

## Important Boundaries
- Core persistence semantics come from `conveyor-persistence-core`; this module should not redefine them.
- Database-specific behavior belongs in engine classes/tests, not in generic code paths.
- Connection lifecycle semantics differ intentionally between shared and pooled usage.
