# Configurator Capability Gaps

This document tracks configuration surface that exists in `conveyor-core`, `conveyor-parallel`, or persistence modules, but is not currently expressible as first-class configurator properties or YAML structure.

It should focus on concrete unsupported capability, not on hypothetical future design.

## Current Gaps

### Persistence

- Redis builder-owned persistence is now supported declaratively.
  - Current config can build Redis persistence through `RedisPersistenceBuilder`.
  - Covered Redis surface now includes:
    - `redisUri`
    - Redis restore-order strategy details
    - Redis pool and client tuning
    - Redis-native archive options
    - Redis additional fields and binary converters
  - Remaining Redis declarative gap is injected/external client ownership:
    - no first-class config path for a prebuilt `JedisPooled`
    - no declarative custom Redis client provider hook

- Generic/custom JDBC wiring is only partially supported.
  - The configurator supports the common preset JDBC flow.
  - It does not expose several advanced builder paths, including:
    - cart/label persistence filters
    - custom `ConnectionFactory`
    - custom `GenericEngine`
    - direct/external JDBC connection modes
    - non-DBCP pooled connection setup beyond the current boolean toggle
  - `type=jdbc` is therefore not fully declarative today.

### Parallel

- Parallel configuration is limited to:
  - `KBalancedParallelConveyor` by numeric factor
  - `LBalancedParallelConveyor` by named dependencies

- There is no first-class configurator support for:
  - `PBalancedParallelConveyor`
  - `TaskPoolConveyor`
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
  - `conveyor-parallel/src/main/java/com/aegisql/conveyor/parallel/PBalancedParallelConveyor.java`
  - `conveyor-accelerators/src/main/java/com/aegisql/conveyor/parallel/utils/task_pool_conveyor/TaskPoolConveyor.java`

- Core meta-info surface:
  - `conveyor-core/src/main/java/com/aegisql/conveyor/meta/ConveyorMetaInfoBuilder.java`

## Priority Order

If configurator work starts from the highest-impact missing capability, the practical order is:

1. `PBalanced` and task-pool support
2. generic/custom JDBC connection and engine wiring
3. injected/external Redis client ownership paths
4. meta-info completeness and named `postFailure`
