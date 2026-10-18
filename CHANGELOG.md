# Changelog

## v2.3.0 candidate (2026-10-17; local implementation, Hubitat validation pending)
- Group each device once with editable primary/secondary selections and their hour thresholds in one responsive row.
- Default to the first two exposed signals in this order: temperature, illuminance, humidity, ultraviolet index, motion, acceleration, 3-axis, contact, button activity, battery, water leak, smoke, pressure.
- Share a 24-hour default per signal, with per-device/per-signal overrides. New freshness checks are enabled by default; None disables a selected slot. The existing app-wide device-silence threshold remains independent and defaults to 24 hours.
- Replace legacy manual selections and threshold overrides as requested; retain genuine matching event history, any-event migrations, and independent cooldowns.
- Treat pushed, held, released, and double-tapped events as a single Button activity signal. Add subscriptions/history for ultraviolet index, 3-axis, and pressure.
- Keep other previously supported events as any-event liveness evidence. Do not infer a device's actual purpose from its automatic reporting priority.
- This configuration change does not implement the roadmap's remaining health classes or complete its runtime-validation milestones.

## v2.2.1 candidate (2026-10-11; threshold input correction)
- Align primary and secondary decimal-field minimums with Hubitat's 0.01-hour input increments so whole-hour values such as 48 pass browser validation.
- New UI values start at 0.01 hours in hundredth-hour increments. Runtime parsing retains the earlier 0.001-hour minimum for existing saved settings; event monitoring, migrations, and cooldowns are unchanged.
- Add a regression scenario for both field types and representative whole/fractional-hour values.

## v2.2.0 candidate (2026-10-11; deployed, runtime validation pending)
- Add optional per-device primary inactivity thresholds and per-secondary-attribute freshness thresholds; all new freshness checks default to disabled.
- Distinguish device silence from stale primary functions and attributes while other supported events remain recent; prioritize device-silence alerts.
- Retain bounded per-attribute event history and stable observation periods for missing history without inventing event timestamps.
- Add independent 24-hour freshness cooldowns, aggregate newly due warnings per device, and preserve device-silence escalation independently.
- Reconcile configuration changes, reject invalid/future timestamps, and keep event history monotonic; preserve legacy any-event migrations.
- Add a local behavioral harness and document deployment, Hubitat validation, and rollback requirements.
- Use strict timestamp syntax and non-lenient parsing without `java.text.ParsePosition`, which Hubitat's compiler prohibits. Hubitat compilation and exact-source readback passed after this compatibility repair; all 21 local scenarios still pass.

## v2.1.0 (2026-10-03)
- Add optional per-device selection of one or more primary-function attributes from the functional attributes each device exposes.
- Track the latest configured primary event separately in `lastPrimaryEvent` while retaining `lastAnyEvent` as the alerting signal.
- Preserve primary history only while the validated selection is unchanged; reseed changed selections from matching cached state without fabricating a new event.
- Leave upgraded and newly selected devices unconfigured by default, warn users, and continue existing any-event monitoring without inferred primary functions.

## v2.0.2 (2026-06-29)
- Narrow device selection to hardware devices that expose Hubitat's `battery` capability.
- Require monitored devices to pass both hardware-device filtering and battery-capability filtering at runtime.
- Preserve migration fallback for older selected temperature devices, but skip migrated devices that do not expose `battery`.

## v2.0.1 (2026-06-29)
- Limit monitored devices to real hardware devices.
- Skip virtual devices and custom devices during subscription, event handling, and scheduled checks.
- Update device-selection wording to clarify that v2.0 monitoring is hardware-device focused.

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
