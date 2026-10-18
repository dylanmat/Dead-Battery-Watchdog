# Dead Battery Watchdog

Dead Battery Watchdog is a Hubitat app that monitors selected battery-capable hardware devices to catch sensors that have likely stopped reporting. When a monitored battery hardware device stops sending supported Hubitat events, the app sends a notification so you can investigate the battery, range, sleep state, or Zigbee routing before the device is needed.

The app is a single self-contained Groovy file with no external runtime dependencies. Hubitat users paste the source directly into Apps Code.

The repository contains the **v2.3.0 candidate** with simpler ranked device configuration, prepared locally on 2026-10-17. It has not been deployed or validated on Hubitat. The last verified deployment was v2.2.1 on 2026-10-11. Roadmap stages remain pending their runtime evidence.

## Installation

1. In Hubitat, open **Apps Code** and choose **+ New App**.
2. Paste the contents of [`dead_battery_watchdog_hubitat_app.groovy`](dead_battery_watchdog_hubitat_app.groovy) into the editor and save.
3. Click **Apps** -> **+ Add User App** and select **Dead Battery Watchdog**.
4. Configure the app and click **Done** to activate monitoring.

## Basic Usage

1. **Select devices:** Choose real hardware devices exposing the Hubitat battery capability. Virtual devices, custom drivers, and devices without battery are skipped.
2. **Review each device once:** Its group contains Primary attribute / Threshold and Secondary attribute / Threshold. Automatic selects the first two available signals in the table below. Each defaults to 24 hours.
3. **Customize where needed:** Choose another exposed signal, change its hours, or choose None to disable that slot. The secondary cannot duplicate the primary. A device with only one ranked attribute gets one check. Hour overrides follow the device/attribute when it changes roles.
4. **Check global settings:** The separate all-event inactivity threshold still defaults to 24 hours; it is not derived from the two attribute thresholds. Checks run every 15, 30, or 60 minutes.
5. **Configure notifications:** Existing push enablement and the optional notification device apply to both silence alerts and freshness warnings. Each issue has an independent 24-hour cooldown; newly due freshness issues are combined per device per check.
6. **Save with Done:** This activates subscriptions and the schedule. Quiet contacts, buttons, leak/smoke sensors, and stable environmental readings may need a longer threshold or a disabled slot; 24 hours is a configuration default, not a guarantee of daily reporting.

## Automatic Ranking

The first exposed signal becomes primary and the next exposed signal becomes secondary. Ranking expresses reporting preference, not the device's actual purpose.

| Priority | Signal | Default hours |
| --- | --- | --- |
| 1 | Temperature | 24 |
| 2 | Illuminance | 24 |
| 3 | Humidity | 24 |
| 4 | Ultraviolet index | 24 |
| 5 | Motion | 24 |
| 6 | Acceleration | 24 |
| 7 | 3-axis | 24 |
| 8 | Contact | 24 |
| 9 | Button activity | 24 |
| 10 | Battery | 24 |
| 11 | Water leak | 24 |
| 12 | Smoke | 24 |
| 13 | Pressure | 24 |

Button activity combines the exposed pushed, held, released, and doubleTapped attributes into one clock. Any delivered supported event counts, even if its value is unchanged. Battery uses real event dates, never the percentage value as proof of life. Other previously supported events, including carbon monoxide, switch, and presence, continue to count for any-event liveness but are outside this ranking.

Device silence takes precedence: if no supported event arrives within the existing inactivity window, the app warns that the device may be dead, asleep, out of range, or affected by routing failure. While other supported activity remains recent, overdue primary functions or secondary attributes receive freshness warnings instead. Stale battery reporting alone never establishes device failure.

Freshness monitoring is now enabled by default for the automatically selected slots. If no valid matching history exists, the app starts a stable observation period and waits one threshold before reporting no observed event. Cached values do not refresh clocks; valid original event dates can seed matching history.

### Upgrading from v2.2

As requested, this change replaces the old manual primary/secondary selections and threshold overrides with the new ranking and 24-hour defaults. New setting names intentionally ignore the old configuration; the original values remain stored and can be used by an older source on rollback. Existing device selection, notification settings, global silence threshold, and genuine event history are retained. Already-overdue real history can produce warnings immediately after activation. Review the defaults before saving the app.

## Configuration Options

| Setting | Description |
| --- | --- |
| **Monitored Battery Hardware Devices** | Real hardware devices with the Hubitat `battery` capability whose supported events will be monitored. Virtual devices, custom devices, and devices without `battery` are skipped. |
| **Primary attribute** | Automatic uses the highest-ranked exposed signal. Choose another signal or None to disable this freshness check. |
| **Secondary attribute** | Automatic uses the highest-ranked remaining exposed signal; choose another or None. It cannot duplicate the primary. |
| **Threshold (hours)** | Default 24 for each signal; editable per device/signal. New UI values range from 0.01 to 1,000,000 in 0.01-hour increments, including 48. Blank/invalid stored new values fall back to the signal default. None disables a slot. |
| **Alert if no device event for (hours)** | The inactivity threshold that triggers an alert. |
| **Check interval** | How frequently the app evaluates device activity: 15, 30, or 60 minutes. |
| **Enable debug logging** | Enables detailed Hubitat logs for troubleshooting. |
| **Send push notifications for device-silence alerts and freshness warnings** | Enables or disables delivery for both types; preserves the existing `sendPush` setting. |
| **Notification Device** | Optional Hubitat notification-capable device used to deliver alerts. If notifications are enabled without one, the app logs a warning. |

## Documentation

The project maintainer is accountable for product direction, technical changes, security policy, quality review, workflow, and releases. Responsibilities may be delegated for a change, but the handoff and verification evidence should be recorded.

| Document | Purpose | Update when |
| --- | --- | --- |
| [CONTEXT.md](CONTEXT.md) | Project goals, users, constraints, terminology, and current state | Product scope, assumptions, or supported behavior changes |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Runtime components, data flow, integrations, state, and failure handling | Runtime design, state, integrations, or operational behavior changes |
| [SECURITY.md](SECURITY.md) | Repository and Hubitat data, permission, credential, and incident rules | Permissions, data handling, notification exposure, or security controls change |
| [STANDARDS.md](STANDARDS.md) | Implementation, verification, review, and release conventions | Engineering or release practices change |
| [DECISIONS.md](DECISIONS.md) | Significant project decisions and their rationale | A durable design or policy tradeoff is accepted or superseded |
| [ROADMAP.md](ROADMAP.md) | Planned v2 health-monitoring stages | Priorities, sequencing, or milestone status changes |
| [CHANGELOG.md](CHANGELOG.md) | Released behavior and historical changes | A release is prepared |
| [AGENTS.md](AGENTS.md) | Project context and workflow for coding agents | Runtime facts, agent responsibilities, or workflow rules change |

## Roadmap

The v2.3.0 candidate adds ranked reporting defaults and unified device controls to the existing silence/freshness distinction. The roadmap's health classes, class-specific thresholds, confidence labels, and manual-test workflows remain future work; the v2.3.0 release number does not mark the entire v2.3 roadmap stage complete.

See [ROADMAP.md](ROADMAP.md) for the staged v2.0 through v2.8 plan.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for release history.

## Verification

See [VERIFICATION.md](VERIFICATION.md) for the candidate's local test evidence, limitations, and proposed Hubitat validation and rollback steps. The [Groovy behavioral harness](tests/watchdog_test.groovy) exercises the actual source using simulated Hubitat bindings; it does not contact a hub or deliver real notifications.

## License

This project is released under the MIT License. See the source file headers for details.
