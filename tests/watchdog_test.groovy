// Local behavioral checks against the actual app source. No Hubitat or notifications.
import java.time.Instant

class FakeDevice {
    String id = '42'
    String displayName = 'Test sensor'
    String typeName = 'Generic Zigbee Sensor'
    String namespace = 'hubitat'
    String deviceNetworkId = '1234'
    Map states = [:]
    Set exposed = ['battery', 'temperature', 'contact', 'humidity'] as Set
    boolean hasAttribute(String name) { exposed.contains(name) }
    def currentState(String name) { states[name] }
    def currentValue(String name) { states[name]?.value }
    def getDataValue(String name) { null }
}

class UnavailableDevice extends FakeDevice {
    def currentState(String name) { throw new IllegalStateException('Unavailable state') }
    def currentValue(String name) { throw new IllegalStateException('Unavailable value') }
}

def warnings = []
def notifications = []
def schedules = []
def subscriptions = []
def inputs = []
def settings = [:]
def state = [:]
def binding = new Binding([
    settings: settings, state: state, location: [timeZone: TimeZone.getTimeZone('America/Los_Angeles')],
    enableDebug: false, inactiveThreshold: 24, scheduleInterval: null, sendPush: false,
    notifierDevice: [deviceNotification: { String text -> notifications << text }],
    log: [warn: { text -> warnings << text }, debug: { text -> }, error: { text -> throw new AssertionError(text) }],
    definition: { Map config -> }, preferences: { Closure body -> },
    section: { String title, Closure body -> body() }, paragraph: { String text -> },
    input: { Map options, String name, String type -> inputs << [name: name, type: type, options: options] },
    schedule: { expression, method -> schedules << [expression, method] },
    deviceEventHandler: 'deviceEventHandler',
    subscribe: { device, name, handler -> subscriptions << name },
    unschedule: { schedules.clear() }, unsubscribe: { subscriptions.clear() }
])
def source = new File(args ? args[0] : 'dead_battery_watchdog_hubitat_app.groovy')
def app = new GroovyShell(binding).parse(source)
app.run()
int checks = 0
def check = { String name, Closure body ->
    body()
    checks++
    println "PASS ${name}"
}
def now = new Date()
long hour = 3600000L
def ago = { Number hours -> new Date(now.time - (hours * hour).longValue()) }
def device = new FakeDevice()
settings.monitoredDevices = [device]
settings.primaryAttributes_42 = ['contact']

check('blank, invalid, bounded and fractional thresholds') {
    [null, '', 'bad', 0, -1, 'NaN', 'Infinity', 1000001, '0.0001', '0.00100001'].each {
        assert app.positiveHoursMillis(it) == null
    }
    assert app.positiveHoursMillis('1.5') == 5400000L
    assert app.positiveHoursMillis('0.001') == 3600L
}
check('strict timestamps accept persisted formats and reject future/malformed dates') {
    assert app.validEventDate(ago(1), now) == ago(1)
    assert app.asDate("2026-10-10T10:30:01.123Z") != null
    assert app.asDate("2026-10-10T10:30:01+0000") != null
    assert app.asDate("2026-10-10T10:30:01.123+00:00") != null
    assert app.asDate('2026-02-31T10:00:00Z') == null
    assert app.asDate('2026-10-10T10:30:01Zjunk') == null
    assert app.validEventDate(new Date(now.time + 1), now) == null
    assert app.validEventDate(-1, now) == null
}
check('upgrade preserves legacy migrations without inventing primary history') {
    def s = app.reconciledDeviceStatus(device, [lastChange: ago(30), lastAlert: ago(2)], now)
    assert s.lastAnyEvent == ago(30)
    assert s.lastReport == ago(30)
    assert s.lastPrimaryEvent == null
    assert s.lastAlert == ago(2)
    assert s.freshnessMonitors == [:]
    assert app.evaluateHealth(s, 24 * hour, now).device == 'silence'
}
check('cached percentage without a valid event timestamp cannot prove reporting') {
    device.states = [battery: [value: 100, date: null]]
    def s = app.reconciledDeviceStatus(device, [:], now)
    assert s.batteryLevel == 100
    assert s.lastAnyEvent == null
    assert app.evaluateHealth(s, hour, now).device == 'unknown'
    def later = new Date(now.time + 2 * hour)
    assert app.evaluateHealth(s, hour, later).device == 'silence'
    assert app.reconciledDeviceStatus(device, s, later).observationStarted == now
}
check('recent contact and stale temperature/battery produce attribute issues') {
    settings.secondaryAttributes_42 = ['temperature', 'battery', 'contact', 'unsupported']
    settings.attributeThreshold_42_temperature = 12
    settings.attributeThreshold_42_battery = 24
    device.states = [contact: [date: ago(1), value: 'open'], temperature: [date: ago(20), value: 19], battery: [date: ago(30), value: 100]]
    def s = app.reconciledDeviceStatus(device, [:], now)
    assert s.freshnessMonitors.keySet() == ['attribute:temperature', 'attribute:battery'] as Set
    def health = app.evaluateHealth(s, 24 * hour, now)
    assert health.device == 'reporting'
    assert health.issues*.key as Set == ['attribute:temperature', 'attribute:battery'] as Set
}
check('primary clock is refreshed by any selected primary attribute') {
    settings.primaryAttributes_42 = ['temperature', 'humidity']
    settings.primaryThreshold_42 = 12
    device.states = [temperature: [date: ago(30), value: 20], humidity: [date: ago(1), value: 45]]
    def s = app.reconciledDeviceStatus(device, [:], now)
    assert s.lastPrimaryEventName == 'humidity'
    assert !app.evaluateHealth(s, 24 * hour, now).issues.any { it.key == 'primary' }
}
check('primary staleness coexists with secondary staleness while reporting continues') {
    settings.primaryAttributes_42 = ['contact']
    device.states = [contact: [date: ago(20), value: 'open'], temperature: [date: ago(20), value: 19], battery: [date: ago(1), value: 100]]
    def s = app.reconciledDeviceStatus(device, [:], now)
    assert app.evaluateHealth(s, 24 * hour, now).issues*.key as Set == ['primary', 'attribute:temperature'] as Set
}
check('missing history uses a stable observation period rather than invented events') {
    device.states = [battery: [date: ago(1), value: 80]]
    def s = app.reconciledDeviceStatus(device, [:], now)
    assert s.lastPrimaryEvent == null
    assert app.evaluateHealth(s, 24 * hour, now).issues == []
    def later = new Date(now.time + 13 * hour)
    s = app.reconciledDeviceStatus(device, s, later)
    assert s.freshnessMonitors.primary.started == now
    def issue = app.evaluateHealth(s, 24 * hour, later).issues.find { it.key == 'primary' }
    assert issue.lastEvent == null && issue.started == now
}
check('configuration reconciliation preserves unchanged cooldowns and resets changed checks') {
    device.states = [battery: [date: ago(1), value: 80]]
    def s = app.reconciledDeviceStatus(device, [:], now)
    s.issueAlerts = [primary: ago(1), 'attribute:temperature': ago(1)]
    def unchanged = app.reconciledDeviceStatus(device, s, now)
    assert unchanged.issueAlerts == s.issueAlerts
    settings.primaryThreshold_42 = 48
    def changed = app.reconciledDeviceStatus(device, s, now)
    assert !changed.issueAlerts.containsKey('primary')
    assert changed.issueAlerts['attribute:temperature'] == ago(1)
    settings.primaryAttributes_42 = ['humidity']
    assert app.reconciledDeviceStatus(device, changed, now).lastPrimaryEvent == null
    settings.secondaryAttributes_42 = []
    assert app.reconciledDeviceStatus(device, changed, now).freshnessMonitors.keySet() == ['primary'] as Set
}
check('new primary selections can use only matching known attribute events') {
    device.states = [:]
    settings.primaryAttributes_42 = ['humidity']
    def s = app.reconciledDeviceStatus(device, [lastAnyEvent: ago(1), lastReport: ago(1), primaryAttributes: ['contact'], lastPrimaryEvent: ago(1), attributeHistory: [humidity: [date: ago(4), value: 45]]], now)
    assert s.lastPrimaryEvent == ago(4)
    assert s.lastPrimaryEventName == 'humidity'
    assert s.lastPrimaryEvent != s.lastAnyEvent
}
check('malformed state and future history are discarded without crashing') {
    device.states = [temperature: [date: new Date(now.time + hour), value: 99]]
    def s = app.reconciledDeviceStatus(device, [attributeHistory: [temperature: 'bad', battery: [date: 'bad']], freshnessMonitors: [primary: 'bad'], issueAlerts: [primary: 'bad'], lastPrimaryEvent: new Date(now.time + hour), primaryAttributes: ['humidity']], now)
    assert s.lastAnyEvent == null && s.lastPrimaryEvent == null
    assert s.issueAlerts == [:]
    assert app.reconciledDeviceStatus(device, 'bad', now).lastAnyEvent == null
}
check('serialized dates preserve event history, observation starts and cooldowns') {
    device.states = [battery: [date: ago(1), value: 80]]
    def s = app.reconciledDeviceStatus(device, [:], now)
    s.issueAlerts = [primary: ago(1)]
    def serialize
    serialize = { value ->
        if (value instanceof Date) return value.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", TimeZone.getTimeZone('UTC'))
        if (value instanceof Map) return value.collectEntries { key, item -> [(key): serialize(item)] }
        if (value instanceof Collection) return value.collect { serialize(it) }
        return value
    }
    def recovered = app.reconciledDeviceStatus(device, serialize(s), now)
    assert recovered.lastAnyEvent == s.lastAnyEvent
    assert recovered.observationStarted == s.observationStarted
    assert recovered.freshnessMonitors.primary.started == s.freshnessMonitors.primary.started
    assert recovered.issueAlerts.primary == ago(1)
}
check('unavailable optional states/values do not prevent device evaluation') {
    def unavailable = new UnavailableDevice()
    def s = app.reconciledDeviceStatus(unavailable, [:], now)
    assert s.lastAnyEvent == null && s.batteryLevel == 'N/A' && s.lastBattery == null
    assert app.evaluateHealth(s, 24 * hour, now).device == 'unknown'
}
check('threshold and cooldown boundaries') {
    assert !app.alertDue(ago(23), now)
    assert app.alertDue(ago(24), now)
    assert app.alertDue('bad', now)
    assert app.evaluateHealth([lastAnyEvent: ago(24), freshnessMonitors: [:]], 24 * hour, now).device == 'reporting'
    assert app.evaluateHealth([lastAnyEvent: ago(25), freshnessMonitors: [:]], 24 * hour, now).device == 'silence'
}
check('an overdue advancing event does not reset device-silence cooldown') {
    device.states = [:]
    state.deviceStatus = ['42': [lastAnyEvent: ago(40), lastAlert: ago(1), primaryAttributes: ['humidity']]]
    app.deviceEventHandler([deviceId: '42', name: 'battery', value: 80, date: ago(30)])
    assert state.deviceStatus['42'].lastAnyEvent == ago(30)
    assert state.deviceStatus['42'].lastAlert == ago(1)
}
check('events remain monotonic and ignore unselected/unsupported/invalid events') {
    settings.primaryAttributes_42 = ['contact']
    settings.secondaryAttributes_42 = ['temperature', 'battery']
    settings.primaryThreshold_42 = 12
    device.states = [:]
    state.deviceStatus = ['42': [lastAnyEvent: ago(1), lastEventName: 'contact', lastEventValue: 'open', lastReport: ago(1), lastTemp: 20, primaryAttributes: ['contact'], lastPrimaryEvent: ago(1), attributeHistory: [temperature: [date: ago(1), value: 20]], issueAlerts: ['attribute:temperature': ago(1), primary: ago(1)], freshnessMonitors: ['attribute:temperature': [threshold: 12 * hour], primary: [threshold: 12 * hour]]]]
    app.deviceEventHandler([deviceId: '42', name: 'temperature', value: 10, date: ago(20), displayName: 'Test sensor'])
    assert state.deviceStatus['42'].lastAnyEvent == ago(1)
    assert state.deviceStatus['42'].attributeHistory.temperature.value == 20
    assert state.deviceStatus['42'].issueAlerts['attribute:temperature'] == ago(1)
    app.deviceEventHandler([deviceId: '42', name: 'contact', value: 'closed', date: ago(0), displayName: 'Test sensor'])
    assert !state.deviceStatus['42'].issueAlerts.containsKey('primary')
    assert state.deviceStatus['42'].issueAlerts['attribute:temperature'] == ago(1)
    app.deviceEventHandler([deviceId: '42', name: 'temperature', value: 21, date: ago(0), displayName: 'Test sensor'])
    assert !state.deviceStatus['42'].issueAlerts.containsKey('attribute:temperature')
    def before = state.deviceStatus['42'].lastAnyEvent
    [['42', 'temperature', 'bad'], ['42', 'temperature', new Date(now.time + hour)], ['42', 'unsupported', now], ['99', 'contact', now]].each { id, name, date ->
        app.deviceEventHandler([deviceId: id, name: name, date: date, value: 'ignored'])
    }
    assert state.deviceStatus['42'].lastAnyEvent == before
}
check('scheduled warnings combine issues, throttle independently and escalate to silence') {
    warnings.clear(); notifications.clear()
    binding.sendPush = true
    device.states = [contact: [date: ago(20), value: 'closed'], temperature: [date: ago(20), value: 19], battery: [date: ago(1), value: 80]]
    state.deviceStatus = [:]
    app.checkDevices()
    assert notifications.size() == 1
    assert notifications[0].contains('Primary function') && notifications[0].contains('temperature')
    assert !notifications[0].contains('may be dead')
    app.checkDevices()
    assert notifications.size() == 1
    device.states.battery.date = ago(30)
    state.deviceStatus['42'].lastAnyEvent = ago(30)
    state.deviceStatus['42'].attributeHistory.battery.date = ago(30)
    device.states.contact.date = ago(30)
    device.states.temperature.date = ago(30)
    state.deviceStatus['42'].attributeHistory.contact.date = ago(30)
    state.deviceStatus['42'].attributeHistory.temperature.date = ago(30)
    state.deviceStatus['42'].lastReport = ago(30)
    app.checkDevices()
    assert notifications.size() == 2 && notifications[1].contains('may be dead')
    app.checkDevices()
    assert notifications.size() == 2
}
check('freshness recovery clears only its own cooldown and recurrence can warn') {
    notifications.clear()
    device.states.battery = [date: ago(1), value: 80]
    device.states.temperature = [date: ago(1), value: 20]
    state.deviceStatus['42'].lastPrimaryEvent = ago(30)
    app.checkDevices()
    assert !state.deviceStatus['42'].issueAlerts.containsKey('attribute:temperature')
    assert state.deviceStatus['42'].issueAlerts.containsKey('primary')
    device.states.temperature.date = ago(20)
    state.deviceStatus['42'].attributeHistory.temperature.date = ago(20)
    app.checkDevices()
    assert notifications.last().contains('temperature')
}
check('notification settings, missing notifier and device eligibility') {
    binding.sendPush = false
    notifications.clear()
    app.deliverWarning('Local warning')
    assert notifications == []
    binding.sendPush = true; binding.notifierDevice = null
    app.deliverWarning('No destination')
    assert warnings.last() == 'Push enabled but no notifier device selected.'
    def virtual = new FakeDevice(id: '43', typeName: 'Virtual Sensor')
    def custom = new FakeDevice(id: '44', namespace: 'custom')
    def noBattery = new FakeDevice(id: '45', exposed: ['contact'] as Set)
    settings.monitoredDevices = [device, virtual, custom, noBattery]
    assert app.monitoredDeviceList() == [device]
    settings.remove('monitoredDevices'); settings.temperatureDevices = [device]
    assert app.monitoredDeviceList() == [device]
}
check('lifecycle scheduling, subscription and legacy replacement timestamp validation') {
    [null: '0 0 * * * ?', '15': '0 0/15 * * * ?', '30': '0 0/30 * * * ?', '60': '0 0 * * * ?'].each { interval, expression ->
        binding.scheduleInterval = interval == 'null' ? null : interval
        app.updated()
        assert schedules == [[expression, 'checkDevices']]
        assert subscriptions as Set == device.exposed
    }
    assert app.formatUnixTimestamp(80) == 'N/A'
    assert app.formatUnixTimestamp(null) == 'N/A'
    assert app.formatUnixTimestamp(1700000000) == app.formatUnixTimestamp(1700000000000)
    assert app.formatLogTimestamp(new Date(0)).startsWith('1969-12-31')
}
check('configuration exposes opt-in controls and excludes primary/unsupported secondary attributes') {
    settings.primaryAttributes_42 = ['contact']
    settings.secondaryAttributes_42 = ['temperature', 'battery', 'contact', 'unsupported']
    binding.preferences = { Closure body -> body() }
    app.run()
    assert inputs.find { it.name == 'primaryThreshold_42' }.options.required == false
    assert inputs.find { it.name == 'secondaryAttributes_42' }.options.options as Set == ['temperature', 'humidity', 'battery'] as Set
    assert inputs.findAll { it.name.startsWith('attributeThreshold_') }*.name as Set == ['attributeThreshold_42_temperature', 'attributeThreshold_42_battery'] as Set
    assert !inputs.find { it.name == 'primaryThreshold_42' }.options.containsKey('defaultValue')
}
check('primary and secondary UI ranges accept whole-hour and hundredth-hour values') {
    def thresholdInputs = inputs.findAll { it.name.startsWith('primaryThreshold_') || it.name.startsWith('attributeThreshold_') }
    assert thresholdInputs.size() == 3
    thresholdInputs.each { field ->
        def bounds = field.options.range.split(/\.\./).collect { new BigDecimal(it) }
        [0.01G, 0.25G, 1G, 12G, 24G, 48G, 48.5G, 1000000G].each { hours ->
            assert hours >= bounds[0] && hours <= bounds[1]
            assert (hours - bounds[0]).remainder(0.01G) == 0G
            assert app.positiveHoursMillis(hours) != null
        }
    }
    assert (48G - 0.001G).remainder(0.01G) != 0G // Original browser step mismatch.
    assert app.positiveHoursMillis(0.001G) == 3600L // Preserve previously saved thresholds.
}
println "${checks} behavioral scenarios passed against ${source.name}"
