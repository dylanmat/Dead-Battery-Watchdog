# Project Decisions

This document records durable technical or policy choices whose rationale would otherwise be lost. Routine implementation details and release notes belong in source comments or `CHANGELOG.md` instead.

## Decision Record Format

Each future record should include:

- **ID:** a unique `D-NNN` identifier.
- **Date:** the acceptance date in `YYYY-MM-DD` format.
- **Status:** Proposed, Accepted, Superseded, or Deprecated.
- **Owner:** the accountable project maintainer or delegated owner.
- **Context:** the problem, constraints, and affected scope.
- **Decision:** the chosen behavior and its boundaries.
- **Alternatives:** viable options considered and why they were not selected.
- **Consequences:** expected benefits, costs, risks, and compatibility effects.
- **Evidence:** supporting requirements, review, experiment, or verification.
- **Approval:** who accepted the decision, when, and for what scope.
- **Related decisions:** records superseded by or dependent on this choice, or `None`.

## Recorded Decisions

### D-002 - Explicit Opt-In Freshness Checks and Independent Cooldowns

- **ID:** D-002
- **Date:** 2026-10-11
- **Status:** Accepted
- **Owner:** Project maintainer
- **Context:** Recent secondary events can coexist with a stale primary function or attribute. Mixed device purposes make one inferred freshness threshold unsafe, and one shared cooldown would suppress distinct problems or escalation.
- **Decision:** Add optional per-device primary inactivity thresholds and per-selected-secondary-attribute freshness thresholds. New checks default to disabled. Any selected primary event refreshes the aggregate primary clock; secondary checks exclude primary attributes and evaluate individually. Prioritize device-silence alerts, deliver freshness warnings only while supported activity is recent, use independent 24-hour issue cooldowns, and combine newly due freshness issues per device per scheduled check. Preserve stable observation periods for missing history without creating event dates. Configuration changes reset check-specific observation/cooldown state while retaining genuine matching event history.
- **Alternatives:** Automatic profiles and thresholds remain v2.3 work. Requiring every primary attribute to report would change v2.1's accepted aggregate semantics. A shared alert cooldown would let a freshness warning suppress a later device-silence alert.
- **Consequences:** Users must choose meaningful thresholds for quiet devices. A source upgrade alone does not enable new warnings. New bounded state supports independent classification, history, and throttling; malformed/future dates and stale cached values cannot establish new activity.
- **Evidence:** Approved v2.2 plan; local behavioral evidence and pending Hubitat checks are recorded in `VERIFICATION.md`.
- **Approval:** Project maintainer approved local implementation, documentation, and review of the v2.2 plan on 2026-10-11. Deployment, publication, and test notification delivery are not covered.
- **Related decisions:** D-001.

### D-001 - Configure Primary Functions Per Device

- **ID:** D-001
- **Date:** 2026-10-03
- **Status:** Accepted
- **Owner:** Project maintainer
- **Context:** Device capabilities reveal which attributes exist but not which ones represent the device's purpose. Automatically treating temperature as primary, for example, would misclassify many multipurpose sensors.
- **Decision:** Let users select zero or more primary attributes independently for each monitored device. Offer supported purpose-bearing attributes exposed by that device; exclude battery, power source, and tamper. Do not infer defaults. An unconfigured device continues any-event monitoring with a visible configuration warning.
- **Alternatives:** Automatic inference was rejected because device purpose cannot be derived reliably from capability metadata. One app-wide selection was rejected because an app instance may monitor mixed device types. Treating every non-battery event as primary was rejected because it would erase the primary/secondary distinction.
- **Consequences:** Configuration is more explicit and accurate but requires user input. Existing installations remain compatible because primary selection is optional and alert behavior continues to use `lastAnyEvent`.
- **Evidence:** v2.1 roadmap requirements, existing mixed-attribute device support, and the approved v2.1 implementation plan.
- **Approval:** Project maintainer approved the decision and v2.1 implementation scope on 2026-10-03.
- **Related decisions:** None.
