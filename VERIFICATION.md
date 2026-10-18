# Verification Record

## Current v2.3.0 Candidate — 2026-10-17

### Approved Scope

The maintainer approved a simpler configuration using an ordered reporting-signal ranking, editable primary/secondary selections and thresholds, 24-hour defaults, and replacement of previous configuration. Button activity is immediately before battery; water leak, smoke, and pressure follow battery. Decision D-003 supersedes the old manual-selection/opt-in defaults.

The independent all-event silence threshold remains at its existing setting/default of 24 hours pending the maintainer's testing. This version does not derive that threshold from signal overrides or introduce device-class profiles.

### Exact Artifacts and Environment

- Baseline Git revision: 8ae9ece1fd574477827d91112933db779819b4c2.
- Source: dead_battery_watchdog_hubitat_app.groovy, APP_VERSION 2.3.0, APP_UPDATED 2026-10-17.
- Source SHA-256: da107ed68af0c7a2110e0164429f21befc7b8a8ca3659765ed842392aafd4802.
- Harness: tests/watchdog_test.groovy.
- Harness SHA-256: 49c6bf7b2e542acd7c99d4dad30b09d4ba9c521feff0058c04bd15e6f6b15e9f.
- Runtime: Java 19.0.2, Groovy 3.0.22, matching Groovy dateutil module. Temporary test jars remain ignored under tmp/.
- Run the actual app source using: java -cp "tmp/groovy-3.0.22.jar;tmp/groovy-dateutil-3.0.22.jar" groovy.ui.GroovyMain tests/watchdog_test.groovy. Use a colon between classpath entries on non-Windows systems.

### Local Results

All 35 behavioral scenarios passed against the source above. The harness uses synthetic devices and captures notifications in memory.

1. Exact priority order and all 13 shared 24-hour defaults.
2. Missing-attribute fallback, one-attribute devices, and no duplicate secondary.
3. Freshness checks enabled by default; Automatic/None and custom selectors.
4. Per-device/per-signal threshold overrides that follow attributes when roles change.
5. Intentional legacy configuration replacement without unrelated timestamp migration.
6. Stable missing-history observation periods and genuine matching history reuse.
7. Independent configuration reconciliation, cooldowns, recovery, recurrence, and silence escalation.
8. Button activity ranking, latest-event aggregation, every exposed button action, and independent recovery in either role.
9. Ultraviolet index, three-axis, and pressure subscription/history support.
10. Continued liveness evidence from other supported events without adding them to ranking.
11. Strict timestamp parsing, malformed/future state rejection, monotonic event handling, and simulated serialized dates.
12. Cached battery percentage is not proof of life; optional driver reads remain defensive.
13. Device-wide 24-hour silence threshold remains independent of per-signal overrides.
14. Existing notification gates, hardware filtering, legacy device selection, scheduling, and timezone/replacement formatting.
15. One configuration group per device, distinct controls for multiple devices, 4/2/4/2 layout widths, and whole-hour input step alignment including 48.

Complete-change review and static checks cover relative documentation links, version/date consistency, supported setting names, superseded decisions, source boundaries, and accidental private/credential values. Input-definition tests are not live browser layout/save validation.

### Readiness and Remaining Evidence

Local implementation/review is complete with no unresolved local code blocker identified. Hubitat compilation, UI rendering and saving, installed-instance activation, real event/state behavior, and notification behavior remain unverified for this version and block release/roadmap completion.

The new default-enabled checks and reset configuration are intentional. Genuine event history already older than 24 hours may generate a warning on the next evaluation after deployment; missing history receives a stable observation period. Quiet contacts, buttons, water/smoke sensors, and stable measurements can legitimately need custom thresholds or None.

The app still preserves eligibility filtering, known event histories, legacy any-event migrations, notification destinations, and the existing global silence threshold. Historical D-001/D-002 behavior is recorded below and in the changelog; it is not the current configuration contract.

### Deployment and Rollback Proposal

No v2.3.0 code, settings, device commands, or test notifications have been sent to Hubitat. Last verified deployment evidence remains v2.2.1, recorded below.

After separate deployment authorization:
1. Re-read the live Dead Battery Watchdog code record and its revision. Compare against the expected baseline and preserve the exact live source for rollback; investigate unexpected changes.
2. Identify affected installed instances and retain their configuration evidence before activation.
3. Push the reviewed source with the fresh revision, confirm Hubitat compilation, and verify exact-source readback.
4. With activation/configuration authorization, open/save the intended instance and verify one device group, both default attributes, 24-hour values, customization/None, duplicate handling after primary changes, and saving 48 hours. Review already-overdue devices before enabling notification delivery.
5. Validate actual primary/secondary and Button activity events, independent cooldowns, unknown-history periods, new attributes, eligibility, scheduling, and real persistence. Real test notifications require authorization.
6. Record observed results; only then consider roadmap completion and separately authorized publication.

Rollback reinstalls the preserved prior source using a fresh code revision and re-saves the affected instance. Old configuration keys are retained, so v2.2.1 can use its previous manual selections/thresholds; new keys are ignored by the old source. This does not restore the entire previous persistent state or observation/cooldown history. Assess actual state before rollback.

### Responsibility Handoff

The implementing agent completed the approved local source changes, affected canonical documentation, new D-003 decision, migrated behavioral harness, and local review. No Git commit, push, publication, Hubitat deployment, or external notification was performed for this version. The maintainer is the next owner for reviewing the candidate and authorizing deployment/activation or further changes.

---

# Historical v2.2.1 Candidate Verification and Deployment Record

## Scope and Status

On 2026-10-11 the maintainer approved local implementation, documentation, and review of the v2.2 plan. The candidate separates device silence from optionally monitored primary/secondary freshness, preserves compatibility, and adds independent cooldowns and stable observation periods.

Local implementation/review and authorized code deployment are complete. On 2026-10-11 the maintainer separately approved deployment. Hubitat compiled the v2.2.0 candidate successfully, updated the existing code record from revision 30 to 31, and returned source exactly matching the local artifact. UI/runtime validation, installed-instance activation, publication, and real test notifications have not been performed. The v2.1 and v2.2 roadmap stages remain pending runtime evidence.

## Tested Artifacts

- Baseline Git revision: `9f892fc4631e5232a04237a082f3b6a8800d3193`.
- Candidate source: `dead_battery_watchdog_hubitat_app.groovy`, version `2.2.1`, update date `2026-10-11`.
- Source SHA-256: `896c5d89466c5edf6c487a609e91d9dbea784cc615534275436d0c570d96ed22`.
- Harness: `tests/watchdog_test.groovy`.
- Harness SHA-256: `efbffb3aff90792c26064d2d72522303694995662e787bc7196e6c34458304ac`.
- Runtime: Java 19.0.2 and Groovy 3.0.22, with the matching Groovy dateutil module. Temporary test libraries are ignored under `tmp/`; they are not product dependencies.

Run from the repository root with those two Maven Central artifacts available under `tmp/`:

```powershell
java -cp "tmp/groovy-3.0.22.jar;tmp/groovy-dateutil-3.0.22.jar" groovy.ui.GroovyMain tests/watchdog_test.groovy
```

On non-Windows systems, use `:` rather than `;` between classpath entries. The harness parses and executes the actual release source with simulated Hubitat bindings. Device names and identifiers are synthetic, and notification calls are intercepted in memory.

## Results

All **22 behavioral scenarios passed**:

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
22. Primary and secondary field ranges align with 0.01-hour steps and accept 48 hours and representative fractional values. This models browser step arithmetic using the emitted input options; it does not claim a live browser save was performed.

Static review covers the complete source/documentation change, relative Markdown links, version/date/state terminology, and self-contained source boundaries. The authorized app-code update was performed; no real device command or test notification was sent.

## Deployment Evidence

- Fresh hub reads confirmed the live v2.1.0 source still matched the reviewed baseline before deployment.
- Preserved the exact pre-deployment source locally as `tmp/hubitat-870-before-v2.2-revision-30.groovy`; a raw-text comparison passed. Rollback source SHA-256: `201ab3f2dd0d82875556261c189c4f04a95c4f3bcb6cc482890bd567cad866b9`.
- The first compile rejected `java.text.ParsePosition`. A fresh read confirmed that the failed compile left revision 30 and its source unchanged.
- Replaced that prohibited helper with complete timestamp-syntax validation and non-lenient `SimpleDateFormat.parse(String)`. Reran all 21 scenarios successfully against the repaired source.
- Retried using the freshly read revision. Hubitat compiled successfully and returned revision 31. The following code read exactly matched the repaired local source.
- Read-only installed-app inventory identified one matching, enabled Dead Battery Watchdog instance. No instance configuration was changed or saved through its UI.

### v2.2.1 Threshold Input Correction

The maintainer reported that entering 48 hours failed browser validation, with neighboring valid values 47.991 and 48.001. These values demonstrate a 0.01 step anchored at the field's 0.001 minimum. Both primary and secondary field ranges now start at 0.01; whole hours and hundredth-hour values align with that step. Runtime parsing remains unchanged for saved settings.

Preserved the exact deployed v2.2.0 source as `tmp/hubitat-870-before-v2.2.1-revision-31.groovy`. All 22 local scenarios passed, Hubitat compiled the correction successfully, and the code record advanced from revision 31 to 32. Exact-source readback passed. The maintainer should refresh the configuration page and retry saving 48 hours; no settings or notification delivery were changed by the agent.

## Review Findings and Limits

- **Blocking for roadmap completion/release:** Configuration rendering, real state serialization, event delivery, scheduling, and notification behavior require authorized runtime evidence. Hubitat compilation now passes, but simulated primary-tracking checks do not complete the outstanding v2.1 Hubitat validation.
- **Local review:** No unresolved blocking code finding identified. Future stages remain explicitly planned; no health classes, severity scheme, confidence scores, battery-age calculations, or manual-test workflows were added.
- **Operational limitation:** Deploying this existing code record affects every installed instance using it. A successful code save alone does not prove that each instance has resubscribed or activated new configuration.
- **Configuration behavior:** Changed checks may warn immediately if genuine known event history is already overdue. Checks with no valid history get a new observation period. Invalid optional thresholds disable their checks; an invalid any-event threshold uses the existing 24-hour default.
- **Notification volume:** Cooldowns are independent. A newly overdue issue or recurrence after a genuine recovery can warn within 24 hours of a different issue. Users should choose thresholds appropriate to quiet contacts and buttons.

## Hubitat Deployment and Remaining Validation

Code deployment steps 1-3 are complete. Steps 4-6 remain pending:

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

The maintainer approved deployment, and the agent completed the authorized code update, compatibility repair, local retest, and exact-source readback. The maintainer is the next owner for installed-instance activation/configuration and runtime validation, including any real test notification authorization. UI configuration/manual event validation needs an available interface or maintainer participation. Publication remains separately authorized.
