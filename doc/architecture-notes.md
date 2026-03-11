# Architecture Notes

## Module Boundaries
- `conveyor-core` defines the public framework vocabulary and most downstream contracts.
- Current repository practice, reflected in the module graph, is that dependency direction flows outward from `conveyor-core`; downstream modules may depend on it, but `conveyor-core` should not depend on downstream sub-projects.
- `conveyor-parallel` depends on `conveyor-core` and composes multiple conveyors rather than redefining the core API.
- `conveyor-configurator` depends on core/parallel/persistence modules and is responsible for runtime assembly from config files.
- `conveyor-persistence-core` defines persistence SPI and byte-conversion/archive concepts.
- `conveyor-persistence-jdbc` specializes persistence-core for relational databases.
- `conveyor-service` is an adapter/application layer over the other modules.

## Cross-Cutting Patterns
- **Immutable loader pattern**
  - Call chains accumulate parameters and submit via `place()`, `create()`, `get()`, or command methods (found in wiki/tests).
- **Result/scrap bifurcation**
  - Success and failure paths are modeled separately via `ProductBin` and `ScrapBin` plus dedicated consumer types.
- **Named conveyor registry**
  - Configurator and service code rely on named conveyor lookup rather than direct dependency injection everywhere (inferred from tests and service/configurator code).
- **JMX exposure**
  - Management and admin operations lean on MBeans for runtime inspection and control (code and service dashboard behavior).
- **Persistence split**
  - Byte conversion/archive policy lives in persistence-core; SQL schema/engine behavior lives in persistence-jdbc.

## Shared Integration Points
- `Conveyor`, loader types, and consumer interfaces are the main cross-module contracts.
- `PersistentConveyor` bridges core and persistence modules.
- `ConveyorConfiguration` bridges config files to runtime module composition.
- `conveyor-service` bridges HTTP/UI concerns onto named conveyors and MBeans.
- `conveyor-core` now also contains the Java HTTP client for `conveyor-service` and the JDBC helper layer used by JDBC result consumers.

## Architectural Constraints
- Multiple modules share the `com.aegisql.conveyor` package root, so package changes can ripple across modules.
- `conveyor-core` should keep external dependencies to a minimum. Adding a new dependency there needs stronger justification than in adapter/application modules because it affects every downstream consumer.
- The repository contains both framework code and a runnable Spring application; not every module should inherit service-oriented assumptions.

## Notable Documentation/Structure Mismatches
- The wiki is still the broadest framework manual, but it does not fully reflect newer additions such as the Java HTTP client and some service/dashboard behavior.
- Root `doc` is mostly an image store today; narrative architecture docs were missing before this pass.
