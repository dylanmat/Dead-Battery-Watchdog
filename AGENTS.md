# Agent Context

## Project

Dead Battery Watchdog is a single-file Hubitat Groovy app plus documentation. It monitors selected real hardware devices and alerts when a device stops reporting supported Hubitat events for a configurable number of hours.

## Important Files

- `dead_battery_watchdog_hubitat_app.groovy`: Hubitat app source. This is the release artifact users paste into Hubitat Apps Code.
- `README.md`: Installation, usage, configuration, and links to release history.
- `CHANGELOG.md`: Release history. Starting with v2, maintain release notes here instead of embedding the changelog in `README.md`.
- `ROADMAP.md`: Planned v2 roadmap for event-based Zigbee battery-device health monitoring.

## Current Behavior

- App version is `2.0.1`.
- The app subscribes to supported event attributes for selected real hardware devices and records the most recent parsed event timestamp in `lastAnyEvent`.
- Virtual devices and custom devices are skipped during subscription, event handling, and scheduled checks.
- `checkDevices()` runs on a scheduled interval of 15, 30, or 60 minutes.
- A device alerts when elapsed time since the last supported device event exceeds `inactiveThreshold`.
- Repeat alerts are throttled per device to once every 24 hours using `status.lastAlert`.
- Alerts can be sent through an optional Hubitat notification device when `sendPush` is enabled. Without a selected notification device, the app logs a warning.

## Device State

`state.deviceStatus` is keyed by Hubitat device id string.

Current fields:

- `lastTemp`: last reported temperature value.
- `lastReport`: timestamp of the latest temperature event or initial current state.
- `lastAnyEvent`: timestamp of the latest supported event from the device.
- `lastEventName`: name of the latest supported event.
- `lastEventValue`: value of the latest supported event.
- `lastEventDisplayName`: display name from the latest supported event.
- `batteryLevel`: value from the device `battery` attribute, or `N/A`.
- `lastBattery`: value from the device `lastBattery` attribute. In v1.3.0 and later this means the Unix timestamp for the last battery replacement.
- `lastAlert`: timestamp of the last dead battery alert.

Older versions stored battery percentage in `lastBattery`. Do not use persisted `lastBattery` as a fallback for replacement time unless it is explicitly validated as a Unix timestamp.

## Timestamp Formatting

- User-facing and debug timestamps should use `formatLogTimestamp(Date)`.
- `formatLogTimestamp` formats in the Hubitat location timezone when available.
- `lastBattery` values should be formatted through `formatUnixTimestamp(value)`, which accepts Unix seconds or milliseconds and returns `N/A` for missing or invalid values.

## Compatibility Notes

- Keep the app self-contained in Groovy. Hubitat users paste this file directly into Apps Code.
- Avoid external dependencies.
- Preserve existing state migrations where possible, especially `lastChange` to `lastReport` and v1 `lastReport` to v2 `lastAnyEvent`.
- Use defensive helpers for device attributes because not every selected monitored hardware device exposes `temperature`, `battery`, or `lastBattery`.

## Planned v2 Direction

v2.0.1 is the current released behavior and uses supported Hubitat events from real hardware devices as the liveness signal. Future v2 roadmap stages continue shifting the project toward broader Zigbee device health monitoring:

- Track primary-function events separately from secondary attributes.
- Classify devices as dead or offline only when useful event streams go silent.
- Classify temperature, battery, or other attributes as stale when the device is still active through other events.
- Add per-device health classes, configurable thresholds, clearer alert severity, confidence levels, and manual-test workflows.
- Keep battery percentage as supporting evidence only; do not treat it as proof that a sleepy Zigbee device is alive.

When implementing v2 stages, follow `ROADMAP.md` as the canonical planning document and avoid wording that implies planned behavior already exists in the released app. When a planned roadmap item is completed, mark its heading complete with the completion date, using the format `### vX.Y - Title - Complete YYYY-MM-DD`.

## Release Documentation

When changing app behavior:

- Update `APP_VERSION` and `APP_UPDATED`.
- Add a `CHANGELOG.md` entry for the release. Do not add detailed changelog entries to `README.md`; keep README pointing to `CHANGELOG.md`.
- If the change completes a planned item in `ROADMAP.md`, mark that roadmap heading complete with the date stamp.
- Keep terminology clear: `battery` is battery level, `lastBattery` is battery replacement timestamp.
