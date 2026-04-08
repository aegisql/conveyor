# Conveyor Persistence Workbench Manual

This manual is for people using the UI, not extending it.

If you want the implementation-focused module summary, see:

- `conveyor-persistence/conveyor-persistence-ui/doc/project-context.md`

## What This Tool Is For
- Save connections to Conveyor persistence backends.
- Check whether a persistence is reachable and initialized.
- Initialize a persistence when the backend supports it.
- Preview what is stored there in a raw but readable form.
- Run a few maintenance actions without writing custom code.

Think of it as a small specialized workbench for Conveyor persistence, not a general database browser.

## Before You Start
- Build the fat jar:

```bash
mvn -pl conveyor-persistence/conveyor-persistence-ui -am -DskipTests package
```

- Run it:

```bash
java -jar conveyor-persistence/conveyor-persistence-ui/target/conveyor-persistence-ui-<version>.jar
```

[Screenshot Placeholder: Application window on first launch]

## Main Screen
- Left side:
  - saved connections
- Right side:
  - one tab per opened connection
- Top toolbar inside each tab:
  - info
  - choose visible columns
  - paging
  - refresh
  - initialize
  - show initialization preview when supported
  - archive expired
  - archive all
- Bottom area:
  - full content of the selected cell
  - small status bar with table / row / column

[Screenshot Placeholder: Main window with saved connections on the left and one open tab on the right]

## Status Colors
- Green:
  - connected and initialized
- Yellow:
  - connected, but the persistence tables or namespace are not initialized yet
- Red:
  - the backend is not reachable right now

The dot on each tab updates automatically.

[Screenshot Placeholder: Three tabs showing green, yellow, and red status dots]

## Creating A Connection
- Click `New Connection`.
- Choose the backend type first.
- The form changes depending on the selected backend.
- Fill in the connection details.
- If the persistence stores encrypted cart values, choose the matching value-encryption mode and enter the decryption secret.
- Save the profile.
- Select it and click `Open`.

[Screenshot Placeholder: Persistence Profile dialog after choosing a backend type]

## Backend-Specific Setup

### MySQL And MariaDB
Use:
- host
- port
- database
- user
- password
- optional value-encryption mode and decryption secret
- part table
- completed log table

You do not need a schema field here because the UI does not use one for these backends.

### PostgreSQL, Oracle, SQL Server
Use:
- host
- port
- database
- optional schema
- user
- password
- optional value-encryption mode and decryption secret
- part table
- completed log table

### SQLite
Use:
- database file
- optional value-encryption mode and decryption secret
- part table
- completed log table

The `Browse` button picks the file path.

### Derby Embedded
Use:
- database home directory
- database name
- optional value-encryption mode and decryption secret
- part table
- completed log table

Important:
- the actual Derby database path is:
  - `<home-directory>/<database-name>`

Example:
- Home directory:
  - `/data/derby`
- Database name:
  - `orders`
- Actual Derby database:
  - `/data/derby/orders`

### Redis
Use:
- Redis URI
- persistence name / namespace
- optional value-encryption mode and decryption secret

Important:
- this is the Redis persistence name
- it is not the runtime conveyor name
- the UI looks for keys like:
  - `conv:{name}:meta`

If you are not sure which Redis persistence names exist, use the `Lookup` button next to the persistence-name field.

## Using Lookup Buttons
Some fields have `Lookup` next to them.

These are there to save guesswork:
- database lookup:
  - shows available databases when the backend supports that
- schema lookup:
  - shows available schemas when the backend supports that
- Redis persistence lookup:
  - scans Redis for available Conveyor namespaces

Choose a value from the list and the field is filled in for you.

[Screenshot Placeholder: Lookup dialog showing database or Redis persistence choices]

## Initializing Persistence

### JDBC
When the database is reachable but the Conveyor tables are missing:
- the tab should be yellow
- `Initialize Persistence` becomes available

You can also open the initialization preview instead of initializing directly.

For JDBC, that preview window includes:
- `SQL`
  - generated initialization SQL
- `Java`
  - equivalent Java builder code that performs the same initialization

This is useful when:
- initialization should be reviewed first
- a DBA wants to run the script separately
- you want to save either the SQL or Java example for later use

[Screenshot Placeholder: JDBC tab before initialization with Initialize and Initialization Preview enabled]
[Screenshot Placeholder: Initialization preview window with SQL and Java tabs]

### Redis
Redis does not use SQL scripts.

For Redis, the same preview window opens with:
- `Java`
  - example `RedisPersistenceBuilder` code that initializes the namespace

There is no SQL tab for Redis.

When the Redis server is reachable but the namespace is not initialized:
- the tab should be yellow
- `Initialize Persistence` becomes available

## Browsing Data
The preview is intentionally raw. It shows persistence data, not application-specific screens.

Typical tabs include:
- JDBC:
  - `Parts`
  - `Completed Keys`
- Redis:
  - `Active Parts`
  - `Static Parts`
  - `Completed Keys`

Use the page buttons to move through larger datasets.

Current default preview size:
- `200` rows per page

[Screenshot Placeholder: Open tab showing preview tables and page navigation]

## Viewing Encrypted Values
If a persistence stores encrypted cart values, the UI can try to decrypt them while previewing data.

Set this in the connection profile:
- value-encryption mode
- encryption secret

Supported viewer modes:
- `None`
- `Managed AES/GCM (default)`
- `Legacy AES/ECB`

What you will see in the preview:
- correct mode and secret:
  - the stored value is shown normally
- no secret or no mode:
  - the cell shows `<encrypted payload>`
- wrong secret or wrong mode:
  - the cell shows `<decryption failed>`

The decryption secret is stored through the secure credential-store path, not as plain profile text.

[Screenshot Placeholder: Encrypted value preview before and after configuring a decryption secret]

## Reading Large Values
If a table cell contains a long value:
- click the cell
- the full value appears in the lower details area

The lower status bar shows:
- table
- row
- column

This is the easiest way to inspect long payloads, long metadata values, or serialized hints.

[Screenshot Placeholder: Selected table cell with full value shown in the lower details area]

## Hiding Columns
If a preview table is too noisy:
- click the visible-columns button
- uncheck the columns you do not want to see

The choice is remembered:
- per saved connection
- per table

So you can keep one profile focused on the columns you care about most.

[Screenshot Placeholder: Column chooser dialog with checkboxes]

## Connection Info Window
Click the `info` button in a tab to open a separate status window.

This window shows:
- summary rows in a `Field / Value` table
- backend-specific metadata
- JDBC index information when available

This is the fastest place to confirm:
- which backend you are really connected to
- which tables are present
- what namespace or URL is in use
- whether indexes exist

[Screenshot Placeholder: Connection Info window with Summary and Indexes tabs]

## Maintenance Actions

### Archive Expired
Use this when you only want to remove or archive expired data.

### Archive All
Use this carefully.

It is a destructive operational action for the selected persistence.

The UI asks for confirmation first, but you should still treat it as a real maintenance action, not a harmless refresh.

## Right-Click Menus

### In The Saved Connections List
Right-click a saved connection to:
- open
- edit
- delete

### On An Open Tab
Right-click a tab to:
- close

Each open tab also has its own close button.

## Practical Tips
- If Redis looks uninitialized even though you think data exists:
  - check that you entered the persistence name, not the conveyor runtime name
- If Derby initialization fails:
  - make sure the database name is a valid Derby identifier
  - remember that the selected directory is the home directory, not the final database path
- If a JDBC server exists but the target database does not:
  - keep the intended database name in the profile
  - initialize from the UI or generate the script first
- If labels or values are only partly readable:
  - the UI may not have your application classes on its classpath
  - built-in converter hints are handled better than application-specific classes
- If an encrypted value still shows `<decryption failed>`:
  - recheck both the encryption mode and the secret in the profile
  - `Managed AES/GCM (default)` is the right first choice for current encrypted payloads
  - use `Legacy AES/ECB` only for older data written with the legacy default encryption setup
- If the app asks for a master password:
  - that is the fallback secure-storage mode for environments without native keychain integration
  - you will be asked to create it the first time a secret needs to be stored
- If you are on macOS:
  - saved passwords go to the system keychain instead of staying in the profile row itself
- If you are on Linux:
  - saved passwords use the desktop secret service when `secret-tool` is available
  - otherwise the app falls back to the master-password flow
- If you are on Windows:
  - saved passwords use Windows-user-bound protected storage
  - you should not see the master-password flow unless native protection is unavailable

## Current Limits
- Linux native secret storage depends on `secret-tool` being available and usable.
- Windows native protection is DPAPI-based, not a full Credential Manager integration.
- Existing old plaintext passwords are migrated when the app first needs to use them.
- The UI does not expose every persistence-builder option.
- Preview is raw and bounded to a page size, not a full domain explorer.
- Redis has no SQL-script workflow.
- Redis does have a Java initialization preview workflow in the UI.
- JDBC index metadata depends on what the driver and database user are allowed to expose.

## Suggested Screenshot Set
If you want to turn this manual into a more visual guide later, these are the highest-value screenshots to add:
- first launch
- new connection dialog for one JDBC backend
- new connection dialog for Redis
- lookup dialog
- yellow pre-init tab
- initialized green tab with data
- cell details area
- column chooser
- connection info window
