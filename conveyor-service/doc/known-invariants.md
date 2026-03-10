# conveyor-service Known Invariants

- Roles in current code are `REST_USER`, `DASHBOARD_VIEWER`, and `DASHBOARD_ADMIN` (code/docs/tests).
- Swagger/OpenAPI endpoints are intended to stay public in the current security model (docs/tests).
- Placement endpoints distinguish async and waited behavior:
  - no `requestTTL` + successful scheduling => `202 Accepted`
  - waited/completed flows can return `200 OK`
  (inferred from controller tests).
- `demo` profile provides local form-login users; `prod` supports external identity integration with optional OAuth2 login and optional JWT resource-server mode (found in docs and tests).
- REST audit logging must not expose request bodies or auth secrets; it records URL/parameters/body size/status instead (inferred from interceptor behavior and tests).
