# Conveyor Service Rebuild Requirements

## 1. Scope

Build a new sub-project named `conveyor-service` inside the `conveyor` repository.  
The module is a Spring Boot application that provides:

- Conveyor part placement REST API
- Conveyor static-part placement REST API
- Conveyor command execution REST API
- Operational dashboard (server-rendered UI + JSON backend endpoints)
- Role-based security for both API and dashboard
- Runtime conveyor upload/reload/delete and MBean-driven administration


## 2. Project and Build Requirements

- Module name: `conveyor-service`
- Parent: `com.aegisql:conveyor:1.7.3-SNAPSHOT`
- Spring Boot: `3.3.4`
- Java version: inherited from parent (`maven.compiler.release=21`)
- Packaging: `jar`

Required dependencies:

- `spring-boot-starter-web`
- `spring-boot-starter-security`
- `spring-boot-starter-oauth2-client`
- `spring-boot-starter-oauth2-resource-server`
- `spring-boot-starter-thymeleaf`
- `spring-boot-starter-actuator`
- `spring-boot-starter-validation`
- `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0`
- `org.webjars.npm:prismjs:1.29.0`
- Conveyor modules:
  - `com.aegisql:conveyor-core`
  - `com.aegisql:conveyor-parallel`
  - `com.aegisql:conveyor-configurator`
  - `com.aegisql.persistence:conveyor-persistence-core`
  - `com.aegisql.conveyor-persistence-jdbc:conveyor-persistence-jdbc`
- Test:
  - `spring-boot-starter-test`
  - `spring-security-test`


## 3. Configuration Requirements

`application.yml` must include:

- `spring.application.name=conveyor-service`
- `conveyor.service.upload-dir` (default used by app: `./upload`)
- Management endpoints exposure: `health,info`
- Logging:
  - default: `com.aegisql.conveyor=INFO`
  - `demo` profile: `com.aegisql.conveyor=DEBUG`

`ConveyorServiceProperties`:

- Binds prefix `conveyor.service`
- Exposes `Path uploadDir`
- Default fallback path in code: `${user.home}/.conveyor-service/uploads`


## 4. Security Requirements

### 4.1 Roles

- `REST_USER`
- `DASHBOARD_VIEWER`
- `DASHBOARD_ADMIN`

### 4.2 `prod` profile

- OAuth2 login enabled
- OAuth2 resource server JWT enabled
- HTTP Basic enabled
- Authorization:
  - `POST /part/**`, `POST /static-part/**`, `POST /command/**` => `REST_USER`
  - `/dashboard/admin/**`, `/api/dashboard/admin/**` => `DASHBOARD_ADMIN`
  - `/dashboard/**`, `/api/dashboard/**` => `DASHBOARD_VIEWER` or `DASHBOARD_ADMIN`
  - Swagger endpoints permitted

### 4.3 `demo` profile

- In-memory users:
  - `rest/rest` => `REST_USER`
  - `viewer/viewer` => `DASHBOARD_VIEWER`
  - `admin/admin` => `REST_USER`, `DASHBOARD_VIEWER`, `DASHBOARD_ADMIN`
- Form login
- Remember-me enabled (`remember-me` parameter, 30 days)
- Additional signed auth cookie (`CONVEYOR_DEMO_AUTH`) for persistent re-auth in demo
- Logout clears demo auth cookie and redirects to `/login?logout`

Static resources and webjars must be accessible.


## 5. Core Conveyor Requirements (upstream support)

The dashboard tree/grouping depends on conveyor-core capabilities:

- `Conveyor.getEnclosingConveyor()` and `setEnclosingConveyor(...)`
- `Conveyor.getKnownConveyorNames()`
- `Conveyor.getRegisteredConveyorNames()`
- `Conveyor.getKnownConveyorNameTree()` returning nested tree:
  - keys are conveyor names
  - values are child subtrees
  - root level = conveyors with no enclosing conveyor
- `MBeanRegister.getRegisteredConveyorNames()` support


## 6. Placement Response Model Requirements

All placement responses use `PlacementResult<T>` with:

- `result`
- `timestamp`
- `correlationId`
- `label`
- `properties`
- `status`
- `errorCode`
- `errorMessage`
- `exceptionClass`

Status enum:

- `ACCEPTED`
- `IN_PROGRESS`
- `COMPLETED`
- `FAILED`
- `REJECTED`
- `TIMEOUT_WAITING_FOR_COMPLETION`


## 7. REST API Requirements

### 7.1 Endpoint

- `POST /part/{conveyor}/{id}/{label}`
- `POST /part/{conveyor}/{label}` (foreach mode, no ID in path)
- Request body: raw `byte[]`
- `Content-Type` header is required for value conversion
- Query params are forwarded/mapped as request properties
- Response: `PlacementResult<Boolean>`

### 7.2 Conveyor selection

- `{conveyor}` resolved by name via `Conveyor.byName`
- Unknown conveyor => HTTP `404`

### 7.3 PartLoader wiring

- Create loader with:
  - ID mode (`POST /part/{conveyor}/{id}/{label}`):
    - `.id(...)` from `{id}` converted to conveyor key type
  - Foreach mode (`POST /part/{conveyor}/{label}`):
    - `.foreach()` (no predicate argument)
  - `.label(...)` from `{label}` converted to conveyor label type
  - `.value(...)` from converted request body
- Always include base properties:
  - `conveyor`, `label`
  - `correlationId` in ID mode
  - `foreach=true` in foreach mode

### 7.4 Query parameter mapping

Reserved keys:

- `ttl` => `loader.ttl(..., MILLISECONDS)`
- `expirationTime` => `loader.expirationTime(...)`
- `creationTime` => `loader.creationTime(...)`
- `priority` => `loader.priority(...)`
- `requestTTL` => service wait timeout (not loader TTL)

All other params go into `loader.addProperties(...)`.

### 7.5 Parsing rules

Durations (`ttl`, `requestTTL`):

- Digits only => milliseconds
- `"<number> <UNIT>"` => parsed via `TimeUnit.valueOf(...)`
- Invalid => HTTP `400`

Date-time (`expirationTime`, `creationTime`):

- Digits only => epoch milliseconds
- ISO-8601 instant => converted to epoch milliseconds
- Invalid => HTTP `400`

### 7.6 Content type + label conversion

Value type is resolved from conveyor meta info (`getSupportedValueTypes(label)`):

- `application/octet-stream` => `byte[]`
- `text/plain` => `String` (UTF-8)
- `application/json` => Jackson deserialization to resolved target type

Unsupported mapping => HTTP `415`  
Conversion failure => HTTP `400`

### 7.7 Future handling

Placement uses `CompletableFuture<Boolean>`.

If `requestTTL` is present:

- Wait with timeout (`future.get(requestTTL, MILLISECONDS)`)
- Completed => `COMPLETED` + boolean result
- Timeout => `TIMEOUT_WAITING_FOR_COMPLETION`
- Exception => `FAILED` + sanitized error fields

If `requestTTL` is absent:

- If not done => return immediately `IN_PROGRESS` (no blocking)
- If done normally => return `COMPLETED` with result
- If done exceptionally => return `FAILED` with error

Duplicates are accepted (no deduplication).

### 7.8 Error mapping

`RestExceptionHandler` maps:

- `ConveyorNotFoundException` => `404`, `errorCode=NOT_FOUND`
- `UnsupportedMappingException` => `415`, `errorCode=UNSUPPORTED_MEDIA_TYPE`
- `IllegalArgumentException` => `400`, `errorCode=BAD_REQUEST`
- other exceptions => `500`, `errorCode=INTERNAL_ERROR`

### 7.9 Static Part endpoint

- `POST /static-part/{conveyor}/{label}`
- Response: `PlacementResult<Boolean>`
- Uses `StaticPartLoader` semantics:
  - label is mandatory
  - supports `priority`, custom properties, and optional `requestTTL`
  - does not support `id`, `ttl`, `creationTime`, `expirationTime`
- Request parameters:
  - `delete=true|false` (optional, default `false`)
  - `priority` (optional)
  - `requestTTL` (optional)
  - all other non-reserved params are forwarded via `addProperties(...)`
- Value/body rules:
  - when `delete=true`, value is not required and is ignored
  - when `delete=false`, value is required and converted by `(Content-Type, label)` mapping rules
- Content-type mapping and conversion follow the same behavior as part placement:
  - `application/octet-stream` => `byte[]`
  - `text/plain` => `String` (UTF-8)
  - `application/json` => Jackson to resolved target type
  - unknown mapping => `415`, conversion error => `400`
- Future completion handling (`requestTTL` wait vs immediate inspect) matches part placement behavior.

### 7.10 Command endpoint

Endpoints:

- `POST /command/{conveyor}/{id}/{command}` (ID mode)
- `POST /command/{conveyor}/{command}` (foreach mode)
- Request body: raw `byte[]` (used by `completeExceptionally` message)
- Response: `PlacementResult<Object>`

Common parameter behavior (same parsing rules as part/static):

- `ttl`, `creationTime`, `expirationTime`, `requestTTL`
- duration and datetime parsing rules are identical to section 7.5

ID/foreach rules:

- ID mode requires `id`
- foreach mode uses `command().foreach()` (no predicate argument)

Supported commands in ID mode:

- `cancel()`
- `completeExceptionally(Throwable)`:
  - controller creates `Throwable` from request-body text message
  - body message is required
- `addProperties(Map<String,Object>)`:
  - uses additional query properties as map
  - requires at least one additional property
- `timeout()`
- `reschedule()`:
  - requires either `ttl` or `expirationTime`
- `create()`
- `peek()`:
  - requires `requestTTL`
  - returns product payload (`OUT`) in `PlacementResult.result`
- `peekId()`:
  - requires `requestTTL`
  - returns `Boolean`

Supported commands in foreach mode:

- `cancel()`
- `timeout()`
- `reschedule()` (requires `ttl` or `expirationTime`)
- `peek()` (requires `requestTTL`, returns list of products)
- `peekId()` (requires `requestTTL`, returns list of matched IDs)

Not supported in foreach mode:

- `completeExceptionally`
- `addProperties`
- `create`

Additional-property rules:

- only `addProperties` accepts additional properties
- for all other commands, extra properties are rejected (`400`)

Future handling:

- with `requestTTL`: wait up to timeout and return `COMPLETED`/`FAILED`/`TIMEOUT_WAITING_FOR_COMPLETION`
- without `requestTTL`: non-blocking inspect path (`IN_PROGRESS`, or immediate `COMPLETED`/`FAILED`)


## 8. Dashboard Backend Requirements

### 8.1 JSON API (`/api/dashboard`)

- `GET /tree` => conveyor name tree
- `GET /{name}` => conveyor inspection data
- Admin-only:
  - `POST /admin/upload` (multipart jar)
  - `POST /admin/reload/{name}`
  - `DELETE /admin/{name}`
  - `POST /admin/{name}/mbean/{method}`
  - `POST /admin/{name}/parameter/{parameter}?value=...`

### 8.2 HTML controller

Routes:

- `GET /` => redirect `/dashboard`
- `GET /dashboard` => dashboard page
- Form-based admin actions under `/dashboard/admin/*`
- Tester endpoint: `POST /dashboard/test/place`
- Static tester endpoint: `POST /dashboard/test/static-part`
- Command tester endpoint: `POST /dashboard/test/command`

Behavior:

- Preserve user messages/errors via flash attributes
- Surface loader/system errors on page
- Keep selection with `?name=...`
- Preserve active dashboard tab with `?tab=...` across tree navigation and form actions


## 9. Dashboard UI Requirements

### 9.1 Main features

- Conveyor tree with parent/child grouping by enclosing conveyor
- Two-column layout:
  - left: conveyor tree
  - right: single dashboard component with tabs
- Tabs:
  - `Conveyor Details`
  - `Operations`
  - `Part Loader Tester`
  - `Static Parts`
  - `Commands`
  - `Admin Actions`
- Conveyor Details tab:
  - running state
  - mbean interface
  - meta-info availability
  - snapshot timestamp
  - upload directory
- Meta-info visual presentation (not raw JSON):
  - key/label/product type cards
  - labels chips
  - supported value types table
- Parameters table:
  - title: `Parameters`
  - shows all readable attributes (read-only and writable)
  - includes value, type, and access (`Read-only` / `Writable`)
- Operations tab:
  - uses one unified `Invoke Actions` list for both writable parameters and MBean operations
  - each action shows expected argument type and has an `Invoke` button (admin only)
  - writable parameters show current value when available
  - operation ordering:
    - parameterized operations first
    - unparameterized operations next
    - `stop`/stop-like operations last
  - stop action safety:
    - visible warning text: irreversible, may cause data loss
    - browser confirmation dialog before submit
- Admin forms:
  - upload jar
  - controlled reload (`completeAndStop + reload`)
  - controlled delete (`completeAndStop + delete`)
  - update parameter and invoke MBean method (also available through Operations tab)
- Header includes role badges and **Logout** button

### 9.2 Part Loader Tester

Available only for top-level conveyors (no enclosing conveyor).  
Validation requires `ttl` or `expirationTime`.

Inputs:

- `label`, `contentType`
- `foreach` checkbox
- `id` (required only when `foreach` is unchecked)
- request body textarea
- `ttl`, `expirationTime`, `creationTime`, `priority`, `requestTTL`
- additional properties via dynamic rows:
  - add row (`+ Add property`)
  - remove row (`X`)
  - each row = separate `key`/`value`

Outputs:

- Placement result shown as formatted JSON
- UI behavior:
  - when `foreach` is checked, ID input is disabled in the form

### 9.3 JSON syntax highlighting

- Use Prism assets from WebJars (`prism-core`, `prism-json`, `prism` theme)
- Highlight:
  - placement result JSON
  - live request-body preview (JSON content type)
  - static-part request-body preview and static placement result JSON

### 9.4 Static Parts tester

Dedicated dashboard tab for static-part workflows.

Inputs:

- `label` (required)
- `contentType`
- `delete` toggle
- request body textarea (disabled/ignored in delete mode)
- `priority`
- `requestTTL`
- additional properties via dynamic key/value rows (`+ Add property`, `X` remove)

Behavior:

- Submits to dashboard route `POST /dashboard/test/static-part`
- Renders static placement response in formatted JSON
- Preserves active tab and entered values across redirects

### 9.5 Commands tester

Dedicated dashboard tab for command-loader workflows.

Inputs:

- `id` (required unless `foreach` is checked)
- `foreach` checkbox
- command body textarea (used by `completeExceptionally`)
- `ttl`, `expirationTime`, `creationTime`, `requestTTL`
- additional properties via dynamic key/value rows (`+ Add property`, `X` remove)
- command buttons:
  - `CANCEL`
  - `COMPLETE EXCEPTIONALLY`
  - `ADD PROPERTIES`
  - `TIMEOUT`
  - `RESCHEDULE`
  - `CREATE`
  - `PEEK`
  - `PEEK ID`

Behavior:

- submits to `POST /dashboard/test/command`
- each command is invoked by its own button (`operation` value)
- when `foreach` is checked, foreach-inapplicable buttons are disabled:
  - `COMPLETE EXCEPTIONALLY`, `ADD PROPERTIES`, `CREATE`
- `ADD PROPERTIES` is disabled when no additional properties are provided
- command result is rendered as formatted JSON
- preserves active tab and entered values across redirects


## 10. Dynamic Conveyor Upload/Reload/Delete Requirements

Upload behavior:

- Accept only `.jar`
- Store in configured upload dir
- Build URLClassLoader from all jars in upload dir
- Discover providers via `ServiceLoader<ConveyorInitiatingService>`

Visibility and replacement behavior:

- Uploaded conveyor names tracked and merged into tree sources
- On name conflict, existing conveyor is stopped/unregistered, then replaced
- Controlled stop uses `completeAndStop()` (5-second wait best effort)
- Uploaded conveyors must become visible through `Conveyor.byName(...)`

Error handling:

- Loader errors recorded with UTC timestamp
- Errors shown on dashboard ("Extension Loader Errors")
- Errors can be drained/cleared between views


## 11. OpenAPI/Swagger Requirements

- Swagger UI path: `/swagger-ui/index.html`
- OpenAPI JSON path: `/v3/api-docs`
- Swagger endpoints are publicly readable (security permitAll)


## 12. Build, Run, and Verification Commands

Compile module:

```bash
mvn -pl conveyor-service -DskipTests test-compile
```

Run in demo mode:

```bash
mvn -pl conveyor-service -Dspring-boot.run.profiles=demo spring-boot:run
```

Basic smoke checks:

- Login as `admin/admin` in demo profile
- Open `/dashboard`
- Ensure tree renders and selecting a conveyor shows details
- Run Part Loader tester and verify `PlacementResult<Boolean>` for:
  - ID mode (explicit `id`)
  - foreach mode (`foreach` checked, no ID)
- Run Static Parts tester:
  - create/update with value and verify placement response
  - delete with `delete=true` and verify value is not required
- Run Commands tester:
  - ID mode: `cancel`, `peek`, `peekId`, `reschedule` (with timing), `create`
  - foreach mode: verify unsupported command buttons are disabled
  - verify `peek`/`peekId` require `requestTTL`
  - verify `ADD PROPERTIES` is disabled without properties
- Upload a valid conveyor jar and confirm new conveyor appears
- Check `/swagger-ui/index.html` and `/v3/api-docs`
