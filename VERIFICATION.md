# v2.2.0 Candidate Verification and Deployment Proposal

## Scope and Status

On 2026-10-11 the maintainer approved local implementation, documentation, and review of the v2.2 plan. The candidate separates device silence from optionally monitored primary/secondary freshness, preserves compatibility, and adds independent cooldowns and stable observation periods.

Local implementation and review are complete. Hubitat compilation, UI/runtime validation, deployment, release publication, and real test notifications have not been performed. A read-only recheck confirmed the hub still holds the original v2.1.0 source. The v2.1 and v2.2 roadmap stages remain pending Hubitat evidence.

## Tested Artifacts

- Baseline Git revision: `9f892fc4631e5232a04237a082f3b6a8800d3193`.
- Candidate source: `dead_battery_watchdog_hubitat_app.groovy`, version `2.2.0`, update date `2026-10-11`.
- Source SHA-256: `9a40ab5c2e9f0f63540007dbbce9f7b703042247763727ef4cbd04b818e06fbe`.
- Harness: `tests/watchdog_test.groovy`.
- Harness SHA-256: `df1fc242c8c8552fd76eeef82fd78b5c495892ce4b315056ddf0ad05387278a3`.
- Runtime: Java 19.0.2 and Groovy 3.0.22, with the matching Groovy dateutil module. Temporary test libraries are ignored under `tmp/`; they are not product dependencies.

Run from the repository root with those two Maven Central artifacts available under `tmp/`:

```powershell
java -cp "tmp/groovy-3.0.22.jar;tmp/groovy-dateutil-3.0.22.jar" groovy.ui.GroovyMain tests/watchdog_test.groovy
```

On non-Windows systems, use `:` rather than `;` between classpath entries. The harness parses and executes the actual release source with simulated Hubitat bindings. Device names and identifiers are synthetic, and notification calls are intercepted in memory.

## Results

All **21 behavioral scenarios passed**:

1. Blank, invalid, bounded, and fractional thresholds.
2. Strict persisted timestamp parsing and invalid/future-date rejection.
3. Legacy migration without invented primary history.
4. Cached battery percentage without a valid event date does not establish reporting.
5. Recent contact activity with stale temperature/battery produces attribute issues.
6. Any selected primary attribute refreshes the aggregate primary clock.
7. Primary and secondary staleness can coexist while supported reporting continues.
8. Missing history uses stable observation periods without invented event dates.
9. Unchanged configuration preserves cooldowns; changed/removed checks reconcile independently.
10. Changed primary selections use only matching known attribute events.
11. Malformed state and future history are discarded safely.
12. Simulated serialized dates preserve history, observation starts, and cooldowns.
13. Unavailable optional states/values do not prevent evaluation.
14. Inactivity and 24-hour cooldown boundaries.
15. An overdue advancing event does not reset a device-silence cooldown.
16. Event timestamps remain monotonic; unsupported, unselected, malformed, and future events are ignored.
17. Scheduled warnings combine issues, throttle independently, and escalate to device silence.
18. Recovery clears only its corresponding issue cooldown; recurrence can warn again.
19. Notification settings, missing destination, eligibility filtering, and legacy device-selection settings.
20. Lifecycle resubscription, all three schedules, battery-replacement validation, and location timezone formatting.
21. Configuration exposes opt-in controls and excludes primary/unsupported secondary attributes.

Static review covers the complete source/documentation change, relative Markdown links, version/date/state terminology, and self-contained source boundaries. No real hub event, device command, notification, or code update was sent.

## Review Findings and Limits

- **Blocking for roadmap completion/release:** Hubitat compiler compatibility, configuration rendering, real state serialization, event delivery, scheduling, and notification behavior require authorized runtime evidence. Simulated primary-tracking checks do not complete the outstanding v2.1 Hubitat validation.
- **Local review:** No unresolved blocking code finding identified. Future stages remain explicitly planned; no health classes, severity scheme, confidence scores, battery-age calculations, or manual-test workflows were added.
- **Operational limitation:** Deploying this existing code record affects every installed instance using it. A successful code save alone does not prove that each instance has resubscribed or activated new configuration.
- **Configuration behavior:** Changed checks may warn immediately if genuine known event history is already overdue. Checks with no valid history get a new observation period. Invalid optional thresholds disable their checks; an invalid any-event threshold uses the existing 24-hour default.
- **Notification volume:** Cooldowns are independent. A newly overdue issue or recurrence after a genuine recovery can warn within 24 hours of a different issue. Users should choose thresholds appropriate to quiet contacts and buttons.

## Proposed Hubitat Deployment and Validation

This procedure is proposed, not executed:

1. With deployment authorization, re-read the existing Dead Battery Watchdog code record and its latest revision. Compare it against the reviewed baseline; investigate concurrent changes before replacing anything.
2. Preserve that exact live source as a rollback copy and record its revision/hash. Identify all installed instances using the code and retain their existing settings before runtime validation.
3. Push the reviewed candidate using the revision from the most recent read. Confirm successful Hubitat compilation, then re-read the saved code and compare its source with the candidate.
4. Open and save each intended instance through Hubitat to activate/resubscribe. Initially keep new freshness checks disabled; confirm existing any-event monitoring and persisted primary selections remain intact. This must be explicitly covered by runtime-validation authorization; the connector's current code tools do not by themselves provide instance configuration editing.
5. Enable suitable test thresholds only on an approved test instance. Validate primary selection changes and aggregate tracking, secondary freshness, missing history, independent cooldowns, escalation, recovery, and all scheduling/notification configurations using actual event evidence. Real test notifications require separate authorization. Production scheduled checks may naturally notify under existing settings once code is deployed.
6. Record source revision, setup, expected/observed results, redacted logs, and limitations. Mark v2.1/v2.2 complete only when their evidence is sufficient. Publication remains separately authorized.

## Rollback Proposal

Reinstall the preserved live v2.1.0 source using a fresh code-record revision and save the affected instances again. Source inspection shows v2.1 retains the legacy any-event, primary, temperature, and battery fields and ignores/reconstructs the new v2.2 fields. It can reset missing-history observation behavior and drops freshness checks; it is not a restoration of the previous full app state. Review actual persisted state/settings before rollback and verify restored scheduling/subscriptions. Do not promise state restoration from a source-only rollback.

## Responsibility Handoff

The implementing agent performed planning follow-through, implementation, documentation, local behavioral verification, and complete-change review. Changed artifacts are the Groovy release source, affected canonical documentation, this evidence/proposal, the behavioral harness, and the temporary-runtime ignore rule. The source remains uncommitted and unpublished.

The maintainer is the next owner for candidate review and deployment/runtime-validation authorization. The agent can then perform the authorized code update and readback; UI configuration/manual event validation needs an available interface or maintainer participation. Pending authorization covers deployment, affected-instance activation/configuration, real test notifications, and eventual publication as applicable.
