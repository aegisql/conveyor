# conveyor-accelerators Instructions

## Purpose
- Holds specialized conveyor implementations that are useful extensions of the framework but do not need to stay inside `conveyor-core`.
- Depends on `conveyor-core`; `conveyor-core` must not depend back on this module.

## Read First
- `../AGENTS.md`
- `../doc/project-context.md`
- `./doc/project-context.md`
- `./doc/known-invariants.md`

## Local Rules
- Preserve behavioral compatibility for extracted classes unless the move explicitly changes their contract.
- Prefer keeping package names stable when moving classes out of `conveyor-core` unless a rename is explicitly approved.
- Keep this module focused on specialized conveyor implementations and closely related builders/tests.
- Avoid introducing dependencies on service, persistence, or configurator modules from here.

## Validation
- Default: `mvn -pl conveyor-accelerators -am test`
- If you move code out of `conveyor-core`, also run `mvn -pl conveyor-core -am test`.
