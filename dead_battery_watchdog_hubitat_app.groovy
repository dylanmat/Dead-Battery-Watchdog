import groovy.transform.Field

@Field final String APP_NAME    = "Dead Battery Watchdog"
@Field final String APP_VERSION = "1.2.4"
@Field final String APP_BRANCH  = "main"          // "main"
@Field final String APP_UPDATED = "2026-06-14"    // ISO date is clean

definition(
    name: APP_NAME,
    namespace: "dylanm.dbw.${APP_BRANCH}",
    author: "Dylan M",
    description: "Alert if a device has stopped reporting temperature (battery may be dead).",
    category: "Convenience",
    version: "${APP_VERSION}",
    importUrl: "https://raw.githubusercontent.com/dylanmat/Dead-Battery-Watchdog/refs/heads/${APP_BRANCH}/dead_battery_watchdog_hubitat_app.groovy",
    documentationLink: "https://github.com/dylanmat/Dead-Battery-Watchdog",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    singleInstance: false
)

preferences {
    section("Select devices to monitor") {
        input "temperatureDevices", "capability.temperatureMeasurement", title: "Temperature Devices", multiple: true, required: true
    }
    section("Configuration") {
        input "inactiveThreshold", "number", title: "Alert if no temperature report for (hours)", defaultValue: 24
        input "scheduleInterval", "enum", title: "Check interval", options: ["15", "30", "60"], defaultValue: "60"
        input "enableDebug", "bool", title: "Enable debug logging", defaultValue: false
    }
    section("Notification") {
        input "sendPush", "bool", title: "Send push notification for dead battery alerts", defaultValue: true
        input "notifierDevice", "capability.notification", title: "Notification Device", required: false
    }
}

def installed() {
    log.debug "${APP_NAME} v${APP_VERSION} (${APP_BRANCH}) installed ${APP_UPDATED}"
    initialize()
}

def updated() {
    log.debug "${APP_NAME} v${APP_VERSION} (${APP_BRANCH}) updated ${APP_UPDATED}"
    unschedule()
    unsubscribe()
    initialize()
}

def initialize() {
    if (enableDebug) log.debug "Initializing $APP_NAME..."

    def cronExpr
    switch (scheduleInterval?.toInteger()) {
        case 15: cronExpr = "0 0/15 * * * ?"; break
        case 30: cronExpr = "0 0/30 * * * ?"; break
        case 60: cronExpr = "0 0 * * * ?"; break
        default: cronExpr = "0 0 * * * ?"; break
    }

    try {
        schedule(cronExpr, "checkDevices")
        if (enableDebug) log.debug "Scheduled with cron expression: ${cronExpr}"
    } catch (e) {
        log.error "Failed to schedule with cron: ${cronExpr} - ${e}"
    }

    if (!state.deviceStatus) {
        state.deviceStatus = [:]
    }

    subscribe(temperatureDevices, "temperature", temperatureEventHandler)

    def now = new Date()
    temperatureDevices.each { device ->
        def key = deviceKey(device)
        def temp = device.currentTemperature
        def battery = currentBatteryValue(device)
        def existingStatus = state.deviceStatus[key] ?: state.deviceStatus[device.id] ?: [:]
        def currentTempState = device.currentState("temperature")
        def lastReportDate = latestDate([currentTempState?.date, existingStatus.lastReport, existingStatus.lastChange], now)

        state.deviceStatus[key] = [
            lastTemp: temp,
            lastReport: lastReportDate,
            lastBattery: battery,
            lastAlert: existingStatus.lastAlert ?: null
        ]
        if (enableDebug) log.debug "Initial state for ${device.displayName}: ${temp} deg @ ${formatLogTimestamp(lastReportDate)}, battery: ${battery}%"
    }
}

def temperatureEventHandler(evt) {
    def device = evt.device ?: temperatureDevices?.find { deviceKey(it) == evt.deviceId?.toString() }
    def key = evt.deviceId?.toString() ?: deviceKey(device)

    if (!key) {
        log.warn "Received temperature event without a device id; event ignored."
        return
    }

    def now = asDate(evt.date, new Date())
    def battery = device ? currentBatteryValue(device) : "N/A"
    def status = state.deviceStatus?.get(key) ?: [:]
    def previousTemp = status.lastTemp

    status.lastTemp = evt.value
    status.lastReport = now
    status.lastBattery = battery
    status.lastAlert = null

    if (!state.deviceStatus) {
        state.deviceStatus = [:]
    }
    state.deviceStatus[key] = status

    if (enableDebug) {
        if (previousTemp != evt.value) {
            log.debug "${evt.displayName} temperature report: ${previousTemp} deg -> ${evt.value} deg @ ${formatLogTimestamp(now)}"
        } else {
            log.debug "${evt.displayName} temperature report unchanged at ${evt.value} deg @ ${formatLogTimestamp(now)}"
        }
    }
}

def checkDevices() {
    log.debug "Running checkDevices() at ${formatLogTimestamp(new Date())}"

    if (!temperatureDevices) {
        log.warn "No temperature devices selected."
        return
    }

    def thresholdMillis = (inactiveThreshold ?: 24) * 60 * 60 * 1000
    def now = new Date()

    temperatureDevices.each { device ->
        def key = deviceKey(device)
        def currentTemp = device.currentTemperature
        def currentBattery = currentBatteryValue(device)
        def currentTempState = device.currentState("temperature")
        def existingStatus = state.deviceStatus[key] ?: state.deviceStatus[device.id] ?: [:]
        def status = [
            lastTemp: existingStatus.lastTemp ?: currentTemp,
            lastReport: latestDate([currentTempState?.date, existingStatus.lastReport, existingStatus.lastChange], now),
            lastBattery: existingStatus.lastBattery ?: currentBattery,
            lastAlert: existingStatus.lastAlert ?: null
        ]

        def lastReportDate = asDate(status.lastReport, now)
        def elapsed = now.time - lastReportDate.time
        def lastAlertDate = status.lastAlert ? asDate(status.lastAlert, null) : null
        def alertCooldownMillis = 24 * 60 * 60 * 1000

        if (elapsed > thresholdMillis) {
            if (!lastAlertDate || now.time - lastAlertDate.time >= alertCooldownMillis) {
                def msg = "${device.displayName} may have a dead battery - no temperature report in ${(elapsed / 3600000).toInteger()} hours.\nLast Temp: ${status.lastTemp} deg, Last Report: ${formatLogTimestamp(lastReportDate)}, Battery: ${status.lastBattery}%"
                log.warn msg
                if (sendPush && notifierDevice) {
                    notifierDevice.deviceNotification(msg)
                } else if (sendPush) {
                    log.warn "Push enabled but no notifier device selected."
                }
                status.lastAlert = now
            } else if (enableDebug) {
                def hoursSinceAlert = ((now.time - lastAlertDate.time) / 3600000).toInteger()
                log.debug "${device.displayName} alert suppressed - last notification sent ${hoursSinceAlert} hours ago."
            }
        } else if (enableDebug) {
            log.debug "${device.displayName} last temperature report was ${formatLogTimestamp(lastReportDate)}"
        }

        state.deviceStatus[key] = status
    }
}

private String deviceKey(def device) {
    return device?.id?.toString()
}

private currentBatteryValue(def device) {
    return device?.hasAttribute("battery") ? device.currentValue("battery") : "N/A"
}

private Date latestDate(List values, Date fallback) {
    Date latest = null
    values.each { value ->
        Date date = asDate(value, null)
        if (date && (!latest || date.time > latest.time)) {
            latest = date
        }
    }
    return latest ?: fallback
}

private Date asDate(def value, Date fallback = null) {
    if (value instanceof Date) return value
    if (value == null) return fallback

    String stringValue = value.toString()
    try {
        return Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", stringValue)
    } catch (ignored) {
        return fallback
    }
}

private String formatLogTimestamp(Date date) {
    if (!date) return "unknown"
    TimeZone tz = location?.timeZone ?: TimeZone.default
    return date.format("yyyy-MM-dd hh:mm:ss.SSS a", tz)
}
