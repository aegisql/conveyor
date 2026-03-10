# conveyor-configurator Known Invariants

- Configuration files can be loaded from explicit filesystem paths or classpath resources (inferred from tests).
- Built conveyors are globally retrievable by name via `Conveyor.byName(...)` after configuration runs (inferred from tests).
- Persistence-enabled configurator tests keep DB/archive outputs under `test-artifacts/persistence` for review until `mvn clean` (inferred from tests/POM).
- Configurator changes can instantiate multiple module types; breakage often shows up only when core, parallel, and persistence are all in play.
