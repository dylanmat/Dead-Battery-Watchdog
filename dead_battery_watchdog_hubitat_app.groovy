import groovy.transform.Field

@Field final String APP_NAME    = "Dead Battery Watchdog"
@Field final String APP_VERSION = "1.2.1"
@Field final String APP_BRANCH  = "main"          // "main"
@Field final String APP_UPDATED = "2025-11-01"    // ISO date is clean

definition(
    name: APP_NAME,
    namespace: "dylanm.dbw.${APP_BRANCH}",
    author: "Dylan M",
    description: "Alert if a device's temperature hasn't changed (battery may be dead).",
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
        input "inactiveThreshold", "number", title: "Alert if temperature unchanged for (hours)", defaultValue: 24
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
        log.error "Failed to schedule with cron: ${cronExpr} — ${e}"
    }

    if (!state.deviceStatus) {
        state.deviceStatus = [:]
    }

    def now = new Date()
    temperatureDevices.each { device ->
        def temp = device.currentTemperature
        def battery = device.hasAttribute("battery") ? device.currentValue("battery") : "N/A"
        state.deviceStatus[device.id] = [
            lastTemp: temp,
            lastChange: now,
            lastBattery: battery,
            lastAlert: null
        ]
        if (enableDebug) log.debug "Initial state for ${device.displayName}: ${temp}° @ ${now}, battery: ${battery}%"
    }
}

def checkDevices() {
    log.debug "Running checkDevices() at ${new Date()}"

    if (!temperatureDevices) {
        log.warn "No temperature devices selected."
        return
    }

    def thresholdMillis = (inactiveThreshold ?: 24) * 60 * 60 * 1000
    def now = new Date()

    temperatureDevices.each { device ->
        def currentTemp = device.currentTemperature
        def currentBattery = device.hasAttribute("battery") ? device.currentValue("battery") : "N/A"

        def status = state.deviceStatus[device.id] ?: [
            lastTemp: currentTemp,
            lastChange: now,
            lastBattery: currentBattery,
            lastAlert: null
        ]

        if (currentTemp != status.lastTemp) {
            if (enableDebug) log.debug "${device.displayName} temperature changed: ${status.lastTemp}° -> ${currentTemp}°"
            status.lastTemp = currentTemp
            status.lastChange = now
            status.lastBattery = currentBattery
            status.lastAlert = null
        } else {
            def lastChangeDate = status.lastChange instanceof Date ? status.lastChange : Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", status.lastChange.toString())
            def elapsed = now.time - lastChangeDate.time
            def lastAlertDate = status.lastAlert ? (status.lastAlert instanceof Date ? status.lastAlert : Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", status.lastAlert.toString())) : null
            def alertCooldownMillis = 24 * 60 * 60 * 1000

            if (elapsed > thresholdMillis) {
                if (!lastAlertDate || now.time - lastAlertDate.time >= alertCooldownMillis) {
                    def msg = "${device.displayName} may have a dead battery — no temperature change in ${(elapsed / 3600000).toInteger()} hours.\nLast Temp: ${status.lastTemp}°, Last Change: ${status.lastChange}, Battery: ${status.lastBattery}%"
                    log.warn msg
                    if (sendPush && notifierDevice) {
                        notifierDevice.deviceNotification(msg)
                    } else if (sendPush) {
                        log.warn "Push enabled but no notifier device selected."
                    }
                    status.lastAlert = now
                } else if (enableDebug) {
                    def hoursSinceAlert = ((now.time - lastAlertDate.time) / 3600000).toInteger()
                    log.debug "${device.displayName} alert suppressed — last notification sent ${hoursSinceAlert} hours ago."
                }
            } else if (enableDebug) {
                log.debug "${device.displayName} temp unchanged at ${status.lastTemp}° since ${status.lastChange}"
            }
        }

        state.deviceStatus[device.id] = status
    }
}