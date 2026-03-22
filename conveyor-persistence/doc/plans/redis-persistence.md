# Redis Persistence Research And Plan

## Verdict

- Do not implement Redis as another `engineType` inside `conveyor-persistence-jdbc`.
- Create a sibling module instead:
  - `conveyor-persistence-redis`
- Reuse the persistence SPI from `conveyor-persistence-core`.
- Do not force Redis through a fake SQL/table abstraction.

This is an architectural recommendation inferred from current code, tests, and official Redis documentation.

## Why The JDBC Module Is The Wrong Layer

The current JDBC persistence implementation is explicitly relational, not just "simple storage with a connection".

Current evidence in code:

- `conveyor-persistence/conveyor-persistence-jdbc/src/main/java/com/aegisql/conveyor/persistence/jdbc/builders/JdbcPersistenceBuilder.java`
  - `init()` creates database, schema, part table, indexes, and completed-log table.
- `conveyor-persistence/conveyor-persistence-jdbc/src/main/java/com/aegisql/conveyor/persistence/jdbc/engine/EngineDepo.java`
  - requires database/schema/table/index existence checks and creation operations.
- `conveyor-persistence/conveyor-persistence-jdbc/src/main/java/com/aegisql/conveyor/persistence/jdbc/engine/GenericEngine.java`
  - builds SQL DDL/DML directly.
  - assumes `CREATE TABLE`, `CREATE INDEX`, `INSERT`, `SELECT`, `ORDER BY`, and `UPDATE ... SET ARCHIVED = 1`.
- `conveyor-persistence/conveyor-persistence-jdbc/src/main/java/com/aegisql/conveyor/persistence/jdbc/JdbcPersistence.java`
  - depends on completed-key storage, static-part reads, expired-part reads, archive operations, and restore ordering.

Redis can satisfy the persistence SPI, but it does not naturally satisfy the JDBC engine contract.

## Redis Capabilities Relevant To This Design

Official Redis documentation confirms the relevant primitives:

- Java integration is client-based, not JDBC:
  - [Jedis Java connect docs](https://redis.io/docs/latest/develop/clients/jedis/connect/)
- Redis scripting/functions can maintain a data model across multiple keys and data structures, with atomic execution:
  - [Redis programmability](https://redis.io/docs/latest/develop/programmability/)
- Hashes fit record-like metadata:
  - [Redis hashes](https://redis.io/docs/latest/develop/data-types/hashes/)
- Sorted sets fit ordered and expiration indexes:
  - [Redis sorted sets](https://redis.io/docs/latest/develop/data-types/sorted-sets/)
- Key expiration is native:
  - [EXPIRE](https://redis.io/docs/latest/commands/expire/)
- Multi-key transactional behavior exists, but clustered behavior needs care:
  - [WATCH](https://redis.io/docs/latest/commands/watch/)
  - [EXEC](https://redis.io/docs/latest/commands/exec/)
- If Redis is used as persistence, durability settings matter:
  - [Redis data persistence](https://redis.io/docs/latest/operate/rc/databases/configuration/data-persistence/)

## Recommended Module Shape
Create a new module:
- `conveyor-persistence-redis`

Main classes:
- `RedisPersistence<K>`
- `RedisPersistenceBuilder<K>`
- Redis-specific archiver implementation, or reuse existing archiver concepts where possible

Reuse:
- `conveyor-persistence/conveyor-persistence-core`

Do not reuse directly:
- `EngineDepo`
- `GenericEngine`
- SQL/table/index initialization flow from `JdbcPersistenceBuilder`

## Recommended Redis Data Model
Use a namespace, not a schema.

Recommended namespace form:
- `conv:{name}:...`

The `{name}` hash tag keeps related keys colocated if cluster support is added later.

Suggested key layout:
- `conv:{name}:seq`
  - `INCR` source for `nextUniquePartId()`
- `conv:{name}:part:{id}:meta`
  - Redis hash for:
    - cart key
    - label
    - load type
    - creation time
    - expiration time
    - priority
    - value type
    - properties
    - archived flag
    - additional fields
- `conv:{name}:part:{id}:payload`
  - binary payload for cart value
- `conv:{name}:parts:active`
  - sorted set of active part ids
- `conv:{name}:parts:static`
  - sorted set of static part ids
- `conv:{name}:parts:by-key:{cartKey}`
  - sorted set of part ids for a single cart key
- `conv:{name}:parts:expires`
  - sorted set of part ids scored by expiration timestamp
- `conv:{name}:completed`
  - set of completed cart keys
- `conv:{name}:meta`
  - module version and init marker

## Operation Mapping
Proposed SPI mapping:
- `nextUniquePartId()`
  - `INCR conv:{name}:seq`
- `savePart(...)`
  - one Lua script or Redis Function updates:
    - part metadata
    - part value
    - active/static membership
    - per-key index
    - expiration index
- `getAllPartIds(key)`
  - `ZRANGE conv:{name}:parts:by-key:{key}`
- `getAllParts()`
  - `ZRANGE conv:{name}:parts:active` then fetch each part
- `getAllStaticParts()`
  - `ZRANGE conv:{name}:parts:static`
- `getExpiredParts()`
  - `ZRANGEBYSCORE conv:{name}:parts:expires -inf now`
- `saveCompletedBuildKey(key)`
  - `SADD conv:{name}:completed key`
- `getCompletedKeys()`
  - `SMEMBERS conv:{name}:completed`
- `archiveKeys/archiveParts/archiveExpiredParts`
  - Redis script/function updates all related keys atomically

## Payload Protection Direction

- Redis payload protection should reuse the shared persistence-core encryption path rather than inventing a Redis-only mechanism.
- Current implementation direction:
  - protect serialized cart value payload bytes only
  - leave Redis key/index encoding deterministic in v1
  - keep indexed-key protection as a later design decision
- This matches the current JDBC direction after the shared encryption core was modernized:
  - managed default `AES/GCM/NoPadding`
  - legacy-default decrypt fallback for historical payloads
  - per-operation cipher creation instead of a shared mutable cipher instance

## Restore Order
Current JDBC behavior supports:
- `NO_ORDER`
- `BY_ID`
- `BY_PRIORITY_AND_ID`

Current Redis implementation:
- v1:
  - supports `BY_ID` natively with sorted-set score = id
  - supports `NO_ORDER` as backend iteration order with no extra re-sorting
  - supports `BY_PRIORITY_AND_ID` by sorting in Java after fetch
- future optimization only if needed:
  - add an optimized priority+id index if performance requires it

This is a deliberate tradeoff to reduce index complexity in the first implementation.

## Unique Fields
Current JDBC persistence can create unique indexes.

Redis equivalent is not automatic.

Decision:
- Redis persistence will not implement `uniqueFields`.
- This is an intentional feature boundary, not a deferred parity item.
- If users need database-enforced uniqueness semantics, they should choose a JDBC backend that naturally supports unique indexes and constraints.

Rationale:
- Redis does not provide this semantic naturally in the same way relational databases do.
- Recreating it in the library would add significant complexity and race-handling burden for a feature that is already well served by JDBC backends.
- The Redis backend should stay Redis-native rather than imitating relational constraints where that stops being a natural fit.

This difference should be explicit in docs, plans, and backend-selection guidance.

## Archive Strategy Analysis

- `DELETE`
  - clean Redis fit
  - already implemented
- `NO_ACTION`
  - clean fit
  - implemented
- `MOVE_TO_PERSISTENCE`
  - feasible and meaningful for Redis
  - implemented through Redis-specific low-level archive hooks
- `MOVE_TO_FILE`
  - feasible and meaningful for Redis
  - implemented through the same Redis-specific low-level archive hooks
- `CUSTOM`
  - feasible with caveats
  - safe only when the custom implementation has Redis-aware low-level cleanup, or when it is truly terminal/no-op
- `SET_ARCHIVED`
  - technically possible but a poor fit for the current Redis model
  - should not be treated as required parity

Recommended direction:

- keep `DELETE` as the primary Redis archive strategy
- support `NO_ACTION` where users want Redis persistence to remain untouched
- support `MOVE_TO_PERSISTENCE` and `MOVE_TO_FILE` through Redis-specific low-level archive hooks
- treat `CUSTOM` as an expert path that still needs Redis-aware cleanup discipline
- treat `SET_ARCHIVED` as a non-goal unless a strong Redis-native requirement appears

## Initialization Semantics
For Redis, `autoInit(true)` should be reinterpreted.

It should mean:
- validate connection and authentication
- validate namespace configuration
- create metadata marker if missing
- initialize sequence key if missing
- load/register scripts or functions
- validate Redis version and required commands/features

It should not mean:
- create database
- create schema
- create table
- create index

For Redis, "schema initialization" becomes:
- namespace bootstrap
- script/function bootstrap
- compatibility validation

## Operational Assumptions
If Redis is used as persistence, it cannot be treated like an evictable cache.

Recommended assumptions:
- dedicated Redis deployment or dedicated namespace/service
- persistence enabled if durability is required
- no-eviction policy for persisted conveyor data

Otherwise, completed-log and stored-cart correctness can be lost.

## Phased Plan
### Phase 0: Design Spike
- Create `conveyor-persistence-redis`.
- Freeze first-scope behavior:
  - standalone Redis only
  - no cluster support in v1
  - `BY_ID` required
  - `DELETE` archive required
  - keep `DELETE` as the required default archive strategy
  - treat `SET_ARCHIVED` as a non-goal for the current Redis model
  - explicitly document that Redis does not support `uniqueFields`

### Phase 1: Minimal Working Backend
- Implement:
  - `nextUniquePartId`
  - `savePart`
  - `getParts`
  - `getAllPartIds`
  - `getAllParts`
  - `getAllStaticParts`
  - `saveCompletedBuildKey`
  - `getCompletedKeys`
  - `archiveKeys`
  - `archiveParts`
  - `archiveExpiredParts`
  - `archiveAll`
- Add Redis-native init/bootstrap.
- Reuse the shared payload-protection abstraction for optional payload encryption.

### Phase 2: Integration Tests
- Add Docker-backed integration tests for Redis.
- Validate:
  - save/restore
  - static parts
  - completed keys
  - expiration
  - archive behavior
  - restore order

### Phase 3: Advanced Parity
- optimized `BY_PRIORITY_AND_ID`
- cluster support
- broader archiver support where it fits Redis naturally:
  - `NO_ACTION`
  - `MOVE_TO_PERSISTENCE`
  - `MOVE_TO_FILE`
  - `CUSTOM` with Redis-aware cleanup discipline

## What Not To Do
- Do not add `redis` as another `JdbcPersistenceBuilder.engineType(...)`.
- Do not create fake schema/table/index APIs over Redis.
- Do not hide semantic differences such as the lack of Redis `uniqueFields` support.
- Do not target Redis Cluster in v1.

## Open Questions
- Should cart values be stored as binary strings directly, or split into metadata hash plus separate binary value key as proposed here?
- Should standalone Redis logical database selection be supported, or should the builder use only URI + namespace?
- Is Redis intended here as durable persistence, fast recovery cache, or both?

## Bottom Line
Redis support is feasible, but it should be implemented as a Redis-native persistence backend.

The clean path is:
- reuse the persistence SPI
- create `conveyor-persistence-redis`
- redefine initialization around namespace/script bootstrap
- avoid forcing Redis through the JDBC engine layer
