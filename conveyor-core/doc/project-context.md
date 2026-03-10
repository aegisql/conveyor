# conveyor-core Context

## Purpose
- Defines the core Conveyor abstraction and most framework-level public APIs (inferred from `Conveyor.java`, wiki, and tests).
- Serves as the foundational dependency for the other repository sub-projects.

## Main Entry Points
- `com.aegisql.conveyor.Conveyor`
- `com.aegisql.conveyor.AssemblingConveyor`
- Loader APIs in `com.aegisql.conveyor.loaders`
- Consumer APIs in `com.aegisql.conveyor.consumers.*`
- Utility conveyors under `com.aegisql.conveyor.utils.*`
- Service client under `com.aegisql.conveyor.utils.http`

## Responsibilities
- Cart and command submission model.
- Product/scrap emission and consumer chaining.
- Delay, readiness, priority, and lifecycle management.
- Helper implementations such as batch, caching, collection, map, counter, delay-line, queue-pump, scalar, and scheduler utilities.
- Provide the smallest practical dependency base for downstream modules and external users of the framework.

## External Integrations
- JMX/MBeans for conveyor management.
- Java `HttpClient` for the service client (inferred from code and module docs).
- SLF4J logging.

## Dependency Direction
- `conveyor-core` is intended to stand alone.
- Other sub-projects may depend on it.
- It should not depend on downstream repository modules.

## Key Tests
- `AssemblingConveyorTest`
- `CommandLoaderTest`
- `ConveyorTest`
- `ConveyorServiceClientTest`
- `ConveyorServiceTestApplicationTest`
- consumer- and utility-specific tests across `src/test/java`
