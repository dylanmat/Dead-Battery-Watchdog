# Dead Battery Watchdog

Dead Battery Watchdog is a Hubitat app that monitors temperature-capable devices to catch sensors that have likely lost power. When a device stops reporting new temperature values, the app sends a notification so you can replace the battery before the device is needed.

## Installation

1. In Hubitat, open **Apps Code** and choose **+ New App**.
2. Paste the contents of [`dead_battery_watchdog_hubitat_app.groovy`](dead_battery_watchdog_hubitat_app.groovy) into the editor and save.
3. Click **Apps** → **+ Add User App** and select **Dead Battery Watchdog**.
4. Configure the app (see below) and click **Done** to activate monitoring.

## Basic Usage

1. **Select devices:** Choose one or more temperature-measurement devices to watch. Devices with a `battery` attribute will have their last reported battery level included in alerts.
2. **Pick the inactivity window:** Set how many hours a device can go without a temperature change before the app considers the battery dead. The default is 24 hours.
3. **Decide how often to check:** Pick an interval (15, 30, or 60 minutes) for the periodic health check.
4. **Configure notifications:** Enable push notifications and optionally pick a Hubitat Notification device. Alerts are now limited to once per device every 24 hours, avoiding overnight notification floods.
5. **Save your changes:** After saving, the app keeps track of each device’s last temperature, the timestamp of that reading, and when the last alert was sent.

## Configuration Options

| Setting | Description |
| --- | --- |
| **Temperature Devices** | The sensors whose temperature readings will be monitored. |
| **Alert if temperature unchanged for (hours)** | The inactivity threshold that triggers a notification. |
| **Check interval** | How frequently the app evaluates device activity (15, 30, or 60 minutes). |
| **Enable debug logging** | Turn on detailed logs while troubleshooting. |
| **Send push notification for dead battery alerts** | Enable or disable push notifications. |
| **Notification Device** | Optional Hubitat notification device used to deliver alerts. |

## Changelog

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
