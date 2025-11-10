# Dead Battery Watchdog

Dead Battery Watchdog is a Hubitat safety net that checks your temperature sensors for stalled readings so you can swap batteries before a device goes silent.

## Installation
1. Open the **Apps Code** section of your Hubitat Elevation hub.
2. Click **New App** and paste the contents of `dead_battery_watchdog_hubitat_app.groovy` from this repository.
3. Save the app, then click **Load New App** (or the equivalent option) to install it.
4. Give the app any required permissions when prompted.

## Usage
1. In the app, choose every device with the `Temperature Measurement` capability that you want to monitor.
2. Set the number of hours that a sensor may report the exact same temperature before the app raises an alert. The default is 24 hours.
3. Pick how frequently Hubitat should run the health check (15, 30, or 60 minutes).
4. Enable debug logging when you want detailed information in your hub logs.
5. (Optional) Select a notifier device if you want push notifications in addition to the hub event log.

### What to expect
- The app saves the most recent temperature and battery value for each device.
- Every scheduled run compares the current temperature to the last recorded value. If the value has not changed for longer than your threshold the app logs a warning and, if configured, sends a push notification.
- When a temperature changes, the timer resets automatically.

## Troubleshooting
- If you do not see any alerts, confirm that the schedule interval is running by checking the hub logs for the "checkDevices" entry.
- Alerts require the notifier device to support `deviceNotification`. If you do not select one, the app still writes warnings to the log.
- Delete and reinstall the app if you change a large number of monitored devices and state data becomes inconsistent.

## Change Log
- **1.2.1** — Documentation refresh: clarify installation, usage, and troubleshooting steps.
- **1.2.0** — Added scheduling options, optional push notifications, and improved debug logging.
