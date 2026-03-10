# conveyor-service Architecture Notes

## Layering
- `service.web`
  - Spring MVC controllers and exception handlers.
- `service.core`
  - Placement, command, watch, dashboard, and admin orchestration services.
- `service.config`
  - Bound properties, security, audit interceptor, Jackson config, and WebSocket config.

## Integration Style
- The service is an adapter over named conveyors and their MBeans; it does not replace core conveyor logic.
- Dashboard behavior is split between server-rendered HTML, JSON endpoints, and WebSocket-driven output updates.
- Security behavior is profile-based (`demo` vs `prod`) with feature flags for OAuth2 login and resource-server mode.

## Important Boundaries
- REST and dashboard auth rules are module-level public behavior and are test-covered.
- OpenAPI documentation is part of the effective public contract and has dedicated tests.
- Audit logging is implemented as a web interceptor/config concern, not inside each controller.
