# Dead Battery Watchdog

Dead Battery Watchdog is a Hubitat app that monitors temperature-capable devices to catch sensors that have likely lost power. When a device stops reporting temperature events, the app sends a notification so you can replace the battery before the device is needed.

## Installation

1. In Hubitat, open **Apps Code** and choose **+ New App**.
2. Paste the contents of [`dead_battery_watchdog_hubitat_app.groovy`](dead_battery_watchdog_hubitat_app.groovy) into the editor and save.
3. Click **Apps** -> **+ Add User App** and select **Dead Battery Watchdog**.
4. Configure the app (see below) and click **Done** to activate monitoring.

## Basic Usage

1. **Select devices:** Choose one or more temperature-measurement devices to watch. Devices with a `battery` attribute will have their last reported battery level included in alerts. Devices with a `lastBattery` attribute will also include the last battery replacement date.
2. **Pick the inactivity window:** Set how many hours a device can go without a temperature report before the app considers the battery dead. The default is 24 hours.
3. **Decide how often to check:** Pick an interval (15, 30, or 60 minutes) for the periodic health check.
4. **Configure notifications:** Enable push notifications and optionally pick a Hubitat Notification device. Alerts are limited to once per device every 24 hours, avoiding overnight notification floods.
5. **Save your changes:** After saving, the app keeps track of each device's last temperature, the timestamp of its most recent temperature report, battery metadata, and when the last alert was sent.

## Configuration Options

| Setting | Description |
| --- | --- |
| **Temperature Devices** | The sensors whose temperature reports will be monitored. |
| **Alert if no temperature report for (hours)** | The inactivity threshold that triggers a notification. |
| **Check interval** | How frequently the app evaluates device activity (15, 30, or 60 minutes). |
| **Enable debug logging** | Turn on detailed logs while troubleshooting. |
| **Send push notification for dead battery alerts** | Enable or disable push notifications. |
| **Notification Device** | Optional Hubitat notification device used to deliver alerts. |

## Roadmap

The current v1 app uses temperature reports as the signal for likely dead battery devices. The planned v2 direction broadens this into event-based Zigbee device health monitoring, where the app tracks any event as proof of life, tracks primary device functions separately, and reports stale attributes without calling the whole device dead.

See [ROADMAP.md](ROADMAP.md) for the staged v2.0 through v2.7 plan.

## Changelog

### v1.3.0 (2026-06-28)
- Treat `lastBattery` as the Unix timestamp for the last battery replacement.
- Include the last battery replacement date in dead battery alerts using the app's local Hubitat timestamp format.
- Store battery percentage separately from `lastBattery` so older state does not confuse battery level with replacement time.

### v1.2.4 (2026-06-14)
- Track the timestamp of the latest temperature report, even when the reported value is unchanged.
- Alert only when a device stops reporting temperature events, preventing false alarms from stable rounded temperatures.
- Preserve existing device state on update and migrate older `lastChange` timestamps to `lastReport`.

### v1.2.3 (2026-02-13)
- Format initial-state debug timestamps in the same Hubitat-style local format used by unchanged-temperature and alert logs.
- Align `checkDevices()` run-time debug logging with the same local timestamp format for consistency.

### v1.2.2 (2026-02-13)
- Format "Last Change" timestamps in logs and alerts using Hubitat-style local time (`yyyy-MM-dd hh:mm:ss.SSS a`) to avoid UTC/local confusion.
- Added safe timestamp parsing helper methods for persisted state values.

### v1.2.1 (2025-11-01)
- Limit repeat notifications to once every 24 hours per device to prevent alert fatigue.
- Updated documentation with usage guidance and a historical changelog.

### v1.2.0 (2025-10-25)
- Added configurable check intervals and inactivity thresholds.
- Included optional notification device support alongside push notifications.
- Persist the last temperature, change timestamp, and battery level for each device.

### Earlier releases
- Initial release established temperature-based monitoring and push notifications. Historical details prior to v1.2.0 were undocumented.

## License

This project is released under the MIT License. See the source file headers for details.
