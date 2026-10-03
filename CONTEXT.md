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
- **Primary function:** one or more user-selected functional attributes representing a device's main purpose. v2.1 tracks their latest event separately from any-event liveness.
- **Attribute stale:** a planned classification for an attribute that stopped updating while other useful events show that the device remains active.
- **Dead threshold:** the allowed period without qualifying activity before the app raises an alert.

## Current State

Version 2.1.0 subscribes to supported attributes on selected battery-capable hardware devices. It records the latest parsed event and the latest event from each device's optional user-selected primary attributes. It checks devices every 15, 30, or 60 minutes and alerts after the configurable any-event inactivity threshold. Alerts include event, temperature, battery-level, and battery-replacement context where available and are throttled to once per device every 24 hours.

Primary tracking does not yet affect alerts. The app does not classify stale primary functions or attributes, assign health classes or confidence levels, or manage manual-test workflows. Those capabilities remain planned in `ROADMAP.md`.

## Success and Invariants

The app succeeds when it alerts on genuine event silence without treating a stale battery percentage as proof of life, avoids repeated notification floods, preserves existing user state across upgrades, and remains simple to install as one Groovy source file.

Changes must preserve:

- Defensive filtering of virtual, custom, non-hardware, and non-battery devices.
- Existing state migrations, especially `lastChange` to `lastReport` and v1 `lastReport` to v2 `lastAnyEvent`.
- User-selected primary attributes and their event timestamps without inferring a device's purpose or migrating secondary timestamps into primary state.
- Hubitat-location timestamp formatting and validation of `lastBattery` seconds or milliseconds.
- The distinction between released behavior in the source and future behavior in the roadmap.
- A self-contained release artifact with no external runtime dependency.
