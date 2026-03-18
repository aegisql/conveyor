# conveyor-accelerators Known Invariants

- `conveyor-accelerators` depends on `conveyor-core`; the dependency direction must not be reversed.
- Extracted classes should preserve their observable behavior unless corresponding tests and docs are intentionally updated.
- `conveyor-core` should not keep duplicate copies of extracted specialized conveyor implementations.
