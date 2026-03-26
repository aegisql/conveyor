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

## Placement Outcome Contract

For persisted placement flows in `PersistentConveyor`:

- storage write failures complete the returned placement future exceptionally
- `false` remains available for non-success conveyor outcomes and is not reserved for persistence failure

This behavior is shared across backends because `PersistentConveyor` combines the forward conveyor future with the persistence acknowledge path, and the concrete persistence implementations propagate write failures by throwing exceptions from `savePart(...)`.

## Key Tests
- `PersistentConveyorTest`
- `ConvertersTest`
- `ConverterAdviserTest`
- `BinaryLogConfigTest`
- `PersistUtilsTest`
