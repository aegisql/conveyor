# conveyor-core Open Questions

## Which utility packages have effectively graduated to supported API
- **Observed evidence:** many reusable classes live under `utils/*`, including the service client and JDBC helpers, while the package also contains exploratory patterns and example-style helpers.
- **Why ambiguous:** current structure suggests `utils/*` is not one uniform contract surface, and the first specialized conveyor extraction now lives in `conveyor-accelerators`, but there is still no explicit line showing which packages should stay in core.
- **Suggested human follow-up:** identify the `utils/*` packages that are intended to remain reusable supported surfaces in `conveyor-core` versus those that should migrate to `conveyor-accelerators`.

## Loader API compatibility policy
- **Observed evidence:** loaders are heavily documented and tested, but there is no explicit compatibility statement.
- **Why ambiguous:** this matters for future refactors in fluent method names, return types, or default behavior.
- **Suggested human follow-up:** document compatibility expectations for loader APIs.
