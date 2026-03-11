# conveyor-persistence-jdbc Open Questions

## External database version support policy
- **Observed evidence:** module depends on current Derby/SQLite/MySQL/Postgres/MariaDB drivers and has engine tests.
- **Why ambiguous:** there is no maintained matrix describing which server versions are expected to remain compatible.
- **Suggested human follow-up:** document the tested/expected database versions.
