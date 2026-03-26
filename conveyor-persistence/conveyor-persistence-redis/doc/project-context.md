# conveyor-persistence-redis

## Purpose

- Redis persistence module under active development.
- Current scope is a Redis-native v1 implementation of the `Persistence` SPI plus learning tests.

## Current State

Production code currently includes:

- `RedisConnectionFactory`
- `RedisPersistenceBuilder`
- `RedisPersistence`
- `RedisLuaScriptBundle`

The current implementation:

- stores itemized cart state in Redis hashes and indexes
- keeps the cart value bytes in `conv:{name}:part:{id}:payload`
- keeps the authoritative cart metadata in `conv:{name}:part:{id}:meta`
- supports builder-declared additional fields through:
  - `fields(...)`
  - `addField(Class<T>, String)`
  - `addField(Class<T>, String, accessor)`
- persists configured additional fields as explicit entries inside the Redis `:meta` hash
- rehydrates stored additional fields back into cart properties on Redis reads
- preserves additional fields through move-style archiving because archived carts now carry them as normal cart properties
- supports optional payload encryption through the same shared encryption builder pattern used by JDBC
- maintains Redis-native indexes for active parts, static parts, expirations, per-key part ids, and completed keys
- commits itemized cart writes through a Lua-backed atomic `savePart`
- uses Lua-backed cleanup for:
  - `archiveParts` delete operations
  - `archiveKeys` under the `DELETE` strategy
  - `archiveCompleteKeys`
  - `archiveExpiredParts` under the `DELETE` strategy
  - `archiveAll`
- supports Redis-appropriate archive strategies:
  - `DELETE`
  - `NO_ACTION`
  - `MOVE_TO_PERSISTENCE`
  - `MOVE_TO_FILE`
- intentionally does not support `SET_ARCHIVED`
- supports both internally managed and externally supplied `JedisPooled` clients
- supports builder-level Redis pool and client tuning for owned clients:
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
- relies on Jedis pooled borrow/return semantics for per-operation connection use
- validates namespace bootstrap metadata before using an existing Redis namespace
- bootstraps Redis namespace metadata lazily on first use when `autoInit(false)`
- validates Redis server version and required Redis command features during bootstrap
- registers the current Lua script bundle during bootstrap and reloads it on `NOSCRIPT`

Tests currently cover:

- basic Redis connectivity and CRUD
- basic Lua scripting proof with `EVAL`, `SCRIPT LOAD`, and `EVALSHA`
- `Persistence` contract methods with local Redis evidence
- `SCRIPT FLUSH` recovery for Lua-backed save and delete/archive cleanup paths
- Redis client ownership behavior for `copy()` and externally supplied clients
- encrypted payload round-trip, wrong-secret failure, and legacy-default compatibility
- direct `SecretKey`-based payload encryption
- persisted command-cart replay during `PersistentConveyor` startup
- incomplete-build replay across `PersistentConveyor` restart
- recovered explicit-acknowledge handle delivery for completed builds
- recovered explicit scrap-acknowledge handle delivery for `TIMED_OUT` and timeout-action `INVALID` builds
- recovered cleanup for the current READY-path auto-ack and explicit-ack completed builds when the recovered conveyor is allowed to drain cleanly
- recovered cleanup for the current CANCELED path when a recovered build is explicitly canceled and the conveyor is allowed to drain cleanly
- recovered cleanup for the current TIMED_OUT path when the recovered conveyor auto-acknowledges `TIMED_OUT` and is allowed to drain cleanly
- recovered cleanup for the current TIMED_OUT path when the recovered conveyor exposes an explicit scrap acknowledge handle and is allowed to drain after `ack()`
- recovered cleanup for the current timeout-action `INVALID` path when the recovered conveyor auto-acknowledges `INVALID` and is allowed to drain cleanly
- recovered cleanup for the current timeout-action `INVALID` path when the recovered conveyor exposes an explicit scrap acknowledge handle and is allowed to drain after `ack()`
- recovered `TIMED_OUT` unload behavior when `PersistentConveyor.unloadOnBuilderTimeout(true)` preserves carts for later replay and completion
- recovered timeout-action success behavior when a timeout action can satisfy the configured readiness rule after restart
- explicit Redis restore-order support:
  - `BY_ID` as the default
  - `NO_ORDER` as backend iteration order with no extra re-sorting
  - builder-selected `BY_PRIORITY_AND_ID` implementation:
    - `JAVA_SORT` as the default
    - `REDIS_INDEX` for Redis-side priority indexes on active, static, per-key, and replay-facing reads
    - expired reads still use the expiration index first and then Java-side priority sort
- itemized Redis storage shape and legacy whole-cart read compatibility
- stabilized itemized Redis payload layout for new writes:
  - `valueHint` stays in metadata
  - value bytes live only in `:payload`
  - older mirrored `valueData` records still read correctly
- configured additional-field metadata written independently from the generic `propertiesData` blob
- configured additional fields restored into cart properties on reads and preserved through move-to-persistence and move-to-file archiving
- first reproduced performance-test scenarios compatible with the current Redis state:
  - direct conveyor baseline
  - persistent conveyor shuffled load
  - persistent conveyor sorted load

## Runtime Assumptions

- Default Redis URI: `redis://localhost:6379`

Override options:

- system property `conveyor.persistence.redis.uri`
- env var `CONVEYOR_PERSISTENCE_REDIS_URI`
- perf-test size can be scaled with:
  - env var `REDIS_PERF_TEST_SIZE`
  - fallback env/system property `PERF_TEST_SIZE`

## Notes

- The module now implements the persistence SPI in a first Redis-native form.
- Redis payload encryption now reuses the same modernized shared protection path as JDBC:
  - managed default `AES/GCM/NoPadding`
  - legacy-default decrypt fallback for historical `AES/ECB/PKCS5Padding` payloads
- Current bootstrap semantics are now stronger than the initial stub:
  - namespace metadata is created once and then validated instead of being blindly rewritten
  - existing Redis namespace metadata must match the expected backend, backend version, and configured persistence name
  - existing Redis namespace metadata must also match the configured `priorityRestoreStrategy`
  - Redis server version must be present, parseable, and above the current conservative minimum support floor
  - required Redis command families are probed during bootstrap:
    - sequence/increment
    - string
    - hash
    - set
    - sorted-set
    - Lua scripting through `SCRIPT LOAD` and `EVALSHA`
  - `autoInit(false)` now means "skip upfront bootstrap, then validate or bootstrap lazily on first use"
  - bootstrap now records and validates the current Lua bundle metadata:
    - `scriptMode=lua`
    - `scriptBundleVersion=1`
  - bootstrap now records and validates the current priority-restore implementation choice:
    - `priorityRestoreStrategy=JAVA_SORT` or `REDIS_INDEX`
  - when older namespaces are missing `priorityRestoreStrategy`:
    - `JAVA_SORT` is adopted in place
    - `REDIS_INDEX` triggers a one-time rebuild of Redis priority indexes for existing active/static/per-key data
  - the current Lua bundle is loaded during bootstrap and reloaded automatically on `NOSCRIPT`
- Builder-level connection configuration is now broader than URI-only setup:
  - owned clients can be tuned through pool sizing, timeouts, database selection, client name, authentication, and SSL flags
  - externally supplied `JedisPooled` clients are still supported and remain the right choice when the host application owns Redis infrastructure configuration
- Builder-level metadata configuration is now broader too:
  - selected cart properties or cart-derived values can be persisted as explicit Redis metadata fields
  - this is the Redis analogue of JDBC `additionalFields`
  - it is separate from the generic persisted property map and separate from the intentional Redis non-goal around `uniqueFields`
- Archive behavior is now broader than the initial delete-only Redis stub:
  - `DELETE` remains the default
  - `NO_ACTION` is exposed through the builder
  - `MOVE_TO_PERSISTENCE` and `MOVE_TO_FILE` are implemented through Redis-specific low-level archive hooks
  - `SET_ARCHIVED` is intentionally unsupported because retained archived-state updates are a poor fit for the current Redis model
- Command-cart replay is now explicitly covered for the current recovered-command path.
- End-to-end recovery is now explicitly covered for the current restart-and-finish path, for recovered explicit acknowledgments, and for the current recovered cleanup paths.
- Current restore behavior is explicitly configurable and proven:
  - `BY_ID` is the default
  - `NO_ORDER` leaves Redis iteration order untouched
  - `BY_PRIORITY_AND_ID` now has an initialization-stage implementation choice:
    - `JAVA_SORT` re-sorts recovered carts in Java by priority descending and id ascending
    - `REDIS_INDEX` uses Redis priority indexes for active/static/per-key and replay-facing reads
    - expired reads still use Java-side sorting after expiration filtering
- Cleanup and acknowledgment parity are still not as broad as JDBC:
  - the current READY-path recovery and cleanup behavior is now proven
  - the current recovered CANCELED cleanup behavior is now proven
  - the current recovered TIMED_OUT cleanup behavior is now proven for both auto-acknowledged and explicit-scrap-acknowledged flows
  - the current recovered timeout-action `INVALID` cleanup behavior is now proven for both auto-acknowledged and explicit-scrap-acknowledged flows
  - the current recovered `TIMED_OUT` unload path is now proven when a later part arrives and finishes the build
  - the current recovered timeout-action success path is now proven for a builder-state readiness rule that the timeout action can satisfy
  - broader recovery-mode coverage still needs evidence only if new forward-conveyor recovery semantics are introduced later
- The current reader still accepts the earlier whole-cart Redis format for backward compatibility.
- New itemized writes no longer mirror value bytes into `:meta.valueData`.
- New itemized writes now reach Redis through a Lua-backed atomic `savePart`.
- The reader still accepts the earlier mirrored itemized value layout when older Redis data contains `valueData`.
- Redis performance coverage currently follows the JDBC perf style only where that matches the current Redis maturity:
  - reproduced now:
    - direct conveyor baseline
    - persistent conveyor shuffled load
    - persistent conveyor sorted load
  - not reproduced yet:
    - parallel persistent perf flows
    - archive-to-file or archive-to-persistence perf flows
    - broader unload/expiration perf scenarios
- Delete-style archive and cleanup operations are now Lua-backed.
- `MOVE_TO_PERSISTENCE` and `MOVE_TO_FILE` still use Java orchestration to export carts before Redis-side cleanup.
- Move-style archivers now tighten the interruption window by deleting each successfully exported batch immediately:
  - `MOVE_TO_PERSISTENCE` uses `Persistence.getMaxArchiveBatchSize()` as the move/delete batch size
  - `MOVE_TO_FILE` uses `BinaryLogConfiguration.getBucketSize()` as the move/delete batch size
  - `archiveAll()` now exports active/static ids incrementally, deletes completed keys, and only then clears the remaining namespace metadata/tracker keys
- Move-style archive export is intentionally not a singleness guarantee.
  - a crash between export and Redis-side deletion can still duplicate a batch in the destination archive
  - this is acceptable for Redis move-style archiving
  - each archived cart still carries its own unique id, so downstream de-duplication remains possible when a consumer truly needs it
  - replay from archive remains a business-sensitive operation even without duplicates, so duplication should not be treated as the only replay risk
- A future Redis-side batch manifest/status mechanism may still be useful for move-style exports, including file exports.
  - that would help with coordination, visibility, and restart handling
  - it would not change the intentional non-guarantee around archive singleness
- See `../doc/plans/redis-persistence.md` for the planned direction.
- See `./progress-report.md` for the current implementation status and JDBC comparison.
