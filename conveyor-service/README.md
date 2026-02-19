# conveyor-service

Spring Boot service for working with Conveyor instances over HTTP:

#### This service is still in experimental alpha version. Use it at your own risk.

- REST APIs for `part`, `static-part`, and `command` loaders.
- Web dashboard for monitoring, configuration, admin operations, and interactive loader testing.
- Pluggable auth model:
  - `demo` profile: local in-memory users.
  - `prod` profile: OAuth2/OIDC.


<img src=https://github.com/aegisql/conveyor/blob/master/doc/img/cs01.png alt="Conveyor Service Dashboard">

<img src=https://github.com/aegisql/conveyor/blob/master/doc/img/cs02.png alt="Conveyor Service Dashboard">

<img src=https://github.com/aegisql/conveyor/blob/master/doc/img/cs03.png alt="Conveyor Service Dashboard">

<img src=https://github.com/aegisql/conveyor/blob/master/doc/img/cs04.png alt="Conveyor Service Dashboard">

<img src=https://github.com/aegisql/conveyor/blob/master/doc/img/cs05.png alt="Conveyor Service Dashboard">


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
- `spring.security.oauth2.resourceserver.jwt.issuer-uri`
  - Used in `prod` profile for JWT validation.
- `logging.level.com.aegisql.conveyor`
  - Conveyor logging level (`DEBUG` in `demo`, `INFO` by default).

You can override properties with environment variables, for example:

- `SPRING_PROFILES_ACTIVE=demo`
- `CONVEYOR_SERVICE_UPLOAD_DIR=./upload`
- `CONVEYOR_SERVICE_UPLOAD_ENABLE=true`
- `CONVEYOR_SERVICE_OAUTH2_LOGIN_ENABLE=true`
- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://your-issuer`

Dashboard details view shows `Upload Dir` exactly as configured (for example `./upload`), not an expanded absolute path.

## Build And Run

From repository root:

```bash
mvn -pl conveyor-core,conveyor-parallel,conveyor-configurator -am -DskipTests install
mvn -pl conveyor-service -Dspring-boot.run.profiles=demo spring-boot:run
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
