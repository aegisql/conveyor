# conveyor-core Open Questions

## Which utility packages have effectively graduated to supported API
- **Observed evidence:** many reusable classes live under `utils/*`, including the service client and JDBC helpers, while the package also contains exploratory patterns and example-style helpers.
- **Why ambiguous:** current structure suggests `utils/*` is not one uniform contract surface, but there is no explicit line showing which packages should be treated as compatibility-sensitive for external users.
- **Suggested human follow-up:** identify the `utils/*` packages that are intended to remain reusable supported surfaces versus those that are primarily illustrative or incubating.

## Loader API compatibility policy
- **Observed evidence:** loaders are heavily documented and tested, but there is no explicit compatibility statement.
- **Why ambiguous:** this matters for future refactors in fluent method names, return types, or default behavior.
- **Suggested human follow-up:** document compatibility expectations for loader APIs.
