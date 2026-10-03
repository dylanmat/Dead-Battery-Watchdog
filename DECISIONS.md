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
