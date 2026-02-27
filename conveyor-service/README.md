# conveyor-service

Spring Boot service for working with Conveyor instances over HTTP:

#### This service is still in experimental alpha version. Use it at your own risk.

- REST APIs for `part`, `static-part`, and `command` loaders.
- Web dashboard for monitoring, configuration, admin operations, and interactive loader testing.
- Pluggable auth model:
  - `demo` profile: local in-memory users.
  - `prod` profile: OAuth2/OIDC with optional JWT resource-server mode.

## Dashboard UI Overview

The dashboard is organized around conveyor inspection and request testing. The sections below use the built-in UI screenshots as a quick map.

### Conveyors and Watchers

<img src="https://github.com/aegisql/conveyor/blob/master/doc/img/conveyors_and_watchers.png" alt="Conveyors and watchers" width="760">

- `Conveyors` tree shows hierarchy and current state (`running`, `suspended`, `stopped`).
- `Watchers` list tracks active watches and quick switching to output tabs.
- Selecting a conveyor in the tree drives context for all workspace tabs.

### Conveyor Details

<img src="https://github.com/aegisql/conveyor/blob/master/doc/img/conveyor_details.png" alt="Conveyor Details" width="760">

- Runtime summary fields: `Name`, `Running`, `MBean`, `MetaInfo`, `Snapshot Time`, `Upload Dir`.
- Meta section lists key/label/product types, labels, and supported value types.
- Parameter table shows current values, types, and writable/read-only access.

### Conveyor Operations

<img src="https://github.com/aegisql/conveyor/blob/master/doc/img/conveyor_operations.png" alt="Conveyor Operations" width="760">

- Set writable MBean parameters using `Set` with typed value input.
- Invoke MBean operations with optional argument input for operations that require one.
- Stop-related operations display an explicit warning before execution.

### Conveyor Parts

<img src="https://github.com/aegisql/conveyor/blob/master/doc/img/conveyor_parts.png" alt="Conveyor Parts" width="760">

- Main controls: `ID`, `Label`, `Foreach`, request body, and content type.
- Timing/priority fields: `TTL`, `Creation Time`, `Expiration Time`, `Priority`, `Request TTL`.
- Supports additional custom properties and `Submit` / `Submit and watch` flows.

### Conveyor Static Parts

<img src="https://github.com/aegisql/conveyor/blob/master/doc/img/conveyor_static_parts.png" alt="Conveyor Static Parts" width="760">

- Main controls: `Label`, `Content Type`, `Delete Mode`, request body (or file drop-in).
- Timing fields: `Priority` and `Request TTL`.
- Supports additional custom properties for static part placement.

### Conveyor Commands

<img src="https://github.com/aegisql/conveyor/blob/master/doc/img/conveyor_commands.png" alt="Conveyor Commands" width="760">

- Main controls: `Watch results`, `ID`, `Foreach`, and command payload.
- Timing fields: `TTL`, `Creation Time`, `Expiration Time`, `Request TTL`.
- Command actions include `CREATE`, `ADD PROPERTIES`, `RESCHEDULE`, `PEEK`, `PEEK ID`, `CANCEL`, `TIMEOUT`, and `COMPLETE EXCEPTIONALLY`.


## Configuration

Default config file: `src/main/resources/application.yml`

Key properties:

- `spring.profiles.active`
  - `demo` for local usage.
  - `prod` for OAuth2/OIDC.
- `conveyor.service.upload-dir`
  - Directory where uploaded extension JARs are stored.
  - Default: `./upload`
- `conveyor.service.upload-enable`
  - Enables/disables runtime upload and delete operations.
  - Default: `true`
  - When `false`:
    - backend blocks upload/delete endpoints with `403 FORBIDDEN`
    - dashboard hides upload/delete controls and shows admin notice
- `conveyor.service.oauth2-login-enable`
  - Enables/disables built-in OAuth2 browser login in `prod`.
  - Default: `true`
  - When `false`:
    - app does not configure `oauth2Login()`
    - JWT resource server auth and HTTP Basic remain available
    - useful when corporate IAM is handled externally
- `conveyor.service.oauth2-resource-server-enable`
  - Enables/disables JWT bearer-token validation in `prod`.
  - Default: `true`
  - Set to `false` when running browser OAuth login only (for example `prod,facebook` locally).
- `spring.security.oauth2.resourceserver.jwt.issuer-uri`
  - Used only when `conveyor.service.oauth2-resource-server-enable=true`.
- `logging.level.com.aegisql.conveyor`
  - Conveyor logging level (`DEBUG` in `demo`, `INFO` by default).

You can override properties with environment variables, for example:

- `SPRING_PROFILES_ACTIVE=demo`
- `CONVEYOR_SERVICE_UPLOAD_DIR=./upload`
- `CONVEYOR_SERVICE_UPLOAD_ENABLE=true`
- `CONVEYOR_SERVICE_OAUTH2_LOGIN_ENABLE=true`
- `CONVEYOR_SERVICE_OAUTH2_RESOURCE_SERVER_ENABLE=false`
- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://your-issuer`

Dashboard details view shows `Upload Dir` exactly as configured (for example `./upload`), not an expanded absolute path.

## Container run (`demo`)

Container instructions are in:

- `docs/CONTAINER.md`

This includes Docker build/run, bind-mount upload directory, Maven integration, and image propagation workflow.

## Build And Run

From repository root:

```bash
mvn -pl conveyor-core,conveyor-parallel,conveyor-configurator -am -DskipTests install
mvn -pl conveyor-service -Dspring-boot.run.profiles=demo spring-boot:run
```

Facebook OAuth local run (no JWT issuer required):

```bash
SPRING_PROFILES_ACTIVE=prod,facebook \
FACEBOOK_CLIENT_ID=<your-app-id> \
FACEBOOK_CLIENT_SECRET=<your-app-secret> \
mvn -pl conveyor-service spring-boot:run
```

The service starts on `http://localhost:8080` by default.

## Access

- Dashboard: `http://localhost:8080/dashboard`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Demo credentials (`demo` profile):

- `admin` / `admin` (`REST_USER`, `DASHBOARD_VIEWER`, `DASHBOARD_ADMIN`)
- `viewer` / `viewer` (`DASHBOARD_VIEWER`)
- `rest` / `rest` (`REST_USER`)

## Main REST Endpoints

- `POST /part/{conveyor}/{id}/{label}`
- `POST /part/{conveyor}/{label}` (foreach mode)
- `POST /static-part/{conveyor}/{label}`
- `POST /command/{conveyor}/{id}/{command}`
- `POST /command/{conveyor}/{command}` (foreach mode)

All responses use the `PlacementResult<T>` envelope.

## Time Field Examples

Time-related query parameters:

- Duration fields: `ttl`, `requestTTL`
- Date/time fields: `creationTime`, `expirationTime`

### Duration fields (`ttl`, `requestTTL`)

Use either milliseconds as a number, or `number + time unit`.

Examples:

- `ttl=2500`
- `requestTTL=5 SECONDS`
- `ttl=2 minutes`
- `requestTTL=1 HOURS`

Example request:

```bash
curl -X POST "http://localhost:8080/part/collector/42/USER?ttl=5%20SECONDS&requestTTL=2%20SECONDS" \
  -H "Content-Type: application/json" \
  -d '{"name":"Ann","age":42}'
```

### Date/time fields (`creationTime`, `expirationTime`)

Accepted formats include:

- Epoch milliseconds: `1770840600000`
- ISO with UTC offset:
  - `2026-02-11T12:30:00Z`
  - `2026-02-11T12:30:00+05`
  - `2026-02-11T12:30:00+0530`
  - `2026-02-11T12:30:00+05:30`
- ISO without timezone (uses server default timezone):
  - `2026-02-11T12:30:00`
  - `2026-02-11`

Note: in query strings, encode `+` as `%2B`.

Example request:

```bash
curl -X POST "http://localhost:8080/command/collector/42/reschedule?creationTime=2026-02-11T12:30:00%2B05&expirationTime=2026-02-11T13:00:00%2B05" \
  -H "Content-Type: text/plain" \
  -d ""
```
