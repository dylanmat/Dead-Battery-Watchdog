# Agent Context

## Project

Dead Battery Watchdog is a single-file Hubitat Groovy app plus README documentation. It monitors selected temperature-capable devices and alerts when a device stops reporting temperature events for a configurable number of hours.

## Important Files

- `dead_battery_watchdog_hubitat_app.groovy`: Hubitat app source. This is the release artifact users paste into Hubitat Apps Code.
- `README.md`: Installation, usage, configuration, and changelog.
- `ROADMAP.md`: Planned v2 roadmap for event-based Zigbee battery-device health monitoring.

## Current Behavior

- App version is `1.3.0`.
- The app subscribes to `temperature` events for selected devices and records the most recent report timestamp even when the rounded temperature value is unchanged.
- `checkDevices()` runs on a scheduled interval of 15, 30, or 60 minutes.
- A device alerts when elapsed time since the last temperature report exceeds `inactiveThreshold`.
- Repeat alerts are throttled per device to once every 24 hours using `status.lastAlert`.
- Alerts can be sent through an optional Hubitat notification device when `sendPush` is enabled. Without a selected notification device, the app logs a warning.

## Device State

`state.deviceStatus` is keyed by Hubitat device id string.

Current fields:

- `lastTemp`: last reported temperature value.
- `lastReport`: timestamp of the latest temperature event or initial current state.
- `batteryLevel`: value from the device `battery` attribute, or `N/A`.
- `lastBattery`: value from the device `lastBattery` attribute. In v1.3.0 this means the Unix timestamp for the last battery replacement.
- `lastAlert`: timestamp of the last dead battery alert.

Older versions stored battery percentage in `lastBattery`. Do not use persisted `lastBattery` as a fallback for replacement time unless it is explicitly validated as a Unix timestamp.

## Timestamp Formatting

- User-facing and debug timestamps should use `formatLogTimestamp(Date)`.
- `formatLogTimestamp` formats in the Hubitat location timezone when available.
- `lastBattery` values should be formatted through `formatUnixTimestamp(value)`, which accepts Unix seconds or milliseconds and returns `N/A` for missing or invalid values.

## Compatibility Notes

- Keep the app self-contained in Groovy. Hubitat users paste this file directly into Apps Code.
- Avoid external dependencies.
- Preserve existing state migrations where possible, especially `lastChange` to `lastReport`.
- Use defensive helpers for device attributes because not every selected temperature device exposes `battery` or `lastBattery`.

## Planned v2 Direction

v1.3.0 is the current released behavior and remains temperature-report based. The v2 roadmap shifts the project toward broader Zigbee device health monitoring:

- Track `lastAnyEvent` as the main liveness signal instead of relying only on temperature reports.
- Track primary-function events separately from secondary attributes.
- Classify devices as dead or offline only when useful event streams go silent.
- Classify temperature, battery, or other attributes as stale when the device is still active through other events.
- Add per-device health classes, configurable thresholds, clearer alert severity, confidence levels, and manual-test workflows.
- Keep battery percentage as supporting evidence only; do not treat it as proof that a sleepy Zigbee device is alive.

When implementing v2 stages, follow `ROADMAP.md` as the canonical planning document and avoid wording that implies planned behavior already exists in the released app.

## Release Documentation

When changing app behavior:

- Update `APP_VERSION` and `APP_UPDATED`.
- Add a README changelog entry.
- Keep terminology clear: `battery` is battery level, `lastBattery` is battery replacement timestamp.
