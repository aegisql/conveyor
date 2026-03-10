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
- Parent: `com.aegisql:conveyor:1.7.3`
- Spring Boot: `4.0.2`
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
- `spring-boot-starter-websocket`
- `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2`
- `org.webjars.npm:prismjs:1.30.0`
- Conveyor modules:
  - `com.aegisql:conveyor-core`
  - `com.aegisql:conveyor-parallel`
  - `com.aegisql:conveyor-configurator`
  - `com.aegisql.persistence:conveyor-persistence-core`
  - `com.aegisql.conveyor-persistence-jdbc:conveyor-persistence-jdbc`
- Test:
  - `spring-boot-starter-test`
  - `spring-boot-webmvc-test`
  - `spring-security-test`


## 3. Configuration Requirements

`application.yml` must include:

- `spring.application.name=conveyor-service`
- `conveyor.service.upload-dir` (default used by app: `./upload`)
- `conveyor.service.upload-enable` (default `true`)
- `conveyor.service.oauth2-login-enable` (default `true`)
- `conveyor.service.oauth2-resource-server-enable` (default `true`)
- `conveyor.service.audit.enabled` (default `false`)
- `conveyor.service.audit.log-file` (default `./logs/conveyor-rest-audit.log`)
- `conveyor.service.audit.max-file-size` (default `50MB`)
- `conveyor.service.audit.max-history` (default `14`)
- `conveyor.service.audit.total-size-cap` (default `1GB`)
- `conveyor.service.audit.clean-history-on-start` (default `false`)
- `conveyor.service.conveyor-log.log-file` (default `./logs/conveyor.log`)
- `conveyor.service.conveyor-log.max-file-size` (default `100MB`)
- `conveyor.service.conveyor-log.max-history` (default `14`)
- `conveyor.service.conveyor-log.total-size-cap` (default `2GB`)
- `conveyor.service.conveyor-log.clean-history-on-start` (default `false`)
- `conveyor.service.conveyor-log.level` (default `INFO`)
- `conveyor.service.dashboard.default-watch-history-limit` (default `100`)
- `conveyor.service.dashboard.default-conveyor-history-limit` (default `100`)
- `conveyor.service.dashboard.default-admin-stop-timeout` (default `1 MINUTES`)
- `server.tomcat.max-part-count=200`
- Management endpoints exposure: `health,info`
- Logging:
  - default: `com.aegisql.conveyor=DEBUG`
  - `demo` profile: `com.aegisql.conveyor=DEBUG`
  - dedicated REST audit logger name: `conveyor.audit.rest`
  - dedicated rolling conveyor logger: `com.aegisql.conveyor` -> `conveyor.service.conveyor-log.log-file`

`ConveyorServiceProperties`:

- Binds prefix `conveyor.service`
- Exposes `Path uploadDir`
- Exposes `boolean uploadEnable` (default `true`)
- Exposes `boolean oauth2LoginEnable` (default `true`)
- Exposes `boolean oauth2ResourceServerEnable` (default `true`)
- Default fallback path in code: `${user.home}/.conveyor-service/upload`


## 4. Security Requirements

### 4.1 Roles

- `REST_USER`
- `DASHBOARD_VIEWER`
- `DASHBOARD_ADMIN`

### 4.2 `prod` profile

- OAuth2 login is controlled by `conveyor.service.oauth2-login-enable`:
  - `true` => built-in OAuth2 browser login enabled
  - `false` => built-in OAuth2 browser login disabled
- OAuth2 resource server JWT is controlled by `conveyor.service.oauth2-resource-server-enable`:
  - `true` => bearer JWT validation enabled (`oauth2ResourceServer().jwt()`)
  - `false` => bearer JWT validation disabled
- HTTP Basic enabled
- Authorization:
  - `POST /part/**`, `POST /static-part/**`, `POST /command/**` => `REST_USER`
  - `/ws/**` => `DASHBOARD_VIEWER` or `DASHBOARD_ADMIN`
  - `/dashboard/admin/**`, `/api/dashboard/admin/**` => `DASHBOARD_ADMIN`
  - `/dashboard/**`, `/api/dashboard/**` => `DASHBOARD_VIEWER` or `DASHBOARD_ADMIN`
  - Swagger endpoints permitted

Facebook profile interoperability requirement:

- `application-facebook.yml` must set `conveyor.service.oauth2-resource-server-enable=false` by default so browser OAuth login works without requiring `spring.security.oauth2.resourceserver.jwt.issuer-uri`.

### 4.3 `demo` profile

- In-memory users:
  - `rest/rest` => `REST_USER`
  - `viewer/viewer` => `DASHBOARD_VIEWER`
  - `admin/admin` => `REST_USER`, `DASHBOARD_VIEWER`, `DASHBOARD_ADMIN`
- Form login
- Remember-me enabled (`remember-me` parameter, 30 days)
- Additional signed auth cookie (`CONVEYOR_DEMO_AUTH`) for persistent re-auth in demo
- Logout clears demo auth cookie and redirects to `/login?logout`
- Dashboard websocket endpoint `/ws/watch` requires dashboard role access.

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
- Optional watch controls on both part endpoints:
  - `watchResults=true|false` (alias: `watch`)
  - `watchLimit=<positive integer>`
  - when enabled, watcher is registered before placement and auto-canceled on placement error

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
- `watchResults` / `watch` and `watchLimit` are consumed by the controller for watch registration and are not forwarded to loader properties.

All other params go into `loader.addProperties(...)`.

### 7.5 Parsing rules

Durations (`ttl`, `requestTTL`):

- Digits only => milliseconds
- `"<number> <UNIT>"` => parsed via `TimeUnit.valueOf(...)`
- Invalid => HTTP `400`

Date-time (`expirationTime`, `creationTime`):

- Digits only => epoch milliseconds
- Supports broad valid datetime formats including:
  - ISO instant / offset / zoned values
  - offsets like `+HH:MM`, `+HHMM`, and `+HH`
  - RFC-1123
  - local date-time and local date
- If timezone is missing (local date-time/date), interpret in server default timezone.
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

HTTP status mapping for successful part/static-part/command placement responses:

- If `requestTTL` is missing and the request is only scheduled (`status=IN_PROGRESS` or `status=ACCEPTED`) => HTTP `202 Accepted`
- If `requestTTL` is present and request completion is obtained in-request (`status=COMPLETED`) => HTTP `200 OK`
- Other non-exception result statuses continue to use HTTP `200 OK`, with detailed outcome carried in `PlacementResult.status`

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
- Optional watch controls on both command endpoints:
  - `watchResults=true|false` (alias: `watch`)
  - `watchLimit=<positive integer>`
  - when enabled, watcher is registered before command execution and auto-canceled on execution error

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
- `completeExceptionally(Throwable)`:
  - message comes from request body
  - implemented as foreach fan-out over active IDs
- `addProperties(Map<String,Object>)`
- `timeout()`
- `reschedule()` (requires `ttl` or `expirationTime`)
- `peek()` (requires `requestTTL`, returns `Map<K, OUT>`)
- `peekId()` (requires `requestTTL`, returns list of matched IDs)

Not supported in foreach mode:

- `create`

Additional-property rules:

- only `addProperties` accepts additional properties (ID and foreach)
- for all other commands, extra properties are rejected (`400`)

Future handling:

- with `requestTTL`: wait up to timeout and return `COMPLETED`/`FAILED`/`TIMEOUT_WAITING_FOR_COMPLETION`
- without `requestTTL`: non-blocking inspect path (`IN_PROGRESS`, or immediate `COMPLETED`/`FAILED`)


## 8. Dashboard Backend Requirements

### 8.1 JSON API (`/api/dashboard`)

- `GET /tree` => conveyor name tree
- `GET /{name}` => conveyor inspection data
- Watch API:
  - `GET /watch` => active watches for authenticated user
  - `POST /watch/cancel` => cancel by `watchId` or by `(conveyor, correlationId|foreach)`
  - `POST /watch/history-limit` => update foreach watch cache limit
- Admin-only:
  - `POST /admin/upload` (multipart jar)
  - `POST /admin/reload/{name}` (optional `stopTimeout`)
  - `DELETE /admin/{name}` (optional `stopTimeout`)
  - `GET /admin/events` (poll async admin completion/failure events for current user)
  - `POST /admin/{name}/mbean/{method}`
  - `POST /admin/{name}/parameter/{parameter}?value=...`
- Reload/delete safety:
  - reload and delete are allowed only for top-level conveyors (no enclosing conveyor)
  - selecting a child conveyor must be rejected with `400` (`BAD_REQUEST`)
  - controlled stop path uses `completeThenForceStop(stopTimeout)` (default `1 MINUTES`), waits for stop (up to timeout), then calls `Conveyor.unRegisterTree(...)`
- Upload/delete feature-gating:
  - when `conveyor.service.upload-enable=false`, upload and delete endpoints are disabled
  - disabled responses use HTTP `403` with placement envelope `errorCode=FORBIDDEN`

### 8.2 HTML controller

Routes:

- `GET /` => redirect `/dashboard`
- `GET /dashboard` => dashboard page
- Form-based admin actions under `/dashboard/admin/*`
- Conveyor watch action: `POST /dashboard/watch`
- Tester endpoint: `POST /dashboard/test/place`
- Static tester endpoint: `POST /dashboard/test/static-part`
- Command tester endpoint: `POST /dashboard/test/command`

Behavior:

- Preserve user messages/errors via flash attributes
- Surface loader/system errors on page
- Keep selection with `?name=...`
- Preserve active dashboard tab with `?tab=...` across tree navigation and form actions
- Emits compact dashboard output events for each submit:
  - source key/type
  - status line payload (`httpStatus`, `result`, `status`, `errorCode`, `errorMessage`, `responseTime`, `summaryLine`)
  - JSON payload body
  - status `httpStatus` mapping must match placement status:
    - `IN_PROGRESS` / `ACCEPTED` => `202`
    - `COMPLETED` / other non-exception statuses => `200`
- Admin reload/delete are asynchronous:
  - submit returns scheduled output event immediately
  - completion/failure events are polled from `/api/dashboard/admin/events`
  - completion events mark tree refresh so UI can reload conveyor tree state

### 8.3 WebSocket watch transport

- Endpoint: `/ws/watch`
- Per-user delivery model; session authentication ties events to the current dashboard user.
- Uses `HttpSessionHandshakeInterceptor` so security context can be resolved during websocket handshake/session lifecycle.
- Conveyor watch service registers bridge consumers (`ResultConsumer`, `ScrapConsumer`) on known/uploaded conveyors.
- Non-foreach (ID-based) watches remain active after `RESULT`/`SCRAP` events and continue receiving future matching updates until explicitly canceled.
- Watch events include metadata for UI routing:
  - `watchId`, `displayName`, `conveyor`, `sourceConveyor`, `foreach`, `historyLimit`, `watchActive`
  - `eventType` (`RESULT`, `SCRAP`, `PING`, `CANCELED`)
  - `eventSignature` and `buildCreationTime` for duplicate suppression and traceability
- Duplicate suppression signature for data events is based on event type + source conveyor + correlation key + build creation timestamp.
- `PING` events are periodic keep-alive/wait-time updates for active watches.

### 8.4 View exception handling

- Dashboard/file-upload failures must not fall back to Spring Whitelabel page.
- `@ControllerAdvice` handles multipart failures (`MultipartException`, `FileCountLimitExceededException`):
  - Dashboard HTML requests => redirect back to dashboard (or referer dashboard URL) with flash error.
  - API/JSON requests => `400` with `PlacementResult` error envelope.
- Custom Thymeleaf error page template (`error.html`) is used for view rendering path.


## 9. Dashboard UI Requirements

### 9.1 Main features

- Conveyor tree with parent/child grouping by enclosing conveyor
- Two-column layout:
  - left: conveyor tree
  - right: single dashboard component with tabs
- Conveyor tree node state coloring:
  - running and not suspended => green
  - running and suspended => yellow
  - not running / unresolved => gray
- Tabs:
  - `Conveyor Details`
  - `Operations`
  - `Parts`
  - `Static Parts`
  - `Commands`
  - `Admin Actions`
- Header shows authenticated username and Logout button (no duplicate role/user labels).
- Conveyor Details tab:
  - running state
  - mbean interface
  - meta-info availability
  - snapshot timestamp
  - upload directory (shown as configured value, not expanded absolute path)
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
  - upload jar supports both:
    - drag-and-drop area
    - native file picker input (`Choose File`)
  - controlled reload/delete form:
    - selected conveyor is read-only and comes from tree selection
    - controls stay visible even with no selection
    - reload/delete buttons are inactive unless a top-level conveyor is selected
    - neutral hint text is shown when selection is missing/invalid (not an error alert)
    - one shared timeout input for both actions with label `Complete and Stop Timeout`
    - timeout request parameter remains `stopTimeout`, default `1 MINUTES`
    - action labels are `Reload` and `Delete`
    - delete action deletes the full conveyor tree
  - update parameter and invoke MBean method (also available through Operations tab)
  - when `upload-enable=false`:
    - upload controls are hidden/disabled
    - explicit message is shown: upload/remove disabled by service admin
    - delete remains server-side guarded (`403`) when attempted
    - reload remains available
### 9.2 Parts tester

Available only for top-level conveyors (no enclosing conveyor).  
Validation requires `ttl` or `expirationTime`.

Inputs:

- `label`, `contentType`
- `foreach` checkbox
- `id` (required only when `foreach` is unchecked)
- request body textarea
- body file upload UI includes:
  - right-side square drag/drop target that loads dropped file into `Body` immediately
  - native file picker (`Choose File`) below the drop zone
  - if present, loaded file content overrides textarea body
- `ttl`, `expirationTime`, `creationTime`, `priority`, `requestTTL`
- additional properties via dynamic rows:
  - add row (`+ Add property`)
  - remove row (`X`)
  - each row = separate `key`/`value`

Outputs:

- Placement submit creates a compact output status event in the shared Output dock (HTTP status, result, placement status, error code/message, response time).
- UI behavior:
  - when `foreach` is checked, ID input is disabled in the form
  - no watch checkbox in Parts tab
  - two submit actions:
    - `Submit`
    - `Submit and watch` (registers watch then submits)

### 9.3 JSON syntax highlighting

- Use Prism assets from WebJars (`prism-core`, `prism-json`, `prism` theme)
- Highlight live JSON previews and Output payload JSON rendering.

### 9.4 Static Parts tester

Dedicated dashboard tab for static-part workflows.

Inputs:

- `label` (required)
- `contentType`
- `delete` toggle
- request body textarea (disabled/ignored in delete mode)
- body file upload UI includes:
  - right-side square drag/drop target that loads dropped file into `Body` immediately
  - native file picker (`Choose File`) below the drop zone
  - if present, loaded file content overrides textarea body
- `priority`
- `requestTTL`
- additional properties via dynamic key/value rows (`+ Add property`, `X` remove)

Behavior:

- Submits to dashboard route `POST /dashboard/test/static-part`
- Emits compact status/output event to the shared Output dock
- Preserves active tab and entered values across redirects
- Same top-level-only visibility/behavior as Parts tab.

### 9.5 Commands tester

Dedicated dashboard tab for command-loader workflows.

Inputs:

- `id` (required unless `foreach` is checked)
- `foreach` checkbox (inline with ID)
- `watch results` checkbox (inline near title)
- exception message textarea (used by `completeExceptionally`)
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
- command buttons are visually separated into two containers:
  - build-safe: `CREATE`, `ADD PROPERTIES`, `RESCHEDULE`, `PEEK`, `PEEK ID`
  - destructive: `CANCEL`, `TIMEOUT`, `COMPLETE EXCEPTIONALLY`
- when `foreach` is checked, only `CREATE` is disabled
- `ADD PROPERTIES` is disabled when no additional properties are provided
- command submit emits compact status/output event to shared Output dock
- preserves active tab and entered values across redirects

### 9.6 Watch panel and watch tags

- Left-side Watchers panel shows active watches for current user.
- Watch creation paths:
  - Conveyor-level watch: `Watch` button in Conveyors card (top-level conveyor only; creates `conveyor|*` watch)
  - Parts `Submit and watch`
  - Commands `watch results` checkbox
- Tag behavior:
  - gray while waiting
  - green when result arrives
  - error style on scrap
  - click tag => open/focus corresponding watch output tab
  - inline `X` on tag cancels watch
- Watcher event history shown in the Output dock must not double-append the same backend event (including after page reload when both session-restored output history and watch API snapshots are available).

### 9.7 Shared Output dock

- Single output area at the bottom of screen for all conveyor submits and watch events.
- Dock behavior:
  - vertically resizable
  - closable with `X`
  - reopen via `Open Output` button in workspace header
  - closing the dock only hides it; tab state/history and tab-level preferences remain in session
- Tab behavior:
  - tabs are created on demand per conveyor and per watch
  - tab content is isolated (no mixing between sources)
  - each tab is closable with `X`
  - closed tabs are recreated automatically when new events arrive
  - closing a tab stores its UI preferences in session (JSONPath and cache limit) and restores them when the same tab reappears in that session
- Event/timeline behavior:
  - left-side vertical timeline of cached events per selected tab
  - timeline item colors by status class (2xx/3xx/4xx/5xx style)
  - watch and conveyor/admin tabs use the same status-to-color semantics
  - click timeline item to view one event
  - timeline item label includes event time and, when available, correlation ID
  - if correlation ID is too long for button width, show ID tail prefixed with `...` (example: `10:18:51 - ...a3d8f`)
  - `Prev` / `Next` navigation
  - `Clear events` removes cached events for selected tab
  - `See all` shows all tab events in aggregate view and disables per-event navigation
  - tail-follow behavior: if currently on latest event, new events auto-focus latest
- Rendering behavior:
  - compact status line above payload area
  - in `See all` mode, do not show synthetic banner text like `Showing all events (...)`; keep status line compact and event-based
  - JSONPath extraction input is per selected Output tab:
    - empty value resolves to `$`
    - default value is `$.payload`
    - each tab can use a different JSONPath
    - user-selected JSONPath is remembered per tab for the lifetime of the browser session, including close/reopen of the same tab
  - `Copy` button copies only currently visible JSON payload content from Output view (selected event or `See all` aggregate JSON)
  - `Copy` action must not include status line text in clipboard content
  - when there is no open output tab, cache-limit and JSONPath controls are hidden
- Cache controls:
  - one cache-limit input is shown for the selected Output tab
  - control label is dynamic:
    - `Watch Cache` for watch tabs
    - `Conveyor Cache` for conveyor/admin tabs
  - default values come from YAML config:
    - watch tabs -> `default-watch-history-limit`
    - conveyor/admin tabs -> `default-conveyor-history-limit`
  - oldest cached entries are dropped when per-tab limit is reached
  - user-selected cache limit is remembered per tab for the lifetime of the browser session, including close/reopen of the same tab
  - watch `PING` events update elapsed wait clock but are not added to event history


## 10. Dynamic Conveyor Upload/Reload/Delete Requirements

Upload behavior:

- Accept only `.jar`
- Store in configured upload dir
- Build URLClassLoader from all jars in upload dir
- Discover providers via `ServiceLoader<ConveyorInitiatingService>`
- Guarded by `conveyor.service.upload-enable=true`

Delete behavior:

- Guarded by `conveyor.service.upload-enable=true`
- Allowed only for top-level conveyors
- Uses controlled stop/unregister-tree for selected conveyor tree

Visibility and replacement behavior:

- Uploaded conveyor names tracked and merged into tree sources
- On name conflict, existing conveyor is stopped/unregistered, then replaced
- Reload is allowed only for top-level conveyors
- Controlled stop uses one shared timeout input (`stopTimeout`) for reload/delete, default `1 MINUTES`
- Unregistration order is: `completeThenForceStop(...)`, wait-for-stop, then `Conveyor.unRegisterTree(...)`
- Uploaded conveyors must become visible through `Conveyor.byName(...)`
- Watch bridge hooks must be attached for newly uploaded/reloaded conveyors as well.

Error handling:

- Loader errors recorded with UTC timestamp
- Errors shown on dashboard ("Extension Loader Errors")
- Errors can be drained/cleared between views


## 11. OpenAPI/Swagger Requirements

- Swagger UI path: `/swagger-ui/index.html`
- OpenAPI JSON path: `/v3/api-docs`
- Swagger endpoints are publicly readable (security permitAll)
- OpenAPI must describe REST endpoints only; server-rendered dashboard form handlers (`/dashboard/**`) are hidden.
- Placement/command/static-part OpenAPI operations must expose both `200` (completed synchronously) and `202` (accepted/in-progress) responses.
- Common placement query parameters (`ttl`, `expirationTime`, `creationTime`, `priority`, `requestTTL`, `watchResults`, `watchLimit`) must be documented in OpenAPI.


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
- Ensure tree renders with state colors (`RUNNING`, `SUSPENDED`, `STOPPED`) and selecting conveyor shows details.
- Run Parts tester:
  - ID mode: submit with `ttl` or `expirationTime`
  - foreach mode: check `Foreach`, verify ID disables
  - verify `Submit and watch` creates/updates watcher tag
- Run Static Parts tester:
  - create/update with value and verify response event appears in Output dock
  - delete with `delete=true` and verify value is not required
- Run Commands tester:
  - ID mode: `create`, `peek`, `peekId`, `reschedule`, `addProperties`, `completeExceptionally`
  - foreach mode: verify `create` disabled and other operations available per rules
  - verify `peek`/`peekId` require `requestTTL`
  - verify `ADD PROPERTIES` is disabled without properties
- Watch flow:
  - start conveyor-level watch via tree `Watch` button
  - confirm websocket watch events appear in Watchers panel and Output dock
  - confirm `PING` updates elapsed timer but does not add timeline entries
  - cancel watch with tag `X` and verify removal
- Output dock flow:
  - close and reopen dock
  - close/reopen per-source output tabs via new events
  - verify timeline navigation (`Prev`, `Next`, `See all`, `Clear events`)
  - verify timeline label shows time and ID tail (`...`) when ID is long
  - verify `Copy` copies only visible JSON payload and excludes status line text
  - verify no duplicate watch events are shown after page reload + new websocket events
  - verify cache label/value switch per selected tab (`Watch Cache` vs `Conveyor Cache`)
  - verify per-tab cache limits trim oldest events
  - verify JSONPath is independent per selected output tab
  - verify closing/reopening the same output tab in-session restores that tab's JSONPath and cache limit choices
- Admin actions flow:
  - verify reload/delete controls stay visible with no conveyor selected
  - verify no-selection and non-top-level guidance is shown as neutral hint text
  - verify reload/delete buttons are inactive until a top-level conveyor is selected
  - verify field label is `Complete and Stop Timeout`
  - verify submit creates scheduled event in `Admin` output tab
  - verify completion/failure arrives later in `Admin` output tab and refreshes tree
- Upload a valid conveyor jar using both native picker and drag/drop, confirm new conveyor appears
- Set `conveyor.service.upload-enable=false` and verify:
  - dashboard admin tab shows upload/remove disabled message
  - upload controls are unavailable
  - delete API remains forbidden (`403`)
  - API upload/delete endpoints return `403` with `errorCode=FORBIDDEN`
- Check `/swagger-ui/index.html` and `/v3/api-docs`

## 13. REST Audit Logging

- Intercept all REST endpoints exposed by service paths:
  - `/part/**`
  - `/static-part/**`
  - `/command/**`
  - `/api/**`
  - dashboard action POST endpoints used by UI forms:
    - `/dashboard/watch`
    - `/dashboard/test/**`
    - `/dashboard/admin/**`
- Exclude high-frequency dashboard admin polling endpoint from audit stream:
  - `GET /api/dashboard/admin/events`
- Exclude high-frequency dashboard polling endpoints from audit stream:
  - `GET /api/dashboard/watch`
  - `GET /api/dashboard/tree`
- For each completed request, write one audit line to dedicated logger `conveyor.audit.rest`.
- Each audit event must include:
  - `timestamp`
  - `userId` (authenticated username, otherwise `anonymous`)
  - `endpoint` (`<HTTP_METHOD> <path>`)
    - for dashboard tester/admin wrapper endpoints, `endpoint` must be rewritten to the equivalent underlying API/placement route (e.g. `/dashboard/test/place` -> `/part/{conveyor}/{id}/{label}`)
  - `sourceEndpoint` when endpoint was rewritten from a dashboard wrapper path
  - `requestKind` (e.g. `part`, `static-part`, `command`, `watch`, `admin-*`) when detectable
  - `parameters`:
    - `pathVariables` (e.g., conveyor/id/label/method/parameter)
    - `requestParameters` (query/form params such as `ttl`, `requestTTL`, `properties`, etc.)
    - request/form body carrier fields (`body`, `bodyFile`) must be excluded from logged parameters
  - `bodySize` (content-length in bytes, `-1` when unavailable)
  - `status` (HTTP response status code)
- Request body content must never be logged.
- Audit logging is controlled by configuration:
  - `conveyor.service.audit.enabled` (default `false`)
- Dedicated audit log file must use size/time rotation policy with limits controlled by:
  - `conveyor.service.audit.log-file`
  - `conveyor.service.audit.max-file-size`
  - `conveyor.service.audit.max-history`
  - `conveyor.service.audit.total-size-cap`
  - `conveyor.service.audit.clean-history-on-start`

## 14. Conveyor Log File Policy

- Conveyor runtime logs (`com.aegisql.conveyor`) must also be written to dedicated rolling file output.
- Conveyor rolling file must use size/time rotation policy.
- Conveyor logger level is controlled by:
  - `conveyor.service.conveyor-log.level`
- Conveyor log file and rotation constraints are controlled by:
  - `conveyor.service.conveyor-log.log-file`
  - `conveyor.service.conveyor-log.max-file-size`
  - `conveyor.service.conveyor-log.max-history`
  - `conveyor.service.conveyor-log.total-size-cap`
  - `conveyor.service.conveyor-log.clean-history-on-start`
