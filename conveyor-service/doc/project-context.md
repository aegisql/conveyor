# conveyor-service Context

## Purpose
- Spring Boot service that exposes conveyors through REST APIs and a dashboard UI (found in module README, docs, and tests).

## Main Entry Points
- `ConveyorServiceApplication`
- Web layer under `service.web`
- Service layer under `service.core`
- Security/audit/WebSocket configuration under `service.config`

## Responsibilities
- Part, static-part, and command placement over HTTP.
- Dashboard views and JSON endpoints for runtime inspection.
- Watch/event streaming over HTTP/WebSocket.
- Admin actions over uploaded extension JARs and JMX/MBean-backed conveyor operations.
- Profile-driven authentication and authorization.

## External Integrations
- Spring Boot 4 / Spring MVC / WebSocket / Actuator.
- Spring Security, OAuth2 client, and JWT resource server.
- Conveyor core/parallel/configurator/persistence modules.
- Docker image and compose-based demo runtime.

## Key Tests
- `PartControllerTest`
- `CommandControllerTest`
- `StaticPartControllerTest`
- `Dashboard*Test`
- `Watch*Test`
- `SecurityConfig*Test`
- `OpenApiDocumentationTest`
- `RestAuditInterceptorTest`
