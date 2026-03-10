# conveyor-persistence-jdbc Known Invariants

- Shared-connection and pooled-connection execution are distinct behaviors and should stay distinct (inferred from code and current docs/wiki).
- Engine tests define observable DB behavior and compatibility expectations.
- Derby engines preserve compatibility fallback between schema-based and database-based configuration paths (inferred from current code/tests).
- Module tests write generated DB files under `test-dbs` and Maven clean removes that directory (inferred from POM/test harness).
