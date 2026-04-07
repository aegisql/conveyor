# conveyor-persistence-ui Context

## Purpose
- Standalone Swing workbench for Conveyor persistence backends.
- First cut focuses on connection profiles, initialization support, and simple read-only inspection.

## Current Scope
- Save multiple persistence connection profiles in a small local SQLite database.
- Connect to built-in JDBC persistence targets:
  - MySQL
  - MariaDB
  - PostgreSQL
  - Oracle
  - SQL Server
  - SQLite
  - Derby embedded
- Connect to Redis persistence namespaces.
- Detect whether a target is reachable but not initialized.
- Initialize JDBC or Redis persistence from the UI.
- Generate JDBC initialization SQL, copy it, and save it to a file.
- Show a simple raw preview of stored data:
  - JDBC: raw `PART` / `COMPLETED_LOG` table previews
  - Redis: namespace metadata, active/static part metadata, completed keys
- Run basic maintenance commands:
  - archive expired data
  - archive all data
- For network JDBC engines, the UI does not invent a default database or service name.
  - Users must choose the actual target database for initialization and maintenance actions.
  - MySQL and MariaDB connection probes can still connect without a selected database.

## Deliberate First-Cut Limits
- The UI does not try to decode application-specific payload classes for preview.
  - JDBC preview is raw SQL-table oriented.
  - Redis preview is raw metadata oriented.
- Redis initialization does not have an SQL-script concept.
- Advanced JDBC engine wiring, custom converters, and full persistence-builder surface are not exposed in the UI yet.
- Credentials are stored in the local profile SQLite database as entered.
  - This is acceptable for the first cut but should be revisited if the app grows into a broader operational tool.

## Saved Profiles
- Default local profile store path:
  - `~/.conveyor/persistence-ui/profiles.db`

## Entry Point
- `com.aegisql.conveyor.persistence.ui.PersistenceWorkbenchApp`

## Packaging
- The module builds a shaded fat jar so the workbench can ship with JDBC drivers and Redis support in one artifact.
