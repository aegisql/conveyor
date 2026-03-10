# Open Questions

## Version line for "current" docs
- **Observed evidence:** root `pom.xml` is `1.7.4-SNAPSHOT`, while several user-facing docs/wiki pages were normalized to the `1.7.3` release line.
- **Why ambiguous:** it is unclear whether future docs should describe the released line or the current development branch.
- **Suggested human follow-up:** decide whether repo docs should track the branch version or the last published release.

## Release workflow source of truth
- **Observed evidence:** `../conveyor.wiki/Build-and-test-instructions.md` still shows `mvn release:*`, while `./scripts/release.sh` and the root build use versions/deploy/Central Publishing flow.
- **Why ambiguous:** both workflows exist in repository evidence, but they are not equivalent.
- **Suggested human follow-up:** retire one path or explicitly document when each is valid.
