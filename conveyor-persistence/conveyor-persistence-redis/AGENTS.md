# conveyor-persistence-redis Instructions

## Purpose
- Redis-backed persistence work lives here.
- This module is intentionally separate from `conveyor-persistence-jdbc`; Redis should not be forced through the JDBC engine abstraction.

## Read First
- `../AGENTS.md`
- `../doc/plans/redis-persistence.md`
- `./doc/project-context.md`

## Local Rules
- Keep Redis connection/bootstrap code here, not in the JDBC modules.
- Prefer Redis-native concepts such as keys, hashes, sorted sets, and expiry over table/schema analogies.
- For early learning tests, keep assertions focused on observable Redis behavior and clean up created keys.

## Validation
- `mvn -pl conveyor-persistence/conveyor-persistence-redis -am test`
- If you change shared persistence contracts later, validate the persistence core module too.
