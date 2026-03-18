# conveyor-accelerators Architecture Notes

## Current Shape
- This module is an extension layer over `conveyor-core`.
- It currently reuses the same `com.aegisql.conveyor.utils.*` package root for extracted classes to keep imports stable during the move.

## Boundaries
- Core framework contracts remain in `conveyor-core`.
- Specialized utility conveyors that are not required as part of the dependency floor should move here incrementally.

## Current Reality Notes
- The package root is shared across modules. That is intentional for this extraction step, but it should be treated carefully because it can make ownership less obvious.
