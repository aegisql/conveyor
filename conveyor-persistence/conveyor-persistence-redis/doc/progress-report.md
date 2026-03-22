# Redis Persistence Progress Report

## Purpose

- This is the living implementation status for `conveyor-persistence-redis`.
- Update this file when Redis behavior changes, when new tests prove parity, or when earlier assumptions are invalidated.
- Use `../doc/plans/redis-persistence.md` for the longer-term design direction.
- Use this file for the current evidence-based status.

## Snapshot

- The module exists and is wired into the persistence reactor.
- A first Redis-native `Persistence<K>` backend is implemented.
- The implementation is usable for local experiments and SPI-level testing.
- The implementation is not yet at JDBC parity.
- Payload encryption now uses the same modernized shared protection core as JDBC.
- The storage model now uses itemized cart metadata plus value payload bytes instead of storing the whole cart as one blob.
- Current Redis archiving now supports:
  - `DELETE`
  - `NO_ACTION`
  - `MOVE_TO_PERSISTENCE`
  - `MOVE_TO_FILE`
  - `SET_ARCHIVED` is intentionally unsupported
- Current Redis `savePart` writes are now Lua-backed atomic units.
- Current Redis delete-style archive and cleanup flows are now Lua-backed atomic units.
- Current Redis move-style archive flows still use Java orchestration to export carts before Redis-side cleanup.
- Current Redis restore behavior is now explicitly configurable and proven for:
  - `BY_ID`
  - `NO_ORDER`
  - `BY_PRIORITY_AND_ID` with a builder-selected implementation:
    - `JAVA_SORT`
    - `REDIS_INDEX`
  - one recovered command-cart path
  - restart-and-finish `PersistentConveyor` recovery
- Recovery cleanup proof now includes the recovered `CANCELED`, `TIMED_OUT`, and timeout-action `INVALID` status paths in addition to the earlier `READY` paths.
- A first compatible slice of the JDBC-style performance tests now exists for Redis.

## Current Implemented Scope

Production code currently includes:

- `src/main/java/com/aegisql/conveyor/persistence/redis/RedisConnectionFactory.java`
- `src/main/java/com/aegisql/conveyor/persistence/redis/RedisPersistenceBuilder.java`
- `src/main/java/com/aegisql/conveyor/persistence/redis/RedisPersistence.java`
- `src/main/java/com/aegisql/conveyor/persistence/redis/RedisLuaScriptBundle.java`

Test evidence currently includes:

- `src/test/java/com/aegisql/conveyor/persistence/redis/LearnRedisConnectionTest.java`
- `src/test/java/com/aegisql/conveyor/persistence/redis/LearnRedisStringCrudTest.java`
- `src/test/java/com/aegisql/conveyor/persistence/redis/LearnRedisHashCrudTest.java`
- `src/test/java/com/aegisql/conveyor/persistence/redis/LearnRedisLuaScriptTest.java`
- `src/test/java/com/aegisql/conveyor/persistence/redis/RedisConnectionFactoryTest.java`
- `src/test/java/com/aegisql/conveyor/persistence/redis/RedisPersistenceBuilderTest.java`
- `src/test/java/com/aegisql/conveyor/persistence/redis/RedisPersistenceTest.java`
- `src/test/java/com/aegisql/conveyor/persistence/redis/RedisPerfTest.java`
- `src/test/java/com/aegisql/conveyor/persistence/redis/harness/Trio.java`
- `src/test/java/com/aegisql/conveyor/persistence/redis/harness/TrioBuilder.java`
- `src/test/java/com/aegisql/conveyor/persistence/redis/harness/TrioConveyor.java`
- `src/test/java/com/aegisql/conveyor/persistence/redis/harness/TrioPart.java`
- `src/test/java/com/aegisql/conveyor/persistence/redis/harness/ThreadPool.java`

## Redis Data Model In Code Today

The current implementation uses a namespace rooted at:

- `conv:{name}`

The current code creates and maintains these key groups:

- `conv:{name}:meta`
  - namespace bootstrap marker with backend name, backend version, configured persistence name, and current Lua bundle markers
- `conv:{name}:seq`
  - Redis `INCR` source for part ids
- `conv:{name}:parts:active`
  - sorted set of active non-static part ids
- `conv:{name}:parts:static`
  - sorted set of static part ids
- `conv:{name}:parts:expires`
  - sorted set of part ids scored by expiration timestamp
- `conv:{name}:completed`
  - set of completed build keys
- `conv:{name}:part:{id}:payload`
  - serialized cart value payload bytes
  - optionally protected by the shared persistence encryption path
- `conv:{name}:part:{id}:meta`
  - authoritative cart metadata hash
  - includes core fields such as `id`, `loadType`, `creationTime`, `expirationTime`, and `priority`
  - includes encoded cart fields such as `keyHint` / `keyData`, `labelHint` / `labelData`, `propertiesHint` / `propertiesData`
  - includes `valueHint` for payload decoding
  - new writes no longer mirror payload bytes into `valueData`
  - the reader still accepts older itemized records that contain mirrored `valueData`
  - includes `commandFilterHint` / `commandFilterData` for filter-based command carts
- `conv:{name}:part:{id}:keys`
  - reverse index from part id to encoded key values
- `conv:{name}:parts:key:{encodedKey}`
  - sorted set of part ids for one encoded build key
- `conv:{name}:tracker`
  - set of all created Redis keys so `archiveAll()` can delete the namespace

Important note:

- The current implementation no longer stores the whole cart as a single serialized payload.
- The current restore path reconstructs carts from itemized metadata plus the value payload key.
- The reader still detects and reads the legacy whole-cart Redis format for backward compatibility.
- The reader also still accepts the earlier itemized form where value bytes were mirrored in `:meta.valueData`.
- The current `savePart(...)` path is now executed by a Lua script so the itemized write lands atomically.

## Comparison With The JDBC Pattern

### Module Shape

- JDBC today:
  - `conveyor-persistence-jdbc` is a mature relational backend with helper modules for concrete engines such as PostgreSQL, MySQL, Oracle, and SQL Server.
- Redis today:
  - `conveyor-persistence-redis` is a standalone sibling backend, which matches the intended architecture.
- Status:
  - aligned with the plan

### Connection Model

- JDBC today:
  - supports several connection ownership modes and engine-specific setup through the builder and connectivity helpers.
- Redis today:
  - uses `JedisPooled` through `RedisConnectionFactory`.
  - supports URI resolution from a system property, environment variable, or default URI.
- Status:
  - good enough for v1 development
  - much simpler than JDBC

### Connection Stability And Ownership

- JDBC today:
  - the code must explicitly decide whether a JDBC connection is direct or pooled and whether `close()` means "close the socket" or "return to the pool".
- Redis today:
  - `RedisPersistence` holds a long-lived `JedisPooled` field.
  - this is not the same thing as holding one long-lived Redis socket.
  - with the current Jedis client version in this module, `JedisPooled` delegates each command through a pooled provider and the borrowed connection is closed in a `finally` block, which returns it to the pool.
- What this means:
  - the current Redis code already has per-operation borrow/return semantics
  - it does not need explicit "return to pool" code the way the legacy JDBC layer does
- The real design issue:
  - independent top-level builds from the same URI still create separate pools unless the application injects a shared client explicitly
- Status:
  - semantically correct for pooled Redis usage
  - much better for long-lived applications after shared-copy client support
  - now improved with explicit pool tuning and richer client configuration for owned clients

### Initialization

- JDBC today:
  - `autoInit(true)` creates or validates relational structures such as database, schema, tables, indexes, and completed log storage.
- Redis today:
  - `init()` bootstraps Redis namespace metadata when missing and validates existing namespace metadata when present.
  - `autoInit(false)` now skips upfront bootstrap but still performs lazy bootstrap or validation on first use.
  - existing namespace metadata must match:
    - backend=`redis`
    - backend version=`1`
    - configured persistence name
    - priority restore strategy
    - script mode=`lua`
    - script bundle version=`1`
  - bootstrap now also validates:
    - Redis server version is present, parseable, and above the current conservative minimum support floor
    - required command families for the current backend are available through live probe operations:
      - sequence/increment
      - string
      - hash
      - set
      - sorted-set
      - Lua scripting through `SCRIPT LOAD` and `EVALSHA`
  - bootstrap now loads the current Lua script bundle used by the atomic `savePart(...)` path
- Status:
  - implemented in a stronger v1 form
  - expanded into Redis server-version checks, required-feature validation, and current Lua script registration

### Storage Model

- JDBC today:
  - stores carts and related metadata in relational tables and uses explicit SQL for fetch, archive, and restore order.
- Redis today:
  - stores itemized cart metadata in Redis hashes and keeps the cart value bytes in a separate payload key.
  - maintains lookup/index structures with sets and sorted sets.
  - keeps legacy whole-cart read fallback so previously written Redis data is still readable.
  - can now protect serialized value payload bytes with the same shared encryptor used by JDBC.
  - now commits itemized cart writes through a Lua-backed atomic `savePart(...)` operation
- Status:
  - implemented
  - intentionally Redis-native
  - closer to the JDBC mental model than the initial Redis version
  - still less mature than JDBC around archive atomicity and broader operational parity

### Payload Protection

- JDBC today:
  - uses the shared persistence-core encryption builder and converter path.
  - the default path now uses versioned `AES/GCM/NoPadding` payload protection with legacy-default decrypt fallback.
- Redis today:
  - reuses the same shared encryption builder for payload bytes only.
  - Redis key/index encoding is intentionally left deterministic and unencrypted in v1.
- Status:
  - implemented for payloads
  - aligned between JDBC and Redis
  - indexed-key protection is still intentionally out of scope

### Engine Or Dialect Layer

- JDBC today:
  - has `GenericEngine`, engine-specific SQL helpers, and helper modules for engine-specific behavior.
- Redis today:
  - has no dialect abstraction, which is appropriate for a single Redis backend.
- Status:
  - aligned with the design recommendation

### Restore Order

- JDBC today:
  - explicitly supports restore-order policies such as `NO_ORDER`, `BY_ID`, and `BY_PRIORITY_AND_ID`.
- Redis today:
  - explicitly supports:
    - `BY_ID` as the default
    - `NO_ORDER` as backend iteration order with no extra re-sorting
    - `BY_PRIORITY_AND_ID` with a builder-selected implementation:
      - `JAVA_SORT` as the default
      - `REDIS_INDEX` as an initialization-stage choice
  - active/static indexes still store part id as their native sorted-set score
  - when `REDIS_INDEX` is selected, Redis also maintains priority indexes for:
    - active parts
    - static parts
    - per-key part lookups
  - expired reads still use the expiration index first and then Java-side priority sorting
- Status:
  - implemented
  - current tests now prove:
    - `BY_ID` behavior for active parts, static parts, expired parts, and per-key indexes
    - `NO_ORDER` behavior for expired-part reads
    - `JAVA_SORT` `BY_PRIORITY_AND_ID` for active parts, static parts, expired parts, and per-key indexes
    - `REDIS_INDEX` `BY_PRIORITY_AND_ID` for active parts, static parts, and per-key indexes
    - legacy namespace upgrade into `REDIS_INDEX` when `priorityRestoreStrategy` metadata is missing
    - `BY_PRIORITY_AND_ID` replay behavior on a recovered `PersistentConveyor` flow for both `JAVA_SORT` and `REDIS_INDEX`

### Archive Strategies

- JDBC today:
  - supports richer archive strategies, including delete-style archiving and other archiver integrations.
- Redis today:
  - now supports Redis-appropriate archive strategies:
    - `archiveParts`
    - `archiveKeys`
    - `archiveCompleteKeys`
    - `archiveExpiredParts`
    - `archiveAll`
- Status:
  - partial
  - SPI methods are implemented
  - builder-exposed archive support now covers:
    - `DELETE`
    - `NO_ACTION`
    - `MOVE_TO_PERSISTENCE`
    - `MOVE_TO_FILE`
  - `CUSTOM` is exposed as an expert path with Redis-specific cleanup caveats
  - `SET_ARCHIVED` is intentionally unsupported

### Archive Strategy Analysis

The JDBC builder exposes these archive-strategy shapes:

- `DELETE`
- `NO_ACTION`
- `SET_ARCHIVED`
- `MOVE_TO_PERSISTENCE`
- `MOVE_TO_FILE`
- `CUSTOM`

Redis should not be judged by whether it can imitate each JDBC strategy literally. The better question is whether the strategy fits the Redis data model cleanly.

#### Clean fit

- `DELETE`
  - Status:
    - implemented
  - Why it fits:
    - Redis persistence already tracks active/static/expiration/per-key indexes and can remove all related keys directly.
    - The current Redis record model is built around active data plus deletion, not retained archived state.

- `NO_ACTION`
  - Status:
    - implemented
    - covered by builder and persistence tests
  - Why it fits:
    - a no-op archiver is backend-neutral.
    - It does not fight the Redis data model.

#### Implemented through Redis-specific low-level archive hooks

- `MOVE_TO_PERSISTENCE`
  - Status:
    - implemented
    - covered by persistence tests
  - Why it fits:
    - Redis can read carts, write them to another `Persistence<K>`, and then delete from the Redis namespace.
    - This is a natural non-delete extension when users want hot Redis recovery plus colder long-term persistence.
  - Implementation note:
    - Redis now uses Redis-specific low-level archive hooks so move-style archivers can export carts and then delete without recursively calling back into `Persistence.archive...()`.

- `MOVE_TO_FILE`
  - Status:
    - implemented
    - covered by persistence tests
  - Why it fits:
    - Redis can stream carts to a binary log or other file sink and then delete them.
    - The file-format concept is not inherently relational.
  - Implementation note:
    - Redis now uses a Redis-specific file archiver built on the same low-level archive hooks as `MOVE_TO_PERSISTENCE`.

- `CUSTOM`
  - Status:
    - exposed with caveats
    - not yet deeply covered by dedicated tests
  - Why it fits:
    - the core `Archiver<K>` SPI is backend-neutral.
  - Main implementation constraint:
    - a custom Redis archiver still cannot safely call back into `Persistence.archive...()` for final deletion without risking recursion.
    - In practice this means custom archivers are useful only if they:
      - are true terminal/no-op behaviors, or
      - have Redis-aware low-level delete helpers available.

#### Poor fit for the current Redis model

- `SET_ARCHIVED`
  - Status:
    - technically possible
    - not recommended for the current Redis backend
  - Why it is a poor fit:
    - the current Redis data layout is organized around active/static/expiration indexes plus deletion.
    - Supporting retained archived state would require new semantics and likely new indexes for:
      - archived membership
      - read filtering
      - expiration/archive interaction
      - `archiveAll()` meaning under retained state
    - That adds complexity without leveraging a natural Redis capability the way relational `ARCHIVED` columns do in JDBC.
  - Recommendation:
    - treat `SET_ARCHIVED` as a non-goal unless a strong Redis-native use case appears.

#### Recommended Redis stance

- Keep `DELETE` as the default and primary Redis archive strategy.
- Support `NO_ACTION`, `MOVE_TO_PERSISTENCE`, and `MOVE_TO_FILE` where they fit the user's lifecycle.
- Keep `CUSTOM` available as an expert path, with explicit caution around Redis-aware cleanup.
- Do not plan `SET_ARCHIVED` as parity work for Redis.

### Completed Build Tracking

- JDBC today:
  - uses relational completed-log storage.
- Redis today:
  - uses `conv:{name}:completed` as a Redis set.
- Status:
  - implemented

### End-To-End Recovery

- JDBC today:
  - has mature `PersistentConveyor` replay behavior and older tests around replay, acknowledgment, and cleanup.
- Redis today:
  - now has end-to-end restart recovery evidence for an incomplete build that is replayed and completed after restart
  - now has explicit recovered-acknowledgment evidence for a completed build
  - now has recovered cleanup evidence for the current READY-path auto-ack and explicit-ack flows when the recovered conveyor is allowed to drain cleanly
  - now has recovered cleanup evidence for the current CANCELED path when a recovered build is explicitly canceled and the conveyor is allowed to drain cleanly
  - now has recovered cleanup evidence for the current TIMED_OUT path when the recovered conveyor auto-acknowledges `TIMED_OUT`
  - now has recovered cleanup evidence for the current timeout-action `INVALID` path when the recovered conveyor auto-acknowledges `INVALID`
- Status:
  - partial
  - restart-and-finish recovery is proven
  - recovered explicit acknowledge delivery is proven
  - recovered READY-path cleanup is proven
  - recovered CANCELED cleanup is proven
  - recovered TIMED_OUT cleanup is proven when the recovered conveyor auto-acknowledges `TIMED_OUT`
  - recovered timeout-action `INVALID` cleanup is proven when the recovered conveyor auto-acknowledges `INVALID`
  - broader recovery-mode coverage is still incomplete

### Unique Field Constraints

- JDBC today:
  - has builder/configuration support for unique fields and can rely on database-level enforcement.
- Redis today:
  - does not implement `uniqueFields`.
  - treats database-enforced uniqueness as a JDBC-specific capability rather than a Redis parity goal.
- Status:
  - intentional non-goal
  - users who need uniqueness constraints should choose a JDBC backend

### Command Carts

- JDBC today:
  - command persistence participates in the broader persistence and recovery flow used by `PersistentConveyor`.
- Redis today:
  - command carts are reconstructed from itemized metadata plus the value payload.
  - filter-based commands store the filter metadata separately when no concrete key is present.
  - current tests now prove that a persisted `GeneralCommand` survives Redis storage and is replayed through the recovered `PersistentConveyor` startup path.
- Status:
  - partial
  - current recovered-command replay is proven
  - broader command coverage beyond the current tested path is still not proven

### Atomicity

- JDBC today:
  - benefits from relational transactional semantics and statement grouping.
- Redis today:
  - current `savePart` and archive operations issue multiple Redis commands.
- Status:
  - partial
  - functionally acceptable for early development
  - not hardened for atomic multi-key updates yet

### Operational Visibility

- JDBC today:
  - exposes richer operational surface area and has older supporting patterns such as MBeans and archiver descriptions.
- Redis today:
  - has module-local `DEBUG` and `TRACE` logging
  - has no Redis-specific MBean or equivalent ops surface yet
- Status:
  - partial

## What Is Done

### Done: module bootstrap

- New module created and added under `conveyor-persistence`.
- Local instructions and module context docs exist.

### Done: stronger namespace bootstrap semantics

- Redis namespace metadata is no longer blindly rewritten on every operation.
- Namespace metadata is now bootstrapped once and then validated.
- Current validation requires:
  - backend=`redis`
  - backend version=`1`
  - configured persistence name match
- `autoInit(false)` is now explicitly proven as a lazy-bootstrap path:
  - building the persistence does not create namespace metadata immediately
  - the first persistence operation bootstraps it
- Incompatible or incomplete namespace metadata is now rejected with `PersistenceException`.

### Done: Redis server-version and required-feature validation

- Bootstrap now validates Redis server compatibility before namespace use.
- Current validation proves:
  - `INFO server` contains a parseable `redis_version`
  - the version is above the current conservative minimum support floor
  - the Redis instance supports the command families the current backend actively uses:
    - sequence/increment
    - string
    - hash
    - set
    - sorted-set
    - Lua scripting
- These checks are run on eager init and on lazy first-use init.

### Done: Lua bootstrap registration for the current atomic write bundle

- Redis namespace bootstrap metadata now includes:
  - `scriptMode=lua`
  - `scriptBundleVersion=1`
- Existing namespaces that predate the Lua bundle are upgraded in place by adding the current script metadata markers.
- Incompatible or incomplete Lua bundle metadata is rejected with `PersistenceException`.
- The current Lua bundle is loaded during bootstrap and is therefore ready before the first script-backed persistence write.
- Current tests also prove the operational reload path:
  - after `SCRIPT FLUSH`
  - the next `savePart(...)` reloads the script on `NOSCRIPT` and still succeeds
  - the next Lua-backed delete/archive cleanup operation also reloads and still succeeds

### Done: Lua-backed delete-style archive and cleanup operations

- Redis delete-style cleanup now goes through the Lua bundle for:
  - `archiveParts`
  - `archiveCompleteKeys`
  - `archiveAll`
- The `DELETE` archive strategy now also uses Redis-native Lua cleanup for:
  - `archiveKeys`
  - `archiveExpiredParts`
- This moves the current Redis delete path away from Java multi-command cleanup and into Redis-side atomic execution.
- Current tests prove:
  - delete-style archive behavior still matches prior observable semantics
  - `SCRIPT FLUSH` followed by delete/archive cleanup reloads the Lua bundle and still succeeds

### Done: builder-level pool tuning and explicit client configuration

- The Redis builder now supports explicit tuning for internally managed Redis clients:
  - `maxTotal`
  - `maxIdle`
  - `minIdle`
  - `connectionTimeoutMillis`
  - `socketTimeoutMillis`
  - `blockingSocketTimeoutMillis`
  - `database`
  - `clientName`
  - `user`
  - `password`
  - `ssl`
- `RedisConnectionFactory` now creates owned `JedisPooled` clients through explicit pool and client config objects instead of URI-only construction.
- URI configuration is still supported and remains the default.
- Explicit builder settings override URI-derived client settings where both are present.
- Externally supplied `JedisPooled` clients remain supported and are still not closed by `RedisPersistence.close()`.
- Current tests prove:
  - pool config materialization
  - client config materialization
  - live owned-client pool tuning
  - external client ownership semantics still hold

### Done: Redis connectivity

- `RedisConnectionFactory` resolves Redis URI from:
  - system property `conveyor.persistence.redis.uri`
  - env var `CONVEYOR_PERSISTENCE_REDIS_URI`
  - fallback `redis://localhost:6379`
- Connection opening is covered by tests.

### Done: builder skeleton

- `RedisPersistenceBuilder` supports:
  - module name
  - Redis URI override
  - externally supplied `JedisPooled`
  - `autoInit`
  - `minCompactSize`
  - `maxArchiveBatchSize`
  - `maxArchiveBatchTime`
  - owned-client pool sizing:
    - `maxTotal`
    - `maxIdle`
    - `minIdle`
  - owned-client config:
    - `connectionTimeoutMillis`
    - `socketTimeoutMillis`
    - `blockingSocketTimeoutMillis`
    - `database`
    - `clientName`
    - `user`
    - `password`
    - `ssl`
  - non-persistent property set
  - persistent-part filter
- Builder immutability style matches the broader builder approach used elsewhere in persistence.

### Done: shared client lifecycle for copies

- `RedisPersistence.copy()` now shares the same Redis client handle instead of opening a new Redis pool.
- Owned clients are reference-counted and close only after the last persistence copy closes.
- Externally supplied `JedisPooled` clients are not closed by `RedisPersistence.close()`.
- This removes the earlier copy-related pool duplication problem inside one logical persistent conveyor flow.

### Done: first `Persistence<K>` implementation

The Redis module currently implements the full method surface of `Persistence<K>`:

- `nextUniquePartId`
- `savePart`
- `isPartPersistent`
- `savePartId`
- `saveCompletedBuildKey`
- `getParts`
- `getAllPartIds`
- `getAllParts`
- `getExpiredParts`
- `getAllStaticParts`
- `getCompletedKeys`
- `archiveParts`
- `archiveKeys`
- `archiveCompleteKeys`
- `archiveExpiredParts`
- `archiveAll`
- `getMaxArchiveBatchSize`
- `getMaxArchiveBatchTime`
- `getNumberOfParts`
- `getMinCompactSize`
- `copy`
- `isPersistentProperty`

Also exercised through the interface and helper flows:

- `getPart`
- `absorb`
- `wrapConveyor`
- `getConveyor()`
- `getConveyor(Supplier)`

### Done: cart round-trip evidence

Current tests prove Redis round-trips for:

- `ShoppingCart`
- `CreatingCart`
- `ResultConsumerCart`
- `MultiKeyCart`

This is meaningful because it shows the current itemized storage and reconstruction path handles more than simple shopping carts.

### Done: itemized Redis record format

- Redis `savePart(...)` now writes:
  - an itemized metadata hash at `conv:{name}:part:{id}:meta`
  - value payload bytes at `conv:{name}:part:{id}:payload`
  - the existing active/static/expiration/per-key indexes
- The current itemized `savePart(...)` write is now committed through Lua rather than a Java multi-command sequence.
- Current tests prove:
  - new writes use the itemized structure instead of legacy whole-cart payload storage
  - the current reader can still read the earlier whole-cart Redis format

### Done: stabilized itemized payload layout

- New writes now keep payload bytes only in `conv:{name}:part:{id}:payload`.
- Metadata keeps `valueHint` but no longer stores mirrored `valueData`.
- The reader still supports the earlier mirrored itemized form so previously written Redis data remains readable.

### Done: current logging

- `RedisConnectionFactory`, `RedisPersistenceBuilder`, and `RedisPersistence` use SLF4J.
- Module test logging is enabled through:
  - `src/test/resources/log4j.properties`
- Current convention:
  - `DEBUG` for lifecycle and key operations
  - `TRACE` for bulky cart/id/key dumps

### Done: shared payload encryption

- The shared persistence encryption core was modernized before wiring Redis to it.
- Current shared behavior:
  - default managed mode uses versioned `AES/GCM/NoPadding`
  - passphrase-based mode derives keys with PBKDF2
  - legacy-default JDBC payloads encrypted with `AES/ECB/PKCS5Padding` can still be read through fallback
  - per-call cipher creation removes the old shared-mutable-`Cipher` concurrency risk
- Redis currently applies that protection to:
  - `conv:{name}:part:{id}:payload`
- Redis intentionally does not apply that protection to:
  - encoded build keys
  - completed-key indexes
  - other Redis key names

### Done: first compatible performance-test slice

- Redis now has a first reproduced subset of the JDBC-style performance suite.
- Current reproduced scenarios:
  - direct conveyor baseline
  - persistent conveyor shuffled load
  - persistent conveyor sorted load
- The Redis perf harness is intentionally local to the Redis module and mirrors the familiar trio-style JDBC perf shape closely enough to compare behavior without overstating parity.
- Performance-test size is currently controlled by:
  - `REDIS_PERF_TEST_SIZE`
  - fallback `PERF_TEST_SIZE`

## Test Evidence By Area

### Learn Redis Tests

- `LearnRedisConnectionTest`
  - proves basic Redis reachability and ping
- `LearnRedisStringCrudTest`
  - proves basic string CRUD assumptions
- `LearnRedisHashCrudTest`
  - proves basic hash CRUD assumptions

### Connection Factory Tests

- `RedisConnectionFactoryTest`
  - covers property, environment, and default URI resolution
  - covers direct and default connection opening

### Builder Tests

- `RedisPersistenceBuilderTest`
  - covers null validation
  - covers lazy bootstrap semantics through `autoInit(false)`
  - covers acceptance of valid pre-bootstrapped namespace metadata
  - covers rejection of incompatible namespace metadata
  - covers non-persistent filter behavior
  - covers archive-strategy builder branches, including `NO_ACTION`
  - covers serializability enforcement for encoded keys
  - covers the defensive null branch inside internal Redis key encoding
  - covers shared-client behavior for `copy()`
  - covers externally supplied `JedisPooled` ownership semantics

- `RedisBootstrapValidatorTest`
  - covers version parsing
  - covers malformed and missing version info
  - covers minimum-version rejection
  - covers required-feature probe success and failure cleanup behavior
  - covers live Redis compatibility validation

### Persistence Contract Tests

- `RedisPersistenceTest`
  - covers normal SPI persistence flow
  - covers manual key indexing
  - covers completed-key tracking
  - covers the itemized Redis storage shape used by new writes
  - covers the stabilized itemized layout with no mirrored `valueData` on new writes
  - covers legacy whole-cart Redis compatibility for reads
  - covers legacy itemized mirrored-`valueData` compatibility for reads
  - covers encrypted payload storage and read-back
  - covers direct `SecretKey` payload encryption
  - covers wrong-secret failure for payload reads
  - covers modern reader compatibility with legacy-default payloads
  - covers static and expired-part queries
  - covers explicit restore-order policies for active, static, expired, and per-key retrieval
  - covers recovered replay order with `BY_PRIORITY_AND_ID`
  - covers persisted command-cart save, reload, and replay into a wrapped conveyor
  - covers incomplete-build replay across `PersistentConveyor` restart
  - covers recovered explicit-acknowledge handle delivery
  - covers recovered cleanup for the current READY-path auto-ack flow when the recovered conveyor drains through `completeAndStop()`
  - covers recovered cleanup for the current explicit-acknowledge flow when the recovered conveyor drains through `completeAndStop()`
  - covers recovered cleanup for the current CANCELED path when the recovered conveyor drains through `completeAndStop()`
  - covers recovered cleanup for the current TIMED_OUT path when the recovered conveyor auto-acknowledges `TIMED_OUT`
  - covers recovered cleanup for the current timeout-action `INVALID` path when the recovered conveyor auto-acknowledges `INVALID`
  - covers delete-style archive methods
  - covers `NO_ACTION`
  - covers `MOVE_TO_PERSISTENCE`
  - covers `MOVE_TO_FILE`
  - covers `copy()`
  - covers `absorb(...)`
  - covers helper methods that produce `PersistentConveyor`

### Performance Compatibility Tests

- `RedisPerfTest`
  - reproduces the current JDBC-style direct conveyor baseline
  - reproduces persistent shuffled-load coverage
  - reproduces persistent sorted-load coverage
  - uses Redis-local harness classes modeled after the JDBC perf-test trio harness
  - scales via:
    - `REDIS_PERF_TEST_SIZE`
    - fallback `PERF_TEST_SIZE`
  - intentionally does not yet reproduce:
    - parallel persistent perf flows
    - archive-to-file or archive-to-persistence perf flows
    - broader unload or expiration perf scenarios

## What Still Needs To Be Done

### Missing Or Partial Compared To JDBC

- broader command-cart coverage beyond the currently proven replay path
- stronger atomic behavior for move-style archive orchestration
- indexed-key protection if the project decides Redis needs more than payload-only protection
- broader recovery-mode coverage beyond the currently proven READY, CANCELED, TIMED_OUT, and timeout-action INVALID paths
- broader performance parity beyond the currently reproduced direct, shuffled persistent, and sorted persistent scenarios

## Ranked Backlog By Complexity

Complexity here means engineering effort plus semantic risk relative to the current Redis codebase.

### Low Complexity

- Keep `doc/project-context.md` and this report in sync after every Redis feature change.
  - Why low:
    - docs-only
  - Why important:
    - the module is still changing quickly

- Current status:
  - the previously listed low-complexity code tasks are now done:
    - explicit command-cart replay test
    - restore-order assumption tests
    - direct `SecretKey` payload-encryption test
  - the remaining low-complexity work is mostly documentation hygiene as the module evolves

### Medium Complexity

- Benchmark and document when `JAVA_SORT` vs `REDIS_INDEX` is the better initialization-stage choice for `BY_PRIORITY_AND_ID`.
  - Candidate scope:
    - compare recovery and replay costs under both strategies
    - compare Redis memory overhead for maintained priority indexes
  - Why medium:
    - the feature is implemented, but the operational tradeoff still needs clearer guidance

- Improve current data layout so metadata stays intentionally queryable without growing redundant mirrors again.
  - Candidate scope:
    - keep value bytes payload-only unless a future query case clearly requires duplication
    - decide whether any remaining metadata fields should be normalized further or dropped
  - Why medium:
    - affects persistence format and future compatibility

- Extend Redis performance coverage toward JDBC parity.
  - Candidate scope:
    - parallel persistent perf flows
    - unload and expiration perf scenarios
    - archive-oriented perf scenarios where Redis feature maturity makes them meaningful
  - Why medium:
    - requires more harness growth and careful alignment with still-changing Redis semantics
  - Why important:
    - keeps Redis comparable to the established JDBC development baseline without pretending parity too early

- Decide whether Redis needs explicit builder docs mirroring the JDBC encryption options.
  - Why medium:
    - code already supports the shared encryption builder surface
  - Why important:
    - without docs, the feature exists but is discoverable mostly through code/tests

### High Complexity

- Replace the remaining move-style archive orchestration with stronger Redis-native atomic boundaries where feasible.
  - Why high:
    - `MOVE_TO_PERSISTENCE` and `MOVE_TO_FILE` still need Java to read carts and hand them off before Redis-side cleanup
  - Why important:
    - delete-style cleanup is now atomic, but export-and-then-delete flows still have wider interruption windows

- Keep archive strategy documentation and test evidence current.
  - Current state:
    - `DELETE`, `NO_ACTION`, `MOVE_TO_PERSISTENCE`, and `MOVE_TO_FILE` are implemented
    - `CUSTOM` is exposed but still an expert path
    - `SET_ARCHIVED` is intentionally unsupported and should stay documented as a Redis non-goal
  - Why high:
    - archive semantics are easy to misstate because JDBC and Redis are intentionally different here

- Define and prove compatibility with the acknowledgment and cleanup flows behind `PersistentConveyor`.
  - Why high:
    - JDBC persistence is mature here, and Redis is only partially proven so far
  - Current state:
    - restart replay is covered
    - recovered explicit acknowledgment is covered
    - recovered cleanup is covered for the current READY-path auto-ack and explicit-ack flows when the recovered conveyor drains cleanly
    - recovered cleanup is covered for the current CANCELED path when the recovered conveyor drains cleanly
    - recovered cleanup is covered for the current TIMED_OUT path when the recovered conveyor auto-acknowledges `TIMED_OUT`
    - recovered cleanup is covered for the current timeout-action `INVALID` path when the recovered conveyor auto-acknowledges `INVALID`
    - broader recovery-mode coverage is still not proven

- Decide whether library-level reconnect or retry behavior should exist at all.
  - Current recommendation:
    - do not add automatic retries before the remaining move-style archive flows have safer atomic boundaries
  - Why high:
    - blind retries around export-and-then-delete Redis flows can create partial-write ambiguity
  - Why important:
    - long-running applications will see network interruptions, but retry policy must respect persistence correctness

### Very High Complexity

- Add Redis Cluster-safe semantics.
  - Candidate scope:
    - key-slot discipline
    - multi-key operation strategy
    - script/function portability
  - Why very high:
    - this changes core assumptions throughout the implementation

- Reach broad feature parity with the full JDBC ecosystem.
  - Candidate scope:
    - operational surfaces
    - advanced archivers
    - more recovery modes
    - mature tuning and diagnostics
  - Why very high:
    - this is a multi-phase program, not one task

## Recommended Next Sequence

1. Broaden recovery proof into additional recovery modes beyond the currently proven READY, CANCELED, TIMED_OUT, and timeout-action INVALID paths.
2. Benchmark and document when `JAVA_SORT` vs `REDIS_INDEX` should be chosen.
3. Decide whether any later Redis Function work is worth the required minimum-version jump beyond the current Lua-compatible floor.
4. Tighten move-style archive orchestration if the current interruption window proves too wide in practice.

## Bottom Line

- Redis persistence is no longer just a plan. It is a real first backend with working SPI coverage and good early test evidence.
- The biggest remaining gap is not basic CRUD. The biggest gap is maturity compared to JDBC:
  - recovery breadth beyond the currently proven flows
  - move-style archive orchestration beyond the now-scripted save and delete cleanup paths
- The module is in a solid v1 development state, but not yet in JDBC-level production-parity territory.
