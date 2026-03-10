# conveyor-core Architecture Notes

## Internal Shape
- Core API types live in the root `com.aegisql.conveyor` package.
- Submission payloads are represented as carts in `cart/*`.
- Loaders provide the fluent public API over cart/command creation.
- Consumers process terminal product/scrap events.
- `utils/*` contains reusable conveyor patterns, newer integration helpers, and exploratory/illustrative utilities.

## Important Boundaries
- `conveyor-core` should not depend on Spring, JDBC engines, or YAML parsing.
- `conveyor-core` should also avoid new third-party dependencies unless there is a strong repository-wide justification for them.
- Downstream modules depend on `Conveyor`, loaders, bins, and consumer interfaces.
- Utility packages are broad. They do not all represent the same level of contract stability as the root conveyor API.

## Current Reality Notes
- The Java HTTP client for `conveyor-service` lives here, not in `conveyor-service`.
- JDBC result-consumer support also lives here, while full JDBC cart persistence lives in the persistence sub-project.
- Some `utils/*` packages are reused directly by users or other modules; others remain more experimental and are better treated as incubating helpers unless tests/docs say otherwise.
