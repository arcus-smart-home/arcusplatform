# Subsystem Internals

Subsystems are the platform's feature modules — each one manages a specific domain (security, climate, cameras, etc.) for a place. They run in the `subsystem-service` container, react to device attribute changes via annotation-driven event handlers, and persist state to Cassandra. All subsystems for a given place share a single-threaded event loop, ensuring consistent, race-free state transitions.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Framework](#framework)
- [Subsystem Lifecycle](#subsystem-lifecycle)
- [Event Handling](#event-handling)
- [State Persistence](#state-persistence)
- [Device Binding](#device-binding)
- [Subsystem Implementations](#subsystem-implementations)
- [Configuration](#configuration)

---

## Architecture Overview

```
Device attribute change (from hub via Kafka)
        |
        v
ModelStore broadcasts ModelChangedEvent
        |
        v
PlatformSubsystemExecutor.accept()    [single-threaded per place]
        |
        v
For each subsystem in place:
   AnnotatedSubsystem.onEvent()
        |
        v
   Match @OnValueChanged / @OnAdded / @OnRemoved annotations
        |
        v
   Invoke handler method with (Model, Event, SubsystemContext)
        |
        v
   Handler updates subsystem model → context.commit() → Cassandra
```

### Key Classes

| Class | Location | Role |
|-------|----------|------|
| `Subsystem<M>` | `arcus-subsystems` | Interface — `getName()`, `getNamespace()`, `onEvent()` |
| `AnnotatedSubsystem<M>` | `arcus-subsystems` | Annotation-driven event dispatch via reflection |
| `BaseSubsystem<M>` | `arcus-subsystems` | Common lifecycle hooks, attribute helpers, request handlers |
| `SubsystemContext<M>` | `arcus-subsystems` | Messaging, scheduling, variables, persistence API |
| `PlatformSubsystemContext` | `subsystem-service` | Implementation — Cassandra DAO, message bus, correlator |
| `PlatformSubsystemExecutor` | `subsystem-service` | Single-threaded event loop per place, model store listener |
| `SubsystemServiceImpl` | `subsystem-service` | Top-level service — message routing, registry, dispatchers |
| `SubsystemCatalog` | `subsystem-service` | Immutable registry of all subsystem types |
| `CachingSubsystemRegistry` | `subsystem-service` | Guava cache of SubsystemExecutor per place UUID |

### Class Hierarchy

```
Subsystem<M>
  └─ AnnotatedSubsystem<M>      (annotation-driven dispatch)
      └─ BaseSubsystem<M>       (lifecycle hooks, attribute helpers)
          ├─ SecuritySubsystem
          ├─ SafetySubsystem
          ├─ AlarmSubsystem
          ├─ ClimateSubsystem
          ├─ CareSubsystem
          ├─ DoorsNLocksSubsystem
          ├─ PresenceSubsystem
          ├─ WaterSubsystem
          ├─ CamerasSubsystem
          ├─ LightsNSwitchesSubsystem
          ├─ WeatherSubsystem
          ├─ LawnNGardenSubsystem
          ├─ CellBackupSubsystem
          ├─ PairingSubsystem
          └─ PlaceMonitorSubsystem
```

---

## Framework

### SubsystemContext

The context is the API subsystems use for all interactions with the outside world:

```java
interface SubsystemContext<M extends Model> extends PlaceContext {
    M model();                                    // Subsystem model (read/write)

    // Messaging
    void broadcast(MessageBody message);
    void send(Address address, MessageBody message);
    void sendResponse(PlatformMessage request, MessageBody message);
    String request(Address address, MessageBody message);   // Fire-and-forget
    void sendAndExpectResponse(Address addr, MessageBody msg,
                               long timeout, TimeUnit unit,
                               ResponseAction<? super M> action);

    // Scheduling
    ScheduledTask wakeUpIn(long time, TimeUnit unit);
    ScheduledTask wakeUpAt(Date timestamp);

    // Variables (JSON-serialized, persisted as _subvars:{name})
    LooselyTypedReference getVariable(String name);
    void setVariable(String name, Object value);

    // Persistence
    boolean isPersisted();
    void commit();              // Save to Cassandra, emit VALUE_CHANGED
    void delete();

    // Subscriptions
    Subscription addBindSubscription(Subscription subscription);
    void unbind();
}
```

**ResponseAction pattern** for async request/response:

```java
interface ResponseAction<M extends Model> {
    void onResponse(SubsystemContext<M> context, PlatformMessage response);
    void onError(SubsystemContext<M> context, Throwable cause);
    void onTimeout(SubsystemContext<M> context);
}
```

### SubsystemExecutor

Manages all subsystems for a single place. Uses a `SingleThreadDispatcher<AddressableEvent>` to ensure all events for a place are processed sequentially.

**Event routing in `accept()`:**

| Event Type | Action |
|------------|--------|
| `MessageReceivedEvent` (broadcast) | Dispatch to all subsystems, update models, commit all |
| `MessageReceivedEvent` (to service) | Route to service-level handler |
| `MessageReceivedEvent` (to subsystem) | Route to specific subsystem |
| `ScheduledEvent` | Route to target subsystem |
| `SubsystemResponseEvent` | Route to target subsystem |
| `ModelAddedEvent` / `ModelChangedEvent` / `ModelRemovedEvent` | Dispatch to all subsystems (synchronous, keeps model store in sync) |

### Subsystem Service

`SubsystemServiceImpl` is the Kafka listener that feeds messages into executors:

- Listens on broadcast address and all subsystem service addresses
- Two dispatchers: `serviceDispatcher` (ListSubsystems, Reload) and `objectDispatcher` (per-subsystem requests)
- Uses `CachingSubsystemRegistry` to load/cache executors per place
- `SubsystemLoader` implements `PartitionListener` to preload subsystems on partition assignment

---

## Subsystem Lifecycle

```
onAdded()                               First creation for a place
    |                                     Sets defaults: available=false, state=ACTIVE
    v
onStarted()                             After Guice init / service restart
    |                                     Binds to models, syncs current state
    |                                     Calls onUpgraded() if version changed
    v
[Active Operation]                       Handles events, requests, scheduled tasks
    |
    v
onStopped()                             Service shutdown or cache eviction
    |                                     Cleans up subscriptions, stops scheduled tasks
    v
onRemoved()                             Place deleted
                                          Deletes from Cassandra
```

Additional transitions:
- `onActivated()` — Subsystem state changes from `SUSPENDED` to `ACTIVE`
- `onDeactivated()` — Subsystem state changes from `ACTIVE` to `SUSPENDED`

### Base Subsystem Capabilities

`BaseSubsystem` provides built-in request handlers:

| Request | Description |
|---------|-------------|
| `Capability.CMD_GET_ATTRIBUTES` | Return model attributes |
| `Capability.CMD_SET_ATTRIBUTES` | Set attributes with type validation and writability checks |
| `AddTagsRequest` | Add tags to subsystem |
| `RemoveTagsRequest` | Remove tags from subsystem |

---

## Event Handling

### Annotation Types

| Annotation | Fires When | Filter |
|------------|-----------|--------|
| `@Request("method:name")` | Platform request message received | Message type |
| `@OnAdded(query = "...")` | Model added to place | Predicate on model |
| `@OnRemoved(query = "...")` | Model removed from place | Predicate on model |
| `@OnValueChanged(attributes = {...})` | Model attribute changed | Attribute names |
| `@OnReport(query = "...")` | Bulk attribute report received | Predicate on model |
| `@OnMessage` | Any message received | Message type |
| `@OnScheduledEvent` | Scheduled wakeup fires | — |

### Query Predicates

Annotations with `query` parameters use `ExpressionCompiler.compile(query)` to produce `Predicate<Model>` filters. Only matching models trigger the handler.

```java
// Only fires for devices with the 'contact' capability
@OnAdded(query = "base:caps contains 'cont'")
public void onContactAdded(Model m, SubsystemContext<M> ctx) { ... }

// Only fires when device connectivity state changes
@OnValueChanged(attributes = { DeviceConnectionCapability.ATTR_STATE })
public void onConnectivity(Model m, SubsystemContext<M> ctx) { ... }
```

### Dispatch Internals

`AnnotatedSubsystem.onEvent()` routes by event type:

```
AddressableEvent
 ├─ MessageReceivedEvent
 │   ├─ Is request? → handleRequest() → @Request handler lookup
 │   └─ Else → fireMessageReceived() → @OnMessage listeners
 ├─ SubsystemResponseEvent → handleResponse() → ResponseAction callback
 ├─ ModelAddedEvent → fireModelAdded() → @OnAdded listeners
 ├─ ModelChangedEvent → fireModelChanged() → @OnValueChanged listeners
 ├─ ModelReportEvent → fireReport() → @OnReport listeners
 ├─ ModelRemovedEvent → fireModelRemoved() → @OnRemoved listeners
 ├─ ScheduledEvent → fireScheduledEvent() → @OnScheduledEvent listeners
 └─ SubsystemLifecycleEvent → onAdded/onStarted/onStopped/etc.
```

---

## State Persistence

### Cassandra Schema

**Table:** `subsystem` in the `dev` (platform) keyspace

| Column | Type | Description |
|--------|------|-------------|
| `place_id` | uuid | Partition key (place) |
| `namespace` | text | Clustering key (subsystem namespace) |
| `attributes` | map<text, text> | JSON-encoded attribute values |
| `created` | timestamp | Creation time |
| `modified` | timestamp | Last modification |

### Commit Flow

```java
context.commit()
  |
  ├─ Not persisted & not added:
  |    emit SubsystemAddedEvent
  |    full upsert to Cassandra
  |
  └─ Persisted & dirty:
       emit VALUE_CHANGED event to clients
       partial update (only changed attributes)
       include failedFromPrevious attributes (resilience retry)
```

### Failure Resilience

`PlatformSubsystemContext` tracks `failedToSave` — a map of attributes that failed to persist in a previous commit. On the next successful commit, these are retried. The in-memory model remains the source of truth.

### Variables

Subsystem variables are JSON-serialized and stored as model attributes with the `_subvars:` prefix:

```java
context.setVariable("alarmState", myState);  // Stored as _subvars:alarmState
context.getVariable("alarmState");            // Returns LooselyTypedReference
```

---

## Device Binding

### AddressesAttributeBinder

Automatically manages a `Set<String>` model attribute that tracks devices matching a query:

```java
private final AddressesAttributeBinder<SecuritySubsystemModel> securityDevices =
    new AddressesAttributeBinder<>(
        "base:caps contains 'seccontact' OR base:caps contains 'secmotion'",
        SecuritySubsystemCapability.ATTR_SECURITYDEVICES
    );

@Override
protected void onStarted(SubsystemContext<SecuritySubsystemModel> context) {
    securityDevices.bind(context);
    // 1. Scans all models for matches, populates initial set
    // 2. Registers ModelAddedListener → auto-adds new matches
    // 3. Registers ModelRemovedListener → auto-removes
    // Returns Subscription for cleanup via context.addBindSubscription()
}
```

Other binder variants:
- `AddressesVariableBinder` — Stores tracked addresses as a variable instead of an attribute
- `MapVariableBinder` — Maps addresses to complex state objects
- `BaseAddressesAttributeBinder` — Abstract base for custom binders

---

## Subsystem Implementations

### Registered Subsystems (16)

Configured in `SubsystemModule.java`:

| # | Subsystem | Namespace | Version | Description |
|---|-----------|-----------|---------|-------------|
| 1 | [Security](#security) | `subsecurity` | 2 | Armed/disarmed security, keypads |
| 2 | [Safety](#safety) | `subsafety` | 2 | Smoke, CO, water leak detection |
| 3 | [Alarm](#alarm) | `subalarm` | 2 | Unified alarm with hub/platform provider switching |
| 4 | [Climate](#climate) | `subclimate` | 1 | Thermostats, temperature, humidity, scheduling |
| 5 | [Care](#care) | `subcare` | 1 | Behavior monitoring, elder care alerts |
| 6 | [Doors & Locks](#doors--locks) | `subdoorlock` | 1 | Locks, motorized doors, pet doors |
| 7 | [Presence](#presence) | `subpres` | 2 | People/device at-home/away tracking |
| 8 | [Water](#water) | `subwater` | 1 | Water heaters, softeners, valves |
| 9 | [Cameras](#cameras) | `subcameras` | 2 | Camera devices, streaming, recording |
| 10 | [Lights & Switches](#lights--switches) | `sublightsnswitches` | 1 | Lights, dimmers, switches |
| 11 | [Weather](#weather) | `subweather` | 1 | NOAA weather radio alerts |
| 12 | [Lawn & Garden](#lawn--garden) | `sublawnngarden` | 1 | Irrigation scheduling, zone control |
| 13 | [Cell Backup](#cell-backup) | `subcellbackup` | 1 | 4G cellular backup monitoring |
| 14 | [Pairing](#pairing) | `subpairing` | 1 | Device pairing workflow state machine |
| 15 | [Place Monitor](#place-monitor) | `subplacemonitor` | 1 | Connectivity, battery, OTA, smart home alerts |
| 16 | Alarm (hub-side) | — | — | See Alarm (provider switching) |

---

### Security

**Namespace:** `subsecurity` | **Version:** 2

Manages armed/disarmed security state with keypad integration.

**Key Attributes:**
- `securityDevices` — All security-capable devices
- `armedDevices` / `bypassedDevices` / `offlineDevices` — Device categorization
- `alarmState` — `DISARMED`, `ARMING`, `ARMED`, `ALERT`, `CLEARING`, `SOAKING`
- `alarmMode` — `OFF`, `ON`, `PARTIAL`
- `keypads` — Keypad device addresses

**Architecture:** Dual V1/V2 implementation. Delegates to `SecuritySubsystemV1` when `AlarmSubsystem` is `INACTIVE`, `SecuritySubsystemV2` otherwise.

**Key Components:**
- `SecurityStateMachine` — State transitions for security modes
- `KeypadBinder` / `KeypadState` — Keypad device tracking

---

### Safety

**Namespace:** `subsafety` | **Version:** 2

Coordinates smoke, CO, and water leak detection across all safety devices.

**Architecture:** Dual V1/V2 with dynamic delegation (same pattern as Security).

**Key Components:**
- `PreSmokeAddressBinder` — Smoke detector binding
- `SensorStateBinder` — Sensor state management
- `WaterValvesAddressBinder` — Water valve tracking for automatic shutoff

---

### Alarm

**Namespace:** `subalarm` | **Version:** 2

Unified alarm system supporting both platform-side and hub-side alarm processing.

**Alarm Types:** `SMOKE`, `CO`, `SECURITY`, `PANIC`, `WATER`

**Provider Switching:**
- Delegates to `PlatformAlarmSubsystem` or `HubAlarmSubsystem` based on `alarmProvider`
- Switching to HUB: sends `HubAlarmCapability.ActivateRequest`
- Switching to PLATFORM: sends `HubAlarmCapability.SuspendRequest`
- Validates minimum hub firmware version (2.1.0.060) before switching to hub provider
- Only switches in steady state (no active alarms)

**Incident Tracking:** `AlarmIncidentService` manages active alarm incidents and historical records.

---

### Climate

**Namespace:** `subclimate` | **Version:** 1

Manages thermostats, temperature/humidity sensors, space heaters, and fans.

**Devices:** Thermostats, temperature sensors, humidity sensors, space heaters, fans

**Key Feature:** Thermostat scheduling with default presets:

| Time | Heat | Cool |
|------|------|------|
| Morning (6:00) | 21.1C / 70F | 25.6C / 78F |
| Day (8:00) | 18.9C / 66F | 27.8C / 82F |
| Evening (18:00) | 21.1C / 70F | 25.6C / 78F |
| Night (22:00) | 18.9C / 66F | 26.7C / 80F |

Integrates with the Scheduler Service for time-based thermostat commands.

---

### Care

**Namespace:** `subcare` | **Version:** 1

Behavior monitoring for elder care scenarios. Detects activity anomalies and generates alerts.

**Key Components:**
- `BehaviorManager` — Manages behavior templates and monitoring rules
- `BehaviorMonitor` — Tracks behavior violations (e.g., no motion detected for N hours)
- `CallTree` — Notification escalation chain

**Features:**
- Motion/contact/presence-based behavior monitoring
- Alert generation and acknowledgment
- Panic button integration
- IVR notification delivery

---

### Doors & Locks

**Namespace:** `subdoorlock` | **Version:** 1

**Devices:** Door locks, motorized doors, pet doors, contact sensors

**Tracked Sets:**
- `lockDevices` / `jammedLocks` — Lock state and jam detection
- `motorizedDoorDevices` / `obstructedDoors` — Motorized door state
- `petdoorDevices` / `unlockedPetDoors` / `autopetDoors` — Pet door modes

**Key Feature:** Person authorization management — queues lock authorization operations to prevent overloading devices.

---

### Presence

**Namespace:** `subpres` | **Version:** 2

Tracks people and presence devices at home vs. away.

**Tracked Sets:**
- `allDevices` — All presence-capable devices
- `devicesAtHome` / `devicesAway` — Unassigned fobs
- `peopleAtHome` / `peopleAway` — Person-assigned fobs mapped to person addresses
- `occupied` — Boolean: true when any person is present

**Key Predicates:**
- `IS_PRESENCE_DEVICE` — Has PresenceCapability
- `IS_ASSIGNED_DEVICE` — usehint=PERSON (fob assigned to a person)
- `IS_PERSON_AWAY` — Person device with presence != PRESENT

---

### Water

**Namespace:** `subwater` | **Version:** 1

**Tracked Sets:**
- `waterDevices` — All water-related devices
- `closedValves` — Valves currently closed
- `continuousWaterUseDevices` / `excessiveWaterUseDevices` — Ecowater softener alerts
- `waterHeater` / `waterSoftener` — Primary device designations

**Events:** `ContinuousWaterUseEvent`, `ExcessiveWaterUseEvent`, `LowSaltEvent`

---

### Cameras

**Namespace:** `subcameras` | **Version:** 2

**Tracked Sets:**
- `cameras` — All camera devices
- `offlineCameras` — Disconnected cameras
- `maxSimultaneousStreams` — Configurable stream limit

Uses `CameraStatusAdapter` for per-camera state tracking, recording monitoring, and OTA updates.

---

### Lights & Switches

**Namespace:** `sublightsnswitches` | **Version:** 1

**Device Types** (by `devtypehint`): Light, Dimmer, Switch, Halo

**Tracking:**
- `switchDevices` — All light/switch/dimmer devices
- `onDeviceCounts` — `{light: N, dimmer: N, switch: N}` map of currently-on devices

State determined by `switch:state=ON` for switches, `dim:brightness > 0` for dimmers.

---

### Weather

**Namespace:** `subweather` | **Version:** 1

**Devices:** NOAA weather radios

**Tracked Sets:**
- `weatherRadios` — All weather radio devices
- `alertingRadios` — Radios in `ALERT` or `ALERT_HUSHED` state

**Alert States:** `ALERT` (active), `ALERT_HUSHED` (muted by user), `READY` (no alerts)

Aggregates alerts by EAS (Emergency Alert System) code. Supports `SnoozeAllAlertsRequest`.

---

### Lawn & Garden

**Namespace:** `sublawnngarden` | **Version:** 1

Manages irrigation controllers with multiple scheduling modes.

**Schedule Modes:** `WEEKLY`, `EVEN` (even days), `ODD` (odd days), `INTERVAL`

**Per-Controller State Machine** (`LawnNGardenStateMachine`):
```
INITIAL → IDLE → WATERING → STOPPING → IDLE
```

**Requests:** CreateWeeklyEvent, CreateScheduleEvent, UpdateScheduleEvent, RemoveScheduleEvent, ConfigureIntervalSchedule, EnableScheduling, DisableScheduling, StopWatering, Skip (skip next watering)

Tracks `zonesWatering` — map of which zones are currently active per controller.

---

### Cell Backup

**Namespace:** `subcellbackup` | **Version:** 1

Monitors hub 4G cellular backup connectivity.

**Status States:**
- `NOTREADY` — Hub lacks 4G modem or SIM not provisioned
- `READY` — 4G available as backup
- `ERRORED` — 4G has error state

**Not Ready Reasons:** `MODEM` (no modem), `SIM` (SIM issue), `BOTH`

**Error States:** `NONE`, `BANNED` (service explicitly banned)

Sends notifications on connectivity type transitions (broadband ↔ cellular).

---

### Pairing

**Namespace:** `subpairing` | **Version:** 1

State machine managing device pairing workflows.

**Key Requests:**
- `StartPairingRequest` — Initiate pairing for a product
- `SearchRequest` — Search for a specific device
- `FactoryResetRequest` — Factory reset a device
- `StopSearchingRequest` — Exit pairing mode
- `DismissAllRequest` — Dismiss pending pairing notifications
- `ListPairingDevicesRequest` — List available devices
- `ListHelpStepsRequest` — Get pairing instructions
- `GetKitInformationRequest` — Kit device information

**Protocols:** ZigBee, Z-Wave, WiFi, Bluetooth, IPCD, OAuth

**Kit Support:** Maps ZigBee EUI-64 IDs to protocol addresses via `ManufactureKittingDao`.

---

### Place Monitor

**Namespace:** `subplacemonitor` | **Version:** 1

Hub and device health monitoring with smart home alerts. Uses a pluggable handler architecture.

**Handler Plugins:**

| Handler | Responsibility |
|---------|---------------|
| `OfflineNotificationsHandler` | Device/hub offline alerts with battery level tracking (Full, Low, Very Low, Critical, Dead) |
| `DeviceOTAHandler` | Firmware update tracking |
| `DefaultRuleHandler` | Auto-create default rules for newly paired devices |
| `PlacePairingModeHandler` | Hub pairing state monitoring |
| `BridgeDeviceAddHandler` | Bridge device addition |
| `SmartHomeAlertHandler` | Platform-level smart home alerts |

**Smart Home Alert Events:**
- `HubOffline` / `HubOnline`
- `DeviceOffline` / `DeviceOnline`
- `DeviceLowBattery` / `DeviceVeryLowBattery` / `DeviceCriticalBattery` / `DeviceDeadBattery`
- `DoorObstruction`, `CellServiceError`, `LockJam`, `CellModemNeeded`

---

## Configuration

### SubsystemConfig Properties

```properties
subsystems.threads.max = 20               # Main thread pool size
subsystems.threads.keepAliveMs = 100       # Thread keep-alive
subsystems.scheduler.threads = 5           # Scheduler pool size
subsystems.queue.depth = 100               # Per-subsystem event queue

subsystem.place.cache.concurrency.level = 64
subsystem.place.cache.expire.access.ms = -1
subsystem.place.cache.initial.capacity = -1
subsystem.place.cache.max.size = -1
subsystem.place.cache.soft.values = false
subsystem.place.preload = true             # Preload subsystems on partition assignment
```

### Model Types Tracked per Place

The subsystem executor subscribes to model changes for: `Account`, `Device`, `Hub`, `PairingDevice`, `Person`, `Place`, `Scheduler`, `Subsystem`.
