# Changelog

## Template guidance

Record notable reviewed changes under Unreleased. Create a versioned entry only when an actual release is prepared, using its real version and date. Adopting projects preserve their own release history.

Use the release-version format defined in [STANDARDS.md](STANDARDS.md). A released entry uses a heading such as `## [v0.0.0] - YYYY-MM-DD`, with the actual release version and date substituted. This example is not a release record.

## [Unreleased]

### Changed

- Standardized release labels and Git tags on `vMAJOR.MINOR.PATCH` formatting, such as `v0.0.0`.
- Reframed the framework as documentation-only and language-neutral, with an adoption checklist and central document ownership table.
- Distinguished template guidance, unresolved project fields, baseline rules, and adopted policy approval.
- Defined scoped authorization, sequential agent responsibilities, recorded handoffs, and blocking/nonblocking review outcomes.
- Moved documentation updates into the reviewed change and separated readiness from authorization to release.
- Expanded architecture, AI security, evaluation evidence, and operational completion criteria.
- Added decision ownership and approval evidence; replaced relative roadmap windows with explicit dates or Unscheduled.

### Added

- Local environment-file ignore rules, retaining a placeholder-only example-file exception.
- ADR-001 documenting the approved framework direction.

### Removed

- Executable starter dependencies, setup/test scripts, assertion-only smoke test, and placeholder code directories.
- Language and UI toolkit requirements, starter-specific automation milestones, and the fictitious dated release entry.
