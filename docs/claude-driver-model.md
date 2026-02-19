# Driver Model

Device drivers are Groovy DSL scripts that bridge platform capabilities to physical device protocols. Each driver is a `.driver` file compiled at startup by `driver-services` using `GroovyDriverFactory`.

See [claude-driver-execution.md](claude-driver-execution.md) for how the platform loads, executes, and manages driver lifecycle at runtime.

---

## Driver File Anatomy

```groovy
// ── Metadata ─────────────────────────────────────────────────
driver           "ZB_CentraLite_ContactSensor"
description      "CentraLite Zigbee Contact Sensor driver"
version          "2.3"
protocol         "ZIGB"          // ZIGB | ZWAV | IPCD | MOCK
deviceTypeHint   "Contact"
productId        "4dd18a"
vendor           "CentraLite"
model            "3320"

// ── Device Matching ───────────────────────────────────────────
matcher 'ZIGB:manufacturer': 0xC2DF, 'ZIGB:vendor': 'CentraLite', 'ZIGB:model': '3320'
matcher 'ZIGB:manufacturer': 0x104E, 'ZIGB:vendor': 'CentraLite', 'ZIGB:model': '3322'

// ── Capabilities ─────────────────────────────────────────────
capabilities DevicePower, Contact, Temperature

// ── Import shared implementations ────────────────────────────
importCapability 'generic/GenericContact'

// ── Default attribute values ──────────────────────────────────
DevicePower {
    source DevicePower.SOURCE_BATTERY
    linecapable false
    bind sourcechanged to source   // auto-timestamp when 'source' changes
}

Contact {
    Contact.contact  Contact.CONTACT_CLOSED
    bind contactchanged to Contact.contact
}

// ── Lifecycle handlers ────────────────────────────────────────
onAdded {
    log.debug "Device added"
    Device.name 'Contact Sensor'
    DevicePower.battery 50
    Contact.contact Contact.CONTACT_CLOSED
}

onConnected {
    log.debug "Device connected"
    // poll current state, configure reports
}

onDisconnected { log.debug "Device disconnected" }
onRemoved      { log.debug "Device removed" }

// ── Protocol-specific section (reflexes + config) ─────────────
Zigbee {
    offlineTimeout 10, MINUTES
    // reflex definitions ...
}

// ── Message handlers ──────────────────────────────────────────
onZigbeeMessage.Zcl.power.zclreportattributes() {
    def attrs = Zigbee.Message.decodeZclAttributes(message)
    def volts  = attrs[pwrCluster.ATTR_BATTERY_VOLTAGE] / 10.0
    DevicePower.battery calculateBatteryPercent(volts)
}
```

---

## Lifecycle Events

| Event | When it fires | Typical use |
|-------|--------------|-------------|
| `onAdded` | Device first paired | Set initial attribute values, default state |
| `onConnected` | Device comes online | Poll current state, configure reporting intervals |
| `onDisconnected` | Device goes offline | Cleanup, stop periodic polls |
| `onRemoved` | Device permanently removed | Send reset command, final cleanup |

---

## Capabilities

Declare capabilities the driver implements:

```groovy
capabilities DevicePower, Contact, Temperature, Identify
```

Import and reuse shared implementations from `resources/generic/`:

```groovy
importCapability 'generic/GenericContact'   // handles setAttributes(Contact), defaults usehint
```

Handle `setAttributes` commands for a capability:

```groovy
setAttributes('leakh2o') {
    def attrs = message.attributes
    for (attribute in attrs) {
        switch (attribute.key) {
            case LeakH2O.state:
                ZWave.basic.set(attribute.value == LeakH2O.STATE_SAFE ? 0x00 : 0xFF)
                break
            default:
                log.error "Unrecognized attribute: {}", attribute
        }
    }
}
```

Handle capability commands:

```groovy
onIdentify.Identify {
    identCluster.identifyCmd(IDENT_PERIOD_SECS)
    sendResponse 'ident:IdentifyResponse', ['result': true]
}
```

---

## Attribute State

Read and write capability attributes using `CapabilityName.attributeName`:

```groovy
// Write
Contact.contact  Contact.CONTACT_OPENED
Contact.contactchanged  new Date()

// Read
def prev = Contact.contact.get()

// Bind timestamp (auto-update when source attribute changes)
bind contactchanged to Contact.contact
```

Changed attributes are batched and written to Cassandra after the handler returns.

---

## Variables (Driver-Local Storage)

Persistent per-device key-value store, survives reconnects:

```groovy
onAdded {
    vars.'runtimeStart'       = 0
    vars.'filterRuntimeTotal' = 0
}

// In a handler:
vars.'runtimeStart' = now().time
def elapsed = now().time - (vars.'runtimeStart' ?: 0)
```

---

## Protocol Sections — Zigbee

```groovy
@Field def ep          = Zigbee.endpoint((byte)1)
@Field def pwrCluster  = ep.Power
@Field def iasCluster  = ep.IasZone

Zigbee {
    offlineTimeout 10, MINUTES

    // Runs once on 'added'
    poll reflex {
        on added
        bind endpoint: 1, profile: 0x0104, cluster: Zcl.IasZone.CLUSTER_ID, server: true
        iaszone enroll
    }

    // Runs on 'connected'
    poll reflex {
        on connected
        ordered {
            read endpoint: 1, cluster: Zcl.IasZone.CLUSTER_ID, attr: Zcl.IasZone.ATTR_ZONE_STATUS
            read endpoint: 1, cluster: Zcl.Power.CLUSTER_ID,   attr: Zcl.Power.ATTR_BATTERY_VOLTAGE
            report endpoint: 1, cluster: Zcl.Power.CLUSTER_ID,
                attr: pwrCluster.ATTR_BATTERY_VOLTAGE,
                type: Data.TYPE_UNSIGNED_8BIT, min: 3600, max: 43200
        }
    }

    // Hub-local match reflexes (no cloud round-trip)
    match reflex {
        on iaszone, endpoint: 1, set: ["alarm1"], maxDelay: 30
        set Contact.contact, Contact.CONTACT_OPENED
    }
    match reflex {
        on iaszone, endpoint: 1, clear: ["alarm1"], maxDelay: 30
        set Contact.contact, Contact.CONTACT_CLOSED
    }
    match reflex {
        on zcl.pollcontrol.checkIn
        send zcl.pollcontrol.checkInResponse, startFastPolling: 0, fastPollTimeout: 0
    }
}

// ZCL message handlers
onZigbeeMessage.Zcl.power.zclreportattributes() {
    def attrs = Zigbee.Message.decodeZclAttributes(message)
    def volts = attrs[pwrCluster.ATTR_BATTERY_VOLTAGE] / 10.0
    DevicePower.battery calculateBatteryPercent(volts)
}

onZigbeeMessage.Zcl.iaszone.zoneEnrollRequest() {
    send zcl.iaszone.zoneEnrollResponse, zoneId: 0x01, enrollResponseCode: 0x00
}
```

Naming convention for handlers: `onZigbeeMessage.Zcl.<cluster>.<messageType>()`

---

## Protocol Sections — Z-Wave

```groovy
ZWave {
    offlineTimeout 60, MINUTES
}

// Z-Wave command class handlers
onZWaveMessage.basic.report {
    def state = message.command.get('value')
    LeakH2O.state (state == 0x00 ? LeakH2O.STATE_SAFE : LeakH2O.STATE_LEAK)
}

onZWaveMessage.battery.report {
    GenericZWaveBattery.handleBatteryReport(this, DEVICE_NAME, message)
}

onZWaveMessage.alarm.report {
    def alarmType = message.command.get('alarmtype')
    def event     = message.command.get('event')
    log.debug "Alarm type:{} event:{}", alarmType, event
}

onZWaveNodeInfo { log.debug "NodeInfo: {}", message }

// Sending Z-Wave commands
onConnected {
    ZWave.battery.get()
    ZWave.basic.get()
    ZWave.configuration.set(PARAM_NO, PARAM_SIZE, VALUE_B3, VALUE_B2, VALUE_B1, VALUE_B0)
    ZWave.association.set(1, 1, 0, 0, 0)
}
```

---

## Protocol Sections — IPCD

```groovy
def final ATTR_CONTACT = "generic.contact"

onConnected {
    Ipcd.Commands.getParameterValues("txnid", [ATTR_CONTACT])
}

onRemoved {
    Ipcd.Commands.factoryReset()
}

// Asynchronous events from the device
onIpcdMessage.event {
    def changes = message.mapify()[VALUE_CHANGES]
    for (c in changes) {
        if (c["parameter"] == ATTR_CONTACT) {
            Contact.contact c["value"] == 'opened' ? Contact.CONTACT_OPENED : Contact.CONTACT_CLOSED
            Contact.contactchanged new Date()
        }
    }
}

// Response to a getParameterValues request
onIpcdMessage.response.getParameterValues("success") {
    def resp = message.mapify()["response"]
    if (resp.containsKey(ATTR_CONTACT)) {
        Contact.contact resp[ATTR_CONTACT] == 'opened' ? Contact.CONTACT_OPENED : Contact.CONTACT_CLOSED
    }
}
```

---

## Protocol Sections — MOCK (Testing)

```groovy
onPlatform ("devmock:Connect") {
    log.debug "devmock:Connect"
    connected()
    sendResponse "devmock:ConnectResponse", [:]
}

onPlatform ("devmock:SetAttributes") {
    attributes << message.attributes.attrs
    if (message.attributes.attrs['cont:contact']) {
        Contact.contactchanged new Date()
    }
    sendResponse "devmock:SetAttributesResponse", [:]
}
```

---

## Messaging DSL Reference

| Keyword | What it does |
|---------|-------------|
| `sendToDevice protocol, payload, timeoutMs` | Send a raw protocol message to the device |
| `sendToPlatform msg` | Send a message to the platform |
| `sendResponse 'ns:Type', [key: val]` | Respond to a platform request |
| `emit 'ns:EventType', [key: val]` | Emit a platform event |
| `connected()` | Signal device is connected |
| `disconnected()` | Signal device is disconnected |
| `log.debug/info/warn/error "msg {}", var` | Log (logger name: `driver.<driverName>`) |
| `now()` | Current `Date` |
| `lastProtocolMessageTimestamp()` | Epoch ms of last protocol message |

---

## Scheduling

```groovy
// Schedule a one-shot event
Scheduler.scheduleIn 'PollBatteryVoltage', 500     // ms

// Handle it
onEvent('PollBatteryVoltage') {
    ZWave.battery.get()
}
```

---

## Compilation and Loading Pipeline

```
driver-services starts
  │
  ▼
GroovyDriverFactory.load("SomeDriver.driver")
  ├─ 1. Create DriverBinding (Groovy script binding with CapabilityRegistry)
  ├─ 2. Plugin.enhanceEnvironment() — each protocol plugin adds global properties:
  │       ZigbeeProtocolPlugin  → adds Zigbee, onZigbeeMessage
  │       ZWaveProtocolPlugin   → adds ZWave, onZWaveMessage, onZWaveNodeInfo
  │       IpcdProtocolPlugin    → adds Ipcd, onIpcdMessage
  │       ReflexPlugin          → adds reflex DSL
  │       CapabilityPlugin      → adds capability name constants
  ├─ 3. GroovyScriptEngine compiles + runs the script
  │       (all handler blocks register themselves with GroovyDriverBuilder)
  ├─ 4. Plugin.postProcessEnvironment() — post-process collected data
  ├─ 5. GroovyDriverBuilder builds DeviceDriver:
  │       - handler chains for protocol, platform, and driver events
  │       - attribute binding handler (auto-timestamps)
  │       - reflex definitions (compiled to ReflexDriver)
  └─ 6. Plugin.enhanceDriver() — add context-aware properties (e.g., cluster objects)

DeviceDriver registered in DriverRegistry (keyed by name + version)
```

Precompiled `.class` files are used if available (same name, `.` → `_`); falls back to dynamic compilation.

---

## DeviceDriver Interface (Platform Side)

```java
// Called by driver-services when messages arrive
void handleDriverEvent(DriverEvent event, DeviceDriverContext context);   // added/connected/removed
void handleProtocolMessage(ProtocolMessage msg, DeviceDriverContext context);
void handlePlatformMessage(PlatformMessage msg, DeviceDriverContext context);

// Driver selection
boolean supports(AttributeMap deviceAttributes);   // evaluated against matcher declarations
```

`DeviceDriverContext` gives handlers access to: device ID, attribute read/write, variable storage, `sendToDevice`, and the last protocol message timestamp.

---

## Generic / Shared Capabilities

Reusable capability implementations live in `driver-services/src/main/resources/generic/` as `.capability` files with the same DSL. They provide standard `setAttributes` handling and default initialization so individual drivers don't repeat boilerplate.

Examples: `GenericContact.capability`, `GenericZWaveBattery.capability`, `GenericZigbeeBattery.capability`

---

## Key Files

| File | Location |
|------|----------|
| `GroovyDriverFactory.java` | `common/arcus-drivers/groovy-bindings/.../driver/groovy/` |
| `GroovyDriverBuilder.java` | same |
| `DriverBinding.java` | same |
| `GroovyDriverPlugin.java` | same `.../groovy/plugin/` |
| `ZigbeeProtocolPlugin.java` | same `.../groovy/zigbee/` |
| `ZWaveProtocolPlugin.java` | same `.../groovy/zwave/` |
| `IpcdProtocolPlugin.java` | same `.../groovy/ipcd/` |
| `ReflexPlugin.java` | same `.../groovy/reflex/` |
| `DeviceDriver.java` | `common/arcus-drivers/drivers-common/.../driver/` |
| `DeviceDriverContext.java` | same |
| `DeviceDriverImpl.java` | same |
| `DeviceService.java` | `platform/arcus-containers/driver-services/` |
| `*.driver` files | `platform/arcus-containers/driver-services/src/main/resources/` |
| `generic/*.capability` | same `/resources/generic/` |
