# conveyor-persistence-ui Context

## Purpose
- `conveyor-persistence-ui` is a standalone Swing workbench for inspecting and operating Conveyor persistence backends.
- It is meant for initialization, connection management, light operational review, and raw persistence inspection.
- It is not an application-specific admin console. The UI stays intentionally close to the stored persistence model.

For a user-facing walkthrough, see:

- `conveyor-persistence/conveyor-persistence-ui/doc/user-manual.md`

## Current Product Shape
- Shaded fat jar with built-in support for:
  - JDBC persistence helpers:
    - MySQL
    - MariaDB
    - PostgreSQL
    - Oracle
    - SQL Server
    - SQLite
    - Derby embedded
  - Redis persistence
- Multiple saved connection profiles in a local SQLite database.
- One tab per opened connection.
- Live connection status polling with tab-level status dots:
  - green: connected and initialized
  - yellow: connected but persistence is not initialized
  - red: connection failed

## What The UI Supports

### Connection Profiles
- Create, edit, duplicate, delete, and open saved profiles.
- Backend-specific setup form behavior:
  - MySQL and MariaDB:
    - host, port, database, user, password
    - schema is not shown
  - PostgreSQL, Oracle, SQL Server:
    - host, port, database, optional schema, user, password
  - SQLite:
    - local database file path
  - Derby embedded:
    - database home directory
    - database name
    - actual Derby path is `<home-directory>/<database-name>`
  - Redis:
    - Redis URI
    - persistence name / namespace
- Lookup actions from the editor:
  - JDBC database lookup where supported
  - JDBC schema lookup where supported
  - Redis persistence-name lookup by scanning `conv:{name}:meta`
- Stored-value decryption settings per profile:
  - encryption mode
  - encryption secret
  - secure secret storage through the same credential-store layer used for connection passwords

### Initialization And Maintenance
- JDBC:
  - initialize persistence
  - generate initialization SQL
  - view/copy/save initialization SQL from the UI
- Redis:
  - initialize persistence namespace
  - no SQL script path
- Both backend families:
  - archive expired data
  - archive all data

### Inspection
- Raw preview tabs with page navigation.
- Current preview page size:
  - `200` rows per table view
- Cell-details area at the bottom:
  - selected cell value only in the main pane
  - table / row / column metadata in the lower status bar
- Per-connection, per-table hidden-column preferences stored in the profile store.
- Status/info dialog with:
  - summary entries rendered as `Field / Value`
  - backend-specific info tabs
  - JDBC index metadata when the driver and permissions allow it

## Persistence-Store Model

### Local UI Profile Store
- Default profile-store path:
  - `~/.conveyor/persistence-ui/profiles.db`
- Stores:
  - saved connection profiles
  - hidden-column preferences
- Depending on the selected credential-store mode, it may also store:
  - master-password store state
  - encrypted credentials
- Hidden-column preferences are keyed by:
  - profile id
  - table title
  - column name

### Credential Storage
- Current behavior:
  - keep non-secret connection metadata in `profiles.db`
  - keep secrets behind a dedicated credential-store abstraction
- Preferred operating mode:
  - use OS-backed secure storage when it is available and healthy
- Required fallback:
  - use a master-password-based encrypted store for environments where OS keychain integration is missing, unreliable, or operationally awkward

Current selection logic:

- macOS:
  - use native keychain integration through `/usr/bin/security` when available
- Windows:
  - use a DPAPI-protected local credential store when PowerShell is available
- Linux:
  - use the desktop secret service through `secret-tool` when it is available
- other environments:
  - use the master-password encrypted store

Current secret-handling behavior:

- passwords are not written to the `profiles.password` column on normal save
- legacy plaintext passwords already present in `profiles.db` are migrated lazily on first credential access
- the master-password store keeps:
  - a verifier record in `credential_store_state`
  - encrypted secrets in `stored_credentials`
- the master-password-derived AES key is cached only for the current UI session after successful unlock

This choice is intentionally biased toward desktop usability rather than profile portability.

- Moving the local UI database between machines is not a primary requirement.
- Recreating a small local connection profile database is acceptable.

Current platform behavior:

- macOS:
  - native keychain integration is implemented and preferred
- Windows:
  - a Windows-user-bound DPAPI store is implemented
  - it stores protected credential blobs in the local profile database
  - it does not depend on a separate application master password
- Linux:
  - native secret-store integration is implemented through `secret-tool`
  - if `secret-tool` is missing or unusable, the app falls back to the master-password store

Credential storage shape:

- `profiles.db`
  - display names
  - backend type
  - host, port, database, schema
  - persistence name
  - table names
  - UI preferences such as hidden columns
- credential store
  - JDBC password
  - Redis password
  - viewer decryption secret for encrypted cart values
  - any future secret fields

### Status Polling
- Each open tab polls connection status every `3` seconds.
- Polling is lightweight:
  - JDBC uses a small health query such as `SELECT 1` or Derby `VALUES 1`
  - Redis uses `PING`
- Polling updates the colored dot even when the last loaded snapshot is stale.

## Backend-Specific Behavior That Matters

### JDBC
- Network JDBC profiles do not invent a default database.
- For MySQL, MariaDB, PostgreSQL, and SQL Server, the UI can often connect at the server level before the target database exists.
- Initialization actions still require the intended database name.
- Derby embedded is treated as:
  - home directory plus database name
  - not as a single pre-existing final path
- JDBC previews are raw table previews for the configured `PART` and `COMPLETED_LOG` tables.
- `CART_VALUE` is decoded when `VALUE_TYPE` maps to a built-in persistence converter.
- If `VALUE_TYPE` indicates encrypted storage and the profile has the right secret and mode configured, the UI tries to decrypt `CART_VALUE` before rendering it.
- If no decryption secret is configured, encrypted JDBC values stay visible as:
  - `<encrypted payload>`
- If the configured secret or mode is wrong, the preview shows:
  - `<decryption failed>`

### Redis
- Redis profiles use the persistence name, not the runtime conveyor name.
- The UI expects Redis keys under:
  - `conv:{persistence-name}:...`
- Redis previews show:
  - namespace metadata
  - active parts
  - static parts
  - completed keys
- Redis preview tries to decode key, label, and value using built-in converter hints.
- Redis value preview also supports encrypted stored values when the profile includes the matching decryption mode and secret.
- When the exact application class is not on the UI classpath, the UI falls back to readable best-effort text where it can.

## Deliberate Limits
- The workbench is persistence-oriented, not business-object-oriented.
  - it does not try to become a full domain admin tool
- It does not expose the full JDBC or Redis builder surface.
  - advanced pooling, custom converters, external clients, and arbitrary encryption-builder tuning are out of scope in the UI
- The UI supports only these viewer decryption modes:
  - none
  - managed default AES/GCM
  - legacy AES/ECB default compatibility
- It does not decode arbitrary application payload classes unless built-in persistence converters are enough
- Redis does not have an initialization-script workflow
- Index metadata is best-effort and depends on JDBC metadata support and database permissions

## Residual Risks And Review Findings
- Linux native secret storage depends on `secret-tool` and an available unlocked secret-service environment.
- Windows native storage currently uses DPAPI-backed local protection, not Credential Manager browsing or management APIs.
- Existing plaintext passwords in old `profiles.db` files are migrated on first access, not proactively in a full-database batch pass.

## Entry Point
- Main class:
  - `com.aegisql.conveyor.persistence.ui.PersistenceWorkbenchApp`

## Packaging
- The module builds a shaded fat jar so the workbench can be distributed with JDBC drivers and Redis support together.
