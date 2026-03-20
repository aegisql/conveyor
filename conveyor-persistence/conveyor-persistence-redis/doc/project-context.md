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

- stores serialized carts as Redis payloads
- supports optional payload encryption through the same shared encryption builder pattern used by JDBC
- maintains Redis-native indexes for active parts, static parts, expirations, per-key part ids, and completed keys
- implements delete-style archive operations
- supports both internally managed and externally supplied `JedisPooled` clients
- relies on Jedis pooled borrow/return semantics for per-operation connection use

Tests currently cover:

- basic Redis connectivity and CRUD
- `Persistence` contract methods with local Redis evidence
- Redis client ownership behavior for `copy()` and externally supplied clients
- encrypted payload round-trip, wrong-secret failure, and legacy-default compatibility

## Runtime Assumptions

- Default Redis URI: `redis://localhost:6379`

Override options:

- system property `conveyor.persistence.redis.uri`
- env var `CONVEYOR_PERSISTENCE_REDIS_URI`

## Notes

- The module now implements the persistence SPI in a first Redis-native form.
- Redis payload encryption now reuses the same modernized shared protection path as JDBC:
  - managed default `AES/GCM/NoPadding`
  - legacy-default decrypt fallback for historical `AES/ECB/PKCS5Padding` payloads
- Current persistence behavior is intentionally delete-oriented; archive-to-other-persistence behavior is not implemented yet.
- Command-cart support is not explicitly covered yet by Redis tests.
- See `../doc/plans/redis-persistence.md` for the planned direction.
- See `./progress-report.md` for the current implementation status and JDBC comparison.
