# conveyor-core Context

## Purpose
- Defines the core Conveyor abstraction and most framework-level public APIs (inferred from `Conveyor.java`, wiki, and tests).
- Serves as the foundational dependency for the other repository sub-projects.

## Main Entry Points
- `com.aegisql.conveyor.Conveyor`
- `com.aegisql.conveyor.AssemblingConveyor`
- Loader APIs in `com.aegisql.conveyor.loaders`
- Consumer APIs in `com.aegisql.conveyor.consumers.*`
- Core utility conveyors and helpers under `com.aegisql.conveyor.utils.*`
- Service client under `com.aegisql.conveyor.utils.http`

## Responsibilities
- Cart and command submission model.
- Product/scrap emission and consumer chaining.
- Delay, readiness, priority, and lifecycle management.
- Shared builder/helper infrastructure used by the framework and extracted accelerator modules.
- Provide the smallest practical dependency base for downstream modules and external users of the framework.

## Current Extraction Boundary
- `BatchCollectingBuilder` and `BatchConveyor` now live in `conveyor-accelerators`.
- `CollectionBuilder` and `CollectionConveyor` now live in `conveyor-accelerators`.
- `CachingConveyor`, `ImmutableReference`, `ImmutableValueConsumer`, `MutableReference`, and `MutableValueConsumer` now live in `conveyor-accelerators`.
- `Counter`, `CountersAggregator`, and `CounterAggregatorConveyor` now live in `conveyor-accelerators`.
- `DelayLineBuilder` and `DelayLineConveyor` now live in `conveyor-accelerators`.
- `BuilderUtils` now lives in `conveyor-accelerators`.
- `MapBuilder` and `MapConveyor` now live in `conveyor-accelerators`.
- `QueuePump`, `PumpId`, `PumpLabel`, and the queue-pump `ScalarHolder` now live in `conveyor-accelerators`.
- `ReflectingValueConsumer` and `SimpleConveyor` now live in `conveyor-accelerators`.
- `ScalarConvertingBuilder` and `ScalarConvertingConveyor` now live in `conveyor-accelerators`.
- `SchedulableClosure`, `Schedule`, `ScheduleBuilder`, and `SimpleScheduler` now live in `conveyor-accelerators`.
- `conveyor-core` still provides the shared base types those specialized implementations build on, including `CommonBuilder`.

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
