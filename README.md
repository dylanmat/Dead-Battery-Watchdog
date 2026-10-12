# Dead Battery Watchdog

Dead Battery Watchdog is a Hubitat app that monitors selected battery-capable hardware devices to catch sensors that have likely stopped reporting. When a monitored battery hardware device stops sending supported Hubitat events, the app sends a notification so you can investigate the battery, range, sleep state, or Zigbee routing before the device is needed.

The app is a single self-contained Groovy file with no external runtime dependencies. Hubitat users paste the source directly into Apps Code.

The repository contains the **v2.2.0 candidate**, with local behavioral verification and successful Hubitat compilation/deployment on 2026-10-11. The saved hub source exactly matches the local artifact. Installed-instance activation and runtime validation remain pending; roadmap stages are not marked complete until that evidence exists.

## Installation

1. In Hubitat, open **Apps Code** and choose **+ New App**.
2. Paste the contents of [`dead_battery_watchdog_hubitat_app.groovy`](dead_battery_watchdog_hubitat_app.groovy) into the editor and save.
3. Click **Apps** -> **+ Add User App** and select **Dead Battery Watchdog**.
4. Configure the app and click **Done** to activate monitoring.

## Basic Usage

1. **Select devices:** Choose one or more real hardware devices with the Hubitat `battery` capability. Virtual devices, custom devices, and devices without `battery` are skipped. The app listens for common device events including temperature, humidity, contact, motion, acceleration, water, battery, button, switch, lock, presence, and activity events.
2. **Choose primary functions:** For each supported device, optionally select one or more attributes that represent its main purpose. For example, use `contact` for a door sensor or `temperature` and `humidity` for an environmental sensor. Unconfigured devices continue any-event monitoring and produce a configuration warning.
3. **Pick the inactivity window:** Set how many hours a device can go without any monitored event before the app alerts. The default is 24 hours.
4. **Decide how often to check:** Pick an interval of 15, 30, or 60 minutes for the periodic health check.
5. **Optionally monitor freshness:** Set a per-device primary inactivity threshold and/or select secondary attributes with individual freshness thresholds. Blank thresholds disable these checks. An event from any selected primary attribute refreshes the primary-function clock. Contact sensors, buttons, and rarely used devices may naturally remain quiet; choose thresholds accordingly.
6. **Configure notifications:** Enable push notifications and optionally select a Hubitat Notification device. Device-silence alerts and each freshness issue have independent 24-hour cooldowns. Newly due freshness warnings are combined into one message per device per scheduled check.
7. **Save your changes:** The app tracks known events, enabled freshness checks, observation periods, and alert cooldowns in Hubitat app state.

Device silence takes precedence: if no supported event arrives within the existing inactivity window, the app warns that the device may be dead, asleep, out of range, or affected by routing failure. While other supported activity remains recent, overdue primary functions or secondary attributes receive freshness warnings instead. Stale battery reporting alone never establishes device failure.

Freshness monitoring is disabled by default, including on upgrades. Secondary options contain exposed supported attributes excluding the configured primary selection. Each selected secondary attribute needs its own positive threshold. If no valid event history exists, the app starts a stable observation period and waits one configured threshold before reporting that no event has been observed. It never invents a last-event timestamp. Cached Hubitat values do not refresh clocks; valid stored event dates may seed history.

## Configuration Options

| Setting | Description |
| --- | --- |
| **Monitored Battery Hardware Devices** | Real hardware devices with the Hubitat `battery` capability whose supported events will be monitored. Virtual devices, custom devices, and devices without `battery` are skipped. |
| **Primary attributes** | Optional per-device multi-select containing supported functional attributes exposed by that device. Battery, power-source, and tamper events cannot be primary, but they still count toward any-event liveness. |
| **Primary inactivity threshold** | Optional per-device hours without any configured primary event before warning while supported activity remains recent. Blank disables. |
| **Secondary attributes** | Optional per-device selection from supported attributes exposed by the device, excluding configured primary attributes. |
| **Attribute freshness threshold** | Optional hours for each selected secondary attribute. Blank disables; valid freshness thresholds are 0.001 through 1,000,000 hours and must resolve to whole milliseconds. |
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

The v2.2.0 candidate distinguishes device silence, overdue primary functions, and stale secondary attributes. Health classes, recommended threshold profiles, confidence labels, and manual-test workflows remain future work.

See [ROADMAP.md](ROADMAP.md) for the staged v2.0 through v2.8 plan.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for release history.

## Verification

See [VERIFICATION.md](VERIFICATION.md) for the candidate's local test evidence, limitations, and proposed Hubitat validation and rollback steps. The [Groovy behavioral harness](tests/watchdog_test.groovy) exercises the actual source using simulated Hubitat bindings; it does not contact a hub or deliver real notifications.

## License

This project is released under the MIT License. See the source file headers for details.
