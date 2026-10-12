import groovy.transform.Field

@Field final String APP_NAME    = "Dead Battery Watchdog"
@Field final String APP_VERSION = "2.2.0"
@Field final String APP_BRANCH  = "main"          // "main"
@Field final String APP_UPDATED = "2026-10-11"    // ISO date is clean
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
@Field final List<String> PRIMARY_ATTRIBUTE_CANDIDATES = [
    "temperature",
    "humidity",
    "contact",
    "motion",
    "acceleration",
    "water",
    "pushed",
    "held",
    "released",
    "doubleTapped",
    "switch",
    "lock",
    "presence",
    "activity",
    "illuminance",
    "smoke",
    "carbonMonoxide"
]

definition(
    name: APP_NAME,
    namespace: "dylanm.dbw.${APP_BRANCH}",
    author: "Dylan M",
    description: "Alert if a monitored hardware device has stopped reporting events.",
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
        input "monitoredDevices", "capability.battery", title: "Monitored Battery Hardware Devices", multiple: true, required: false, submitOnChange: true
    }
    def configuredDevices = monitoredDeviceList()
    if (configuredDevices) {
        section("Select each device's primary function") {
            paragraph "An event from any selected primary attribute refreshes the primary-function clock. Optional freshness checks warn while other supported events remain recent. Rarely used contacts and buttons may naturally remain quiet."
            configuredDevices.each { device ->
                def availableAttributes = availablePrimaryAttributes(device)
                if (availableAttributes) {
                    input primaryAttributeSettingName(device), "enum", title: "${device.displayName} primary attributes", options: availableAttributes, multiple: true, required: false, submitOnChange: true
                    if (configuredPrimaryAttributes(device)) {
                        input "primaryThreshold_${deviceKey(device)}", "decimal", title: "${device.displayName} primary inactivity threshold (hours; blank disables)", required: false, range: "0.001..1000000"
                    }
                    if (!configuredPrimaryAttributes(device)) {
                        paragraph "${device.displayName} has no primary attributes selected. Any-event monitoring will continue."
                    }
                } else {
                    paragraph "${device.displayName} exposes no supported primary-function attributes. Any-event monitoring will continue."
                }
            }
        }
        section("Optional secondary-attribute freshness") {
            paragraph "Select secondary attributes and set a positive threshold for each. Blank thresholds disable checks. Cached values do not prove that a device is reachable."
            configuredDevices.each { device ->
                def secondaryOptions = availableSecondaryAttributes(device)
                if (secondaryOptions) {
                    input "secondaryAttributes_${deviceKey(device)}", "enum", title: "${device.displayName} secondary attributes", options: secondaryOptions, multiple: true, required: false, submitOnChange: true
                    configuredSecondaryAttributes(device).each { attributeName ->
                        input "attributeThreshold_${deviceKey(device)}_${attributeName}", "decimal", title: "${device.displayName} ${attributeName} freshness threshold (hours; blank disables)", required: false, range: "0.001..1000000"
                    }
                }
            }
        }
    }
    section("Configuration") {
        input "inactiveThreshold", "number", title: "Alert if no device event for (hours)", defaultValue: 24, range: "1..1000000"
        input "scheduleInterval", "enum", title: "Check interval", options: ["15", "30", "60"], defaultValue: "60"
        input "enableDebug", "bool", title: "Enable debug logging", defaultValue: false
    }
    section("Notification") {
        input "sendPush", "bool", title: "Send push notifications for device-silence alerts and freshness warnings", defaultValue: true
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

    if (!(state.deviceStatus instanceof Map)) {
        state.deviceStatus = [:]
    }

    def selectedDevices = selectedDeviceList()
    def devices = monitoredDeviceList()
    def skippedNonHardwareDevices = selectedDevices.findAll { !isRealHardwareDevice(it) }
    def skippedNonBatteryDevices = selectedDevices.findAll { isRealHardwareDevice(it) && !hasAttribute(it, "battery") }

    if (skippedNonHardwareDevices && enableDebug) {
        log.debug "Skipping non-hardware monitored devices: ${skippedNonHardwareDevices.collect { it.displayName }.join(', ')}"
    }
    if (skippedNonBatteryDevices && enableDebug) {
        log.debug "Skipping hardware devices without battery capability: ${skippedNonBatteryDevices.collect { it.displayName }.join(', ')}"
    }

    def unconfiguredDevices = devices.findAll { !configuredPrimaryAttributes(it) }
    if (unconfiguredDevices) {
        log.warn "Primary-function tracking is not configured for: ${unconfiguredDevices.collect { it.displayName }.join(', ')}. Any-event monitoring will continue."
    }

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
        def existingStatus = state.deviceStatus[key] ?: state.deviceStatus[device.id] ?: [:]
        def status = reconciledDeviceStatus(device, existingStatus, now)
        state.deviceStatus[key] = status
        if (enableDebug) log.debug "Initial state for ${device.displayName}: last event @ ${formatLogTimestamp(asDate(status.lastAnyEvent))}, last primary event @ ${formatLogTimestamp(asDate(status.lastPrimaryEvent))}"
    }
}

def deviceEventHandler(evt) {
    def device = monitoredDeviceList().find { deviceKey(it) == (evt.deviceId?.toString() ?: deviceKey(evt.device)) }
    def key = evt.deviceId?.toString() ?: deviceKey(device)

    if (!key) {
        log.warn "Received device event without a device id; event ignored."
        return
    }

    if (!device || !MONITORED_ATTRIBUTES.contains(evt.name?.toString()) || !hasAttribute(device, evt.name.toString())) {
        if (enableDebug) log.debug "Ignoring event from an unselected device or unsupported attribute."
        return
    }

    def now = validEventDate(evt.date, new Date())
    if (!now) {
        if (enableDebug) log.debug "Ignoring event with an invalid or future timestamp."
        return
    }
    def batteryLevel = device ? currentBatteryValue(device) : "N/A"
    def lastBattery = device ? currentLastBatteryValue(device) : null
    if (!(state.deviceStatus instanceof Map)) state.deviceStatus = [:]
    def savedStatus = state.deviceStatus.get(key)
    def status = savedStatus instanceof Map ? savedStatus : [:]
    def previousTemp = status.lastTemp
    def primaryAttributes = device ? configuredPrimaryAttributes(device) : normalizePrimaryAttributes(status.primaryAttributes)

    if (device && normalizePrimaryAttributes(status.primaryAttributes) != primaryAttributes) {
        def primaryTracking = reconciledPrimaryTracking(device, status)
        status.primaryAttributes = primaryTracking.primaryAttributes
        status.lastPrimaryEvent = primaryTracking.lastPrimaryEvent
        status.lastPrimaryEventName = primaryTracking.lastPrimaryEventName
        status.lastPrimaryEventValue = primaryTracking.lastPrimaryEventValue
    }

    def previousAny = validEventDate(status.lastAnyEvent, new Date())
    if (!previousAny || now.time >= previousAny.time) {
        status.lastAnyEvent = now
        status.lastEventName = evt.name
        status.lastEventValue = evt.value
        status.lastEventDisplayName = evt.displayName
        def deadThreshold = positiveHoursMillis(inactiveThreshold) ?: 24L * 60 * 60 * 1000
        if (new Date().time - now.time <= deadThreshold) status.lastAlert = null
    }
    status.batteryLevel = batteryLevel
    status.lastBattery = lastBattery
    status.primaryAttributes = primaryAttributes
    def history = status.attributeHistory instanceof Map ? status.attributeHistory : [:]
    def previousEntry = history[evt.name] instanceof Map ? history[evt.name] : [:]
    def previousAttribute = validEventDate(previousEntry.date, new Date())
    if (!previousAttribute || now.time >= previousAttribute.time) {
        history[evt.name] = [date: now, value: evt.value]
    }
    status.attributeHistory = history

    def previousPrimary = validEventDate(status.lastPrimaryEvent, new Date())
    if (primaryAttributes.contains(evt.name?.toString()) && (!previousPrimary || now.time >= previousPrimary.time)) {
        status.lastPrimaryEvent = now
        status.lastPrimaryEventName = evt.name
        status.lastPrimaryEventValue = evt.value
    }
    // A real recovery between scheduled checks must clear only its own cooldown.
    def issueAlerts = status.issueAlerts instanceof Map ? status.issueAlerts : [:]
    def monitors = status.freshnessMonitors instanceof Map ? status.freshnessMonitors : [:]
    ["attribute:${evt.name}".toString(), primaryAttributes.contains(evt.name?.toString()) ? "primary" : null].findAll { it }.each { issueKey ->
        def monitor = monitors[issueKey] instanceof Map ? monitors[issueKey] : [:]
        def threshold = asLong(monitor.threshold)
        if (threshold && threshold > 0 && new Date().time - now.time <= threshold) issueAlerts.remove(issueKey)
    }
    status.issueAlerts = issueAlerts

    def previousReport = validEventDate(status.lastReport, new Date())
    if (evt.name == "temperature" && (!previousReport || now.time >= previousReport.time)) {
        status.lastTemp = evt.value
        status.lastReport = now
    }

    if (!(state.deviceStatus instanceof Map)) {
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
        log.warn selectedDeviceList() ? "No supported battery hardware monitored devices selected." : "No monitored devices selected."
        return
    }

    def thresholdMillis = positiveHoursMillis(inactiveThreshold) ?: 24L * 60 * 60 * 1000
    def now = new Date()
    if (!(state.deviceStatus instanceof Map)) state.deviceStatus = [:]

    devices.each { device ->
        def key = deviceKey(device)
        def existingStatus = state.deviceStatus[key] ?: state.deviceStatus[device.id] ?: [:]
        def status = reconciledDeviceStatus(device, existingStatus, now)
        def health = evaluateHealth(status, thresholdMillis, now)
        status.health = [device: health.device, issues: health.issues.collect { it.key }, evaluatedAt: now]
        // Recovery is evaluated even while device silence suppresses freshness delivery.
        def issueAlerts = status.issueAlerts instanceof Map ? status.issueAlerts : [:]
        def activeKeys = health.issues.collect { it.key }
        issueAlerts = issueAlerts.findAll { keyName, value -> activeKeys.contains(keyName) }
        status.issueAlerts = issueAlerts

        if (health.device == "silence") {
            if (alertDue(status.lastAlert, now)) {
                def lastAny = validEventDate(status.lastAnyEvent, now)
                def evidence = lastAny ? "no supported event since ${formatLogTimestamp(lastAny)}" : "no supported event observed since monitoring began at ${formatLogTimestamp(asDate(status.observationStarted))}"
                deliverWarning("${device.displayName} may be dead, asleep, out of range, or affected by routing failure - ${evidence}.\nLast Event: ${formatEventSummary(status)}, Last Temp: ${formatOptionalValue(status.lastTemp, ' deg')}, Battery: ${formatOptionalValue(status.batteryLevel, '%')}, Last Battery Replacement: ${formatUnixTimestamp(status.lastBattery)}")
                status.lastAlert = now
            }
        } else {
            status.lastAlert = null
            if (health.device == "reporting") {
                def due = health.issues.findAll { alertDue(issueAlerts[it.key], now) }
                if (due) {
                    def details = due.collect { issue ->
                        def evidence = issue.lastEvent ? "last event @ ${formatLogTimestamp(issue.lastEvent)}" : "no event observed since monitoring began @ ${formatLogTimestamp(issue.started)}"
                        def explanation = issue.key == "primary" ? "possible function or driver issue" : "reporting is stale; this alone does not establish device failure"
                        return "${issue.label}: ${evidence}; ${explanation}."
                    }.join("\n")
                    deliverWarning("${device.displayName} freshness warning:\n${details}\nOther supported activity remains recent: ${formatEventSummary(status)} @ ${formatLogTimestamp(asDate(status.lastAnyEvent))}.")
                    due.each { issueAlerts[it.key] = now }
                }
            }
        }

        state.deviceStatus[key] = status
    }
}

private void deliverWarning(String message) {
    log.warn message
    if (sendPush && notifierDevice) {
        notifierDevice.deviceNotification(message)
    } else if (sendPush) {
        log.warn "Push enabled but no notifier device selected."
    }
}

private boolean alertDue(def lastAlert, Date now) {
    def date = validEventDate(lastAlert, now)
    return !date || now.time - date.time >= 24L * 60 * 60 * 1000
}

private Map evaluateHealth(Map status, Long deadThreshold, Date now) {
    def lastAny = validEventDate(status.lastAnyEvent, now)
    def start = validEventDate(status.observationStarted, now) ?: now
    def reference = lastAny ?: start
    def deviceState = now.time - reference.time > deadThreshold ? "silence" : (lastAny ? "reporting" : "unknown")
    def issues = []
    def monitors = status.freshnessMonitors instanceof Map ? status.freshnessMonitors : [:]
    monitors.each { key, monitor ->
        def lastEvent = key == "primary" ? validEventDate(status.lastPrimaryEvent, now) : validEventDate(status.attributeHistory?.get(monitor.attribute)?.date, now)
        def started = validEventDate(monitor.started, now) ?: now
        def threshold = asLong(monitor.threshold)
        if (threshold && threshold > 0 && now.time - (lastEvent ?: started).time > threshold) {
            issues << [key: key, label: key == "primary" ? "Primary function (${status.primaryAttributes.join(', ')})" : monitor.attribute, lastEvent: lastEvent, started: started]
        }
    }
    return [device: deviceState, issues: issues]
}

private Map reconciledDeviceStatus(def device, def existing, Date now) {
    def status = existing instanceof Map ? new LinkedHashMap(existing) : [:]
    def history = [:]
    def oldHistory = status.attributeHistory instanceof Map ? status.attributeHistory : [:]
    MONITORED_ATTRIBUTES.findAll { hasAttribute(device, it) }.each { attributeName ->
        def previous = oldHistory[attributeName] instanceof Map ? oldHistory[attributeName] : [:]
        def previousDate = validEventDate(previous.date, now)
        def cached = safeCurrentState(device, attributeName)
        def cachedDate = validEventDate(cached?.date, now)
        if (previousDate && (!cachedDate || previousDate.time >= cachedDate.time)) {
            history[attributeName] = [date: previousDate, value: previous.value]
        } else if (cachedDate) {
            history[attributeName] = [date: cachedDate, value: cached.value]
        }
    }
    status.attributeHistory = history
    status.observationStarted = validEventDate(status.observationStarted, now) ?: now
    def latestEntry = history.entrySet().max { it.value.date.time }
    def legacyDate = latestDate([status.lastAnyEvent, status.lastReport, status.lastChange], null, now)
    if (latestEntry && (!legacyDate || latestEntry.value.date.time > legacyDate.time)) {
        status.lastAnyEvent = latestEntry.value.date
        status.lastEventName = latestEntry.key
        status.lastEventValue = latestEntry.value.value
        status.lastEventDisplayName = null
    } else {
        status.lastAnyEvent = legacyDate
    }
    status.lastReport = latestDate([history.temperature?.date, status.lastReport, status.lastChange], null, now)
    status.lastTemp = history.temperature ? history.temperature.value : currentTemperatureValue(device)
    status.batteryLevel = currentBatteryValue(device)
    status.lastBattery = currentLastBatteryValue(device)
    def primary = reconciledPrimaryTracking(device, status, now)
    status.putAll(primary)

    def desired = [:]
    def primaryThreshold = positiveHoursMillis(settings?.get("primaryThreshold_${deviceKey(device)}".toString()))
    if (primary.primaryAttributes && primaryThreshold) {
        desired.primary = [threshold: primaryThreshold, attributes: primary.primaryAttributes]
    }
    configuredSecondaryAttributes(device).each { attributeName ->
        def threshold = positiveHoursMillis(settings?.get("attributeThreshold_${deviceKey(device)}_${attributeName}".toString()))
        if (threshold) desired["attribute:${attributeName}".toString()] = [threshold: threshold, attribute: attributeName]
    }
    def previousMonitors = status.freshnessMonitors instanceof Map ? status.freshnessMonitors : [:]
    def previousAlerts = status.issueAlerts instanceof Map ? status.issueAlerts : [:]
    def monitors = [:]
    def alerts = [:]
    desired.each { key, config ->
        def previous = previousMonitors[key] instanceof Map ? previousMonitors[key] : [:]
        def unchanged = previous.threshold == config.threshold && previous.attributes == config.attributes && previous.attribute == config.attribute
        monitors[key] = config + [started: unchanged ? (validEventDate(previous.started, now) ?: now) : now]
        if (unchanged && validEventDate(previousAlerts[key], now)) alerts[key] = validEventDate(previousAlerts[key], now)
    }
    status.freshnessMonitors = monitors
    status.issueAlerts = alerts
    status.lastAlert = validEventDate(status.lastAlert, now)
    return status
}

private Long positiveHoursMillis(def value) {
    if (value == null || value.toString().trim() == "") return null
    try {
        def hours = new BigDecimal(value.toString())
        if (hours < 0.001G || hours > 1000000G) return null
        return (hours * 3600000G).longValueExact()
    } catch (ignored) {
        return null
    }
}

private List<String> availableSecondaryAttributes(def device) {
    def primary = configuredPrimaryAttributes(device)
    return MONITORED_ATTRIBUTES.findAll { hasAttribute(device, it) && !primary.contains(it) }
}

private List<String> configuredSecondaryAttributes(def device) {
    def value = settings?.get("secondaryAttributes_${deviceKey(device)}".toString())
    def values = value instanceof Collection ? value : (value ? [value] : [])
    def names = values.collect { it?.toString() }
    return availableSecondaryAttributes(device).findAll { names.contains(it) }
}

private safeCurrentState(def device, String attributeName) {
    if (!hasAttribute(device, attributeName)) return null
    try {
        return device.currentState(attributeName)
    } catch (ignored) {
        return null
    }
}

private Date validEventDate(def value, Date now) {
    def date = asDate(value, null)
    return date && date.time > 0 && date.time <= now.time ? date : null
}

private List monitoredDeviceList() {
    return selectedDeviceList().findAll { isRealHardwareDevice(it) && hasAttribute(it, "battery") }
}

private List selectedDeviceList() {
    def devices = settings?.monitoredDevices ?: settings?.temperatureDevices
    if (!devices) return []
    if (devices instanceof Collection) return devices.findAll { it != null }
    return [devices]
}

private String deviceKey(def device) {
    return device?.id?.toString()
}

private String primaryAttributeSettingName(def device) {
    def key = deviceKey(device)
    return key ? "primaryAttributes_${key}" : null
}

private List<String> availablePrimaryAttributes(def device) {
    return PRIMARY_ATTRIBUTE_CANDIDATES.findAll { hasAttribute(device, it) }
}

private List<String> configuredPrimaryAttributes(def device) {
    def settingName = primaryAttributeSettingName(device)
    def configured = settingName ? settings?.get(settingName) : null
    return normalizePrimaryAttributes(configured).findAll { hasAttribute(device, it) }
}

private List<String> normalizePrimaryAttributes(def value) {
    if (!value) return []
    def values = value instanceof Collection ? value : [value]
    def names = values.collect { it?.toString() }
    return PRIMARY_ATTRIBUTE_CANDIDATES.findAll { names.contains(it) }
}

private Map reconciledPrimaryTracking(def device, def existingStatus, Date now = new Date()) {
    def configuredAttributes = configuredPrimaryAttributes(device)
    if (!configuredAttributes) {
        return [
            primaryAttributes: [],
            lastPrimaryEvent: null,
            lastPrimaryEventName: null,
            lastPrimaryEventValue: null
        ]
    }

    def existingAttributes = normalizePrimaryAttributes(existingStatus?.primaryAttributes)
    def currentPrimaryState = mostRecentCurrentState(device, configuredAttributes, now)
    def knownHistory = existingStatus?.attributeHistory instanceof Map ? existingStatus.attributeHistory : [:]
    configuredAttributes.each { attributeName ->
        def entry = knownHistory[attributeName] instanceof Map ? knownHistory[attributeName] : [:]
        def date = validEventDate(entry.date, now)
        if (date && (!currentPrimaryState || date.time > currentPrimaryState.date.time)) {
            currentPrimaryState = [name: attributeName, value: entry.value, date: date]
        }
    }
    def currentPrimaryDate = validEventDate(currentPrimaryState?.date, now)
    def existingPrimaryDate = configuredAttributes == existingAttributes ? validEventDate(existingStatus?.lastPrimaryEvent, now) : null

    if (existingPrimaryDate && (!currentPrimaryDate || existingPrimaryDate.time >= currentPrimaryDate.time)) {
        return [
            primaryAttributes: configuredAttributes,
            lastPrimaryEvent: existingPrimaryDate,
            lastPrimaryEventName: existingStatus.lastPrimaryEventName,
            lastPrimaryEventValue: existingStatus.lastPrimaryEventValue
        ]
    }

    return [
        primaryAttributes: configuredAttributes,
        lastPrimaryEvent: currentPrimaryDate,
        lastPrimaryEventName: currentPrimaryState?.name,
        lastPrimaryEventValue: currentPrimaryState?.value
    ]
}

private currentBatteryValue(def device) {
    return valueOrDefault(safeCurrentValue(device, "battery"), "N/A")
}

private currentTemperatureValue(def device) {
    return safeCurrentValue(device, "temperature")
}

private currentLastBatteryValue(def device) {
    return safeCurrentValue(device, "lastBattery")
}

private safeCurrentValue(def device, String attributeName) {
    if (!hasAttribute(device, attributeName)) return null
    try { return device.currentValue(attributeName) } catch (ignored) { return null }
}

private boolean hasAttribute(def device, String attributeName) {
    try {
        return device?.hasAttribute(attributeName) == true
    } catch (ignored) {
        return false
    }
}

private boolean isRealHardwareDevice(def device) {
    return device && !isVirtualDevice(device) && !isCustomDevice(device)
}

private boolean isVirtualDevice(def device) {
    String typeName = safeDeviceValue(device, "typeName")?.toString()?.toLowerCase()
    String deviceNetworkId = safeDeviceValue(device, "deviceNetworkId")?.toString()?.toLowerCase()

    return typeName?.contains("virtual") || deviceNetworkId?.startsWith("virtual")
}

private boolean isCustomDevice(def device) {
    String typeName = safeDeviceValue(device, "typeName")?.toString()?.toLowerCase()
    String namespace = safeDeviceValue(device, "namespace")?.toString()?.toLowerCase()

    if (typeName?.contains("custom")) return true
    if (!namespace) return false
    return !namespace.startsWith("hubitat")
}

private safeDeviceValue(def device, String propertyName) {
    if (!device) return null

    try {
        def value = device."${propertyName}"
        if (value != null) return value
    } catch (ignored) {
    }

    try {
        String methodName = "get${propertyName[0].toUpperCase()}${propertyName.substring(1)}"
        def value = device."${methodName}"()
        if (value != null) return value
    } catch (ignored) {
    }

    try {
        return device.getDataValue(propertyName)
    } catch (ignored) {
        return null
    }
}

private Map mostRecentCurrentState(def device) {
    return mostRecentCurrentState(device, MONITORED_ATTRIBUTES)
}

private Map mostRecentCurrentState(def device, Collection<String> attributeNames, Date now = new Date()) {
    Date latest = null
    Map latestState = null
    attributeNames.each { attributeName ->
        if (hasAttribute(device, attributeName)) {
            def currentState = safeCurrentState(device, attributeName)
            Date date = validEventDate(currentState?.date, now)
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

private Date latestDate(List values, Date fallback, Date now = new Date()) {
    Date latest = null
    values.each { value ->
        Date date = validEventDate(value, now)
        if (date && (!latest || date.time > latest.time)) {
            latest = date
        }
    }
    return latest ?: fallback
}

private Date asDate(def value, Date fallback = null) {
    if (value instanceof Date) return value
    if (value == null) return fallback

    if (value instanceof Number) {
        try { return new Date(new BigDecimal(value.toString()).longValueExact()) } catch (ignored) { return fallback }
    }
    String stringValue = value.toString().trim()
    for (String pattern : ["yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX"]) {
        try {
            def formatter = new java.text.SimpleDateFormat(pattern)
            formatter.setLenient(false)
            def position = new java.text.ParsePosition(0)
            def parsed = formatter.parse(stringValue, position)
            if (parsed && position.index == stringValue.length()) return parsed
        } catch (ignored) { }
    }
    return fallback
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
