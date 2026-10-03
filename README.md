# Dead Battery Watchdog

Dead Battery Watchdog is a Hubitat app that monitors selected battery-capable hardware devices to catch sensors that have likely stopped reporting. When a monitored battery hardware device stops sending supported Hubitat events, the app sends a notification so you can investigate the battery, range, sleep state, or Zigbee routing before the device is needed.

The app is a single self-contained Groovy file with no external runtime dependencies. Hubitat users paste the source directly into Apps Code.

## Installation

1. In Hubitat, open **Apps Code** and choose **+ New App**.
2. Paste the contents of [`dead_battery_watchdog_hubitat_app.groovy`](dead_battery_watchdog_hubitat_app.groovy) into the editor and save.
3. Click **Apps** -> **+ Add User App** and select **Dead Battery Watchdog**.
4. Configure the app and click **Done** to activate monitoring.

## Basic Usage

1. **Select devices:** Choose one or more real hardware devices with the Hubitat `battery` capability. Virtual devices, custom devices, and devices without `battery` are skipped. The app listens for common device events including temperature, humidity, contact, motion, acceleration, water, battery, button, switch, lock, presence, and activity events.
2. **Pick the inactivity window:** Set how many hours a device can go without any monitored event before the app alerts. The default is 24 hours.
3. **Decide how often to check:** Pick an interval of 15, 30, or 60 minutes for the periodic health check.
4. **Configure notifications:** Enable push notifications and optionally select a Hubitat Notification device. Alerts are limited to once per device every 24 hours.
5. **Save your changes:** The app tracks each device's most recent monitored event, optional temperature context, battery metadata, and last alert time in Hubitat app state.

## Configuration Options

| Setting | Description |
| --- | --- |
| **Monitored Battery Hardware Devices** | Real hardware devices with the Hubitat `battery` capability whose supported events will be monitored. Virtual devices, custom devices, and devices without `battery` are skipped. |
| **Alert if no device event for (hours)** | The inactivity threshold that triggers an alert. |
| **Check interval** | How frequently the app evaluates device activity: 15, 30, or 60 minutes. |
| **Enable debug logging** | Enables detailed Hubitat logs for troubleshooting. |
| **Send push notification for dead battery alerts** | Enables or disables notification delivery. |
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

The current v2.0.2 app uses common Hubitat device events from real battery-capable hardware devices as the signal for likely dead, asleep, out-of-range, or non-reporting devices. Future v2 stages will track primary device functions separately and report stale attributes without calling the whole device dead.

See [ROADMAP.md](ROADMAP.md) for the staged v2.0 through v2.8 plan.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for release history.

## License

This project is released under the MIT License. See the source file headers for details.
