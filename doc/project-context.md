# Project Context

## What This Repository Is
- A Java 21, Maven-based multi-module codebase for the Conveyor framework and adjacent tooling (found in root `pom.xml`, wiki, and module POMs).
- Primary purpose: build asynchronous, stateful aggregators around a "conveyor" abstraction, then expose them through configuration, persistence, parallel composition, and an HTTP/dashboard service (found in wiki and code).

## Major Sub-Projects
- `conveyor-core`
  - Core conveyor API, loaders, carts, bins, consumers, utility conveyors, Java HTTP client, and JDBC result-consumer helpers (inferred from code and tests).
  - Foundational module. It can be used on its own; other modules extend or adapt it.
- `conveyor-parallel`
  - Parallel conveyor implementations and balancing strategies over multiple child conveyors (inferred from code and tests).
- `conveyor-configurator`
  - Properties/YAML-driven conveyor factory and persistence wiring (found in wiki and configurator tests).
- `conveyor-persistence/conveyor-persistence-core`
  - Persistence SPI, `PersistentConveyor`, archiving, binary converters, and persistence utilities (inferred from code and tests).
- `conveyor-persistence/conveyor-persistence-jdbc`
  - JDBC persistence implementation, DB-engine adapters, statement executors, and archivers (inferred from code and tests).
- `conveyor-service`
  - Spring Boot service exposing REST APIs, dashboard UI, WebSocket/watch flows, audit logging, and profile-driven authentication (found in module docs and tests).

## High-Level Structure
- In-process flow:
  - loaders create carts/commands -> conveyors aggregate state -> products or scraps are emitted -> consumers persist/log/forward outputs (found in wiki and core code).
- Optional persistence flow:
  - `PersistentConveyor` and JDBC persistence store carts and completed keys, then archive according to configured strategy (inferred from persistence code and tests).
- Configuration flow:
  - `ConveyorConfiguration` builds named conveyors from properties/YAML, including persistence and parallel wrappers (found in configurator tests).
- Service flow:
  - REST/dashboard requests hit Spring MVC controllers -> service layer -> named conveyors, watchers, MBeans, or extension uploads (found in service code/tests).

## External Integrations
- JDBC databases: Derby, SQLite, MySQL, PostgreSQL, MariaDB (module POMs and JDBC tests).
- Spring Security OAuth2/OIDC and JWT resource-server support in `conveyor-service` (service docs and tests).
- JMX/MBeans across conveyors and persistence/service management surfaces (inferred from code).
- Docker/demo packaging for `conveyor-service` (service docs).

## Tech Stack
- Java 21, Maven multi-module build (root `pom.xml`).
- SLF4J logging across code; concrete backend chosen by runtime/module (inferred from code and service config).
- Spring Boot 4 in `conveyor-service`.
- SnakeYAML and GraalJS in `conveyor-configurator`.
- JUnit 5 across modules; JaCoCo configured at root.

## Operational Assumptions
- Modules now keep generated test artifacts inside module-local cleanup directories such as `test-artifacts` and `test-dbs` (inferred from recent POM/test wiring).
- `conveyor-service` is the only module with container/runtime deployment packaging.
