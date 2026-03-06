# Conveyor Service Client Library

This manual describes the Java client library implemented in `conveyor-core` for working with a running `conveyor-service` instance.

The client code lives in:

- `conveyor-core/src/main/java/com/aegisql/conveyor/utils/http/ConveyorServiceClient.java`
- `conveyor-core/src/main/java/com/aegisql/conveyor/utils/http/ConveyorServiceAuthentication.java`
- `conveyor-core/src/main/java/com/aegisql/conveyor/utils/http/ConveyorServiceTestApplication.java`

The HTTP implementation itself uses only JDK APIs.

## 1. Purpose

The client gives Java code the same loader-style API used for local conveyors:

- `PartLoader`
- `StaticPartLoader`
- `CommandLoader`

Instead of placing directly into a local conveyor, these loaders call the REST endpoints exposed by `conveyor-service`.

This is useful when:

- the service is running remotely
- you want loader-style request construction with HTTP transport hidden behind it

## 2. Artifact

The client is part of the `conveyor-core` artifact:

```xml
<dependency>
  <groupId>com.aegisql</groupId>
  <artifactId>conveyor-core</artifactId>
  <version>1.7.3-SNAPSHOT</version>
</dependency>
```

Adjust the version to the one you build or publish.

## 3. Supported Authentication

The client supports headless authentication modes that make sense for Java callers:

- `ConveyorServiceAuthentication.none()`
- `ConveyorServiceAuthentication.basic(username, password)`
- `ConveyorServiceAuthentication.bearer(token)`
- `ConveyorServiceAuthentication.cookie(cookieHeader)`
- `ConveyorServiceAuthentication.session(username, password)`

Examples:

```java
ConveyorServiceClient client = ConveyorServiceClient.builder("http://localhost:8080")
        .authentication(ConveyorServiceAuthentication.basic("rest", "rest"))
        .build();
```

```java
ConveyorServiceClient client = ConveyorServiceClient.builder("http://localhost:8080")
        .authentication(ConveyorServiceAuthentication.session("admin", "admin"))
        .build();
```

Notes:

- `session(...)` performs form login against `/login` and then reuses the returned session cookie.
- `cookie(...)` is useful when a user logs in through the browser and then copies the session cookie value into an automated client.
- Browser OAuth2 flows such as LinkedIn or Facebook are not executed by the client itself. For those cases, use either:
  - a session cookie obtained after browser login
  - a bearer token if your deployment exposes resource-server mode

See `AUTHENTICATION.md` for service-side authentication setup.

## 4. Creating the Client

```java
ConveyorServiceClient client = ConveyorServiceClient.builder("http://localhost:8080")
        .authentication(ConveyorServiceAuthentication.basic("rest", "rest"))
        .build();
```

Optional builder settings:

- `connectTimeout(Duration)`
- `requestTimeout(Duration)`
- `valueCodec(HttpValueCodec)`

The default value codec automatically chooses:

- `application/json` for maps, records, beans, numbers, booleans, enums
- `text/plain; charset=UTF-8` for plain strings
- `application/octet-stream` for `byte[]`

## 4.1 Client Logging

The client uses JDK logging (`java.util.logging`) only.

Two logger names are available:

- `com.aegisql.conveyor.utils.http.ConveyorServiceClient`
- `conveyor.audit.client`

Recommended usage:

- enable `FINE` for `com.aegisql.conveyor.utils.http.ConveyorServiceClient` when troubleshooting request flow
- enable `INFO` for `conveyor.audit.client` when you need a structured audit trail

What the audit logger records:

- timestamp
- user id, when the selected authentication mode provides one
- HTTP method
- full URL
- sanitized request parameters
- body size
- HTTP status

What is intentionally not logged:

- passwords
- bearer tokens
- cookie values and session ids
- request body contents
- arbitrary property values that are not explicitly safe for audit

Redaction rules:

- safe operational query fields such as `ttl`, `requestTTL`, `creationTime`, `expirationTime`, `priority`, `delete`, `watchResults`, and `watchLimit` keep their values
- query keys containing markers such as `password`, `token`, `secret`, `cookie`, `session`, `authorization`, or `auth` are logged as `REDACTED`
- all other custom query values are logged as `PRESENT`

Example `logging.properties` snippet:

```properties
com.aegisql.conveyor.utils.http.ConveyorServiceClient.level=FINE
conveyor.audit.client.level=INFO
```

## 5. Sending Parts

### 5.1 Regular part placement

```java
boolean accepted = client.part("collector")
        .id("42")
        .label("USER")
        .ttl(1, java.util.concurrent.TimeUnit.MINUTES)
        .addProperty("requestTTL", 250)
        .value(java.util.Map.of("firstName", "Ann"))
        .place()
        .get();
```

Notes:

- `ttl(...)` maps to the service `ttl` request parameter.
- `requestTTL` is currently passed as a regular property because the loader API does not have a dedicated setter for it.
- `place()` returns `CompletableFuture<Boolean>`.

### 5.2 Foreach placement

```java
boolean accepted = client.part("collector")
        .foreach()
        .label("RECALCULATE")
        .value("run")
        .place()
        .get();
```

Current limitation:

- remote foreach placement supports only `SerializablePredicate.ANY`
- custom foreach filters are rejected with `UnsupportedOperationException`

## 6. Sending Static Parts

### 6.1 Create or replace a static part

```java
boolean accepted = client.staticPart("collector")
        .label("CONFIG")
        .priority(10)
        .value(java.util.Map.of("enabled", true))
        .place()
        .get();
```

### 6.2 Delete a static part

```java
boolean accepted = client.staticPart("collector")
        .label("CONFIG")
        .delete()
        .place()
        .get();
```

## 7. Sending Commands

### 7.1 Supported commands

The service currently exposes HTTP endpoints for these command operations:

- `cancel()`
- `completeExceptionally(Throwable)`
- `addProperty(...)`
- `addProperties(...)`
- `timeout()`
- `reschedule()`
- `create()`
- `peek()`
- `peek(Consumer<ProductBin<K,OUT>>)`
- `peekId(Consumer<K>)`

Examples:

```java
client.command("collector").id("42").cancel().get();
client.command("collector").id("42").timeout().get();
client.command("collector").id("42").reschedule().get();
client.command("collector").id("42").addProperty("source", "replay").get();
client.command("collector").id("42").completeExceptionally(new IllegalStateException("bad input")).get();
client.command("collector").id("42").create().get();
```

### 7.2 Peek examples

Single key:

```java
ProductBin<String, Object> bin = client.<String, Object>command("collector")
        .id("42")
        .peek()
        .get();
```

Foreach:

```java
java.util.List<ProductBin<String, Object>> bins = client.<String, Object>command("collector")
        .foreach()
        .peek()
        .get();
```

Key-only peek:

```java
java.util.List<String> ids = new java.util.ArrayList<>();

client.<String, Object>command("collector")
        .foreach()
        .peekId(ids::add)
        .get();
```

### 7.3 Unsupported commands

These loader methods are not mapped because the current service does not expose matching REST operations:

- `complete(result)`
- `check()`
- `memento()`
- `restore(...)`
- `suspend()`
- `create(builderSupplier)`
- custom foreach filters other than `ANY`

The client fails fast with `UnsupportedOperationException` for those cases.

## 8. Error Handling

Service-level failures are reported as `ConveyorServiceException`.

Useful fields:

- `getHttpStatus()`
- `getPlacementStatus()`
- `getRawBody()`

Typical example:

```java
try {
    client.part("collector")
            .id("42")
            .label("USER")
            .value("{bad json")
            .place()
            .join();
} catch (java.util.concurrent.CompletionException ex) {
    if (ex.getCause() instanceof ConveyorServiceException cse) {
        System.err.println("HTTP status: " + cse.getHttpStatus());
        System.err.println("Placement status: " + cse.getPlacementStatus());
        System.err.println("Raw body: " + cse.getRawBody());
    }
}
```

## 9. Java Test Application

The example application is:

- `conveyor-core/src/main/java/com/aegisql/conveyor/utils/http/ConveyorServiceTestApplication.java`

It can be used as:

- a runnable example of the client API
- a Java replacement for shell-based request loaders
- a PSV file replay tool

### 9.1 Build

From repository root:

```bash
mvn -pl conveyor-core -DskipTests compile
```

### 9.2 Run the fixed three-part flow

```bash
java -cp conveyor-core/target/classes \
  com.aegisql.conveyor.utils.http.ConveyorServiceTestApplication \
  --base-url http://localhost:8080 \
  --auth-mode basic \
  --user rest \
  --password rest \
  --conveyor collector \
  --id 42
```

This sends:

- `USER`
- `ADDRESS`
- `DONE`

for the same correlation id.

### 9.3 Replay a PSV file

```bash
java -cp conveyor-core/target/classes \
  com.aegisql.conveyor.utils.http.ConveyorServiceTestApplication \
  --base-url http://localhost:8080 \
  --auth-mode cookie \
  --cookie 'JSESSIONID=<your-session-cookie>' \
  --file scripts/test-data.psv \
  --shuffle
```

### 9.4 PSV formats

Part-oriented header:

```text
CONVEYOR_NAME|ID|LABEL|BODY|source|requestTTL
collector|1001|USER|{"name":"John"}|bulk|250
collector|1001|ADDRESS|{"zip":"11111"}|bulk|250
collector||CONFIG|{"enabled":true}|seed|
```

Rules:

- if the header contains `ID` as the second column, rows are treated as part rows
- if `ID` is blank in that format, the row is sent to the static-part endpoint

Static-part-oriented header:

```text
CONVEYOR_NAME|LABEL|BODY|delete|source
collector|CONFIG|{"enabled":true}||seed
collector|CONFIG|ignored|true|cleanup
```

Rules:

- `delete=true` switches the row to static-part deletion mode
- blank extra columns are ignored

### 9.5 Command-line options

- `--base-url <url>`
- `--conveyor <name>`
- `--id <value>`
- `--file <path>`
- `--shuffle`
- `--ttl <value>`
- `--request-ttl <value>`
- `--auth-mode none|session|basic|bearer|cookie`
- `--user <name>`
- `--password <value>`
- `--token <value>`
- `--cookie <value>`

Positional shorthand is also supported:

```bash
java -cp conveyor-core/target/classes \
  com.aegisql.conveyor.utils.http.ConveyorServiceTestApplication \
  collector 42
```

That is equivalent to:

- conveyor name = `collector`
- id = `42`

## 10. Recommended Usage Pattern

For service-to-service calls:

- use `basic(...)` or `bearer(...)`

For browser-assisted testing:

- log into the dashboard
- copy the session cookie
- use `cookie(...)` or `--auth-mode cookie`

For local demo profile:

- use `basic(rest, rest)` for REST-only calls
- use `session(admin, admin)` when you want dashboard-style authenticated session behavior

## 11. Tests

Focused tests for the client package are here:

- `conveyor-core/src/test/java/com/aegisql/conveyor/utils/http/ConveyorServiceClientTest.java`
- `conveyor-core/src/test/java/com/aegisql/conveyor/utils/http/ConveyorServiceTestApplicationTest.java`
- `conveyor-core/src/test/java/com/aegisql/conveyor/utils/http/SimpleJsonTest.java`

Example verification command:

```bash
mvn -pl conveyor-core \
  -Dtest=SimpleJsonTest,ConveyorServiceClientTest,ConveyorServiceTestApplicationTest \
  test jacoco:report
```
