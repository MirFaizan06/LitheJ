## What does this change?

<!-- One or two sentences. -->

## Why?

<!-- The motivating use case, bug, or inconsistency. -->

## Checklist

- [ ] `./mvnw clean verify` passes locally (tests, Checkstyle, SpotBugs, PMD, JaCoCo, Javadoc)
- [ ] New/changed public API has full Javadoc (including null/error behavior and thread safety)
- [ ] Tests cover the relevant edge cases (null, empty, boundary values, error paths)
- [ ] `CHANGELOG.md` updated under `[Unreleased]` if this changes public behavior
- [ ] No new dependency added to a core module without discussion (see CONTRIBUTING.md)
