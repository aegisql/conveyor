# Configurator Capability Gaps

This document tracks configuration surface that exists in `conveyor-core`, `conveyor-parallel`, or persistence modules, but is not currently expressible as first-class configurator properties or YAML structure.

Implemented support belongs in `project-context.md`, not here.

## Current Gaps

### Persistence

- Generic/custom JDBC wiring is only partially supported.
  - The configurator supports the common preset JDBC flow.
  - It does not expose several advanced builder paths, including:
    - cart/label persistence filters
    - custom `ConnectionFactory`
    - custom `GenericEngine`
    - direct/external JDBC connection modes
    - non-DBCP pooled connection setup beyond the current boolean toggle
  - `type=jdbc` is therefore not fully declarative today.

- Redis still lacks external-client wiring.
  - There is no first-class config path for:
    - a prebuilt `JedisPooled`
    - a custom Redis client provider hook

### Parallel

- There is no first-class configurator support for:
  - `ParallelConveyor.setBalancingCartAlgorithm(...)`
  - `ParallelConveyor.setBalancingCommandAlgorithm(...)`

- Wrapper order is fixed for factor-based parallel plus persistence.
  - The configurator currently builds the parallel conveyor first and then wraps it in `PersistentConveyor`.
  - It cannot declaratively choose alternative compositions such as persistent child lanes inside a balanced conveyor.

### Core And Meta Info

- `ConveyorMetaInfo` support is partial.
  - Current config covers:
    - `keyType`
    - `labelType`
    - `productType`
    - `supportedValueTypes`
  - It does not expose first-class configuration for:
    - explicit meta-info labels without supported types
    - meta-info builder supplier

- Named `postFailure` hooks are not supported.
  - Default/global `postFailure` is supported.
  - Named conveyor-level `postFailure` handling is missing, while named `postInit` is supported.

## Important Distinction

- Some missing capability is still reachable through Java-level escape hatches such as:
  - custom `supplier`
  - `JAVAPATH`
  - pre-built persistence registered by name
- Those are useful escape valves, but they are not equivalent to first-class declarative support.

## Evidence Anchors

- Configurator routing surface:
  - `conveyor-configurator/src/main/java/com/aegisql/conveyor/config/ConveyorConfiguration.java`
  - `conveyor-configurator/src/main/java/com/aegisql/conveyor/config/ConveyorBuilder.java`
  - `conveyor-configurator/src/main/java/com/aegisql/conveyor/config/PersistenceProperties.java`

- Persistence builder surface:
  - `conveyor-persistence/conveyor-persistence-jdbc/src/main/java/com/aegisql/conveyor/persistence/jdbc/builders/JdbcPersistenceBuilder.java`
  - `conveyor-persistence/conveyor-persistence-redis/src/main/java/com/aegisql/conveyor/persistence/redis/RedisPersistenceBuilder.java`
  - `conveyor-persistence/conveyor-persistence-core/src/main/java/com/aegisql/conveyor/persistence/core/PersistentConveyor.java`

- Parallel surface:
  - `conveyor-parallel/src/main/java/com/aegisql/conveyor/parallel/ParallelConveyor.java`

- Core meta-info surface:
  - `conveyor-core/src/main/java/com/aegisql/conveyor/meta/ConveyorMetaInfoBuilder.java`

## Priority Order

If configurator work starts from the highest-impact missing capability, the practical order is:

1. generic/custom JDBC connection and engine wiring
2. injected/external Redis client ownership paths
3. meta-info completeness and named `postFailure`
4. balancing-algorithm surface beyond current K/L/P-balanced declarative support
