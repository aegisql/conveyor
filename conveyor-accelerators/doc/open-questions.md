# conveyor-accelerators Open Questions

## Which utility packages should move here next
- **Observed evidence:** `conveyor-core` still contains multiple specialized implementations under `utils/*`, while repository docs now describe that area as partly incubating.
- **Why ambiguous:** there is no explicit migration list or prioritization for the next extractions.
- **Suggested human follow-up:** decide which `utils/*` packages are better treated as accelerators rather than part of the core dependency floor.

## Long-term package strategy
- **Observed evidence:** extracted classes currently keep their original `com.aegisql.conveyor.utils.*` package names for compatibility.
- **Why ambiguous:** that reduces churn now, but it leaves package ownership split across artifacts.
- **Suggested human follow-up:** decide whether keeping the shared package root is the long-term plan or only a transition strategy.
