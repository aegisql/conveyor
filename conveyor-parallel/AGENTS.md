# conveyor-parallel Instructions

## Purpose
- Parallel/distribution layer over `conveyor-core` conveyors.

## Read First
- `../doc/architecture-notes.md`
- `../conveyor-core/doc/known-invariants.md`
- `src/test/java/com/aegisql/conveyor/parallel/*Test.java`

## Local Rules
- Do not change balancing or shutdown semantics without reading the corresponding tests first.
- `completeAndStop()` behavior is test-defined and affects higher-level modules.
- Keep this module focused on composition/orchestration; core API changes belong in `conveyor-core`.

## Validation
- `mvn -pl conveyor-parallel -am test`
