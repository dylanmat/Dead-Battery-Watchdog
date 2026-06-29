# Dead Battery Watchdog Roadmap

Dead Battery Watchdog v2.0.2 is an event-liveness watchdog: it monitors selected real hardware devices with the Hubitat `battery` capability and alerts when a device stops reporting supported Hubitat events. Earlier v1 releases used temperature reports as the liveness signal.

The v2 roadmap continues evolving the project into broader Zigbee battery-device health monitoring. The main design shift is that a device should be considered likely dead only when useful event streams go silent, not merely because one attribute stops updating.

## Why v2 Changes Direction

Temperature reporting is useful, but it is not a reliable universal liveness signal. Many Zigbee battery devices are sleepy devices, and polling often returns the hub's last saved value rather than proving that the device is awake and reachable.

Multipurpose devices can also keep reporting contact, motion, acceleration, button, water, or battery events even when temperature becomes stale. In that case, the device is still alive and the correct alert is an attribute-stale warning, not a dead-device alert.

The v2 model separates device liveness from individual attribute freshness:

- Device dead or offline: no useful events of any kind for longer than the configured threshold.
- Primary function stale: the device is alive, but its main purpose has not reported recently.
- Attribute stale: a secondary attribute such as temperature or battery has stopped updating while other events continue.

## Core Concepts

- `lastAnyEvent`: the latest known event from the device, regardless of attribute.
- Primary attributes: the events that prove the device's main function is working, such as contact, motion, acceleration, water, pushed, held, temperature, or humidity.
- Secondary attributes: useful supporting signals such as temperature, humidity, and battery when they are not the device's primary purpose.
- Stale attribute: an attribute that has not reported within its threshold while the device still appears alive through other events.
- Health class: a per-device profile type that defines expected behavior and default thresholds.
- Threshold: the maximum expected time between events before a health state changes.
- Health state: the current classification, such as healthy, attribute stale, primary stale, possibly dead, or probably dead.
- Confidence: a label that explains how strongly the app trusts a health classification.

## Version Stages

### v2.0 - Track Any-Event Liveness - Complete 2026-06-29

Implemented baseline: track `lastAnyEvent` per device and use it as the primary liveness signal for dead-device detection. Any parsed Hubitat event from a monitored device can count as evidence of life, including contact, motion, temperature, humidity, button, water, acceleration, battery, and other device events.

Battery percentage should not be used as proof that a device is alive. It may be stale hub state.

### v2.1 - Track Primary Function Separately

Add primary-function tracking so each device can distinguish its main purpose from secondary attributes. Examples:

- Contact sensors use contact events as their primary signal.
- Motion sensors use motion events as their primary signal.
- Fridge and freezer sensors use temperature or humidity as primary signals.
- Multipurpose sensors use contact or acceleration before temperature.
- Buttons use pushed or held events, but should usually require manual testing.

### v2.2 - Separate Dead Devices From Stale Attributes

Introduce separate classifications for device liveness and attribute freshness. A temperature-stale device should not be called dead if contact, motion, acceleration, button, water, battery, or other useful events continue.

Alert language should distinguish:

- No events of any kind: likely dead, asleep, out of range, or routing failed.
- Primary function missing while secondary events continue: function or driver issue.
- Temperature stale while other events continue: temperature reporting is stale, but the device is not dead.
- Battery stale: battery reporting is stale or unreliable, but not proof of device failure by itself.

### v2.3 - Add Health Classes and Default Thresholds

Add per-device health classes with default thresholds that users can override. Initial classes should cover common battery-device patterns:

| Health class | Typical devices | Default dead threshold | Attribute stale threshold |
| --- | --- | --- | --- |
| `temp_critical` | Fridge/freezer temperature or humidity sensors | 12-18 hours | 12 hours |
| `room_environment` | Room temperature or humidity sensors | 24-36 hours | 12-36 hours |
| `multipurpose_active` | Doors, dryer, garage doors, acceleration/contact sensors | 48 hours | 48-72 hours |
| `motion_active` | Motion sensors in active rooms | 48 hours | 72 hours for temperature if present |
| `rare_contact` | Cabinets, rarely opened doors | 7-14 days | Not applicable unless attributes exist |
| `leak_sensor` | Dishwasher or utility leak sensors | 7-30 days or manual test overdue | Not applicable unless attributes exist |
| `manual_button` | Buttons and remotes | 30 days or manual test overdue | Battery stale threshold only |

### v2.4 - Improve Alert Wording

Replace one generic dead-battery warning with severity-specific messages:

- `ALERT`: no useful events for the dead threshold. The device is likely dead, asleep, out of range, or affected by Zigbee routing.
- `WARNING`: the primary function or an important attribute is stale, but other events prove the device is still alive.
- `NOTICE`: battery reporting is stale or suspicious, but other events continue and battery percentage is not reliable proof of failure.

Example temperature-stale wording:

`WARNING: Dryer temperature has not updated for 48 hours, but the device is still reporting contact or acceleration events. Temperature reporting may be stale; device is not dead.`

### v2.5 - Report Battery Replacement Age When Available

For devices that expose a valid `lastBattery` replacement timestamp, include estimated battery age when the device is suspected dead. The reported lifespan should be calculated as suspected death date minus `lastBattery`.

This should remain optional supporting context only:

- Only use `lastBattery` after validating that it is a Unix replacement timestamp, not legacy persisted battery percentage.
- Do not assume every device tracks `lastBattery`.
- Do not use battery age as proof that a device is alive or dead.
- Phrase it as observed battery lifespan by suspected death date, such as `battery age at suspected failure: 14 months`.

### v2.6 - Add Confidence Levels

Add confidence labels to health classifications so alerts explain how strong the evidence is.

| Condition | Classification | Confidence |
| --- | --- | --- |
| No events for 2x expected interval | Possibly dead | Moderate |
| No events for 3x expected interval | Probably dead | High |
| No primary-function event, but secondary events continue | Function or driver issue | Moderate |
| Primary events continue, temperature stopped | Temperature stale only | High |
| Battery low plus missed events | Battery likely dying | Moderate-high |
| Battery low but events continue | Battery value unreliable | Low |
| Battery 100% but no events | Battery value irrelevant; liveness suspicious | High |

### v2.7 - Add Manual-Test Workflows

Support devices that cannot be judged well by passive monitoring alone. Buttons, leak sensors, and rarely used contacts may need a scheduled manual challenge instead of automatic dead claims.

Manual-test workflows should track:

- Last successful manual test.
- Manual test due date.
- Whether overdue manual tests should notify.
- Whether a device can be marked healthy after the expected event arrives.

### v2.8 - Refine Migration and User Guidance

Document how users should move from v1 temperature monitoring to v2 event-health monitoring. The guidance should explain:

- Existing v1 behavior was temperature-based; v2.0 and later use event-liveness monitoring.
- v2 profiles should be chosen by device purpose, not just by available attributes.
- Temperature-only alerts become stale-temperature alerts unless all useful event streams are silent.
- Battery percentage and battery reports are supporting evidence, not the primary dead-device detector.

## Example Profiles

Dryer multipurpose sensor:

- Health class: `multipurpose_active`
- Primary attributes: contact, acceleration, active/inactive
- Secondary attributes: temperature, battery
- Dead threshold: 48 hours with no events of any kind
- Temperature stale threshold: 48 hours
- Battery stale threshold: 30 days

Garage freezer sensor:

- Health class: `temp_critical`
- Primary attributes: temperature, humidity
- Secondary attributes: battery
- Dead threshold: 12-18 hours with no temperature or humidity events
- Temperature stale threshold: 12 hours
- Battery stale threshold: 30 days

Bathroom button:

- Health class: `manual_button`
- Primary attributes: pushed, held
- Secondary attributes: battery
- Dead threshold: 30 days or manual test overdue
- Manual test required: yes
