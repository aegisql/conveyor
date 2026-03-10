# conveyor-configurator Open Questions

## Property precedence and override rules
- **Observed evidence:** wiki describes file/environment/system layering; tests cover pieces of that behavior.
- **Why ambiguous:** there is no short maintained doc that spells out the full precedence model for current code.
- **Suggested human follow-up:** capture the exact precedence rules in one maintained module doc.

## Scope of GraalJS-backed features
- **Observed evidence:** the module depends on GraalJS and suppresses interpreter warnings during tests.
- **Why ambiguous:** current docs do not clearly describe which configuration features require script execution versus simple parsing.
- **Suggested human follow-up:** document the supported scripting/templating surface explicitly.
