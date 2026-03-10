# conveyor-persistence-core Context

## Purpose
- Database-neutral persistence SPI and supporting infrastructure for persistent conveyors.

## Main Entry Points
- `Persistence<K>`
- `PersistentConveyor`
- archive types in `persistence.archive`
- converters in `persistence.converters`
- utilities in `persistence.utils`

## Responsibilities
- Store and restore carts/build state through the `Persistence` contract.
- Convert values and metadata to byte-oriented persistence forms.
- Archive persisted records according to strategy.
- Support recovery/absorption flows.

## Key Tests
- `PersistentConveyorTest`
- `ConvertersTest`
- `ConverterAdviserTest`
- `BinaryLogConfigTest`
- `PersistUtilsTest`
