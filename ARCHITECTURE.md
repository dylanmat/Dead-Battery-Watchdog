# Architecture

## System Boundary

Dead Battery Watchdog is one Hubitat Groovy app installed and executed on a user's Hubitat hub. The Groovy source contains configuration, lifecycle hooks, device filtering, event handling, scheduling, state migration, alert evaluation, logging, and notification delivery.

There is no separate server, client, database, model, retrieval layer, background worker, credential store, or third-party runtime dependency.

## Components

- **Preferences:** select battery-capable devices, then show each device in one section with a primary selector/threshold and secondary selector/threshold. Supported layout widths 4/2/4/2 request one responsive row. Automatic chooses the first two exposed signals in the approved priority order; overrides and None are supported. Both selected signals default to enabled at 24 hours. Global any-event inactivity, check interval, logging, and notification settings remain independent.
- **Lifecycle:** `installed()` and `updated()` call `initialize()`; updates first remove existing schedules and subscriptions.
- **Device filtering:** `monitoredDeviceList()` permits selected real hardware devices that expose `battery`. Virtual devices, custom drivers, and devices lacking that attribute are skipped.
- **Subscriptions:** `initialize()` subscribes to each attribute in `MONITORED_ATTRIBUTES` that a monitored device exposes.
- **Event processing:** `deviceEventHandler(evt)` requires a selected eligible device, exposed supported attribute, and valid non-future event date. It updates any-event, per-attribute, primary, and temperature histories monotonically. Recent matching events clear only the corresponding freshness cooldown; device-silence cooldown clears only when activity restores recency.
- **Reconciliation:** `reconciledDeviceStatus()` merges valid stored event dates with matching cached states, retains legacy migrations, prunes attribute history to exposed supported names, and reconciles enabled checks. Unchanged checks retain observation periods and cooldowns; changed or disabled checks reset or remove their check-specific state.
- **Scheduled evaluation:** `evaluateHealth()` classifies the device as reporting, unknown during an initial observation period, or silent after the any-event threshold. Primary and individual secondary freshness issues are evaluated independently. `checkDevices()` gives device silence delivery precedence and permits freshness warning delivery only while supported activity remains recent.
- **Notification:** `deliverWarning()` logs and optionally calls `deviceNotification`. `lastAlert` preserves the device-silence cooldown. `issueAlerts` provides independent 24-hour primary/attribute cooldowns; newly due freshness issues are aggregated into one message per device per scheduled check.
- **Persistence:** Hubitat `state.deviceStatus` stores one status map per device ID string.

## Data Flow

1. The user selects devices and settings through Hubitat.
2. Initialization filters the selection, resolves each device's primary/secondary reporting signals, and subscribes to supported attributes exposed by each eligible device, including ultravioletIndex, threeAxis, and pressure.
3. Hubitat delivers valid events to `deviceEventHandler`, which updates the appropriate clocks and context without allowing older events to regress history.
4. The scheduled check reconciles persisted state and evaluates device liveness independently from enabled freshness checks.
5. Recovery removes only recovered issue cooldowns. Device silence suppresses freshness delivery but does not let a freshness cooldown suppress device-silence escalation. Otherwise, newly due freshness issues are combined into one warning. Delivery follows the user's existing notification settings.

Polling `currentValue` or `currentState` reads Hubitat's saved state and is not proof that a sleepy device is currently reachable. Valid cached event dates may seed matching histories; their age remains unchanged. Missing history remains null, with separate stable observation-start dates used to allow one threshold before reporting no observed event. Malformed, nonpositive, and future dates are rejected as freshness evidence.

## State and Compatibility

`state.deviceStatus` contains `lastTemp`, `lastReport`, `lastAnyEvent`, `lastEventName`, `lastEventValue`, `lastEventDisplayName`, `primaryAttributes`, `lastPrimaryEvent`, `lastPrimaryEventName`, `lastPrimaryEventValue`, `batteryLevel`, `lastBattery`, and `lastAlert`. The app accepts older `temperatureDevices` settings and migrates useful timestamps from `lastChange` and `lastReport` when newer any-event fields are absent.

The v2.2.0 candidate adds:

- `attributeHistory`: latest valid `date` and `value` keyed by exposed supported attribute name.
- `observationStarted`: stable start for devices with no valid any-event history.
- `freshnessMonitors`: enabled `primary` or `attribute:<name>` checks with threshold milliseconds, selection identity, and observation start.
- `issueAlerts`: independent last-warning dates keyed by enabled issue.
- `health`: latest evaluated device classification (`reporting`, `unknown`, or `silence`), overdue issue keys, and evaluation time. This is a scheduled snapshot, not a new device event.

Primary and secondary each represent one reporting signal. A normal signal maps to one real attribute; Button activity maps to the device's exposed pushed/held/released/doubleTapped attributes, and the latest event from any of them refreshes that signal. Secondary choices exclude the effective primary. Ranked defaults identify reporting preferences, not actual device purpose.

SIGNAL_LABELS defines the priority order; SIGNAL_DEFAULT_HOURS defines shared per-signal defaults, currently 24 for all 13 signals. New reportingSignal_<id>_<role> settings select Automatic, None, or an exposed signal. New reportingHours_<id>_<signal> overrides follow the signal if its role changes. Missing/invalid new hours use the signal's default; None disables a slot. Legacy primaryAttributes/secondaryAttributes/primaryThreshold/attributeThreshold settings are intentionally ignored under D-003, applying fresh ranked defaults to existing installations while retaining the old values for rollback.

The independent any-event threshold remains unchanged at its existing setting/default of 24 hours. A primary override of 48 hours does not raise the device-silence threshold. Configuration changes may immediately expose genuinely old cached history; missing history receives a new observation period.

v2.2.1 aligns primary/secondary decimal input ranges to `0.01..1000000`. A `0.001` minimum combined with Hubitat's 0.01 step offset the browser's valid values and rejected whole hours. The runtime's earlier 0.001-hour minimum remains supported for persisted settings; monitoring and state semantics are unchanged.

Primary attributes are stored as a validated snapshot. When the configuration changes, prior aggregate primary-event metadata is discarded and reseeded only from matching cached states or known per-attribute event history belonging to the new selection. Any-event and legacy temperature timestamps are never migrated into primary state.

`lastBattery` is only a battery-replacement timestamp after Unix-value validation. Battery percentage belongs in `batteryLevel`; legacy values must not be silently reinterpreted as replacement dates.

## Trust and Data Boundaries

Device attributes, event names, event values, display names, and driver metadata originate outside the app and may be absent or malformed. Filtering, null checks, date parsing, Unix timestamp validation, and optional-attribute helpers contain that uncertainty.

Data remains within the user's Hubitat environment except when the user configures a notification device whose own driver or service may deliver messages elsewhere. The app does not manage that device's authentication or transport.

## Failure Handling and Operations

- Scheduling failures are caught and logged as errors.
- Unsupported devices and attributes are skipped; debug logging can explain filtered selections.
- Devices with unavailable or disabled reporting slots continue any-event monitoring. Effective overrides are validated against exposed ranked signals; invalid/unavailable or duplicate secondary choices fall back to the next available ranked signal. No primary-purpose inference is performed.
- Events without a device ID are ignored with a warning.
- Missing notification-device configuration produces a warning instead of a delivery attempt.
- Missing optional context is displayed as `N/A` rather than preventing evaluation.
- Reinstalling a previous known-good source version is the rollback mechanism. State compatibility should be reviewed before any rollback across schema changes.

Runtime observability is provided by Hubitat logs and notification outcomes. Manual Hubitat verification should cover installation, update/resubscription, event receipt, scheduled evaluation, filtering, cooldown behavior, and notification configuration for behavior-changing releases.

Local simulated checks and the activation/rollback procedure are recorded in `VERIFICATION.md`. The v2.3.0 candidate is local and has not been compiled/deployed on Hubitat. Successful v2.2.1 compilation/readback is historical evidence only. A source update alone is not evidence that installed instances have resubscribed or adopted new configuration.
