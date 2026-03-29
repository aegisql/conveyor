# conveyor-accelerators

Specialized conveyor implementations and helpers built on top of `conveyor-core`.

This module exists for conveyors that are useful and reusable, but do not need to stay in the dependency floor of the framework. If you only need the core framework contracts, use `conveyor-core`. If you want ready-made patterns for common assembly styles, use `conveyor-accelerators`.

## Dependency

```xml
<dependency>
  <groupId>com.aegisql</groupId>
  <artifactId>conveyor-accelerators</artifactId>
  <version>1.8.0-SNAPSHOT</version>
</dependency>
```

## What Is In This Module

### BatchConveyor
- Class: `com.aegisql.conveyor.utils.batch.BatchConveyor`
- Produces batches of values as `List<V>`.
- Good for:
  - buffering events into chunks
  - sending items downstream in fixed-size groups
  - flushing on timeout or explicit completion

### CollectionConveyor
- Class: `com.aegisql.conveyor.utils.collection.CollectionConveyor`
- Collects many incoming items into a `Collection<V>`.
- Good for:
  - gathering a variable number of items under one correlation id
  - waiting for an explicit `COMPLETE` signal
  - building simple bags/lists of values

### MapConveyor
- Class: `com.aegisql.conveyor.utils.map.MapConveyor`
- Builds a `Map<L,V>` from labeled parts.
- Good for:
  - dynamic key/value assembly
  - lightweight record building without a dedicated builder class
  - cases where labels naturally become map keys

### CounterAggregatorConveyor
- Class: `com.aegisql.conveyor.utils.counter.CounterAggregatorConveyor`
- Aggregates named counters and expected values into a nested result map.
- Good for:
  - task progress tracking
  - expected-vs-actual counters
  - small monitoring or reporting pipelines

### DelayLineConveyor
- Class: `com.aegisql.conveyor.utils.delay_line.DelayLineConveyor`
- Delivers values after their TTL/expiration delay.
- Good for:
  - delayed processing
  - simple scheduling by delay
  - ordering work by time rather than immediate completion

### QueuePump
- Class: `com.aegisql.conveyor.utils.queue_pump.QueuePump`
- A very small conveyor that always uses one fixed key and label.
- Good for:
  - bridging queue-driven or push-driven input into a conveyor
  - pushing scalar values through a uniform loader API
  - cases where correlation is not the point

### CachingConveyor
- Class: `com.aegisql.conveyor.utils.caching.CachingConveyor`
- Keeps builders alive as a cache and exposes product suppliers by key.
- Good for:
  - read-mostly assembled state
  - keeping mutable builder-backed objects accessible by key
  - in-memory cache-like assembly flows

### ScalarConvertingConveyor
- Class: `com.aegisql.conveyor.utils.scalar.ScalarConvertingConveyor`
- Converts one scalar input into one product.
- Good for:
  - string-to-object conversion
  - single-field parsing pipelines
  - simple staged conversions

### SimpleConveyor
- Class: `com.aegisql.conveyor.utils.reflection.SimpleConveyor`
- A string-label conveyor backed by `ReflectingValueConsumer`.
- Good for:
  - rapid prototypes
  - string-driven assembly
  - cases where builder field/method binding by label is more convenient than writing explicit consumers

### SimpleScheduler
- Class: `com.aegisql.conveyor.utils.schedule.SimpleScheduler`
- Schedules `SchedulableClosure` values for execution using timeout/reschedule behavior.
- Good for:
  - recurring or one-shot delayed actions
  - lightweight task scheduling
  - embedding simple timer-driven work into a conveyor-based system

## Related Helpers

These are not conveyors themselves, but they are part of the module because they support accelerator-style assembly:

- `com.aegisql.conveyor.utils.builder.BuilderUtils`
  - wraps ordinary builders so they can be used as conveyor builders with custom product suppliers and readiness rules
- `com.aegisql.conveyor.utils.reflection.ReflectingValueConsumer`
  - string-label-to-builder binding used by `SimpleConveyor`
- `com.aegisql.conveyor.utils.caching.ImmutableReference`
- `com.aegisql.conveyor.utils.caching.MutableReference`
- `com.aegisql.conveyor.utils.caching.ImmutableValueConsumer`
- `com.aegisql.conveyor.utils.caching.MutableValueConsumer`

## Choosing Quickly

Use:
- `MapConveyor` when labels should become map keys
- `CollectionConveyor` when you are just collecting items
- `BatchConveyor` when you want chunking by size or timeout
- `CounterAggregatorConveyor` when labels represent named counters
- `DelayLineConveyor` when time is the main completion trigger
- `QueuePump` when you need a fixed-id single-lane pump
- `CachingConveyor` when you want to keep assembled state available by key
- `ScalarConvertingConveyor` when one input becomes one output
- `SimpleConveyor` when string labels and reflection are acceptable
- `SimpleScheduler` when the product is executable scheduled work

## Simple Examples

### MapConveyor

```java
MapConveyor<Integer, String, String> conveyor = new MapConveyor<>();
conveyor.setBuilderSupplier(MapBuilder::new);

conveyor.resultConsumer(bin -> {
    System.out.println(bin.product);
}).set();

var part = conveyor.part().id(1);
part.label("FIRST").value("John").place();
part.label("LAST").value("Doe").place();

// null label + null value marks completion for the map builder
part.label(null).value(null).place().join();
```

Expected product:

```java
{FIRST=John, LAST=Doe}
```

### CollectionConveyor

```java
CollectionConveyor<Integer, Integer> conveyor = new CollectionConveyor<>();
conveyor.setBuilderSupplier(CollectionBuilder::new);

conveyor.resultConsumer(bin -> {
    System.out.println(bin.product);
}).set();

for (int i = 0; i < 5; i++) {
    conveyor.part().id(1).value(i).place();
}

conveyor.part().id(1).label(conveyor.COMPLETE).place().join();
```

### BatchConveyor

```java
BatchConveyor<Integer> conveyor = new BatchConveyor<>();
conveyor.setBuilderSupplier(() -> new BatchCollectingBuilder<>(3, 1, TimeUnit.SECONDS));

conveyor.resultConsumer(bin -> {
    System.out.println(bin.product);
}).set();

conveyor.part().value(1).place();
conveyor.part().value(2).place();
conveyor.part().value(3).place(); // first batch of 3 is ready

conveyor.part().value(4).place();
conveyor.completeBatch().join();  // flush remaining items
```

### ScalarConvertingConveyor

```java
class StringToUserBuilder extends ScalarConvertingBuilder<String, User> {
    @Override
    public User get() {
        String[] fields = scalar.split(",");
        return new User(fields[0], fields[1], Integer.parseInt(fields[2]));
    }
}

ScalarConvertingConveyor<String, String, User> conveyor = new ScalarConvertingConveyor<>();
conveyor.setBuilderSupplier(StringToUserBuilder::new);

conveyor.resultConsumer(bin -> {
    System.out.println(bin.product);
}).set();

conveyor.part().id("user-1").value("John,Doe,1990").place().join();
```

### SimpleConveyor

```java
SimpleConveyor<Integer, Person> conveyor = new SimpleConveyor<>(PersonBuilder::new);
conveyor.setReadinessEvaluator(
    Conveyor.getTesterFor(conveyor).accepted("firstName", "lastName", "dateOfBirth")
);

conveyor.resultConsumer(bin -> {
    System.out.println(bin.product);
}).set();

conveyor.part().id(1).label("firstName").value("Ann").place();
conveyor.part().id(1).label("lastName").value("Lee").place();
conveyor.part()
    .id(1)
    .label("dateOfBirth")
    .value(new SimpleDateFormat("yyyy-MM-dd").parse("1990-02-14"))
    .place();
```

This style is convenient, but it is more dynamic than explicit `SmartLabel`-based assembly.

### SimpleScheduler

```java
SimpleScheduler<String> scheduler = new SimpleScheduler<>();
SchedulableClosure task = () -> System.out.println("run once after delay");

scheduler.part()
    .id("job-1")
    .label(Schedule.EXECUTE_ONCE)
    .value(task)
    .ttl(1, TimeUnit.SECONDS)
    .place();
```

Use this when the product is scheduled executable work rather than an assembled domain object.

## More Example Sources

If you want working examples from the current codebase, start here:

- `conveyor-accelerators/src/test/java/com/aegisql/conveyor/utils/map/MapBuillderTest.java`
- `conveyor-accelerators/src/test/java/com/aegisql/conveyor/utils/collection/CollectionBuilderTest.java`
- `conveyor-accelerators/src/test/java/com/aegisql/conveyor/utils/batch/BatchConveyorBuilderTest.java`
- `conveyor-accelerators/src/test/java/com/aegisql/conveyor/utils/counter/CounterAggregatorConveyorTest.java`
- `conveyor-accelerators/src/test/java/com/aegisql/conveyor/utils/delay_line/DelayLineConveyorTest.java`
- `conveyor-accelerators/src/test/java/com/aegisql/conveyor/utils/queue_pump/QueuePumpTest.java`
- `conveyor-accelerators/src/test/java/com/aegisql/conveyor/utils/caching/CachingConveyorTest.java`
- `conveyor-accelerators/src/test/java/com/aegisql/conveyor/utils/scalar/ScalarConvertingConveyorTest.java`
- `conveyor-accelerators/src/test/java/com/aegisql/conveyor/utils/reflection/SimpleConveyorTest.java`
- `conveyor-accelerators/src/test/java/com/aegisql/conveyor/utils/schedule/ScheduleBuilderTest.java`
- `conveyor-accelerators/src/test/java/com/aegisql/conveyor/demo/caching_conveyor/Demo.java`
- `conveyor-accelerators/src/test/java/com/aegisql/conveyor/demo/reflection/Demo.java`
- `conveyor-accelerators/src/test/java/com/aegisql/conveyor/demo/simple_conveyor/Demo.java`

## Notes

- This module depends on `conveyor-core`.
- `conveyor-core` must not depend back on this module.
- The package root is still `com.aegisql.conveyor.utils.*` for compatibility during extraction.
