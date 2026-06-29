# Changelog

## v2.0.0 (2026-06-29)
- Track `lastAnyEvent` for each monitored device and use it as the liveness signal for alerts.
- Broaden monitoring from temperature-only devices to selected monitored devices with supported Hubitat event attributes.
- Include last event name, value, and timestamp in alerts while keeping temperature, battery level, and last battery replacement as optional context.
- Preserve existing v1 temperature state by migrating `lastReport` and `lastChange` into the new event-based state when needed.
- Keep battery percentage as supporting context only; polling the current battery value does not prove the device is alive.

## v1.3.0 (2026-06-28)
- Treat `lastBattery` as the Unix timestamp for the last battery replacement.
- Include the last battery replacement date in dead battery alerts using the app's local Hubitat timestamp format.
- Store battery percentage separately from `lastBattery` so older state does not confuse battery level with replacement time.

## v1.2.4 (2026-06-14)
- Track the timestamp of the latest temperature report, even when the reported value is unchanged.
- Alert only when a device stops reporting temperature events, preventing false alarms from stable rounded temperatures.
- Preserve existing device state on update and migrate older `lastChange` timestamps to `lastReport`.

## v1.2.3 (2026-02-13)
- Format initial-state debug timestamps in the same Hubitat-style local format used by unchanged-temperature and alert logs.
- Align `checkDevices()` run-time debug logging with the same local timestamp format for consistency.

## v1.2.2 (2026-02-13)
- Format "Last Change" timestamps in logs and alerts using Hubitat-style local time (`yyyy-MM-dd hh:mm:ss.SSS a`) to avoid UTC/local confusion.
- Added safe timestamp parsing helper methods for persisted state values.

## v1.2.1 (2025-11-01)
- Limit repeat notifications to once every 24 hours per device to prevent alert fatigue.
- Updated documentation with usage guidance and a historical changelog.

## v1.2.0 (2025-10-25)
- Added configurable check intervals and inactivity thresholds.
- Included optional notification device support alongside push notifications.
- Persist the last temperature, change timestamp, and battery level for each device.

## Earlier releases
- Initial release established temperature-based monitoring and push notifications. Historical details prior to v1.2.0 were undocumented.
