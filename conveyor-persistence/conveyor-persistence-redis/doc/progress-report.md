# Redis Persistence Progress Report

## Purpose

- Keep this file focused on live Redis status, remaining gaps, and next work.
- Do not restate implemented behavior that is already described clearly elsewhere.

Implemented behavior and current semantics live in:

- `conveyor-persistence/conveyor-persistence-redis/doc/project-context.md`
- `conveyor-persistence/README.md`
- `conveyor-persistence/doc/plans/redis-persistence.md`

## Current Status

- `conveyor-persistence-redis` is a usable v1 backend with Redis-native storage, recovery, archiving, shared encryption support, Lua-backed atomic save/delete paths, and basic MBean support.
- The `Persistence<K>` surface is implemented and backed by Redis-local tests.
- The module is suitable for development and targeted integration work.
- Redis is still behind the JDBC family in overall maturity and ecosystem depth.

## Active V1 Boundaries

- `uniqueFields` is an intentional Redis non-goal.
- `SET_ARCHIVED` is an intentional Redis non-goal.
- Move-style archive export is intentionally at-least-once at batch granularity.
- Redis Functions are deferred to v2; current v1 atomicity stays on Lua.
- Retry policy is client-owned.
  - the library does not add automatic retry for placement or move-style export flows
  - storage write failures surface through exceptional placement-future completion, consistent with JDBC

## Evidence Anchors

Primary Redis evidence is grouped in:

- learn/basic Redis behavior:
  - `conveyor-persistence/conveyor-persistence-redis/src/test/java/com/aegisql/conveyor/persistence/redis/`
- builder/bootstrap/runtime:
  - `conveyor-persistence/conveyor-persistence-redis/src/test/java/com/aegisql/conveyor/persistence/redis/RedisConnectionFactoryTest.java`
  - `conveyor-persistence/conveyor-persistence-redis/src/test/java/com/aegisql/conveyor/persistence/redis/RedisPersistenceBuilderTest.java`
  - `conveyor-persistence/conveyor-persistence-redis/src/test/java/com/aegisql/conveyor/persistence/redis/RedisBootstrapValidatorTest.java`
- persistence, recovery, restore order, converters, and archiving:
  - `conveyor-persistence/conveyor-persistence-redis/src/test/java/com/aegisql/conveyor/persistence/redis/RedisPersistenceTest.java`
  - `conveyor-persistence/conveyor-persistence-redis/src/test/java/com/aegisql/conveyor/persistence/redis/archive/`
- current performance slice:
  - `conveyor-persistence/conveyor-persistence-redis/src/test/java/com/aegisql/conveyor/persistence/redis/RedisPerfTest.java`

Cross-backend placement-future evidence lives in:

- `conveyor-persistence/conveyor-persistence-core/src/test/java/com/aegisql/conveyor/persistence/core/PersistentConveyorTest.java`

## Remaining Gaps Compared To JDBC

- Broader performance parity.
  - Redis has a representative initial perf slice, not the broader JDBC-style matrix.

- Broader operational tooling depth.
  - Redis now has logging and a basic MBean, but not the fuller operational surface accumulated on the JDBC side.

- Move-style export coordination beyond the current batched export/delete model.
  - Current semantics are acceptable for v1.
  - Remaining work here is operational bookkeeping and visibility, not archive singleness.

- Redis Cluster-safe semantics.
  - The backend is still effectively standalone-first.

- Broad ecosystem parity with JDBC.
  - This remains a multi-version effort rather than a short missing-feature list.

## Recovery Status

- Recovery proof is no longer a broad gap.
- The currently proven recovery matrix is described in:
  - `conveyor-persistence/conveyor-persistence-redis/doc/project-context.md`
- Additional recovery work is only needed when:
  - new Redis-specific orchestration is introduced, or
  - new forward-conveyor recovery semantics appear in core

## Ranked Backlog By Complexity

### Low Complexity

- Keep this report, the Redis project context, and the top-level persistence README aligned after Redis feature changes.

### Medium Complexity

- Extend performance coverage only where comparison to JDBC remains meaningful.
- Decide whether Redis-side batch manifests and processing status would materially improve move-style coordination.

### High Complexity

- Broaden the Redis operational surface beyond the current basic MBean/logging level.

### Very High Complexity

- Add Redis Cluster-safe semantics.
- Reach broader ecosystem parity with the JDBC family.

## Recommended Next Sequence

1. Decide later whether Redis-side batch manifests and processing status are worth adding for move-style coordination.
2. Extend performance coverage only where the comparison still helps Redis design choices.
3. Leave any Redis Function migration for v2.
