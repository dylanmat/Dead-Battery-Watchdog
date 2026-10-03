# Architecture

## System Boundary

Dead Battery Watchdog is one Hubitat Groovy app installed and executed on a user's Hubitat hub. The Groovy source contains configuration, lifecycle hooks, device filtering, event handling, scheduling, state migration, alert evaluation, logging, and notification delivery.

There is no separate server, client, database, model, retrieval layer, background worker, credential store, or third-party runtime dependency.

## Components

- **Preferences:** select battery-capable devices, optional per-device primary attributes, inactivity threshold, 15/30/60-minute check interval, debug logging, push-notification enablement, and an optional notification device.
- **Lifecycle:** `installed()` and `updated()` call `initialize()`; updates first remove existing schedules and subscriptions.
- **Device filtering:** `monitoredDeviceList()` permits selected real hardware devices that expose `battery`. Virtual devices, custom drivers, and devices lacking that attribute are skipped.
- **Subscriptions:** `initialize()` subscribes to each attribute in `MONITORED_ATTRIBUTES` that a monitored device exposes.
- **Event processing:** `deviceEventHandler(evt)` validates the source device and records the parsed event as the latest evidence of life. Events matching a device's configured primary attributes also update primary-function state. Temperature events update temperature-specific state.
- **Scheduled evaluation:** `checkDevices()` runs from a Hubitat cron schedule and compares `lastAnyEvent` with the configured inactivity threshold.
- **Notification:** an overdue device produces a Hubitat warning log and, when enabled and configured, a `deviceNotification` call. `lastAlert` enforces a 24-hour per-device cooldown.
- **Persistence:** Hubitat `state.deviceStatus` stores one status map per device ID string.

## Data Flow

1. The user selects devices and settings through Hubitat.
2. Initialization filters the selection, validates each device's optional primary attributes, and subscribes to supported attributes exposed by each eligible device.
3. Hubitat delivers device events to `deviceEventHandler`, which stores the latest event timestamp, name, value, display name, optional primary event, temperature, battery level, and validated replacement-time source value.
4. The scheduled check reconciles persisted state with available current Hubitat state, preserving migration fallbacks where required.
5. If event silence exceeds the threshold and the cooldown allows it, the app formats an alert and writes it to the Hubitat log; it optionally sends the same content through the selected notification device.

Polling `currentValue` or `currentState` reads Hubitat's saved state and is not treated as proof that a sleepy device is currently reachable. Parsed subscribed events update `lastAnyEvent`, which remains the alerting signal in v2.1. Cached `currentState` timestamps may seed known primary-event history but do not represent a newly observed event.

## State and Compatibility

`state.deviceStatus` contains `lastTemp`, `lastReport`, `lastAnyEvent`, `lastEventName`, `lastEventValue`, `lastEventDisplayName`, `primaryAttributes`, `lastPrimaryEvent`, `lastPrimaryEventName`, `lastPrimaryEventValue`, `batteryLevel`, `lastBattery`, and `lastAlert`. The app accepts older `temperatureDevices` settings and migrates useful timestamps from `lastChange` and `lastReport` when newer any-event fields are absent.

Primary attributes are stored as a validated snapshot. When the configuration changes, prior primary-event metadata is discarded and reseeded only from cached states belonging to the new selection. Any-event and legacy temperature timestamps are never migrated into primary state.

`lastBattery` is only a battery-replacement timestamp after Unix-value validation. Battery percentage belongs in `batteryLevel`; legacy values must not be silently reinterpreted as replacement dates.

## Trust and Data Boundaries

Device attributes, event names, event values, display names, and driver metadata originate outside the app and may be absent or malformed. Filtering, null checks, date parsing, Unix timestamp validation, and optional-attribute helpers contain that uncertainty.

Data remains within the user's Hubitat environment except when the user configures a notification device whose own driver or service may deliver messages elsewhere. The app does not manage that device's authentication or transport.

## Failure Handling and Operations

- Scheduling failures are caught and logged as errors.
- Unsupported devices and attributes are skipped; debug logging can explain filtered selections.
- Devices without configured primary attributes continue any-event monitoring and produce a configuration warning rather than receiving inferred defaults.
- Events without a device ID are ignored with a warning.
- Missing notification-device configuration produces a warning instead of a delivery attempt.
- Missing optional context is displayed as `N/A` rather than preventing evaluation.
- Reinstalling a previous known-good source version is the rollback mechanism. State compatibility should be reviewed before any rollback across schema changes.

Runtime observability is provided by Hubitat logs and notification outcomes. Manual Hubitat verification should cover installation, update/resubscription, event receipt, scheduled evaluation, filtering, cooldown behavior, and notification configuration for behavior-changing releases.
