# Reflex System

Reflexes are hub-local automation rules that execute directly on the Iris hub hardware, without a round-trip to the platform. They solve the core IoT challenge of low-latency, network-resilient device automation — automations keep working even when the cloud connection is lost.

---

## Concept

A reflex is a **condition → action** pair:

```
ReflexDefinition = List<ReflexMatch> + List<ReflexAction>
```

When a message arrives at the hub (from a Zigbee/Z-Wave device or from the platform), the hub evaluates each active reflex's match conditions. If a match fires, the associated actions execute immediately on the hub.

---

## Run Modes

`ReflexRunMode` controls where a reflex executes:
- `HUB` — runs only on the hub (requires hub support); default
- `PLATFORM` — runs only on the platform
- `MIXED` — runs on either depending on hub capabilities

Declared in driver files with `reflexMode Reflex.MODE_HUB`.

---

## Match Types (Triggers)

| Match | Fires when... |
|-------|--------------|
| `ReflexMatchLifecycle` | Device is ADDED, CONNECTED, DISCONNECTED, or REMOVED |
| `ReflexMatchAttribute` | A device attribute changes to a specific value |
| `ReflexMatchMessage` | A specific platform message arrives |
| `ReflexMatchRegex` | Binary protocol bytes match a pattern (compiled to DFA at build time) |
| `ReflexMatchPollRate` | A polling interval elapses |
| `ReflexMatchZigbeeAttribute` | A Zigbee attribute value is received |
| `ReflexMatchZigbeeIasZoneStatus` | IAS Zone (security sensor) status changes |
| `ReflexMatchZigbeeIasZoneEnroll` | IAS Zone enrollment occurs |
| `ReflexMatchAlertmeLifesign` | AlertMe device heartbeat received |

---

## Action Types

| Action | Does... |
|--------|---------|
| `ReflexActionSetAttribute` | Sets a device attribute on the hub model |
| `ReflexActionSendProtocol` | Sends raw bytes over Zigbee or Z-Wave |
| `ReflexActionSendPlatform` | Emits an event/message to the platform |
| `ReflexActionForward` | Forwards a matched message further up the stack |
| `ReflexActionDelay` | Wraps other actions with a time delay |
| `ReflexActionLog` | Writes to hub logs (TRACE/DEBUG/INFO/WARN/ERROR) |
| `ReflexActionOrdered` | Executes actions in sequence |
| `ReflexActionMulti` | Groups multiple actions |
| `ReflexActionAlertmeLifesign` | Handles AlertMe heartbeat logic |
| `ReflexActionZigbeeIasZoneEnroll` | Handles IAS Zone enrollment |
| `ReflexActionBuiltin` | Built-in special-purpose actions |

---

## Lifecycle

### 1. Definition (in driver `.groovy` files)
Driver authors declare reflex mode and handlers in Groovy. The `ReflexPlugin` adds reflex DSL support; `ReflexContext` collects matches and actions into a `ReflexDefinition`.

### 2. Build-time generation (`reflex-generator`)
`ReflexGeneratorTask` (Gradle) runs `ReflexGenerator.java`:
- Parses driver files via `GroovyDriverFactory`
- Compiles `ReflexMatchRegex` entries into minimized DFAs
- Validates serialization round-trips
- Serializes each `ReflexDriverDefinition` via `ReflexJson` (GSON with custom adapters) → JSON → GZIP → Base64

### 3. Hub sync (platform → hub)
The hub periodically sends a `SyncDevices` request to `driver-services` containing its current device states (`SyncDeviceInfo`: protocol address, driver name/version/hash, online status). The platform responds with `SyncDevicesResponse` containing the current `ReflexDriverDefinition` blobs (+ PIN hashes for keypads) for each device.

### 4. Hub storage (SQLite via `ReflexDao`)
```sql
CREATE TABLE reflexconfig (key TEXT PRIMARY KEY, value TEXT);
CREATE TABLE reflexes    (addr TEXT, key TEXT, value TEXT, PRIMARY KEY(addr,key));
CREATE TABLE drivers     (addr TEXT, key TEXT, value TEXT, PRIMARY KEY(addr,key));
```
Stores compressed driver definitions, per-device reflex state, and PIN codes.

### 5. Hub execution (`ReflexController` + `ReflexProcessor`)
- `ReflexController` loads reflexes from the DB at startup and creates a `ReflexProcessor` per device
- When a protocol or platform message arrives, the processor evaluates all match conditions
- Matching reflexes execute their action list immediately (set attribute, send protocol, emit event, etc.)
- Scheduling uses a Netty `HashedWheelTimer` for deferred/periodic actions

```
Incoming message (Zigbee/Z-Wave/Platform)
  → ReflexController
  → ReflexProcessor.handle()
  → evaluate ReflexMatch list
  → execute ReflexAction list
  → device state updated / protocol sent / event emitted to platform
```

---

## Special Features

**DFA optimization** — `ReflexMatchRegex` entries are compiled into Deterministic Finite Automata at build time (in `ReflexDriverDFA`), enabling efficient O(n) byte-pattern matching on binary protocol messages at runtime.

**Hub-local PIN verification** — Keypads (AlertMe, GreatStar) verify PIN codes locally using SHA-1 hashed with the place UUID. Platform sends hashed PINs down during sync; no cloud round-trip needed to arm/disarm.

**Degraded mode** — If a hub feature required by a reflex is unavailable, the processor marks the device as degraded and emits a `DevicesDegradedEvent` to the platform, which notifies the driver.

**Version tracking** — `HubReflex` capability attributes (`numDrivers`, `dbHash`, `numDevices`, `numPins`, `versionSupported`) let the platform monitor the hub's reflex state.

---

## Agent-Side Processing

### Class Roles

| Class | Role |
|-------|------|
| `ReflexController` | Central orchestrator: owns the processor map, routes messages, drives hub↔platform sync, provides scheduling and PIN verification |
| `ReflexProcessor` | Interface for anything that processes reflexes; defines the `State` enum (INITIAL → ADDED → CONNECTED/DISCONNECTED → REMOVED) |
| `AbstractReflexProcessor` | Base implementation: synchronized state transitions, calls lifecycle callbacks (`onAdded`, `onConnected`, etc.), persists state to SQLite after each transition |
| `ReflexDriverProcessor` | Adapts a generic `ReflexDriver` (compiled from DSL) to the `ReflexProcessor` interface; delegates all message handling to the driver and calls `ctx.commit()` after each message |
| `ReflexDriverHubContext` | Execution environment handed to the driver: holds current attribute state, dirty/emit maps, transient variables, and the pending response message; `commit()` persists and emits |
| `AbstractHubDriver` | Base for builtin hardware-specific drivers (keypad variants); typesafe `Variable<T>` state, scheduled tasks, attribute emission |
| `HubDrivers` | Registry of builtin `HubDriver.Factory` instances keyed by driver name+version (CentraLiteKeyPad, GreatStarKeyPad, AlertmeKeyPad) |
| `ReflexDriverFactory` | Weak-valued cache of compiled `ReflexDriver` instances keyed by `name-version-hash`; avoids recompiling the same driver |
| `ReflexDao` | SQLite persistence: three tables (`reflexconfig`, `reflexes`, `drivers`); async writes for attributes |
| `ReflexLocalProcessing` | Bridge from alarm controller → reflexes; translates arm/disarm/alert state changes into `MessageBody` events delivered to all processors |

### Threading Model

All reflex execution runs on a **single port thread** (the Router's executor). There is no locking needed on the processor map. The only synchronization is a brief `synchronized` block in `AbstractReflexProcessor` for state transitions.

```
Netty HashedWheelTimer
  └─ enqueues Runnable → port.queue(task)

Protocol/Platform message arrives
  └─ Router delivers → port thread

port thread:
  recv(message)
    └─ processor.handle(message)
         └─ ReflexDriver.handle(ctx, msg)   ← reflex pattern matching + actions
    └─ ctx.commit()
         ├─ ReflexDao.putDriverState()      ← async SQLite write
         └─ emit attributes to platform
```

### Startup and Recovery Sequence

```
1. ReflexDao.start()
     └─ Opens SQLite DB; loads reflexconfig into memory

2. Load last-known drivers:
     ReflexDao.getReflexDB()         → gzip+base64 blob
     ReflexDao.getAllReflexStates()   → per-device {driver, version, state}
     ReflexDao.getAllDriverStates()   → per-device attribute key-value pairs

3. applyDeviceReflexes()
     For each device in reflexStates:
       a. Determine driver type (builtin via HubDrivers registry, or generic)
       b. Create ReflexDriver via ReflexDriverFactory (cache by name-version-hash)
       c. Wrap in ReflexDriverProcessor (or HubDriver impl)
       d. Call processor.start(driverState, persistedState)
            └─ Restores attributes from DB strings (type-coerced)
            └─ If state == INITIAL → calls onAdded() immediately
```

### Hub ↔ Platform Sync Sequence

Triggered when the hub transitions to the `AUTHORIZED` lifecycle state, and again after device add/remove operations.

```
Hub                                              Platform (driver-services)
 │                                                        │
 │─── SyncDevicesRequest ─────────────────────────────────▶│
 │    (compressed per-device: driver, version, hash,       │
 │     online status, sync-state attributes)               │
 │                                                         │
 │                              computes current reflexes  │
 │                              for each device            │
 │                                                         │
 │◀── SyncDevicesResponse ────────────────────────────────│
 │    (compressed driver JSON blobs + PIN hashes)          │
 │                                                         │
 │    handleReflexSyncResponse()                           │
 │      ├─ verify sync token (stale responses discarded)   │
 │      ├─ ReflexDao.putReflexDB() ← persist new drivers  │
 │      ├─ applyDeviceReflexes()  ← install/update procs  │
 │      └─ update pin map + emit degraded events if needed │
```

Failures retry with exponential backoff (90 s initial, 15 min max).

### Message Routing: Protocol Message

```
ProtocolMessage arrives from Zigbee/Z-Wave
  │
  ▼
ReflexController.recv(Port, ProtocolMessage)
  │
  ├─ ControlProtocol (DeviceOnline/Offline)?
  │    └─ setCurrentState(CONNECTED / DISCONNECTED)
  │         └─ calls onConnected() / onDisconnected() on processor
  │         └─ forward to GATEWAY_ADDRESS (platform)
  │
  └─ Device protocol frame?
       └─ processor.handle(ProtocolMessage)
            └─ ReflexDriver.handle(ctx, msg, V0)
                 ├─ Evaluates ReflexMatch list against message bytes (DFA or field checks)
                 ├─ On match: executes ReflexAction list
                 │    ├─ setAttribute  → ctx.setAttribute()
                 │    ├─ sendProtocol  → Zigbee/Z-Wave raw bytes
                 │    ├─ sendPlatform  → ctx.emit()
                 │    └─ delay         → schedules wrapped actions
                 └─ returns consumed flag
            └─ ctx.commit()
                 ├─ persist dirty attrs → ReflexDao (async)
                 └─ emit changed attrs → platform
       └─ if NOT consumed → forward to GATEWAY_ADDRESS
```

### Message Routing: Platform Message

```
PlatformMessage arrives (SetAttributes, custom command, etc.)
  │
  ▼
ReflexController.recv(Port, PlatformMessage)
  └─ processor.handle(PlatformMessage)
       └─ ReflexDriver.handle(ctx, msg)
            ├─ ReflexMatch evaluation (attribute value checks, message type)
            ├─ ReflexAction execution
            └─ ctx.getAndResetResponse() → sent as reply if set
       └─ ctx.commit()
  └─ if NOT consumed → forward to platform
```

### ReflexDriverHubContext — State Lifecycle Per Message

```
Before handling:    ctx fields reset (attrs={}, variables={}, response=null, handled=false)
During handling:    driver calls ctx.setAttribute(), ctx.emit(), ctx.setResponse()
After handling:     ctx.commit()
                      ├─ putDriverState(addr, dirty map)  [async SQLite]
                      └─ if attrs non-empty → emit ValueChangeEvent to platform
```

### SQLite Schema

```sql
reflexconfig (key TEXT PK, value TEXT)
  reflexdb      -- gzip+base64 JSON blob of all ReflexDriverDefinitions
  reflexdbpins  -- JSON: { "userUUID": "base64(sha1(placeUUID+pin))", ... }

reflexes (addr TEXT, key TEXT, value TEXT, PK(addr,key))
  -- per-device: driver name, version, current state

drivers (addr TEXT, key TEXT, value TEXT, PK(addr,key))
  -- per-device: all persisted attribute/variable values (string-encoded)
```

### PIN Verification (Hub-Local Keypad Security)

```
Keypad sends PIN bytes
  └─ AbstractHubDriver.verifyPinCode(bytes)
       └─ ReflexController.verifyPinCode(String code)
            └─ hash = base64(sha1(placeUUID + code))
            └─ pinToUser.get(hash)   → UUID or null
  └─ Driver arms/disarms alarm locally — no cloud round-trip needed
```

### Builtin vs. Generic Driver Selection

During `applyDeviceReflexes()`, the controller inspects each `ReflexDriverDefinition`:
- **Builtin** — has exactly one `ReflexActionBuiltin` action → looked up in `HubDrivers` registry by name+version; hardware-specific `AbstractHubDriver` subclass instantiated
- **Generic** — all other drivers → wrapped in `ReflexDriverProcessor` + `ReflexDriverHubContext`

### Environment Variables

| Variable | Effect |
|----------|--------|
| `IRIS_AGENT_DISABLE_LOCAL_PROCESSING` | Skip all local reflex execution; all messages forwarded directly to platform |
| `IRIS_AGENT_REFLEX_LOGGING` | Enable debug-level logging inside `ReflexDriverHubContext` |

---

## Key Files

| File | Location |
|------|----------|
| `ReflexDefinition.java` | `common/arcus-drivers/drivers-common/.../driver/reflex/` |
| `ReflexMatch*.java` (9 files) | same |
| `ReflexAction*.java` (15 files) | same |
| `ReflexJson.java` | same — GSON type adapters |
| `ReflexDriverDefinition.java` | same — full driver def + DFA construction |
| `ReflexRunMode.java` | same |
| `ReflexPlugin.java` | `common/arcus-drivers/groovy-bindings/.../driver/groovy/reflex/` |
| `ReflexContext.java` | same — builder used by Groovy DSL |
| `ReflexGenerator.java` | `common/arcus-drivers/reflex-generator/` |
| `ReflexGeneratorTask.groovy` | same |
| `ReflexController.java` | `agent/arcus-reflex-controller/` |
| `AbstractReflexProcessor.java` | same |
| `ReflexDriverProcessor.java` | same |
| `ReflexDriverHubContext.java` | same |
| `ReflexDao.java` | same — SQLite persistence |
| `ReflexDriverFactory.java` | same — weak-valued driver cache |
| `ReflexLocalProcessing.java` | same — alarm→reflex bridge |
| `HubDrivers.java` | `agent/arcus-reflex-controller/.../reflex/drivers/` |
| `AbstractHubDriver.java` | same |
| `PlatformDriverReflexContext.java` | `platform/arcus-containers/driver-services/` |
| `hubreflex.xml` | `common/arcus-model/src/main/resources/capability/` |
| `syncdeviceinfo.xml` / `syncdevicestate.xml` | `common/arcus-model/src/main/resources/type/` |
