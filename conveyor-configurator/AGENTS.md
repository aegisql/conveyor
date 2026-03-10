# conveyor-configurator Instructions

## Purpose
- Builds named conveyors from properties/YAML configuration, including persistence and parallel compositions.

## Read First
- `../doc/project-context.md`
- `./doc/project-context.md`
- `../../conveyor.wiki/1.---Configuration-Files.md`

## Local Rules
- Tests are the primary source of truth for configuration semantics.
- Do not guess property precedence, templating rules, or persistence wiring.
- Keep test-created persistence artifacts under the configured `test-artifacts` area.
- GraalJS is used here for some configuration features; avoid introducing runtime assumptions that require a full GraalVM JIT unless clearly necessary.

## Validation
- `mvn -pl conveyor-configurator -am test`
- If persistence wiring changes, also run the relevant persistence module tests.
