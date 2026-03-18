# conveyor-accelerators Context

## Purpose
- Houses specialized conveyor implementations that are useful to framework users but do not have to remain in `conveyor-core`.
- Current extracted surface:
  - `com.aegisql.conveyor.utils.collection.CollectionBuilder`
  - `com.aegisql.conveyor.utils.collection.CollectionConveyor`
  - `com.aegisql.conveyor.utils.map.MapBuilder`
  - `com.aegisql.conveyor.utils.map.MapConveyor`
  - `com.aegisql.conveyor.utils.queue_pump.PumpId`
  - `com.aegisql.conveyor.utils.queue_pump.PumpLabel`
  - `com.aegisql.conveyor.utils.queue_pump.ScalarHolder`
  - `com.aegisql.conveyor.utils.queue_pump.QueuePump`

## Main Entry Points
- `com.aegisql.conveyor.utils.collection.CollectionConveyor`
- `com.aegisql.conveyor.utils.map.MapBuilder`
- `com.aegisql.conveyor.utils.map.MapConveyor`
- `com.aegisql.conveyor.utils.queue_pump.QueuePump`

## Responsibilities
- Carry specialized utility conveyors and adjacent builders after they are extracted from `conveyor-core`.
- Preserve compatibility for those extracted implementations while reducing the dependency and maintenance surface of `conveyor-core`.

## Dependency Direction
- `conveyor-accelerators` depends on `conveyor-core`.
- `conveyor-core` must not depend on `conveyor-accelerators`.
