# Project Context

## Purpose and Users

Dead Battery Watchdog helps Hubitat users identify battery-powered hardware devices that may have stopped reporting before the device is needed. It is intended for people who operate a Hubitat hub and want passive monitoring of selected Zigbee or other battery-capable devices.

The project maintainer is accountable for product direction, technical design, security, quality, workflow, and releases. The installed app is operated by each Hubitat user in that user's own hub environment.

## Runtime and Repository

- The released product is the single self-contained `dead_battery_watchdog_hubitat_app.groovy` file.
- Users install the source in Hubitat Apps Code and configure an app instance through Hubitat.
- Hubitat provides device selection, event subscriptions, scheduling, persistent app state, logging, and optional notification-device delivery.
- The app has no external library, hosted service, model, retrieval system, queue, database, or separate deployment component.
- Project documentation, release history, and future work are maintained in this repository.

## Constraints and Assumptions

- Hubitat's Groovy app runtime and device APIs are the supported execution environment.
- Selected devices must be real hardware devices exposing the Hubitat `battery` capability. Runtime filtering remains necessary because migrated settings or unusual drivers may not meet those conditions.
- Sleepy devices cannot be proven alive by polling a cached battery value. Parsed supported events are the current liveness evidence.
- Drivers differ in their attributes and metadata, so access to optional values such as `temperature` and `lastBattery` must remain defensive.
- Hubitat current state may be historical hub state rather than evidence that a device answered a live request.
- The repository has no automated Hubitat integration-test environment; runtime behavior requires proportionate manual validation on a Hubitat hub.

## Domain Vocabulary

- **Battery level (`battery`):** the device's reported charge percentage. It is supporting context and may be stale.
- **Battery replacement (`lastBattery`):** an optional Unix timestamp recording when a battery was replaced. Legacy persisted values must be validated before being treated as timestamps.
- **Any-event liveness (`lastAnyEvent`):** the latest supported parsed event from a monitored device and the released app's primary liveness signal.
- **Primary/secondary reporting signal:** the first/second available attribute in the approved ranking, or an explicit override. This expresses reporting preference rather than actual device purpose. Button activity groups the device's exposed button events into one signal.
- **Attribute stale:** an enabled signal whose event history exceeds its freshness threshold while other useful events show recent reporting.
- **Dead threshold:** the allowed period without qualifying activity before the app raises an alert.

## Current State

The local v2.3.0 candidate groups each selected battery-capable hardware device once, with editable primary/secondary reporting signals and thresholds. It applies the maintainer's ranking and a shared 24-hour default per signal, replacing previous manual configuration as requested. Both slots default to enabled when available; None disables a slot. New per-device/per-signal hour overrides follow the attribute across role changes. Button activity groups pushed, held, released, and doubleTapped. Ultraviolet index, three-axis, and pressure join the supported event list.

Checks still run every 15, 30, or 60 minutes. Device-silence alerts retain the independent any-event threshold/default of 24 hours, which is not derived from the signal thresholds. Each freshness issue has an independent 24-hour cooldown, and newly due issues combine per device. Bounded event histories and stable observation periods remain intact; no timestamps are invented. Quiet devices may require custom thresholds despite the universal 24-hour defaults.

The last verified Hubitat deployment was v2.2.1 on 2026-10-11. The v2.3.0 candidate is local only and awaits separate deployment authorization and Hubitat compiler/UI/runtime validation. Outstanding v2.1/v2.2 evidence and the roadmap's health classes, confidence labels, and manual-test workflows remain pending.

## Success and Invariants

The app succeeds when it alerts on genuine event silence without treating a stale battery percentage as proof of life, avoids repeated notification floods, preserves existing user state across upgrades, and remains simple to install as one Groovy source file.

Changes must preserve:

- Defensive filtering of virtual, custom, non-hardware, and non-battery devices.
- Existing state migrations, especially `lastChange` to `lastReport` and v1 `lastReport` to v2 `lastAnyEvent`.
- Approved ranked reporting defaults and validated overrides without inferring actual device purpose. Reset prior configuration under D-003 while preserving genuine matching history; never migrate unrelated/legacy timestamps into primary state.
- Hubitat-location timestamp formatting and validation of `lastBattery` seconds or milliseconds.
- The distinction between released behavior in the source and future behavior in the roadmap.
- A self-contained release artifact with no external runtime dependency.
