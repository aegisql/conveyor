# conveyor-core Known Invariants

- Loaders are documented and used as immutable request builders (found in wiki, code, and tests).
- `Conveyor` exposes part/static-part/build/future/command/result-consumer/scrap-consumer loaders as stable entry points.
- `ProductBin` and `ScrapBin` carry metadata needed by consumer chains; changing them ripples into consumers and service integration.
- `completeThenForceStop(timeout, unit)` waits first, then force-stops on timeout/failure (inferred from code).
- JDBC result consumers distinguish shared-connection and pooled-connection execution paths (inferred from code and wiki).
- The service client uses JDK HTTP APIs but logs through SLF4J (inferred from code and docs).
