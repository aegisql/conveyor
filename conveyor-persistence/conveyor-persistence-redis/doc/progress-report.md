# Redis Persistence Progress Report

## Purpose

- This file tracks the live status of Redis persistence as a work program.
- It should focus on remaining gaps, open decisions, and the current evidence baseline.
- Do not repeat implemented behavior here when it is already described clearly elsewhere.

Implemented behavior and current semantics live in:

- `/Users/mike/work/conveyor/conveyor-persistence/conveyor-persistence-redis/doc/project-context.md`
- `/Users/mike/work/conveyor/conveyor-persistence/README.md`
- `/Users/mike/work/conveyor/conveyor-persistence/doc/plans/redis-persistence.md`

## Current Status

- `conveyor-persistence-redis` is a real backend, not just a design spike.
- The `Persistence<K>` surface is implemented and covered by Redis-local tests.
- Redis-specific bootstrap, Lua-backed atomic paths, payload encryption reuse, restore-order configuration, and Redis-native archivers are implemented.
- Basic Redis MBean registration is implemented.
- The module is usable for development and targeted integration work.
- The module is not yet at JDBC maturity.

Current v1 decisions:

- `uniqueFields` is an intentional Redis non-goal.
- Redis now supports JDBC-style `additionalFields` in a Redis-native metadata form.
  - selected fields are persisted in part metadata, not as relational constraints
  - stored field metadata is rehydrated into cart properties on Redis reads, so current move-style archivers preserve it
  - detailed design notes remain in `/Users/mike/work/conveyor/conveyor-persistence/doc/plans/redis-persistence.md`
- Redis now supports JDBC-style custom binary converter registration.
  - builder configuration mirrors JDBC `addBinaryConverter(...)` for class-based and label-based converters
  - the current Redis read/write path applies those converters to payload values and additional-field metadata
- `SET_ARCHIVED` is an intentional Redis non-goal.
- Move-style archive export is intentionally at-least-once at batch granularity.
- Redis Functions are deferred to v2; v1 stays on Lua.

## Evidence Anchors

Primary test evidence currently lives in:

- `/Users/mike/work/conveyor/conveyor-persistence/conveyor-persistence-redis/src/test/java/com/aegisql/conveyor/persistence/redis/LearnRedisConnectionTest.java`
- `/Users/mike/work/conveyor/conveyor-persistence/conveyor-persistence-redis/src/test/java/com/aegisql/conveyor/persistence/redis/LearnRedisStringCrudTest.java`
- `/Users/mike/work/conveyor/conveyor-persistence/conveyor-persistence-redis/src/test/java/com/aegisql/conveyor/persistence/redis/LearnRedisHashCrudTest.java`
- `/Users/mike/work/conveyor/conveyor-persistence/conveyor-persistence-redis/src/test/java/com/aegisql/conveyor/persistence/redis/LearnRedisLuaScriptTest.java`
- `/Users/mike/work/conveyor/conveyor-persistence/conveyor-persistence-redis/src/test/java/com/aegisql/conveyor/persistence/redis/RedisConnectionFactoryTest.java`
- `/Users/mike/work/conveyor/conveyor-persistence/conveyor-persistence-redis/src/test/java/com/aegisql/conveyor/persistence/redis/RedisPersistenceBuilderTest.java`
- `/Users/mike/work/conveyor/conveyor-persistence/conveyor-persistence-redis/src/test/java/com/aegisql/conveyor/persistence/redis/RedisBootstrapValidatorTest.java`
- `/Users/mike/work/conveyor/conveyor-persistence/conveyor-persistence-redis/src/test/java/com/aegisql/conveyor/persistence/redis/RedisPersistenceTest.java`
- `/Users/mike/work/conveyor/conveyor-persistence/conveyor-persistence-redis/src/test/java/com/aegisql/conveyor/persistence/redis/archive/RedisArchiverTest.java`
- `/Users/mike/work/conveyor/conveyor-persistence/conveyor-persistence-redis/src/test/java/com/aegisql/conveyor/persistence/redis/RedisPerfTest.java`

## What Still Needs To Be Done

### Remaining Gaps Compared To JDBC

- Builder/API convenience parity.
  - Redis now has the core converter-registration capability that JDBC exposes.
  - It still does not expose the same convenience surface around cart-property and label-persistence filters.

- Broader command-cart recovery coverage.
  - One recovered command path is proven.
  - Command behavior is not yet as broadly evidenced as the JDBC persistence stack.

- Move-style export coordination beyond the current batched export/delete model.
  - Current semantics are acceptable for Redis v1.
  - The remaining question is operational coordination, not singleness.
  - Redis-tracked batch manifests and processing status are still a possible future enhancement.

- Broader performance parity.
  - Redis has an initial compatible slice of the JDBC perf style.
  - It does not yet cover the broader JDBC-style perf matrix.

- Broader operational surface.
  - Redis now has basic MBean registration plus logging and test visibility.
  - It does not yet have the richer operational tooling shape that the JDBC side accumulated over time.

- Retry and reconnect policy.
  - Long-running Redis deployments will see interruptions.
  - The library should not grow blind retry behavior before the remaining move-style export semantics are considered stable enough.

- Redis Cluster-safe semantics.
  - The current implementation is still effectively standalone-first.

- Broad ecosystem parity with JDBC.
  - This remains a multi-version effort, not a short feature list.

### Recovery Status

Recovery is no longer a broad unknown.

What is already proven is described in:

- `/Users/mike/work/conveyor/conveyor-persistence/conveyor-persistence-redis/doc/project-context.md`

What remains is narrow:

- additional command-driven recovery behavior, if we decide that wider command proof matters for v1
- any future recovery semantics introduced later in core or in Redis-specific orchestration

## Ranked Backlog By Complexity

### Low Complexity

- Keep `/Users/mike/work/conveyor/conveyor-persistence/conveyor-persistence-redis/doc/project-context.md` and this report aligned after Redis feature changes.
- Keep the top-level persistence README aligned with Redis capability decisions.

### Medium Complexity

- Broaden command-cart recovery proof.
- Extend performance coverage toward the useful JDBC comparison cases.
- Decide whether Redis needs more explicit user-facing docs for the shared encryption builder surface.
- Keep the `JAVA_SORT` vs `REDIS_INDEX` choice documented as a user-side operational choice rather than a project-side benchmark target.

### High Complexity

- Decide whether Redis-tracked batch manifests and processing status are worth adding for move-style export coordination.
- Keep archive semantics and evidence aligned as move-style export evolves.
- Revisit whether library-level retry or reconnect behavior should exist at all.

### Very High Complexity

- Add Redis Cluster-safe semantics.
- Reach broad feature parity with the JDBC ecosystem.

## Recommended Next Sequence

1. Broaden command-cart recovery proof if we still want wider v1 recovery evidence.
2. Decide later whether Redis-tracked batch manifests and processing status would materially improve move-style export coordination.
3. Extend performance coverage only where comparison to JDBC remains meaningful for the current Redis design.
4. Revisit retry and reconnect policy only after move-style export semantics are considered settled enough.
5. Leave any Redis Function migration for v2.

## Bottom Line

- Redis persistence is in a solid v1 development state.
- The main remaining work is no longer basic CRUD or basic recovery.
- The main remaining work is maturity:
  - broader command recovery proof
  - move-style export coordination and bookkeeping
  - performance and operational depth
  - long-term cluster and ecosystem parity
