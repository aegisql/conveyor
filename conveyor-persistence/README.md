# conveyor-persistence

Persistence backends and recovery infrastructure for Conveyor.

This module group gives you a common persistence contract and multiple storage backends. The main choice is not only "which database do I have", but also:

- how much infrastructure you want to operate
- how much relational behavior you need
- how important current feature maturity is
- whether you need a lightweight local setup or a long-lived production store

## Module Layout

- `conveyor-persistence-core`
  - database-neutral SPI, `PersistentConveyor`, archive infrastructure, converters
- `conveyor-persistence-jdbc`
  - shared JDBC runtime and generic JDBC builder
- `conveyor-persistence-jdbc-derby`
  - Derby helper module
- `conveyor-persistence-jdbc-sqlite`
  - SQLite helper module
- `conveyor-persistence-jdbc-mysql`
  - MySQL helper module
- `conveyor-persistence-jdbc-mariadb`
  - MariaDB helper module
- `conveyor-persistence-jdbc-postgres`
  - PostgreSQL helper module
- `conveyor-persistence-jdbc-oracle`
  - Oracle helper module
- `conveyor-persistence-jdbc-sqlserver`
  - Microsoft SQL Server helper module
- `conveyor-persistence-redis`
  - Redis-native backend, currently behind JDBC in feature parity

## Common Features

All current persistence backends are built around the same core ideas:

- `Persistence<K>` as the storage contract
- `PersistentConveyor` for recovery-aware conveyor execution
- persisted carts, static parts, completed build keys, and archive operations
- builder-based configuration
- optional application-level payload encryption

### Connection Ownership

Connection and pooling strategy are part of persistence design.

JDBC supports multiple connection models:

- direct driver-manager connections
- client-provided `DataSource`
- client-provided pooled `DataSource`
- built-in DBCP2 pool support
- externally supplied connection suppliers for direct or pooled usage

This matters because JDBC persistence must make an explicit distinction between:

- closing a direct connection
- returning a borrowed connection to a pool

Redis is different:

- the current Redis backend uses `JedisPooled`
- the persistence object owns or shares the pooled client wrapper
- individual operations borrow and return actual Redis connections through the client internally

### Encryption

Encryption is optional.

- If you do not configure an encryption secret or key, payloads are stored unencrypted.
- If you do configure encryption, the current managed default is:
  - `AES/GCM/NoPadding`
- Supported key entry styles:
  - passphrase/secret text
  - `SecretKey`

Current compatibility behavior:

- Modern readers can still decrypt historical payloads written by the older default encryption path.
- The historical default path was effectively:
  - `AES/ECB/PKCS5Padding`
  - older secret-to-key derivation
- JDBC still allows explicitly selecting the legacy cipher settings when you need migration or compatibility testing.

Backend-specific encryption scope:

- JDBC
  - encrypts persisted payload bytes through the shared converter/encryption path
- Redis
  - currently encrypts cart value payload bytes only
  - Redis lookup/index keys remain deterministic and unencrypted so the backend can still query them efficiently

## How To Choose

Use these criteria as weights:

- `setup effort`
  - how hard it is to get running locally
- `operational weight`
  - how much external infrastructure you must operate
- `current project maturity`
  - how complete the current Conveyor persistence implementation is
- `relational feature fit`
  - how much you want SQL/database-native indexing and mature JDBC behavior
- `deployment fit`
  - whether the backend matches the databases your application already uses

## Short Recommendation

- Choose `SQLite` when you want the easiest file-based local persistence with no separate server.
- Choose `Derby` when you want an embedded Java database and strong local testability.
- Choose `PostgreSQL`, `MySQL`, or `MariaDB` when you want a production relational backend on common open-source infrastructure.
- Choose `Oracle` or `SQL Server` when your environment already standardizes on those platforms.
- Choose generic JDBC when your database has a JDBC driver but is not covered by one of the packaged helper modules.
- Choose `Redis` only when a Redis-native persistence model is a deliberate goal and you accept that it is still behind JDBC in feature coverage.

## Decision Matrix

Scores are relative to the current repository state, not to the databases in general.

| Backend | Artifact To Use | Setup Effort | Ops Weight | Current Project Maturity | Best Fit |
|---|---|---:|---:|---:|---|
| Derby | `com.aegisql.persistence:conveyor-persistence-jdbc-derby` | 5/5 | 5/5 | 5/5 | Embedded Java apps, local recovery tests, no external DB |
| SQLite | `com.aegisql.persistence:conveyor-persistence-jdbc-sqlite` | 5/5 | 5/5 | 5/5 | Single-file local persistence, simple deployments |
| PostgreSQL | `com.aegisql.persistence:conveyor-persistence-jdbc-postgres` | 3/5 | 3/5 | 5/5 | Production relational deployments on PostgreSQL |
| MySQL | `com.aegisql.persistence:conveyor-persistence-jdbc-mysql` | 3/5 | 3/5 | 5/5 | Production relational deployments on MySQL |
| MariaDB | `com.aegisql.persistence:conveyor-persistence-jdbc-mariadb` | 3/5 | 3/5 | 5/5 | Production relational deployments on MariaDB |
| Oracle | `com.aegisql.persistence:conveyor-persistence-jdbc-oracle` | 2/5 | 2/5 | 4/5 | Environments already committed to Oracle |
| SQL Server | `com.aegisql.persistence:conveyor-persistence-jdbc-sqlserver` | 2/5 | 2/5 | 4/5 | Environments already committed to SQL Server |
| Generic JDBC | `com.aegisql.persistence:conveyor-persistence-jdbc` plus your driver and engine configuration | 2/5 | 2/5 | 4/5 | JDBC-capable databases outside the packaged helper set |
| Redis | `com.aegisql.persistence:conveyor-persistence-redis` | 4/5 | 3/5 | 2/5 | Redis-native experiments and staged backend work |

How to read this:

- `5/5 setup effort` means easiest to start with
- `5/5 ops weight` means lowest operational burden
- `5/5 current project maturity` means best parity with the current Conveyor persistence feature set

## What Stays The Same Across JDBC Backends

All JDBC helper modules share the same builder/runtime shape from `conveyor-persistence-jdbc`.

Common JDBC characteristics:

- relational storage model
- explicit database/schema/table initialization through the builder, optional
- broader current feature maturity than Redis
- mature recovery flows through `PersistentConveyor`
- common optional encryption path
- shared archiving and restore behavior
- explicit connection ownership choices

In practice, choosing one JDBC helper over another is mostly about:

- what database you already run
- whether you want embedded or server-based storage
- how much database administration you want
- engine-specific initialization and SQL behavior

Important scope note:

- the helper modules are the packaged, ready-to-use JDBC options maintained in this repository
- the JDBC layer itself is more general than that
- if a database has a JDBC driver and you can provide an appropriate engine/table model, the base JDBC persistence can still be used

So there are two valid JDBC paths:

- packaged helper module for a maintained database family
- generic JDBC integration for another database with the right driver and engine behavior

## What Is Different

### Derby

What it does well:

- embedded relational database inside the JVM
- no separate database service required
- very convenient for local and integration testing
- strong current test evidence in this repository

Use it when:

- you want persistence without adding a separate server
- you want to exercise realistic JDBC recovery behavior in tests

Tradeoffs:

- not the typical choice for externally managed production database platforms

### SQLite

What it does well:

- single-file persistence
- very simple local setup
- good fit for local tools, demos, and lightweight deployments

Use it when:

- you want the smallest operational footprint
- one file on disk is the right persistence shape

Tradeoffs:

- fewer natural parallels with server-managed relational deployments than PostgreSQL/MySQL/MariaDB

### PostgreSQL

What it does well:

- strong fit for server-side production relational deployments
- common operational choice in modern JVM stacks

Use it when:

- your application already uses PostgreSQL
- you want production SQL persistence without changing platform choices

Tradeoffs:

- requires a real database service

### MySQL / MariaDB

What they do well:

- familiar production relational choices
- good fit when your environment already uses these engines

Use them when:

- your operational standard is MySQL or MariaDB
- you want Conveyor persistence close to the rest of your SQL stack

Tradeoffs:

- requires a real database service
- choose the module that matches the actual driver and engine you run

### Oracle

What it does well:

- fits environments where Oracle is already the mandated relational platform

Use it when:

- Oracle is already part of the platform decision

Tradeoffs:

- heavier setup and operations than Derby/SQLite
- not the simplest starting point for development unless Oracle is already available

### SQL Server

What it does well:

- fits environments standardized on Microsoft SQL Server

Use it when:

- SQL Server is already the target platform

Tradeoffs:

- heavier setup and operations than Derby/SQLite
- not the simplest local starting point unless a SQL Server environment already exists

### Generic JDBC

What it does well:

- lets you use the JDBC persistence runtime beyond the databases packaged in this repository
- lets the application choose its own connection or pooling model

Use it when:

- your database has a JDBC driver
- you want Conveyor persistence to plug into an application-owned `Connection` or `DataSource`
- you are ready to provide the engine-specific behavior and table model needed by that database

Tradeoffs:

- less turnkey than the packaged helper modules
- more responsibility moves to the integrator

### Redis

What it does well today:

- Redis-native persistence model instead of forcing Redis into SQL-style abstractions
- SPI-level storage and retrieval are implemented
- itemized Redis storage shape with cart metadata in hashes and value bytes in a separate payload key
- optional payload encryption uses the same shared encryption core as JDBC

Use it when:

- you deliberately want a Redis backend
- you are comfortable adopting a backend that is still growing toward JDBC parity

Current differences from JDBC:

- Redis archive behavior now supports:
  - `DELETE` as the default
  - `NO_ACTION`
  - `MOVE_TO_PERSISTENCE`
  - `MOVE_TO_FILE`
  - `SET_ARCHIVED` is intentionally unsupported because retained archived-state updates are a poor fit for the current Redis model
- Redis intentionally does not implement `uniqueFields`; if you need database-enforced uniqueness constraints, choose a JDBC backend
- Redis now does support builder-declared additional fields as explicit part metadata
  - this is the Redis analogue of JDBC `additionalFields`
  - Redis read paths rehydrate those fields into cart properties, so current move-style archive flows preserve them
  - it is intentionally separate from the `uniqueFields` non-goal
  - the first increment is metadata-oriented, not relational
- Redis now does support JDBC-style custom binary converter registration
  - class-based and label-based `addBinaryConverter(...)` registration are available in the Redis builder
  - custom converters are used for payload values and for Redis additional-field metadata
- Redis now registers a basic persistence MBean during `build()`
  - the builder exposes `getJMXObjName()`
  - `Persistence.byName(...)` can resolve the registered Redis persistence by that JMX name
- restore-order support now includes:
  - `BY_ID` as the default
  - `NO_ORDER` as backend iteration order
  - `BY_PRIORITY_AND_ID` with an initialization-stage choice:
    - `JAVA_SORT` as the default
    - `REDIS_INDEX` when you want Redis to maintain extra priority indexes for active/static/per-key and replay-facing reads
    - expired reads still use the expiration index first and then Java-side priority sorting
    - the better choice is workload-dependent, so users should evaluate it on their own real data loads when tuning matters
- command-cart behavior still needs broader proof
- Redis now exposes the main JDBC-style persistence-filter convenience methods, though the JDBC stack still has a broader accumulated operational surface
- current save and delete-style cleanup paths are Lua-backed atomic units
- move-style archive flows still use Java orchestration before Redis-side cleanup, but they now delete each successfully exported batch immediately so interruption risk is bounded by the configured batch size rather than the whole request
- move-style archive export is intentionally at-least-once at batch granularity rather than singleness-guaranteed
  - duplicate archive records are acceptable
  - archived cart ids still allow downstream de-duplication when a consumer needs it
  - replay from archive should be treated as a business-sensitive special case even without duplicates
- a later Redis enhancement may use Redis itself to hold batch key collections and processing status for move-style export coordination, including file export, but that would be for orchestration visibility rather than singleness guarantees

Read before choosing Redis:

- `./conveyor-persistence-redis/doc/project-context.md`
- `./conveyor-persistence-redis/doc/progress-report.md`
- `./doc/plans/redis-persistence.md`

## Recommendation By Scenario

### I want the easiest local start

Choose:

- `SQLite` first
- `Derby` second

### I want embedded JDBC behavior with strong local test evidence

Choose:

- `Derby`

### I already have a production SQL platform

Choose the matching helper:

- PostgreSQL
- MySQL
- MariaDB
- Oracle
- SQL Server

### I have another JDBC-capable database

Choose:

- the generic JDBC path

That means:

- use `conveyor-persistence-jdbc`
- add your driver
- choose the right direct or pooled connection strategy
- provide the engine behavior needed for that database

### I want the most mature Conveyor persistence behavior today

Choose:

- one of the JDBC backends

### I want Redis specifically

Choose:

- `Redis`, but do it knowingly

That means:

- good for staged adoption and backend experimentation
- not yet the best choice when you need full JDBC-level persistence behavior today

## Practical Dependency Rule

For the packaged relational engines, depend on the helper module that matches your database.

Examples:

- `com.aegisql.persistence:conveyor-persistence-jdbc-sqlite`
- `com.aegisql.persistence:conveyor-persistence-jdbc-derby`
- `com.aegisql.persistence:conveyor-persistence-jdbc-postgres`

Those helper modules bring in:

- the shared JDBC persistence runtime
- the matching database driver
- the engine-specific implementation

If you are integrating with another JDBC-capable database:

- depend on `com.aegisql.persistence:conveyor-persistence-jdbc`
- add your JDBC driver
- choose the connection ownership model that matches your application
- provide the engine-specific behavior required by that database

For Redis, depend on:

- `com.aegisql.persistence:conveyor-persistence-redis`

## Usage Examples

### JDBC Example

This example uses:

- a packaged JDBC helper module
- an application-owned pooled `DataSource`
- optional payload encryption
- `PersistentConveyor` recovery wrapping

```java
import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;
import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;

import javax.sql.DataSource;

DataSource pooledDataSource = buildApplicationPool();

Persistence<Integer> persistence =
        JdbcPersistenceBuilder.presetInitializer("postgres", Integer.class)
                .database("conveyor_db")
                .schema("conveyor_app")
                .partTable("PART")
                .completedLogTable("COMPLETED_LOG")
                .dbcpConnection(pooledDataSource)
                .autoInit(true)
                .encryptionSecret("change-this-secret")
                .build();

AssemblingConveyor<Integer, String, String> conveyor = new AssemblingConveyor<>();
PersistentConveyor<Integer, String, String> persistent =
        persistence.wrapConveyor(conveyor);
```

Notes:

- `dbcpConnection(dataSource)` is the pool-oriented path.
- `jdbcConnection(dataSource)` is the shared/cached path.
- `driverManagerJdbcConnection()` is the direct non-`DataSource` path.
- If you are targeting another JDBC-capable database, the same persistence shape can be used with `conveyor-persistence-jdbc` plus your driver and engine-specific configuration.

### Redis Example

This example uses:

- a shared `JedisPooled` client owned by the application
- optional payload encryption
- a named Redis namespace
- `PersistentConveyor` recovery wrapping

```java
import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;
import com.aegisql.conveyor.persistence.redis.RedisPersistenceBuilder;
import redis.clients.jedis.JedisPooled;

JedisPooled jedis = new JedisPooled("redis://localhost:6379");

Persistence<Integer> persistence =
        new RedisPersistenceBuilder<Integer>("orders")
                .jedis(jedis)
                .autoInit(true)
                .encryptionSecret("change-this-secret")
                .build();

AssemblingConveyor<Integer, String, String> conveyor = new AssemblingConveyor<>();
PersistentConveyor<Integer, String, String> persistent =
        persistence.wrapConveyor(conveyor);
```

Notes:

- the builder name, `"orders"` in this example, becomes the Redis namespace root
- each persisted part is currently represented by:
  - `conv:{name}:part:{id}:meta` for cart metadata
  - `conv:{name}:part:{id}:payload` for the cart value bytes
- if you do not inject a `JedisPooled`, the builder can create its own client from the configured Redis URI
- when the builder owns the Redis client, it can now tune pool and client settings directly, for example:
  - `maxTotal(...)`
  - `maxIdle(...)`
  - `minIdle(...)`
  - `connectionTimeoutMillis(...)`
  - `socketTimeoutMillis(...)`
  - `blockingSocketTimeoutMillis(...)`
  - `database(...)`
  - `clientName(...)`
  - `user(...)`
  - `password(...)`
  - `ssl(...)`
- externally supplied `JedisPooled` clients remain the right option when the host application already owns Redis connection infrastructure
- current Redis encryption protects value payload bytes, not Redis lookup/index keys

## Concept Mapping

The same Conveyor concepts land in different storage structures depending on the backend.

| Conveyor Concept | JDBC Mapping | Redis Mapping |
|---|---|---|
| logical persistence name | database + schema + table set chosen in the builder; helper presets give starting defaults that can be overridden | builder `name`, mapped to the Redis namespace `conv:{name}:*` |
| part storage | rows in the part table, `PART` by default unless overridden | `conv:{name}:part:{id}:meta` plus `conv:{name}:part:{id}:payload` and Redis index keys |
| part metadata | relational columns such as key, label, load type, timestamps, priority, properties, and configured additional fields | fields in `conv:{name}:part:{id}:meta` |
| part value | serialized or converted payload stored in the row value column(s) | serialized or converted bytes in `conv:{name}:part:{id}:payload` |
| completed build keys | rows in the completed log table, `COMPLETED_LOG` by default unless overridden | members of `conv:{name}:completed` |
| static parts | stored in the same relational part model and restored through SQL queries | tracked in `conv:{name}:parts:static` |
| active parts | queried from the relational part table and indexes | tracked in `conv:{name}:parts:active` |
| expiration | `EXPIRATION_TIME` column in the relational part model | sorted set `conv:{name}:parts:expires` plus per-part metadata |
| per-key part lookup | SQL queries and indexes over cart key fields | sorted sets under `conv:{name}:parts:key:{encodedKey}` |
| archive-all operation | archive strategy applied over the relational tables | delete of the tracked Redis namespace keys |
| payload encryption | encrypted payload bytes in the stored row value | encrypted payload bytes in `part:{id}:payload` only |

Practical interpretation:

- In JDBC, the main mental model is:
  - database/schema/table design
- In Redis, the main mental model is:
  - one namespace prefix plus sets, sorted sets, hashes, and payload keys

## Related Docs

- JDBC notes:
  - `./conveyor-persistence-jdbc/doc/project-context.md`
  - `./conveyor-persistence-jdbc/doc/integration-tests.md`
- Redis notes:
  - `./conveyor-persistence-redis/doc/project-context.md`
  - `./conveyor-persistence-redis/doc/progress-report.md`
  - `./conveyor-persistence-redis/doc/redis-cheat-sheet.md`
- Redis long-term plan:
  - `./doc/plans/redis-persistence.md`
