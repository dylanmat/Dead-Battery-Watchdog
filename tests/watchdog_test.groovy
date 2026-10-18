// Execute the actual app source with synthetic Hubitat devices and intercepted notifications.
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

def warnings = [], notifications = [], schedules = [], subscriptions = [], inputs = [], sections = []
def settings = [:], state = [:]
def binding = new Binding([
    settings: settings, state: state, location: [timeZone: TimeZone.getTimeZone('America/Los_Angeles')],
    enableDebug: false, inactiveThreshold: 24, scheduleInterval: null, sendPush: false,
    notifierDevice: [deviceNotification: { String text -> notifications << text }],
    log: [warn: { text -> warnings << text }, debug: { text -> }, error: { text -> throw new AssertionError(text) }],
    definition: { Map config -> }, preferences: { Closure body -> },
    section: { String title, Closure body -> sections << title; body() }, paragraph: { String text -> },
    input: { Map options, String name, String type -> inputs << [name: name, type: type, options: options] },
    schedule: { expression, method -> schedules << [expression, method] }, deviceEventHandler: 'deviceEventHandler',
    subscribe: { device, name, handler -> subscriptions << name },
    unschedule: { schedules.clear() }, unsubscribe: { subscriptions.clear() }
])
def source = new File(args ? args[0] : 'dead_battery_watchdog_hubitat_app.groovy')
def app = new GroovyShell(binding).parse(source)
app.run()
def now = new Date()
long hour = 3600000L
def ago = { Number hours -> new Date(now.time - (hours * hour).longValue()) }
def device = new FakeDevice()
int checks = 0
def check = { String name, Closure body ->
    settings.clear(); state.clear(); warnings.clear(); notifications.clear()
    schedules.clear(); subscriptions.clear(); inputs.clear(); sections.clear()
    device.states = [:]
    device.exposed = ['battery', 'temperature', 'contact', 'humidity'] as Set
    settings.monitoredDevices = [device]
    binding.sendPush = false; binding.inactiveThreshold = 24; binding.scheduleInterval = null
    binding.notifierDevice = [deviceNotification: { String text -> notifications << text }]
    binding.preferences = { Closure ignored -> }
    body()
    checks++
    println 'PASS ' + name
}

check('priority order selects first two exposed signals, not device purpose') {
    assert app.availableSignals(device) == ['temperature', 'humidity', 'contact', 'battery']
    assert app.configuredSignal(device, 'primary') == 'temperature'
    assert app.configuredSignal(device, 'secondary') == 'humidity'
    device.exposed = ['battery', 'temperature', 'illuminance', 'humidity', 'ultravioletIndex', 'motion', 'acceleration', 'threeAxis', 'contact', 'pushed', 'water', 'smoke', 'pressure'] as Set
    assert app.availableSignals(device) == ['temperature', 'illuminance', 'humidity', 'ultravioletIndex', 'motion', 'acceleration', 'threeAxis', 'contact', 'buttonActivity', 'battery', 'water', 'smoke', 'pressure']
    app.availableSignals(device).each { assert app.configuredSignalThreshold(device, it) == 24 * hour }
}
check('missing attributes fall through in approved order') {
    device.exposed = ['motion', 'battery'] as Set
    assert app.configuredSignal(device, 'primary') == 'motion'
    assert app.configuredSignal(device, 'secondary') == 'battery'
    device.exposed = ['battery', 'water', 'smoke', 'pressure'] as Set
    assert app.configuredSignal(device, 'primary') == 'battery'
    assert app.configuredSignal(device, 'secondary') == 'water'
    device.exposed = ['battery', 'smoke', 'pressure'] as Set
    assert app.configuredSignal(device, 'secondary') == 'smoke'
}
check('single-attribute devices get one check and no duplicate secondary') {
    device.exposed = ['battery'] as Set
    def s = app.reconciledDeviceStatus(device, [:], now)
    assert app.configuredSignal(device, 'primary') == 'battery'
    assert app.configuredSignal(device, 'secondary') == null
    assert s.freshnessMonitors.keySet() == ['primary'] as Set
}
check('all ranked signals default to 24 hours and new checks are enabled') {
    def s = app.reconciledDeviceStatus(device, [:], now)
    assert s.freshnessMonitors.keySet() == ['primary', 'attribute:humidity'] as Set
    assert s.freshnessMonitors.values().every { it.threshold == 24 * hour }
    app.availableSignals(device).each { assert app.configuredSignalThreshold(device, it) == 24 * hour }
}
check('legacy selections and thresholds are replaced without copying legacy times to primary') {
    settings.primaryAttributes_42 = ['contact']
    settings.secondaryAttributes_42 = ['battery']
    settings.primaryThreshold_42 = 96
    settings.attributeThreshold_42_humidity = 96
    def s = app.reconciledDeviceStatus(device, [lastChange: ago(30), lastAlert: ago(2), primaryAttributes: ['contact'], lastPrimaryEvent: ago(1)], now)
    assert s.primaryAttributes == ['temperature']
    assert s.lastPrimaryEvent == null
    assert s.lastAnyEvent == ago(30) && s.lastReport == ago(30)
    assert s.lastAlert == ago(2)
    assert s.freshnessMonitors.values().every { it.threshold == 24 * hour }
}
check('primary/secondary overrides are validated and cannot duplicate signals') {
    settings.reportingSignal_42_primary = 'contact'
    settings.reportingSignal_42_secondary = 'battery'
    assert app.configuredPrimaryAttributes(device) == ['contact']
    assert app.configuredSecondaryAttributes(device) == ['battery']
    settings.reportingSignal_42_secondary = 'contact'
    assert app.configuredSignal(device, 'secondary') == 'temperature'
    settings.reportingSignal_42_primary = 'unsupported'
    assert app.configuredSignal(device, 'primary') == 'temperature'
    device.exposed.remove('temperature')
    assert app.configuredSignal(device, 'primary') == 'humidity'
}
check('None disables individual checks and Automatic restores ranking') {
    settings.reportingSignal_42_primary = 'none'
    settings.reportingSignal_42_secondary = 'none'
    assert app.reconciledDeviceStatus(device, [:], now).freshnessMonitors == [:]
    settings.reportingSignal_42_primary = 'auto'
    settings.reportingSignal_42_secondary = 'auto'
    assert app.configuredSignal(device, 'primary') == 'temperature'
    assert app.configuredSignal(device, 'secondary') == 'humidity'
}
check('threshold overrides follow attributes when roles change') {
    settings.reportingHours_42_temperature = 48
    settings.reportingHours_42_humidity = 72
    def s = app.reconciledDeviceStatus(device, [:], now)
    assert s.freshnessMonitors.primary.threshold == 48 * hour
    assert s.freshnessMonitors['attribute:humidity'].threshold == 72 * hour
    settings.reportingSignal_42_primary = 'humidity'
    settings.reportingSignal_42_secondary = 'temperature'
    s = app.reconciledDeviceStatus(device, s, now)
    assert s.freshnessMonitors.primary.threshold == 72 * hour
    assert s.freshnessMonitors['attribute:temperature'].threshold == 48 * hour
}
check('blank or invalid new threshold settings use the shared 24-hour default') {
    [null, '', 'bad', 0, -1, 'NaN', 1000001].each {
        settings.reportingHours_42_temperature = it
        assert app.configuredSignalThreshold(device, 'temperature') == 24 * hour
    }
    assert app.positiveHoursMillis('1.5') == 5400000L
    assert app.positiveHoursMillis('0.001') == 3600L
    assert app.positiveHoursMillis('0.00100001') == null
}
check('strict timestamps accept persisted formats and reject invalid/future dates') {
    assert app.validEventDate(ago(1), now) == ago(1)
    ['2026-10-10T10:30:01.123Z', '2026-10-10T10:30:01+0000', '2026-10-10T10:30:01.123+00:00'].each { assert app.asDate(it) != null }
    assert app.asDate('2026-02-31T10:00:00Z') == null
    assert app.asDate('2026-10-10T10:30:01Zjunk') == null
    assert app.validEventDate(new Date(now.time + 1), now) == null
    assert app.validEventDate(-1, now) == null
}
check('cached battery percentage without a valid event date does not establish reporting') {
    device.states = [battery: [value: 100, date: null]]
    def s = app.reconciledDeviceStatus(device, [:], now)
    assert s.batteryLevel == 100 && s.lastAnyEvent == null && s.lastPrimaryEvent == null
    assert app.evaluateHealth(s, hour, now).device == 'unknown'
    def later = new Date(now.time + 2 * hour)
    assert app.evaluateHealth(s, hour, later).device == 'silence'
    assert app.reconciledDeviceStatus(device, s, later).observationStarted == now
}
check('missing primary and secondary history use stable observation periods') {
    device.states = [contact: [date: ago(1), value: 'open']]
    def s = app.reconciledDeviceStatus(device, [:], now)
    assert app.evaluateHealth(s, 48 * hour, now).issues == []
    def later = new Date(now.time + 25 * hour)
    s = app.reconciledDeviceStatus(device, s, later)
    def health = app.evaluateHealth(s, 48 * hour, later)
    assert health.device == 'reporting'
    assert health.issues*.key as Set == ['primary', 'attribute:humidity'] as Set
    assert health.issues.every { it.lastEvent == null && it.started == now }
}
check('unchanged checks preserve cooldowns and changed/removed checks reset independently') {
    def s = app.reconciledDeviceStatus(device, [:], now)
    s.issueAlerts = [primary: ago(1), 'attribute:humidity': ago(1)]
    assert app.reconciledDeviceStatus(device, s, now).issueAlerts == s.issueAlerts
    settings.reportingHours_42_temperature = 48
    def changed = app.reconciledDeviceStatus(device, s, now)
    assert !changed.issueAlerts.containsKey('primary')
    assert changed.issueAlerts['attribute:humidity'] == ago(1)
    settings.reportingSignal_42_secondary = 'none'
    assert app.reconciledDeviceStatus(device, changed, now).freshnessMonitors.keySet() == ['primary'] as Set
}
check('selection changes seed primary only from matching real attribute history') {
    settings.reportingSignal_42_primary = 'humidity'
    def s = app.reconciledDeviceStatus(device, [lastAnyEvent: ago(1), lastReport: ago(1), primaryAttributes: ['contact'], lastPrimaryEvent: ago(1), attributeHistory: [humidity: [date: ago(4), value: 45]]], now)
    assert s.lastPrimaryEvent == ago(4) && s.lastPrimaryEventName == 'humidity'
    assert s.lastPrimaryEvent != s.lastAnyEvent
}
check('button events form a single ranked reporting signal before battery') {
    device.exposed = ['pushed', 'held', 'released', 'doubleTapped', 'battery'] as Set
    assert app.availableSignals(device) == ['buttonActivity', 'battery']
    assert app.configuredPrimaryAttributes(device) == ['pushed', 'held', 'released', 'doubleTapped']
    device.states = [pushed: [date: ago(30), value: 1], held: [date: ago(1), value: 1]]
    def s = app.reconciledDeviceStatus(device, [:], now)
    assert s.lastPrimaryEventName == 'held'
    assert !app.evaluateHealth(s, 24 * hour, now).issues.any { it.key == 'primary' }
}
check('secondary Button activity uses any exposed button event and recovers independently') {
    device.exposed = ['temperature', 'held', 'doubleTapped', 'battery'] as Set
    device.states = [temperature: [date: ago(30), value: 20], held: [date: ago(30), value: 1], battery: [date: ago(1), value: 80]]
    def s = app.reconciledDeviceStatus(device, [:], now)
    assert s.freshnessMonitors['attribute:buttonActivity'].attributes == ['held', 'doubleTapped']
    assert app.evaluateHealth(s, 24 * hour, now).issues*.key as Set == ['primary', 'attribute:buttonActivity'] as Set
    s.issueAlerts = [primary: ago(1), 'attribute:buttonActivity': ago(1)]
    state.deviceStatus = ['42': s]
    app.deviceEventHandler([deviceId: '42', name: 'doubleTapped', value: 1, date: now])
    assert !state.deviceStatus['42'].issueAlerts.containsKey('attribute:buttonActivity')
    assert state.deviceStatus['42'].issueAlerts.containsKey('primary')
    s = app.reconciledDeviceStatus(device, state.deviceStatus['42'], now)
    assert !app.evaluateHealth(s, 24 * hour, now).issues.any { it.key == 'attribute:buttonActivity' }
}
check('each exposed primary button event refreshes the grouped clock without inventing history') {
    device.exposed = ['pushed', 'held', 'released', 'doubleTapped', 'battery'] as Set
    state.deviceStatus = ['42': app.reconciledDeviceStatus(device, [:], now)]
    assert state.deviceStatus['42'].lastPrimaryEvent == null
    ['pushed', 'held', 'released', 'doubleTapped'].each { attribute ->
        app.deviceEventHandler([deviceId: '42', name: attribute, value: 1, date: now])
        assert state.deviceStatus['42'].lastPrimaryEventName == attribute
        assert state.deviceStatus['42'].lastPrimaryEvent == now
    }
}
check('ultraviolet, 3-axis and pressure events are subscribed and recorded') {
    device.exposed = ['battery', 'ultravioletIndex', 'threeAxis', 'pressure'] as Set
    app.updated()
    assert subscriptions as Set == device.exposed
    ['ultravioletIndex', 'threeAxis', 'pressure'].each {
        app.deviceEventHandler([deviceId: '42', name: it, value: it == 'threeAxis' ? [x: 1, y: 2, z: 3] : 10, date: now])
        assert state.deviceStatus['42'].attributeHistory[it].date == now
    }
}
check('other supported events count for liveness without entering the automatic ranking') {
    device.exposed = ['battery', 'carbonMonoxide', 'switch'] as Set
    assert app.availableSignals(device) == ['battery']
    device.states = [battery: [date: ago(30), value: 80], carbonMonoxide: [date: ago(1), value: 'clear']]
    def s = app.reconciledDeviceStatus(device, [:], now)
    assert app.evaluateHealth(s, 24 * hour, now).device == 'reporting'
    assert app.evaluateHealth(s, 24 * hour, now).issues*.key == ['primary']
}
check('malformed state and future history are discarded safely') {
    device.states = [temperature: [date: new Date(now.time + hour), value: 99]]
    def s = app.reconciledDeviceStatus(device, [attributeHistory: [temperature: 'bad', battery: [date: 'bad']], freshnessMonitors: [primary: 'bad'], issueAlerts: [primary: 'bad'], lastPrimaryEvent: new Date(now.time + hour), primaryAttributes: ['temperature']], now)
    assert s.lastAnyEvent == null && s.lastPrimaryEvent == null && s.issueAlerts == [:]
    assert app.reconciledDeviceStatus(device, 'bad', now).lastAnyEvent == null
}
check('serialized dates retain known history, observation starts and cooldowns') {
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
    assert recovered.lastAnyEvent == s.lastAnyEvent && recovered.observationStarted == s.observationStarted
    assert recovered.freshnessMonitors.primary.started == s.freshnessMonitors.primary.started
    assert recovered.issueAlerts.primary == ago(1)
}
check('unavailable optional states and values do not prevent evaluation') {
    def s = app.reconciledDeviceStatus(new UnavailableDevice(), [:], now)
    assert s.lastAnyEvent == null && s.batteryLevel == 'N/A' && s.lastBattery == null
    assert app.evaluateHealth(s, 24 * hour, now).device == 'unknown'
}
check('threshold boundaries and 24-hour cooldowns are preserved') {
    assert !app.alertDue(ago(23), now) && app.alertDue(ago(24), now)
    assert app.alertDue('bad', now)
    assert app.evaluateHealth([lastAnyEvent: ago(24), freshnessMonitors: [:]], 24 * hour, now).device == 'reporting'
    assert app.evaluateHealth([lastAnyEvent: ago(25), freshnessMonitors: [:]], 24 * hour, now).device == 'silence'
}
check('overdue advancing events do not reset device-silence cooldowns') {
    state.deviceStatus = ['42': [lastAnyEvent: ago(40), lastAlert: ago(1), primaryAttributes: ['temperature']]]
    app.deviceEventHandler([deviceId: '42', name: 'battery', value: 80, date: ago(30)])
    assert state.deviceStatus['42'].lastAnyEvent == ago(30)
    assert state.deviceStatus['42'].lastAlert == ago(1)
}
check('events remain monotonic and invalid/unselected/unsupported events are ignored') {
    def s = app.reconciledDeviceStatus(device, [lastAnyEvent: ago(1), lastReport: ago(1), lastTemp: 20, primaryAttributes: ['temperature'], lastPrimaryEvent: ago(1), attributeHistory: [temperature: [date: ago(1), value: 20]]], now)
    s.issueAlerts = [primary: ago(1), 'attribute:humidity': ago(1)]
    state.deviceStatus = ['42': s]
    app.deviceEventHandler([deviceId: '42', name: 'temperature', value: 10, date: ago(30)])
    assert state.deviceStatus['42'].attributeHistory.temperature.value == 20
    assert state.deviceStatus['42'].issueAlerts.primary == ago(1)
    app.deviceEventHandler([deviceId: '42', name: 'temperature', value: 21, date: now])
    assert !state.deviceStatus['42'].issueAlerts.containsKey('primary')
    assert state.deviceStatus['42'].issueAlerts['attribute:humidity'] == ago(1)
    [['42', 'temperature', 'bad'], ['42', 'temperature', new Date(now.time + hour)], ['42', 'unsupported', now], ['99', 'contact', now]].each { id, name, date ->
        app.deviceEventHandler([deviceId: id, name: name, date: date, value: 'ignored'])
    }
    assert state.deviceStatus['42'].lastAnyEvent == now
}
check('scheduled freshness warnings combine, throttle and escalate independently') {
    binding.sendPush = true
    device.states = [temperature: [date: ago(30), value: 19], humidity: [date: ago(30), value: 45], contact: [date: ago(1), value: 'open']]
    app.checkDevices()
    assert notifications.size() == 1
    assert notifications[0].contains('Primary reporting signal') && notifications[0].contains('Humidity')
    app.checkDevices()
    assert notifications.size() == 1
    device.states.contact.date = ago(30)
    state.deviceStatus['42'].lastAnyEvent = ago(30)
    state.deviceStatus['42'].attributeHistory.contact.date = ago(30)
    app.checkDevices()
    assert notifications.size() == 2 && notifications[1].contains('may be dead')
    app.checkDevices()
    assert notifications.size() == 2
}
check('recovery clears only the recovered issue and recurrence can warn again') {
    binding.sendPush = true
    device.states = [temperature: [date: ago(30), value: 19], humidity: [date: ago(30), value: 45], contact: [date: ago(1), value: 'open']]
    app.checkDevices()
    device.states.humidity.date = ago(1)
    app.checkDevices()
    assert state.deviceStatus['42'].issueAlerts.containsKey('primary')
    assert !state.deviceStatus['42'].issueAlerts.containsKey('attribute:humidity')
    device.states.humidity.date = ago(30)
    state.deviceStatus['42'].attributeHistory.humidity.date = ago(30)
    app.checkDevices()
    assert notifications.size() == 2 && notifications.last().contains('Humidity')
}
check('the device-wide 24-hour threshold is independent of per-signal overrides') {
    settings.reportingHours_42_temperature = 48
    settings.reportingHours_42_humidity = 72
    device.states = [temperature: [date: ago(30), value: 20], humidity: [date: ago(30), value: 45]]
    def s = app.reconciledDeviceStatus(device, [:], now)
    def health = app.evaluateHealth(s, 24 * hour, now)
    assert health.device == 'silence' && health.issues == []
}
check('notification settings and missing destinations remain respected') {
    app.deliverWarning('Local warning')
    assert notifications == []
    binding.sendPush = true; binding.notifierDevice = null
    app.deliverWarning('No destination')
    assert warnings.last() == 'Push enabled but no notifier device selected.'
}
check('hardware eligibility and legacy device-selection settings remain supported') {
    settings.monitoredDevices = [device, new FakeDevice(id: '43', typeName: 'Virtual Sensor'), new FakeDevice(id: '44', namespace: 'custom'), new FakeDevice(id: '45', exposed: ['contact'] as Set)]
    assert app.monitoredDeviceList() == [device]
    settings.remove('monitoredDevices'); settings.temperatureDevices = [device]
    assert app.monitoredDeviceList() == [device]
}
check('all schedules, resubscription and replacement timestamp/timezone formatting work') {
    ['15': '0 0/15 * * * ?', '30': '0 0/30 * * * ?', '60': '0 0 * * * ?'].each { interval, expression ->
        binding.scheduleInterval = interval
        app.updated()
        assert schedules == [[expression, 'checkDevices']]
        assert subscriptions as Set == device.exposed
    }
    assert app.formatUnixTimestamp(80) == 'N/A' && app.formatUnixTimestamp(null) == 'N/A'
    assert app.formatUnixTimestamp(1700000000) == app.formatUnixTimestamp(1700000000000)
    assert app.formatLogTimestamp(new Date(0)).startsWith('1969-12-31')
}
check('each device appears once with two editable selectors and attribute-specific thresholds') {
    binding.preferences = { Closure body -> body() }
    app.run()
    assert sections.count(device.displayName) == 1
    assert inputs*.name as Set == ['monitoredDevices', 'reportingSignal_42_primary', 'reportingHours_42_temperature', 'reportingSignal_42_secondary', 'reportingHours_42_humidity', 'inactiveThreshold', 'scheduleInterval', 'enableDebug', 'sendPush', 'notifierDevice'] as Set
    def secondary = inputs.find { it.name == 'reportingSignal_42_secondary' }
    assert !secondary.options.options.containsKey('temperature')
    assert secondary.options.options.auto == 'Automatic (Humidity)'
    assert inputs.findAll { it.name.startsWith('reportingHours_') }.every { it.options.defaultValue == 24 && it.options.required }
    assert inputs.findAll { it.name.startsWith('reportingSignal_') }.every { it.options.defaultValue == 'auto' && it.options.multiple != true }
    assert inputs.findAll { it.name.startsWith('reportingSignal_') || it.name.startsWith('reportingHours_') }.sum { it.options.width } == 12
}
check('both threshold fields accept whole hours including 48 on the browser step grid') {
    binding.preferences = { Closure body -> body() }
    app.run()
    def thresholdInputs = inputs.findAll { it.name.startsWith('reportingHours_') }
    assert thresholdInputs.size() == 2
    thresholdInputs.each { field ->
        def bounds = field.options.range.split(/\.\./).collect { new BigDecimal(it) }
        [0.01G, 0.25G, 1G, 24G, 48G, 48.5G, 1000000G].each { hours ->
            assert hours >= bounds[0] && hours <= bounds[1]
            assert (hours - bounds[0]).remainder(0.01G) == 0G
            assert app.positiveHoursMillis(hours) != null
        }
    }
}
check('battery-only and no-secondary layouts do not emit duplicate threshold controls') {
    device.exposed = ['battery'] as Set
    binding.preferences = { Closure body -> body() }
    app.run()
    assert sections.count(device.displayName) == 1
    assert inputs.findAll { it.name.startsWith('reportingHours_') }*.name == ['reportingHours_42_battery']
    assert inputs*.name.size() == inputs*.name.unique().size()
}
check('multiple devices each get one group with separate controls and defaults') {
    def other = new FakeDevice(id: '73', displayName: 'Other test sensor', exposed: ['motion', 'battery'] as Set)
    settings.monitoredDevices = [device, other]
    settings.reportingHours_42_temperature = 48
    binding.preferences = { Closure body -> body() }
    app.run()
    assert sections.count(device.displayName) == 1 && sections.count(other.displayName) == 1
    assert inputs.findAll { it.name.startsWith('reportingHours_') }*.name as Set == ['reportingHours_42_temperature', 'reportingHours_42_humidity', 'reportingHours_73_motion', 'reportingHours_73_battery'] as Set
    assert app.configuredSignalThreshold(device, 'temperature') == 48 * hour
    assert app.configuredSignalThreshold(other, 'motion') == 24 * hour
    assert inputs*.name.size() == inputs*.name.unique().size()
}
println checks.toString() + ' behavioral scenarios passed against ' + source.name
