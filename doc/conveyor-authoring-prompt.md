# Conveyor Authoring Prompt

## Purpose
- Reusable instruction for an AI/UI coding agent that must design and implement a working conveyor setup for this repository.
- Based on current examples in tests, demos, `./doc`, and `../conveyor.wiki`.
- Focuses on practical output: conveyor choice, builder, labels, consumers, readiness, loaders, and tests.

## Evidence Highlights
- `AssemblingConveyor` remains the default/general conveyor pattern across core tests, demos, and wiki.
- `BuilderSupplier`, `SmartLabel`, `LabeledValueConsumer`, `Testing`, `TestingState`, result consumers, and scrap consumers are stable framework concepts.
- Specialized utility conveyors now live in `conveyor-accelerators` and should be chosen only when the problem clearly matches them.
- Configurator YAML/properties can express common conveyor setups, especially `builderSupplier`, `defaultCartConsumer`, `readyWhenAccepted`, static parts, consumers, persistence, and parallel wiring.

## Conveyor Selection Rules
- Prefer `AssemblingConveyor<K,L,OUT>` unless the requested behavior clearly matches a specialized conveyor.
- Use `SimpleConveyor<K,OUT>` only when the user explicitly wants JavaPath/string-path label binding or reflection-style labels.
- Use `ScalarConvertingConveyor<K,String,OUT>` for one scalar input that is converted directly into the product or an intermediate builder.
- Use `CollectionConveyor<K,V>` when the product is just a collected ordered set/list of values.
- Use `MapConveyor<K,L,V>` when the product is a label-to-value map.
- Use `BatchConveyor<V>` when the goal is fixed-size or timeout-driven batching.
- Use `DelayLineConveyor<K,V>` when result release must be delayed intentionally.
- Use `CachingConveyor<K,L,OUT>` only for cache-like use cases where access to the live supplier/product lifecycle is required.
- Use `CounterAggregatorConveyor` for expected-vs-actual counting workflows.
- Use `QueuePump` or `SimpleScheduler` only when the problem is queue-driving or scheduling rather than ordinary product assembly.

## Default Design Rules
- Builder implements `Supplier<OUT>`.
- Builder fields/methods should reflect part labels clearly.
- Prefer explicit label handling over reflection unless reflection is the requested feature.
- Prefer enum or `SmartLabel` labels when the label vocabulary is fixed and type safety matters.
- Prefer string labels plus explicit `LabeledValueConsumer` when labels must stay readable or externally configurable.
- Keep result consumers non-blocking where possible.
- Always define what happens on timeout, invalid data, and incomplete builds.
- Do not introduce dependencies from `conveyor-core` to downstream modules.
- If the requested conveyor is specialized or illustrative, prefer `conveyor-accelerators`.

## What The Agent Must Ask Or Infer
- If the user gives a real business/domain specification, ask only for missing facts that materially change the design.
- If the user does not provide a real specification and only wants a demonstration, infer a small complete example and state the assumptions explicitly.
- Output/product class.
- Key type.
- Label type and expected parts.
- For each part:
  - label name
  - Java value type
  - whether required, optional, repeatable, or static
  - how it mutates the builder
- Completion criteria:
  - accepted labels/counts
  - builder predicate
  - state-aware predicate
  - timeout-based completion/defaulting
- Consumer behavior:
  - result destination
  - scrap/error handling
- Timing:
  - default builder timeout
  - per-request TTL/expiration expectations
- Whether commands, static parts, persistence, or parallelism are required.
- Whether Java code only is needed, or also configurator YAML/properties.

## Expected Output From The Agent
- A short design summary with explicit assumptions.
- Chosen conveyor type and why it fits better than alternatives.
- Product class and builder class.
- Label model:
  - enum `SmartLabel`
  - explicit string labels with `LabeledValueConsumer`
  - or reflection labels, if explicitly requested
- Conveyor setup code:
  - constructor choice
  - builder supplier
  - default cart consumer or smart-label mapping
  - readiness evaluator
  - result consumer
  - scrap consumer
  - timeout/default behavior if needed
- Example loader usage:
  - `part()`
  - `staticPart()` if defaults are needed
  - `command()` if lifecycle control is needed
- Tests:
  - happy path
  - incomplete/timeout path when relevant
  - invalid-data/scrap path when relevant
- Optional configurator YAML/properties only if the user asked for configuration-driven setup.

## Authoring Heuristics

### 1. Choose the simplest working conveyor
- If a regular builder plus explicit label mapping solves the problem, use `AssemblingConveyor`.
- Do not reach for reflection or specialized conveyors just to reduce a few lines of setup.

### 2. Make readiness explicit
- Prefer `Conveyor.getTesterFor(conveyor).accepted(...)` for simple mandatory-part completion.
- Use a custom `(state, builder) -> boolean` predicate when completion depends on value content, duplicates, or builder state.
- Use builder-implemented `Testing` or `TestingState` only when readiness belongs naturally inside the builder itself.

### 3. Keep label handling obvious
- For fixed business labels, use an enum `SmartLabel<B>` if compile-time safety matters.
- For external/configurable labels, use string labels and an explicit `LabeledValueConsumer`.
- For reflection labels, document that JavaPath semantics are in use.

### 4. Separate concerns cleanly
- Builder assembles state and creates the product.
- Conveyor orchestrates lifecycle, readiness, timeouts, and routing.
- Consumers route results and scraps; they should not hide builder logic.

### 5. Prefer testable configurations
- Include at least one end-to-end test that places parts and asserts the product.
- If using timeout/default behavior, include a timeout-specific test.
- If using commands or static parts, include a test that demonstrates them.

## Prompt Template

Use the following prompt when you want an agent to generate a conveyor design and implementation:

```text
You are working in the conveyor repository.

Design and implement a working conveyor solution for the following specification.

Inputs
- Target module: <conveyor-core | conveyor-accelerators | conveyor-configurator example | other>
- Product/output class: <name and purpose>
- Key type: <type>
- Expected parts:
  - <label>: <value type>, <required/optional/repeatable/static>, <meaning>
  - ...
- Completion criteria:
  - <accepted labels/counts and/or predicate logic>
- Timeout/default behavior:
  - <none or explicit rule>
- Result handling:
  - <where result goes>
- Scrap/error handling:
  - <what to log/store/ignore>
- Extra lifecycle needs:
  - <commands, static parts, priority, properties, futures, persistence, parallelism>
- Configuration output needed:
  - <Java only | Java + YAML/properties>

Required behavior
- Prefer `AssemblingConveyor` unless a specialized conveyor is clearly a better fit.
- Use specialized conveyors only when the requested behavior directly matches them.
- Keep label handling explicit unless reflection-style labels are explicitly requested.
- Builder must implement `Supplier<OUT>`.
- Define readiness explicitly.
- Add result and scrap consumer setup.
- Add focused tests for the generated behavior.
- If information is missing:
  - ask for it when it materially affects the design or public contract
  - otherwise make the smallest reasonable assumption and state it clearly before the code

Return format
1. Design summary
2. Assumptions
3. Conveyor choice and rationale
4. Product and builder code
5. Labels and cart-consumer mapping
6. Conveyor setup code
7. Example loader usage
8. Tests
9. Optional configurator YAML/properties, only if requested
```

## Short Checklist For The Agent
- Is the chosen conveyor the simplest one that fits?
- Is the builder a `Supplier<OUT>`?
- Are labels and part types explicit?
- Is readiness defined and testable?
- Are result and scrap consumers configured?
- Are timeout semantics explicit?
- Are loader examples included?
- Are tests included for success and failure/timeout where relevant?
- Is the code placed in the correct module?
