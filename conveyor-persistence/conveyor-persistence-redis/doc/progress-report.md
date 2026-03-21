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
- Current Redis archiving is delete-oriented only.
- Current Redis writes are multi-command operations, not Lua/function-backed atomic units.
- Current Redis restore behavior is now explicitly configurable and proven for:
  - `BY_ID`
  - `NO_ORDER`
  - Java-side `BY_PRIORITY_AND_ID`
  - one recovered command-cart path
  - restart-and-finish `PersistentConveyor` recovery
- Recovery cleanup proof now includes the recovered `CANCELED` status path in addition to the earlier `READY` paths.
- A first compatible slice of the JDBC-style performance tests now exists for Redis.

## Current Implemented Scope

Production code currently includes:

- `src/main/java/com/aegisql/conveyor/persistence/redis/RedisConnectionFactory.java`
- `src/main/java/com/aegisql/conveyor/persistence/redis/RedisPersistenceBuilder.java`
- `src/main/java/com/aegisql/conveyor/persistence/redis/RedisPersistence.java`

Test evidence currently includes:

- `src/test/java/com/aegisql/conveyor/persistence/redis/LearnRedisConnectionTest.java`
- `src/test/java/com/aegisql/conveyor/persistence/redis/LearnRedisStringCrudTest.java`
- `src/test/java/com/aegisql/conveyor/persistence/redis/LearnRedisHashCrudTest.java`
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
  - namespace bootstrap marker with backend name and version
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
- Status:
  - implemented in a stronger v1 form
  - not yet expanded into Redis server-version checks, script registration, or broader feature validation

### Storage Model

- JDBC today:
  - stores carts and related metadata in relational tables and uses explicit SQL for fetch, archive, and restore order.
- Redis today:
  - stores itemized cart metadata in Redis hashes and keeps the cart value bytes in a separate payload key.
  - maintains lookup/index structures with sets and sorted sets.
  - keeps legacy whole-cart read fallback so previously written Redis data is still readable.
  - can now protect serialized value payload bytes with the same shared encryptor used by JDBC.
- Status:
  - implemented
  - intentionally Redis-native
  - closer to the JDBC mental model than the initial Redis version
  - still less mature than JDBC around ordering, atomicity, and archive strategy breadth

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
    - Java-side `BY_PRIORITY_AND_ID`
  - active/static indexes still store part id as their native sorted-set score
- Status:
  - implemented
  - current tests now prove:
    - `BY_ID` behavior for active parts, static parts, expired parts, and per-key indexes
    - `NO_ORDER` behavior for expired-part reads
    - Java-side `BY_PRIORITY_AND_ID` for active parts, static parts, expired parts, and per-key indexes
    - `BY_PRIORITY_AND_ID` replay behavior on a recovered `PersistentConveyor` flow

### Archive Strategies

- JDBC today:
  - supports richer archive strategies, including delete-style archiving and other archiver integrations.
- Redis today:
  - implements delete-style operations only:
    - `archiveParts`
    - `archiveKeys`
    - `archiveCompleteKeys`
    - `archiveExpiredParts`
    - `archiveAll`
- Status:
  - partial
  - SPI methods are implemented
  - strategy parity with JDBC is not implemented

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
- Status:
  - partial
  - restart-and-finish recovery is proven
  - recovered explicit acknowledge delivery is proven
  - recovered READY-path cleanup is proven
  - recovered CANCELED cleanup is proven
  - timeout-driven recovery cleanup is still not proven
  - broader recovery and cleanup coverage is still incomplete

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
  - covers archive and no-op branches
  - covers serializability enforcement for encoded keys
  - covers the defensive null branch inside internal Redis key encoding
  - covers shared-client behavior for `copy()`
  - covers externally supplied `JedisPooled` ownership semantics

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
  - covers delete-style archive methods
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
- archive strategy parity beyond delete behavior
- stronger atomic update behavior for multi-key writes
- indexed-key protection if the project decides Redis needs more than payload-only protection
- broader recovery and cleanup coverage beyond the currently proven READY and CANCELED paths
- timeout-driven recovery cleanup and replay semantics are still not proven end to end
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

- Extend bootstrap semantics beyond namespace metadata validation.
  - Candidate scope:
    - Redis server-version checks
    - required command/feature validation
    - script/function registration
  - Why medium:
    - the namespace semantics are now in place, but deeper compatibility checks still need to be decided carefully

- Optimize `BY_PRIORITY_AND_ID` if Redis replay volume makes the current Java-side sort too expensive.
  - Candidate scope:
    - dedicated priority+id index
    - reduced metadata lookups during replay-oriented reads
  - Why medium:
    - the current behavior is correct, but it may become a tuning target under larger recovery loads

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

- Replace multi-command save/archive flows with Redis scripts or functions.
  - Why high:
    - requires careful atomic update design across payload, indexes, and completed-key state
  - Why important:
    - this is the main durability/correctness hardening step

- Add archive strategy parity beyond delete behavior.
  - Candidate scope:
    - Redis-native equivalent of `SET_ARCHIVED`
    - archive-to-other-persistence behavior if desired
  - Why high:
    - the current Redis model is built around deletion, not archived-state transitions

- Define and prove compatibility with the acknowledgment and cleanup flows behind `PersistentConveyor`.
  - Why high:
    - JDBC persistence is mature here, and Redis is only partially proven so far
  - Current state:
    - restart replay is covered
    - recovered explicit acknowledgment is covered
    - recovered cleanup is covered for the current READY-path auto-ack and explicit-ack flows when the recovered conveyor drains cleanly
    - recovered cleanup is covered for the current CANCELED path when the recovered conveyor drains cleanly
    - broader status and recovery-mode coverage is still not proven

- Decide whether library-level reconnect or retry behavior should exist at all.
  - Current recommendation:
    - do not add automatic retries before save/archive operations are made atomic
  - Why high:
    - blind retries on today's multi-command Redis writes can create partial-write ambiguity
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

1. Broaden recovery and cleanup proof beyond the currently covered READY and CANCELED paths.
2. Clarify and then prove timeout-driven recovery semantics, including whether additional timeout-action wiring is required for Redis `PersistentConveyor` cleanup.
3. Extend bootstrap semantics with Redis server-version and required-feature validation.
4. Move save/archive operations toward Lua or Redis Functions for atomicity.
5. Revisit whether `BY_PRIORITY_AND_ID` needs an optimized Redis-side index or if the current Java-side sort remains sufficient.

## Bottom Line

- Redis persistence is no longer just a plan. It is a real first backend with working SPI coverage and good early test evidence.
- The biggest remaining gap is not basic CRUD. The biggest gap is maturity compared to JDBC:
  - archive strategy breadth
  - recovery breadth beyond the currently proven flows
  - atomic multi-key correctness
- The module is in a solid v1 development state, but not yet in JDBC-level production-parity territory.
