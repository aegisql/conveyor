# Repository Instructions

## Scope
- This repository contains multiple Maven sub-projects with different responsibilities: framework core, parallel execution, configuration, persistence, and a Spring Boot service.
- Use the nearest `AGENTS.md` for module-specific guidance. When instructions conflict, the more local file wins.
- Shared repository documentation lives under `./doc`.
- Module-specific docs live under each module's `doc` directory when present.
- The project wiki at `../conveyor.wiki` is an important source of maintained behavior notes.

## Source Of Truth
When sources disagree, use this order:
1. Explicit current architecture/design docs in the wiki or `./doc` that appear maintained.
2. Passing tests that define observable behavior.
3. Production code and public interfaces.
4. Older narrative docs and comments.

Do not assume older wiki or README text is still correct if tests and code disagree.

## Working Rules
- Treat the current system behavior as the subject to document or change. This project predates AI-assisted development.
- Keep changes narrow. Avoid cross-module churn unless the contract really spans modules.
- Treat `conveyor-core` as the primary development unit and dependency floor for the repository. Other sub-projects build on it; it must not depend on downstream sub-projects.
- Inspect related tests before changing behavior.
- Document ambiguity in `open-questions.md` rather than guessing intent.
- Do not rename repository entities without prior approval. This includes files, directories, modules, packages, public classes, endpoints, configuration keys, and documented paths such as `./doc`.
- Preserve module-local generated test artifact conventions unless you are intentionally updating the owning module's build and docs.

## Validation Expectations
- Run the narrowest relevant module tests before completing code changes.
- If you touch shared APIs in `conveyor-core`, validate downstream modules that depend on them.
- Core changes require a higher bar than other modules: broad regression coverage, careful dependency review, and explicit verification of downstream impact.
- If you touch persistence contracts, validate both persistence modules and any configurator/service code that depends on them.
- For documentation-only changes, do a repository scan to catch stale paths, versions, or contradictions.

## Build Notes
- Root versioning uses both the project version and the root `project.version` property (inferred from `pom.xml` and `scripts/release.sh`).
- Release automation currently lives in `./scripts/release.sh`; older Maven release-plugin instructions still exist in the wiki.
