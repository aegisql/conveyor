# conveyor-service Instructions

## Purpose
- Spring Boot application exposing conveyors through REST endpoints, a dashboard UI, WebSocket/watch flows, and admin operations.

## Read First
- `./README.md`
- `./doc/project-context.md`
- `./doc/architecture-notes.md`
- `./doc/AUTHENTICATION.md`

## Local Rules
- Controller, security, and dashboard behavior are heavily test-defined; inspect tests before changing endpoints or auth rules.
- Keep OpenAPI docs aligned with actual controller behavior.
- Do not guess security defaults from old docs; use code and tests.
- Admin, watcher, and output-panel behaviors are part of the effective user-facing contract even when expressed through UI tests/docs rather than a formal API.

## Validation
- Default: `mvn -pl conveyor-service test`
- If changing security: run the `SecurityConfig*` tests.
- If changing REST contract: run the relevant controller tests and `OpenApiDocumentationTest`.
- If changing dashboard/view behavior: run the related `Dashboard*`, `Watch*`, and `ViewExceptionHandler*` tests.
