# Dead Battery Watchdog

Dead Battery Watchdog is a Hubitat app that monitors selected battery-capable hardware devices to catch sensors that have likely stopped reporting. When a monitored battery hardware device stops sending supported Hubitat events, the app sends a notification so you can investigate the battery, range, sleep state, or Zigbee routing before the device is needed.

## Installation

1. In Hubitat, open **Apps Code** and choose **+ New App**.
2. Paste the contents of [`dead_battery_watchdog_hubitat_app.groovy`](dead_battery_watchdog_hubitat_app.groovy) into the editor and save.
3. Click **Apps** -> **+ Add User App** and select **Dead Battery Watchdog**.
4. Configure the app (see below) and click **Done** to activate monitoring.

## Basic Usage

1. **Select devices:** Choose one or more real hardware devices with the Hubitat `battery` capability. Virtual devices, custom devices, and devices without `battery` are skipped. The app listens for common device events including temperature, humidity, contact, motion, acceleration, water, battery, button, switch, lock, presence, and activity events.
2. **Pick the inactivity window:** Set how many hours a device can go without any monitored event before the app alerts. The default is 24 hours.
3. **Decide how often to check:** Pick an interval (15, 30, or 60 minutes) for the periodic health check.
4. **Configure notifications:** Enable push notifications and optionally pick a Hubitat Notification device. Alerts are limited to once per device every 24 hours, avoiding overnight notification floods.
5. **Save your changes:** After saving, the app keeps track of each device's most recent monitored event, optional temperature context, battery metadata, and when the last alert was sent.

## Configuration Options

| Setting | Description |
| --- | --- |
| **Monitored Battery Hardware Devices** | The real hardware devices with the Hubitat `battery` capability whose supported events will be monitored. Virtual devices, custom devices, and devices without `battery` are skipped. |
| **Alert if no device event for (hours)** | The inactivity threshold that triggers a notification. |
| **Check interval** | How frequently the app evaluates device activity (15, 30, or 60 minutes). |
| **Enable debug logging** | Turn on detailed logs while troubleshooting. |
| **Send push notification for dead battery alerts** | Enable or disable push notifications. |
| **Notification Device** | Optional Hubitat notification device used to deliver alerts. |

## Roadmap

The current v2.0.2 app uses common Hubitat device events from real battery-capable hardware devices as the signal for likely dead, asleep, out-of-range, or non-reporting devices. Future v2 stages will track primary device functions separately and report stale attributes without calling the whole device dead.

See [ROADMAP.md](ROADMAP.md) for the staged v2.0 through v2.8 plan.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for release history.

## License

This project is released under the MIT License. See the source file headers for details.
