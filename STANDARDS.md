# Project Standards

## Implementation Conventions

- Keep the runtime in `dead_battery_watchdog_hubitat_app.groovy` as a single self-contained Groovy app compatible with Hubitat Apps Code.
- Do not introduce external dependencies, build-time assembly, or a separate service without an accepted project decision and updated installation guidance.
- Preserve Hubitat lifecycle behavior, settings compatibility, and persisted state migrations unless the approved change explicitly replaces them.
- Use defensive helpers for driver properties and optional attributes. Do not assume every battery-capable device exposes temperature or `lastBattery`.
- Key `state.deviceStatus` by device ID string and keep state-field meaning consistent across initialization, event handling, and scheduled checks.
- Use `formatLogTimestamp(Date)` for user-facing and debug dates and `formatUnixTimestamp(value)` for validated battery-replacement timestamps.
- Use `battery` or `batteryLevel` for percentage and `lastBattery` only for replacement time.
- Keep comments and logs concise and avoid claims that a cached battery value proves device liveness.

## Documentation Conventions

- `README.md` describes installation and released user-facing behavior; `CHANGELOG.md` contains release history; `ROADMAP.md` contains planned behavior.
- Do not describe an unfinished roadmap stage as released functionality.
- Update `CONTEXT.md`, `ARCHITECTURE.md`, `SECURITY.md`, `STANDARDS.md`, `DECISIONS.md`, and `AGENTS.md` when their authoritative subject changes.
- Record significant durable tradeoffs in `DECISIONS.md`; do not create a decision record for routine edits.
- Keep relative links valid and terminology, versions, dates, defaults, state fields, and configuration names consistent with the source.

## Verification

There is no repository-hosted automated Hubitat runtime test suite. Verification must be proportionate to the change and must distinguish static evidence from behavior observed on a Hubitat hub.

For every change:

- Inspect the complete diff and confirm it matches the approved scope.
- Search for inconsistent version numbers, dates, setting names, state-field meanings, and released-versus-planned claims.
- Check relative Markdown links and documentation references.
- Confirm the Groovy release artifact remains self-contained and contains no secret or unintended private value.

For app behavior changes, manually exercise relevant scenarios on Hubitat where practical:

- Fresh installation and update/resubscription behavior.
- Real hardware filtering and rejection of virtual, custom, or non-battery devices.
- Supported event handling and updates to `lastAnyEvent` and temperature-specific state.
- Scheduled checks at supported intervals, threshold comparison, and 24-hour alert cooldown.
- Notifications enabled with and without a selected notification device.
- Missing optional attributes, malformed persisted values, timezone formatting, and migration from older state.

Record the tested source revision, setup, scenarios, expected and observed results, limitations, and any relevant Hubitat logs with private data removed. If Hubitat testing is unavailable, state that limitation rather than claiming runtime validation.

## Review Gates

A change is ready only when its acceptance criteria are met, applicable checks pass, affected documentation is current, and no blocking finding remains.

- A **blocking finding** is a behavior defect, compatibility break, security-policy violation, unmet acceptance criterion, released/planned mismatch, or missing evidence required for the approved change.
- A **nonblocking finding** is an improvement that does not prevent the approved behavior or documentation from being correct. Record the rationale and follow-up owner if it is deferred.
- Documentation-only changes require link, consistency, source-alignment, and diff review; they do not require artificial runtime tests.
- Review readiness does not authorize publication or deployment.

## Releases and Rollback

- Use `vMAJOR.MINOR.PATCH` for release labels and Git tags.
- When app behavior changes, update `APP_VERSION`, `APP_UPDATED`, and `CHANGELOG.md` together.
- Mark a roadmap stage complete only after its acceptance evidence exists, using `Complete YYYY-MM-DD` in the stage heading.
- Before release, verify the exact Groovy file users will paste, confirm documentation matches it, and obtain publication authorization.
- Roll back by reinstalling a reviewed earlier source version. Before rollback, assess whether newer persisted state remains compatible with that version.
