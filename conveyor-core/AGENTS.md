# conveyor-core Instructions

## Purpose
- Core framework module: `Conveyor`, loaders, carts, bins, consumers, utility conveyors, Java HTTP client, and JDBC helper utilities.
- Current repository practice treats this as the main development unit and the base dependency for the rest of the repository.

## Read First
- `../doc/project-context.md`
- `./doc/project-context.md`
- `../../conveyor.wiki/2.-Loaders.md`
- `../../conveyor.wiki/3.-Consumers.md`

## Local Rules
- Treat `Conveyor`, loader types, `ProductBin`, `ScrapBin`, and consumer interfaces as high-risk public API.
- `conveyor-core` must remain usable on its own. Do not introduce dependencies on downstream sub-projects.
- Keep external dependencies at the bare minimum here. Adding a new dependency in `conveyor-core` requires unusually strong justification because it propagates to every downstream consumer of the framework.
- Do not assume everything under `utils/*` is part of the undisputed framework contract. That package currently also serves as an incubator for use cases and patterns that may later move to separate projects, stay as examples, or be reused mainly by tests and adjacent modules.
- Inspect related tests before changing loader or command behavior.
- Changes under `utils/http` can affect `conveyor-service` documentation and tests; changes under `utils/jdbc` can affect JDBC result consumers and persistence consumers.
- Prefer documenting uncertainty in `conveyor-core/doc/open-questions.md` rather than inferring old intent.

## Validation
- Default: `mvn -pl conveyor-core -am test`
- Core changes should be validated more aggressively than changes in other modules because the rest of the repository is sensitive to them. Prefer broad test coverage and downstream verification over minimal smoke checks.
- If changing the HTTP client: run `ConveyorServiceClientTest` and `ConveyorServiceTestApplicationTest`.
- If changing JDBC result-consumer helpers: also validate the affected persistence/service modules.
