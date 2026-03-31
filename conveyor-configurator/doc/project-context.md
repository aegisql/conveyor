# conveyor-configurator Context

## Purpose
- Creates conveyors from `.properties` and YAML files and registers them by name (found in wiki and tests).

## Main Entry Points
- `ConveyorConfiguration`
- `ConveyorBuilder`
- `ConveyorProperty` / `PersistenceProperty`
- `ConfigurationPaths`
- `TemplateEditor`

## Responsibilities
- Parse configuration files from classpath or filesystem.
- Resolve environment/system property inputs used by configuration.
- Instantiate assembling, parallel, batch, and persistent conveyors.
- Bridge configuration to JDBC and Redis persistence builders and archive settings.

## Current Declarative Surface
- Core conveyor construction:
  - assembling conveyors
  - batch conveyors
  - persistent conveyors
- Parallel construction:
  - `KBalancedParallelConveyor` by numeric `parallel`
  - `LBalancedParallelConveyor` by named `parallel` list
  - `PBalancedParallelConveyor` by YAML `pBalanced` route map using exact-match property values
- Persistence construction:
  - preset JDBC persistence through configurator persistence properties
  - builder-owned Redis persistence through configurator persistence properties
- Timeout-recovery choice:
  - configurator supports `unloadOnBuilderTimeout`

## Scripting Boundary
- GraalJS is an advanced configuration path, not the primary model.
- The module uses the `graal.js` `ScriptEngine` surface for function expressions and object construction shortcuts.
- Common conveyor configuration should remain declarative when possible.
- Several configurator features can also resolve through Java references, constructors, or `JAVAPATH`-registered objects.
- GraalJS-backed configuration is currently relevant for:
  - builder suppliers and custom conveyor suppliers
  - result/scrap consumers
  - timeout and acknowledge actions
  - readiness/default cart functions
  - validators and before-eviction/rescheduling actions
  - forward rules with optional key transformers
  - persistence accessors and converters

## Scope Boundary
- `TaskPoolConveyor` is not currently part of the configurator declarative surface.
- It now lives in `conveyor-accelerators`, and support for it should be considered separately from the core configurator surface.

## External Integrations
- SnakeYAML.
- GraalJS / script engine for some configuration features.
- Core, parallel, and persistence modules.

## Key Tests
- `ConveyorConfigurationTest`
- `ConveyorConfigurationTest2`
- `PersistencePropertyTest`
- `ConfigurationPathsTest`
- `ConfigUtilsTest`

## Related Docs
- `conveyor-configurator/doc/configuration-gaps.md`
