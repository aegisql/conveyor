# conveyor-accelerators Context

## Purpose
- Houses specialized conveyor implementations that are useful to framework users but do not have to remain in `conveyor-core`.
- Current extracted surface:
  - `com.aegisql.conveyor.utils.collection.CollectionBuilder`
  - `com.aegisql.conveyor.utils.collection.CollectionConveyor`
  - `com.aegisql.conveyor.utils.batch.BatchCollectingBuilder`
  - `com.aegisql.conveyor.utils.batch.BatchConveyor`
  - `com.aegisql.conveyor.utils.builder.BuilderUtils`
  - `com.aegisql.conveyor.utils.caching.CachingConveyor`
  - `com.aegisql.conveyor.utils.caching.ImmutableReference`
  - `com.aegisql.conveyor.utils.caching.ImmutableValueConsumer`
  - `com.aegisql.conveyor.utils.caching.MutableReference`
  - `com.aegisql.conveyor.utils.caching.MutableValueConsumer`
  - `com.aegisql.conveyor.utils.counter.Counter`
  - `com.aegisql.conveyor.utils.counter.CountersAggregator`
  - `com.aegisql.conveyor.utils.counter.CounterAggregatorConveyor`
  - `com.aegisql.conveyor.utils.delay_line.DelayLineBuilder`
  - `com.aegisql.conveyor.utils.delay_line.DelayLineConveyor`
  - `com.aegisql.conveyor.utils.map.MapBuilder`
  - `com.aegisql.conveyor.utils.map.MapConveyor`
  - `com.aegisql.conveyor.utils.queue_pump.PumpId`
  - `com.aegisql.conveyor.utils.queue_pump.PumpLabel`
  - `com.aegisql.conveyor.utils.queue_pump.ScalarHolder`
  - `com.aegisql.conveyor.utils.queue_pump.QueuePump`
  - `com.aegisql.conveyor.utils.reflection.ReflectingValueConsumer`
  - `com.aegisql.conveyor.utils.reflection.SimpleConveyor`
  - `com.aegisql.conveyor.utils.scalar.ScalarConvertingBuilder`
  - `com.aegisql.conveyor.utils.scalar.ScalarConvertingConveyor`
  - `com.aegisql.conveyor.utils.schedule.SchedulableClosure`
  - `com.aegisql.conveyor.utils.schedule.Schedule`
  - `com.aegisql.conveyor.utils.schedule.ScheduleBuilder`
  - `com.aegisql.conveyor.utils.schedule.SimpleScheduler`
  - `com.aegisql.conveyor.parallel.utils.task_pool_conveyor.TaskId`
  - `com.aegisql.conveyor.parallel.utils.task_pool_conveyor.TaskLoader`
  - `com.aegisql.conveyor.parallel.utils.task_pool_conveyor.TaskPoolConveyor`
  - `com.aegisql.conveyor.parallel.utils.task_pool_conveyor.TaskPoolConveyorMBean`

## Main Entry Points
- `com.aegisql.conveyor.utils.batch.BatchConveyor`
- `com.aegisql.conveyor.utils.collection.CollectionConveyor`
- `com.aegisql.conveyor.utils.caching.CachingConveyor`
- `com.aegisql.conveyor.utils.counter.CounterAggregatorConveyor`
- `com.aegisql.conveyor.utils.delay_line.DelayLineConveyor`
- `com.aegisql.conveyor.utils.map.MapBuilder`
- `com.aegisql.conveyor.utils.map.MapConveyor`
- `com.aegisql.conveyor.utils.queue_pump.QueuePump`
- `com.aegisql.conveyor.utils.reflection.SimpleConveyor`
- `com.aegisql.conveyor.utils.scalar.ScalarConvertingConveyor`
- `com.aegisql.conveyor.utils.schedule.SimpleScheduler`
- `com.aegisql.conveyor.parallel.utils.task_pool_conveyor.TaskPoolConveyor`

## Responsibilities
- Carry specialized utility conveyors and adjacent builders after they are extracted from `conveyor-core`.
- Preserve compatibility for those extracted implementations while reducing the dependency and maintenance surface of `conveyor-core`.

## Dependency Direction
- `conveyor-accelerators` depends on `conveyor-core`.
- `conveyor-core` must not depend on `conveyor-accelerators`.
