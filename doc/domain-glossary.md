# Domain Glossary

## Major Sub-Projects

- **`conveyor-core`**
  - Core framework module. Defines the main conveyor API, loader model, carts, bins, consumers, and utility helpers (found in code, tests, and wiki).
- **`conveyor-parallel`**
  - Extension module that composes multiple conveyors and distributes work across them (inferred from code and tests).
- **`conveyor-configurator`**
  - Configuration/factory module that builds named conveyors from properties and YAML files (found in wiki and configurator tests).
- **`conveyor-persistence-core`**
  - Database-neutral persistence SPI, persistent conveyor support, converters, and archive abstractions (inferred from code and tests).
- **`conveyor-persistence-jdbc`**
  - JDBC-backed persistence implementation with engine-specific SQL behavior for Derby, SQLite, MySQL, PostgreSQL, and MariaDB (inferred from code and tests).
- **`conveyor-service`**
  - Spring Boot application exposing conveyors through REST APIs, a dashboard UI, watchers, and admin operations (found in service docs and tests).

## Shared Framework Terms

- **Conveyor**
  - The core aggregation abstraction. It accepts carts and produces products or scraps (found in `Conveyor` interface and wiki).
- **Cart**
  - A unit of work submitted to a conveyor. Includes part carts, static parts, commands, futures, and result-consumer carts (inferred from `cart` package).
- **Key / ID**
  - The build identifier used to target one specific build instance inside a conveyor. In service and UI contexts it is often shown as `ID`; in core APIs it is the generic key type `K` (found in loaders, service endpoints, and tests).
- **Label**
  - The semantic name of a part or static part. Labels tell the builder what kind of input arrived and often drive readiness logic (found in loaders/wiki/tests).
- **Part**
  - A labeled input value targeted at a specific build key (found in loaders/wiki).
- **Static Part**
  - Conveyor-level value applied outside a single build key; typically configuration-like state for future builds (found in loaders/wiki).
- **Command**
  - A control/lifecycle message sent through `CommandLoader`. Commands are distinct from parts and static parts: they target build lifecycle or inspection behavior such as create, cancel, timeout, reschedule, peek, or completion overrides, and they are treated as high-priority conveyor inputs (found in `CommandLoader`, cart command classes, wiki, and tests).
- **Creation Time**
  - The timestamp associated with a cart or command when it is created for scheduling/expiration purposes. Other timing values such as TTL are applied relative to it (inferred from loader code).
- **TTL**
  - Time-to-live for a cart or command. In loader APIs, TTL is converted into an expiration time by adding it to the creation time (inferred from loader code and wiki).
- **Expiration Time**
  - Absolute time after which a cart or command is considered expired. Unlike TTL, it is provided directly as a timestamp or instant rather than computed relative to creation time (inferred from loader code and wiki).
- **Priority**
  - Scheduling weight attached to a cart-like submission. Parts, static parts, and several other loaders expose it directly; commands are separate high-priority control messages by design (found in loaders/wiki/tests).
- **Properties**
  - Arbitrary metadata attached to carts or commands through `addProperty(...)` / `addProperties(...)`. These values travel with the message and can influence routing, filtering, persistence, or consumer logic (found in loaders, service docs, and tests).
- **Foreach**
  - Loader targeting mode that applies a part or command to all active keys, or to all keys matching a predicate/filter, instead of a single key (found in loaders/wiki/tests).
- **Builder / BuilderSupplier**
  - The mutable assembly object or supplier that creates product instances (found in wiki and core code).
- **Product**
  - The assembled output value produced when readiness is reached.
- **ProductBin**
  - Wrapper for a completed product plus metadata such as key, status, and properties (inferred from core code and consumer APIs).
- **ScrapBin**
  - Wrapper for rejected or failed output plus metadata describing the failure/disposal path (inferred from core code and scrap consumers).
- **Loader**
  - Immutable helper object used to parameterize and submit parts, static parts, builders, futures, commands, and consumers (found in wiki and tests).
- **ResultConsumer / ScrapConsumer**
  - Post-processing hooks for successful and failed outputs respectively (found in wiki and code).
- **ConveyorInitiatingService**
  - SPI used for runtime conveyor initialization/discovery, including service extension loading (found in core/service code).
- **MBean / JMX**
  - Management surface exposed by conveyors, persistence, and dashboard administration (inferred from code and service docs).

## Parallel, Configuration, and Persistence Terms

- **Parallel Conveyor**
  - Wrapper that distributes or balances work across multiple child conveyors (parallel module).
- **Configurator**
  - The properties/YAML-driven factory that builds named conveyors from configuration files (configurator module).
- **Persistence**
  - The persistence SPI that stores carts, completed keys, and related metadata for recovery/archive flows (inferred from `Persistence<K>` and tests).
- **PersistentConveyor**
  - Conveyor variant backed by a `Persistence<K>` implementation (inferred from persistence-core code/tests).
- **Archiver**
  - Strategy object that removes, marks, or exports persisted records after completion or expiry (persistence modules).

## `conveyor-service` Terms

- **Watch / Watcher**
  - Service-side stream of conveyor or admin events shown in the dashboard output panel or over WebSocket (service docs/tests).
- **Request TTL**
  - Service-side HTTP parameter used to wait for completion before responding; absence allows `202 Accepted` async scheduling behavior (inferred from controller tests).
