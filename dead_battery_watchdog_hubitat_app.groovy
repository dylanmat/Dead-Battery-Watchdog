import groovy.transform.Field

@Field final String APP_NAME    = "Dead Battery Watchdog"
@Field final String APP_VERSION = "2.0.0"
@Field final String APP_BRANCH  = "main"          // "main"
@Field final String APP_UPDATED = "2026-06-29"    // ISO date is clean
@Field final List<String> MONITORED_ATTRIBUTES = [
    "temperature",
    "humidity",
    "contact",
    "motion",
    "acceleration",
    "water",
    "battery",
    "pushed",
    "held",
    "released",
    "doubleTapped",
    "switch",
    "lock",
    "presence",
    "activity",
    "tamper",
    "illuminance",
    "smoke",
    "carbonMonoxide",
    "powerSource"
]

definition(
    name: APP_NAME,
    namespace: "dylanm.dbw.${APP_BRANCH}",
    author: "Dylan M",
    description: "Alert if a monitored device has stopped reporting events.",
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
        input "monitoredDevices", "capability.*", title: "Monitored Devices", multiple: true, required: false
    }
    section("Configuration") {
        input "inactiveThreshold", "number", title: "Alert if no device event for (hours)", defaultValue: 24
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

    def devices = monitoredDeviceList()
    devices.each { device ->
        MONITORED_ATTRIBUTES.each { attributeName ->
            if (hasAttribute(device, attributeName)) {
                subscribe(device, attributeName, deviceEventHandler)
            }
        }
    }

    def now = new Date()
    devices.each { device ->
        def key = deviceKey(device)
        def temp = currentTemperatureValue(device)
        def batteryLevel = currentBatteryValue(device)
        def lastBattery = currentLastBatteryValue(device)
        def existingStatus = state.deviceStatus[key] ?: state.deviceStatus[device.id] ?: [:]
        def currentTempState = device.currentState("temperature")
        def lastReportDate = latestDate([currentTempState?.date, existingStatus.lastReport, existingStatus.lastChange], now)
        def currentEventState = mostRecentCurrentState(device)
        def lastAnyEventDate = latestDate([currentEventState?.date, existingStatus.lastAnyEvent, existingStatus.lastReport, existingStatus.lastChange], now)

        state.deviceStatus[key] = [
            lastTemp: temp,
            lastReport: lastReportDate,
            batteryLevel: batteryLevel,
            lastBattery: lastBattery,
            lastAnyEvent: lastAnyEventDate,
            lastEventName: existingStatus.lastEventName ?: currentEventState?.name,
            lastEventValue: valueOrDefault(existingStatus.lastEventValue, currentEventState?.value),
            lastEventDisplayName: existingStatus.lastEventDisplayName ?: null,
            lastAlert: existingStatus.lastAlert ?: null
        ]
        if (enableDebug) log.debug "Initial state for ${device.displayName}: last event @ ${formatLogTimestamp(lastAnyEventDate)}, temperature: ${formatOptionalValue(temp, ' deg')}, battery: ${formatOptionalValue(batteryLevel, '%')}, last battery replacement: ${formatUnixTimestamp(lastBattery)}"
    }
}

def deviceEventHandler(evt) {
    def device = evt.device ?: monitoredDeviceList()?.find { deviceKey(it) == evt.deviceId?.toString() }
    def key = evt.deviceId?.toString() ?: deviceKey(device)

    if (!key) {
        log.warn "Received device event without a device id; event ignored."
        return
    }

    def now = asDate(evt.date, new Date())
    def batteryLevel = device ? currentBatteryValue(device) : "N/A"
    def lastBattery = device ? currentLastBatteryValue(device) : null
    def status = state.deviceStatus?.get(key) ?: [:]
    def previousTemp = status.lastTemp

    status.lastAnyEvent = now
    status.lastEventName = evt.name
    status.lastEventValue = evt.value
    status.lastEventDisplayName = evt.displayName
    status.batteryLevel = batteryLevel
    status.lastBattery = lastBattery
    status.lastAlert = null

    if (evt.name == "temperature") {
        status.lastTemp = evt.value
        status.lastReport = now
    } else {
        status.lastTemp = status.lastTemp
        status.lastReport = status.lastReport
    }

    if (!state.deviceStatus) {
        state.deviceStatus = [:]
    }
    state.deviceStatus[key] = status

    if (enableDebug) {
        if (evt.name == "temperature" && previousTemp != evt.value) {
            log.debug "${evt.displayName} temperature event: ${previousTemp} deg -> ${evt.value} deg @ ${formatLogTimestamp(now)}"
        } else if (evt.name == "temperature") {
            log.debug "${evt.displayName} temperature event unchanged at ${evt.value} deg @ ${formatLogTimestamp(now)}"
        } else {
            log.debug "${evt.displayName} ${evt.name} event: ${evt.value} @ ${formatLogTimestamp(now)}"
        }
    }
}

def checkDevices() {
    log.debug "Running checkDevices() at ${formatLogTimestamp(new Date())}"

    def devices = monitoredDeviceList()
    if (!devices) {
        log.warn "No monitored devices selected."
        return
    }

    def thresholdMillis = (inactiveThreshold ?: 24) * 60 * 60 * 1000
    def now = new Date()

    devices.each { device ->
        def key = deviceKey(device)
        def currentTemp = currentTemperatureValue(device)
        def currentLastBattery = currentLastBatteryValue(device)
        def currentTempState = device.currentState("temperature")
        def existingStatus = state.deviceStatus[key] ?: state.deviceStatus[device.id] ?: [:]
        def currentEventState = mostRecentCurrentState(device)
        def lastReportDate = latestDate([currentTempState?.date, existingStatus.lastReport, existingStatus.lastChange], null)
        def lastAnyEventDate = latestDate([existingStatus.lastAnyEvent, currentEventState?.date, lastReportDate, existingStatus.lastChange], now)
        def status = [
            lastTemp: valueOrDefault(existingStatus.lastTemp, currentTemp),
            lastReport: lastReportDate,
            lastBattery: currentLastBattery,
            batteryLevel: valueOrDefault(existingStatus.batteryLevel, currentBatteryValue(device)),
            lastAnyEvent: lastAnyEventDate,
            lastEventName: existingStatus.lastEventName ?: currentEventState?.name,
            lastEventValue: valueOrDefault(existingStatus.lastEventValue, currentEventState?.value),
            lastEventDisplayName: existingStatus.lastEventDisplayName ?: null,
            lastAlert: existingStatus.lastAlert ?: null
        ]

        def lastAnyEvent = asDate(status.lastAnyEvent, now)
        def elapsed = now.time - lastAnyEvent.time
        def lastAlertDate = status.lastAlert ? asDate(status.lastAlert, null) : null
        def alertCooldownMillis = 24 * 60 * 60 * 1000

        if (elapsed > thresholdMillis) {
            if (!lastAlertDate || now.time - lastAlertDate.time >= alertCooldownMillis) {
                def msg = "${device.displayName} may be dead, asleep, out of range, or not reporting - no device event in ${(elapsed / 3600000).toInteger()} hours.\nLast Event: ${formatEventSummary(status)} @ ${formatLogTimestamp(lastAnyEvent)}, Last Temp: ${formatOptionalValue(status.lastTemp, ' deg')}, Battery: ${formatOptionalValue(status.batteryLevel, '%')}, Last Battery Replacement: ${formatUnixTimestamp(status.lastBattery)}"
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
            log.debug "${device.displayName} last device event was ${formatLogTimestamp(lastAnyEvent)}"
        }

        state.deviceStatus[key] = status
    }
}

private List monitoredDeviceList() {
    def devices = settings?.monitoredDevices ?: settings?.temperatureDevices
    if (!devices) return []
    if (devices instanceof Collection) return devices.findAll { it != null }
    return [devices]
}

private String deviceKey(def device) {
    return device?.id?.toString()
}

private currentBatteryValue(def device) {
    return hasAttribute(device, "battery") ? device.currentValue("battery") : "N/A"
}

private currentTemperatureValue(def device) {
    return hasAttribute(device, "temperature") ? device.currentValue("temperature") : null
}

private currentLastBatteryValue(def device) {
    return hasAttribute(device, "lastBattery") ? device.currentValue("lastBattery") : null
}

private boolean hasAttribute(def device, String attributeName) {
    return device?.hasAttribute(attributeName) == true
}

private Map mostRecentCurrentState(def device) {
    Date latest = null
    Map latestState = null
    MONITORED_ATTRIBUTES.each { attributeName ->
        if (hasAttribute(device, attributeName)) {
            def currentState = device.currentState(attributeName)
            Date date = asDate(currentState?.date, null)
            if (date && (!latest || date.time > latest.time)) {
                latest = date
                latestState = [
                    name: attributeName,
                    value: currentState?.value,
                    date: date
                ]
            }
        }
    }
    return latestState
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

private String formatUnixTimestamp(def value) {
    Long unixTime = asLong(value)
    if (!unixTime || unixTime <= 0) return "N/A"

    if (unixTime < 100000000000L) {
        if (unixTime < 946684800L) return "N/A"
        unixTime = unixTime * 1000
    } else if (unixTime < 946684800000L) {
        return "N/A"
    }

    return formatLogTimestamp(new Date(unixTime))
}

private String formatEventSummary(def status) {
    def name = status.lastEventName ?: "unknown"
    def value = status.lastEventValue
    if (value == null || value.toString() == "") return name
    return "${name}: ${value}"
}

private String formatOptionalValue(def value, String suffix = "") {
    if (value == null || value == "N/A" || value.toString() == "") return "N/A"
    return "${value}${suffix}"
}

private valueOrDefault(def value, def fallback) {
    return value == null ? fallback : value
}

private Long asLong(def value) {
    if (value instanceof Number) return value.toLong()
    if (value == null) return null

    String stringValue = value.toString()?.trim()
    if (!stringValue) return null

    try {
        return stringValue.toLong()
    } catch (ignored) {
        return null
    }
}
