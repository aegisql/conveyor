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
- Bridge configuration to JDBC persistence builders and archive settings.

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
