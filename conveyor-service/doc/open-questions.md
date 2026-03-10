# conveyor-service Open Questions

## Effective default upload directory
- **Observed evidence:** code defaults to `${user.home}/.conveyor-service/upload`, while README/requirements still describe `./upload` as the default used by the app.
- **Why ambiguous:** operators and docs readers will get different answers depending on source.
- **Suggested human follow-up:** align code and docs on one default.

## Maturity label vs current footprint
- **Observed evidence:** the README still calls the service an experimental alpha, but the module now has broad controller/security/dashboard/OpenAPI/audit/container documentation and tests.
- **Why ambiguous:** the maturity label may no longer reflect how the module is being maintained.
- **Suggested human follow-up:** decide whether to keep or revise the alpha label.
