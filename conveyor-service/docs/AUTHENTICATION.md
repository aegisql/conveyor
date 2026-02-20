# Authentication and Authorization

This document describes the authentication model currently implemented in `conveyor-service` and how to run each supported mode.

## 1. Implemented approach

The service supports two runtime security profiles:

- `demo`: local in-memory users with form login
- `prod`: external identity integration with optional JWT resource server, optional OAuth2 browser login, and HTTP Basic

Authentication methods currently implemented:

- Form login (`demo` only)
- Remember-me + signed auth cookie (`demo` only)
- OAuth2 login (`prod`, optional via config)
- JWT bearer token validation (`prod`)
- HTTP Basic (`prod`)

Authorization is role-based.

## 2. Roles and protected endpoints

Roles used by the service:

- `ROLE_REST_USER`
- `ROLE_DASHBOARD_VIEWER`
- `ROLE_DASHBOARD_ADMIN`

Access rules:

- `POST /part/**`, `POST /static-part/**`, `POST /command/**` -> `ROLE_REST_USER`
- `/dashboard/**`, `/api/dashboard/**` -> `ROLE_DASHBOARD_VIEWER` or `ROLE_DASHBOARD_ADMIN`
- `/dashboard/admin/**`, `/api/dashboard/admin/**` -> `ROLE_DASHBOARD_ADMIN`
- `/ws/**` -> `ROLE_DASHBOARD_VIEWER` or `ROLE_DASHBOARD_ADMIN`
- Swagger endpoints are public: `/swagger-ui/**`, `/v3/api-docs/**`, `/v3/api-docs.yaml`

Notes:

- Static assets and webjars are excluded from security.
- Feature flag `conveyor.service.upload-enable=false` does not change authentication, but upload/delete admin operations return `403 FORBIDDEN`.

## 3. Configuration keys

Main security-related properties:

- `spring.profiles.active` (`demo` or `prod`)
- `spring.security.oauth2.resourceserver.jwt.issuer-uri`
- `conveyor.service.oauth2-login-enable` (default `true`)
- `conveyor.service.oauth2-resource-server-enable` (default `true`)

Optional, but relevant to admin operations:

- `conveyor.service.upload-enable` (default `true`)

Profile note:

- `demo` enables local form-login mode and sets local logging defaults
- `dev` is logging-focused only; it does not switch auth mode by itself
- To use local form login, `demo` must be active

## 4. Demo profile (local testing)

This is the easiest mode for local evaluation.

### 4.1 Start

```bash
SPRING_PROFILES_ACTIVE=demo mvn -pl conveyor-service spring-boot:run
```

### 4.2 Built-in users

- `rest/rest` -> `ROLE_REST_USER`
- `viewer/viewer` -> `ROLE_DASHBOARD_VIEWER`
- `admin/admin` -> `ROLE_REST_USER`, `ROLE_DASHBOARD_VIEWER`, `ROLE_DASHBOARD_ADMIN`

### 4.3 Browser login

1. Open `http://localhost:8080/dashboard`
2. Login form appears
3. Use `viewer` or `admin` credentials
4. On success, app redirects to `/dashboard`

### 4.4 Remember-me and demo auth cookie

In `demo`, the app sets:

- remember-me cookie: `CONVEYOR_DEMO_REMEMBER_ME`
- signed auth cookie: `CONVEYOR_DEMO_AUTH`

Logout clears the demo auth cookie and redirects to `/login?logout`.

### 4.5 REST test in demo

Use Basic auth with `rest/rest`:

```bash
curl -u rest:rest -X POST "http://localhost:8080/part/collector/1/USER" \
  -H "Content-Type: application/json" \
  -d '{"name":"Ann"}'
```

## 5. Prod profile with OAuth2 login enabled

Use this when users should sign in through OAuth2/OIDC in browser.

### 5.1 Required config

Set:

- `spring.profiles.active=prod`
- `conveyor.service.oauth2-login-enable=true` (default)
- `conveyor.service.oauth2-resource-server-enable=true|false` (set `false` when only browser login is needed)
- `spring.security.oauth2.resourceserver.jwt.issuer-uri=<your issuer>` only when resource server is enabled
- `spring.security.oauth2.client.registration.*` and `provider.*` entries (for browser login)
- Runtime profile files live under `src/main/resources`:
  - `application-linkedin.yml`
  - `application-facebook.yml`
  - `application-demo.yml`
  - `application-dev.yml`
- For LinkedIn, use `client-authentication-method=client_secret_post`
- For local LinkedIn callback, register exact redirect URI: `http://localhost:8080/login/oauth2/code/linkedin`
- LinkedIn example config is available in `docs/application-prod-linkedin.example.yml`
- For Facebook, use `client-authentication-method=client_secret_post`
- For local Facebook callback, register exact redirect URI: `http://localhost:8080/login/oauth2/code/facebook`
- Facebook example config is available in `docs/application-prod-facebook.example.yml`

### 5.2 Start

From repository root:

```bash
SPRING_PROFILES_ACTIVE=prod,linkedin mvn -pl conveyor-service spring-boot:run
```

From `conveyor-service` directory:

```bash
SPRING_PROFILES_ACTIVE=prod,facebook mvn spring-boot:run
```

Minimal Facebook local run (no JWT issuer required):

```bash
SPRING_PROFILES_ACTIVE=prod,facebook \
FACEBOOK_CLIENT_ID=<your-app-id> \
FACEBOOK_CLIENT_SECRET=<your-app-secret> \
mvn -pl conveyor-service spring-boot:run
```

### 5.3 Login flow

1. Open dashboard URL
2. Spring Security OAuth2 login flow starts
3. Authenticate at IdP
4. Return to service with authenticated session
5. Current implementation redirects successful OAuth2 login to `/dashboard`

### 5.4 Role mapping in current implementation

For OAuth2 browser login, the service currently grants these roles after successful authentication:

- `ROLE_DASHBOARD_VIEWER`
- `ROLE_REST_USER`

Admin operations still require:

- `ROLE_DASHBOARD_ADMIN`

Authorization expectations remain:

- `ROLE_DASHBOARD_VIEWER` for dashboard/watch
- `ROLE_DASHBOARD_ADMIN` for admin operations
- `ROLE_REST_USER` for part/static-part/command APIs

If IdP tokens/users are not mapped to these roles, requests will return `403`.

### 5.5 LinkedIn interoperability notes (current implementation)

For `linkedin` profile, the service normalizes the authorization/token request shape to avoid provider incompatibilities observed during local testing:

- Uses `client_secret_post` token client authentication
- Removes PKCE parameters (`code_challenge`, `code_challenge_method`)
- Removes `code_verifier` from LinkedIn token POST
- Removes nonce from custom authorization request handling for LinkedIn
- Uses `openid` and `email` scopes by default in `application.yml`

### 5.6 Facebook profile notes (current implementation)

For `facebook` profile, the service uses Spring Security provider defaults plus explicit registration settings:

- Registration id: `facebook`
- `client-authentication-method=client_secret_post`
- scopes: `email`, `public_profile`
- callback URI: `http://localhost:8080/login/oauth2/code/facebook`
- `conveyor.service.oauth2-resource-server-enable=false` by default (no JWT issuer required for browser login)
- `spring.security.oauth2.resourceserver.jwt.issuer-uri` is not required unless you turn resource-server mode back on

Unlike LinkedIn flow normalization, Facebook uses the default OAuth2 authorization request shape.

### 5.7 Running `test-part-loader.sh` with LinkedIn/Facebook login

The script cannot execute the interactive OAuth browser flow on its own.

Use this flow:

1. Start service with either `prod,linkedin` or `prod,facebook` profiles.
2. Open `/dashboard` in browser and complete provider login.
3. Copy the `JSESSIONID` cookie value for `http://localhost:8080`.
4. Run script with cookie mode:

```bash
AUTH_MODE=cookie SESSION_COOKIE='JSESSIONID=<cookie-value>' \
/Users/mike/work/conveyor/conveyor-service/scripts/test-part-loader.sh collector 2003
```

### 5.8 Dashboard OAuth user display

For OAuth2 logins shown in the dashboard header, display name resolution is:

1. `email` / `emailAddress`
2. `preferred_username` / `username` / `login` / `name`
3. composed first+last name (`given_name` + `family_name`, or `first_name`/`last_name`)
4. fallback to `authentication.getName()`

This avoids showing opaque provider identifiers when richer claims (especially email) are available.

## 6. Prod profile with OAuth2 login disabled

Use this when corporate users want to replace built-in browser OAuth login with their own IAM entry flow.

### 6.1 Required config

- `spring.profiles.active=prod`
- `conveyor.service.oauth2-login-enable=false`
- `conveyor.service.oauth2-resource-server-enable=true`
- `spring.security.oauth2.resourceserver.jwt.issuer-uri=<your issuer>`

### 6.2 Behavior

With OAuth2 login disabled, the app still supports:

- JWT bearer tokens (`oauth2ResourceServer().jwt()`)
- HTTP Basic (`httpBasic()`)

This allows corporate setups where browser/API identity is handled externally and forwarded as JWT or Basic credentials.

### 6.3 Start

```bash
SPRING_PROFILES_ACTIVE=prod \
CONVEYOR_SERVICE_OAUTH2_LOGIN_ENABLE=false \
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://issuer.example.com \
mvn -pl conveyor-service spring-boot:run
```

## 7. JWT usage (prod)

For bearer auth, pass token in `Authorization` header:

```bash
curl -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:8080/part/collector/1/USER" \
  -d '{"name":"Ann"}'
```

Token must be issued by configured `issuer-uri` and contain authorities mapped to required service roles.

## 8. HTTP Basic usage (prod)

HTTP Basic is enabled in `prod` by design.

```bash
curl -u user:password -X POST "http://localhost:8080/part/collector/1/USER" \
  -H "Content-Type: application/json" \
  -d '{"name":"Ann"}'
```

Credentials must be backed by your configured authentication provider chain for prod deployment.

## 9. WebSocket authentication

Watch socket endpoint:

- `/ws/watch`

Access requires dashboard roles (`ROLE_DASHBOARD_VIEWER` or `ROLE_DASHBOARD_ADMIN`).

Current implementation registers the socket with `HttpSessionHandshakeInterceptor`, so browser session authentication is used for dashboard watch streaming.

## 10. Quick verification checklist

1. Confirm active profile (`demo` or `prod`).
2. Confirm auth method expected for that profile.
3. Confirm role mapping for requested endpoint class (REST, dashboard, admin).
4. If admin operations fail with `403 FORBIDDEN` and error code `FORBIDDEN`, also verify `conveyor.service.upload-enable=true`.
5. For WebSocket watch, confirm dashboard role and active authenticated browser session.
6. For local OAuth testing, prefer an incognito window to avoid stale login/session cookies.

## 11. OAuth2 troubleshooting (local)

Common errors and what they usually mean:

- `invalid_client` from token endpoint:
  - Usually client credentials mismatch or redirect URI mismatch in IdP app config
  - Validate credentials directly with a token curl call (a code-related error like `invalid_grant` or `authorization code not found` with a dummy code confirms credentials are accepted)
- `invalid_nonce` after token exchange:
  - Token exchange succeeded, but nonce validation failed in OIDC callback handling
  - Use latest service build and clear local browser session/cookies
- Browser loop back to `/login`:
  - Often a downstream OAuth2 callback failure; check `OAuth2 login failed:` log line first

Security note:

- Do not keep debug logs containing client secrets; rotate leaked OAuth client secrets immediately.
