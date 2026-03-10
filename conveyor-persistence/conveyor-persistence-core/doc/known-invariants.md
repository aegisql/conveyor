# conveyor-persistence-core Known Invariants

- `Persistence<K>` is the abstraction that downstream JDBC implementations must satisfy.
- Persistence tracks both parts and completed keys; restore/archive behavior depends on both (inferred from interface and tests).
- `PersistentConveyor` and archivers depend on converters remaining consistent for persisted data.
- Generated test artifacts for this module live under `test-artifacts` and are cleaned by Maven clean (inferred from POM/test setup).
