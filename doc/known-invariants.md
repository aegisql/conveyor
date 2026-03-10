# Known Invariants

- Loader APIs are the main user-facing submission mechanism and are treated as public surface by code, wiki, and tests.
- Generated test databases/artifacts are expected to stay in module-local cleanup directories (`test-artifacts`, `test-dbs`) and be removable with `mvn clean` (inferred from module POMs/tests).
- Release automation updates both the Maven project version and the root `project.version` property (inferred from `scripts/release.sh`).
