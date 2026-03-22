# conveyor-persistence-redis

## Purpose

- Redis persistence module under active development.
- Current scope is a Redis-native v1 implementation of the `Persistence` SPI plus learning tests.

## Current State

Production code currently includes:

- `RedisConnectionFactory`
- `RedisPersistenceBuilder`
- `RedisPersistence`

The current implementation:

- stores itemized cart state in Redis hashes and indexes
- keeps the cart value bytes in `conv:{name}:part:{id}:payload`
- keeps the authoritative cart metadata in `conv:{name}:part:{id}:meta`
- supports optional payload encryption through the same shared encryption builder pattern used by JDBC
- maintains Redis-native indexes for active parts, static parts, expirations, per-key part ids, and completed keys
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

Tests currently cover:

- basic Redis connectivity and CRUD
- `Persistence` contract methods with local Redis evidence
- Redis client ownership behavior for `copy()` and externally supplied clients
- encrypted payload round-trip, wrong-secret failure, and legacy-default compatibility
- direct `SecretKey`-based payload encryption
- persisted command-cart replay during `PersistentConveyor` startup
- incomplete-build replay across `PersistentConveyor` restart
- recovered explicit-acknowledge handle delivery for completed builds
- recovered cleanup for the current READY-path auto-ack and explicit-ack completed builds when the recovered conveyor is allowed to drain cleanly
- recovered cleanup for the current CANCELED path when a recovered build is explicitly canceled and the conveyor is allowed to drain cleanly
- explicit Redis restore-order support:
  - `BY_ID` as the default
  - `NO_ORDER` as backend iteration order with no extra re-sorting
  - Java-side `BY_PRIORITY_AND_ID` for active, static, expired, and per-key reads
- itemized Redis storage shape and legacy whole-cart read compatibility
- stabilized itemized Redis payload layout for new writes:
  - `valueHint` stays in metadata
  - value bytes live only in `:payload`
  - older mirrored `valueData` records still read correctly
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
  - `autoInit(false)` now means "skip upfront bootstrap, then validate or bootstrap lazily on first use"
- Builder-level connection configuration is now broader than URI-only setup:
  - owned clients can be tuned through pool sizing, timeouts, database selection, client name, authentication, and SSL flags
  - externally supplied `JedisPooled` clients are still supported and remain the right choice when the host application owns Redis infrastructure configuration
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
  - `BY_PRIORITY_AND_ID` re-sorts recovered carts in Java by priority descending and id ascending
- Cleanup and acknowledgment parity are still not as broad as JDBC:
  - the current READY-path recovery and cleanup behavior is now proven
  - the current recovered CANCELED cleanup behavior is now proven
  - timeout-driven recovery cleanup is still not proven
  - broader status coverage and recovery modes still need evidence
- The current reader still accepts the earlier whole-cart Redis format for backward compatibility.
- New itemized writes no longer mirror value bytes into `:meta.valueData`.
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
- See `../doc/plans/redis-persistence.md` for the planned direction.
- See `./progress-report.md` for the current implementation status and JDBC comparison.
