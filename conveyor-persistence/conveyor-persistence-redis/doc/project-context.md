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
- maintains Redis-native indexes for active parts, static parts, expirations, per-key part ids, and completed keys
- implements delete-style archive operations

Tests currently cover:

- basic Redis connectivity and CRUD
- `Persistence` contract methods with local Redis evidence

## Runtime Assumptions

- Default Redis URI: `redis://localhost:6379`

Override options:

- system property `conveyor.persistence.redis.uri`
- env var `CONVEYOR_PERSISTENCE_REDIS_URI`

## Notes

- The module now implements the persistence SPI in a first Redis-native form.
- Current persistence behavior is intentionally delete-oriented; archive-to-other-persistence behavior is not implemented yet.
- Command-cart support is not explicitly covered yet by Redis tests.
- See `../doc/plans/redis-persistence.md` for the planned direction.
