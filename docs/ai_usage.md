# AI usage note — complete before submission

AI assistance was used during planning, implementation and debugging of the synthetic invoice automation project. Assistance included project structure, generator and rule-engine design, Spring Boot/JPA wiring, API scaffolding, validation checks, documentation and test scaffolding.

The project owner must personally review and understand every file, run the Maven tests and application locally, inspect the generated data and SQL seed, verify the public repository contains no secrets, and revise this note to accurately describe what was accepted, modified or rejected. Do not claim a test passed unless it was actually run and its result reviewed.

## Human review checklist

- [ ] Review the full code and be able to explain the generator and matching rules.
- [ ] Run `./mvnw test` or `mvnw.cmd test` and record the actual result.
- [ ] Run the API pipeline and inspect the validation summary.
- [ ] Review generated CSV/JSON/SQL and independently verify the fixed-seed checksum.
- [ ] Remove credentials and personal/sensitive data from Git history and current files.
- [ ] Complete the prompt log with the full prompts sent to all AI tools, as required by the assessment.
