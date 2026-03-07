# ZigBee Controller Implementation Status

## Overview

The `arcus-zigbee-controller` module provides hub-local ZigBee device management using the
[zsmartsystems](https://github.com/zsmartsystems/com.zsmartsystems.zigbee) library (v1.4.16) as the
ZigBee protocol engine. It follows the same architectural patterns as `arcus-zw-controller`.

The module handles:
- ZigBee network lifecycle (bootstrap, join/leave, shutdown)
- Device discovery, pairing, and removal
- Inbound/outbound ZCL message translation
- Node persistence and offline detection
- Integration with the hub reflex driver system

## Architecture

```
Platform Messages                    ZigBee Radio
       |                                  |
  ZigbeeController                 ZigbeeEmberDriver
  (PortHandler)                   (ZigBeeNetworkManager)
       |                                  |
       +----------+  +-------------------+
                  |  |
            ZBMessageTranslator
          (Arcus <-> zsmartsystems)
                  |
              ZBNetwork
           (node state maps)
                  |
               ZBDao
            (SQLite via DbService)
```

**Key integration points:**
- `ZigbeeController` connects to the hub router as a `PortHandler` on address `ZIGB`
- `ReflexController` injects `ZigbeeLocalProcessing` for hub driver device operations
- `AbstractZigbeeHubDriver` exposes `bind()`, `read()`, `write()`, `zcl()`, `zclmsp()` to concrete drivers
- Guice bindings in `IrisHalImpl.ZigbeeModuleHubV2` wire everything together

## File Inventory

### Controller & Driver Layer

| File | Status | Description |
|------|--------|-------------|
| `ZigbeeController.java` | **Complete** | Main controller: PortHandler, LifeCycleListener, ZBEventListener. Routes platform messages, dispatches device add/remove/online/offline, handles pairing requests. |
| `ZigbeeDriverFactory.java` | **Complete** | Abstract factory with `create()` method. |
| `ZigbeeEmberDriverFactory.java` | **Complete** | Concrete factory: configures serial port, baud rate, flow control from IrisHal. |
| `ember/ZigbeeDriver.java` | **Complete** | Interface: `initialize()`, `shutdown()`, `permitJoin()`, `denyJoin()`, `leave()`, `send()`, `getNetworkManager()`, `getCoordinatorEui64()`. Defines `ZBNetworkCallbacks` inner interface. |
| `ember/ZigbeeEmberDriver.java` | **Complete** | Wraps `ZigBeeDongleEzsp` + `ZigBeeNetworkManager`. Registers node/command/announce listeners that bridge to `ZBNetworkCallbacks`. |

### Local Processing (Reflex Driver API)

| File | Status | Description |
|------|--------|-------------|
| `ZigbeeLocalProcessing.java` | **Complete** | Interface: 13 methods for hub drivers to interact with ZigBee devices. |
| `ZigbeeLocalProcessingDefault.java` | **Partial** | See [Gaps](#gaps-in-local-processing) below. |
| `ZigbeeLocalProcessingNoop.java` | **Complete** | No-op implementation used when `ZIGBEE_DISABLE` env var is set. |

### Message Translation

| File | Status | Description |
|------|--------|-------------|
| `ZBMessageTranslator.java` | **Complete** | Bidirectional translation. Inbound: `ZigBeeCommand` -> `ZigbeeMessage.Protocol` -> `ProtocolMessage`. Outbound: routes by message type (Zcl, Zdp, SetOfflineTimeout, Control). Filters OTA cluster messages (0x0019). |

### Network & Node Model

| File | Status | Description |
|------|--------|-------------|
| `ZBNetwork.java` | **Complete** | Three `ConcurrentHashMap`s: IEEE addr, NWK addr, ProtocolDeviceId. Loads from DB on init. Listens for events to update state. |
| `node/ZBNode.java` | **Complete** | Node entity: 16 persisted fields + in-memory `strikes`/`lastCall`. Computes `ProtocolDeviceId` from EUI-64. Online state changes dispatch events. |
| `node/ZBNodeBuilder.java` | **Complete** | Builder for incremental node construction during pairing. |

### Database Layer

| File | Status | Description |
|------|--------|-------------|
| `db/ZBDao.java` | **Complete** | Static DAO: schema setup from classpath, config key-value cache, full node CRUD. |
| `db/ZBBinders.java` | **Complete** | `DbBinder` implementations for config and node insert/update/delete. |
| `db/ZBExtractors.java` | **Complete** | `DbExtractor` implementations for config and node result sets. |
| `db/KeyValuePair.java` | **Complete** | Simple key-value DTO for config storage. |
| `resources/sql/zigbee.sql` | **Complete** | Schema: 6 tables. See [Unused Tables](#unused-database-tables) below. |

### Services & Infrastructure

| File | Status | Description |
|------|--------|-------------|
| `ZBServices.java` | **Complete** | Singleton service locator: holds ZBNetwork, ZigbeeDriver, ZBOfflineService. |
| `service/ZBOfflineService.java` | **Complete** | Periodic offline detection with adaptive timing, strike counting, sleepy device handling. |
| `util/ZBConfig.java` | **Complete** | Constants: offline check period (60s), minimum timeout (300s), strike threshold (2). |
| `util/ZBScheduler.java` | **Complete** | Singleton `ScheduledExecutorService` wrapper (4 threads). |

### Event System

| File | Status | Description |
|------|--------|-------------|
| `events/ZBEvent.java` | **Complete** | Interface + `ZBEventType` enum (12 types). |
| `events/ZBEventListener.java` | **Complete** | Functional interface. |
| `events/ZBEventDispatcher.java` | **Complete** | Singleton with `CopyOnWriteArraySet`. |
| `events/ZBBootstrapFinishedEvent.java` | **Complete** | |
| `events/ZBNodeAddedEvent.java` | **Complete** | Carries `ZBNode`. |
| `events/ZBNodeRemovedEvent.java` | **Complete** | Carries `ieeeAddr`. |
| `events/ZBNodeCommandEvent.java` | **Complete** | Carries `ieeeAddr` + `ZigbeeMessage.Protocol`. |
| `events/ZBNodeHeardFromEvent.java` | **Complete** | Carries `ieeeAddr`. |
| `events/ZBNodeGoneOnlineEvent.java` | **Complete** | Carries `ieeeAddr`. |
| `events/ZBNodeGoneOfflineEvent.java` | **Complete** | Carries `ieeeAddr`. |
| `events/ZBNodeOfflineTimeoutEvent.java` | **Complete** | Carries `ieeeAddr` + timeout. |

### Process / Lifecycle

| File | Status | Description |
|------|--------|-------------|
| `process/ZBBootstrapper.java` | **Complete** | Orchestrates startup: stores driver, starts DAO, initializes network, wires callbacks, dispatches bootstrap event. |
| `process/ZBPairing.java` | **Complete** | `startPairing()` calls `permitJoin()`, `stopPairing()` calls `denyJoin()`. Removal mode sends `leave()`. Auto-stop via scheduler. |

## What Needs To Be Done

### Gaps in Local Processing

`ZigbeeLocalProcessingDefault` has two stub methods that hub drivers actively call:

**`zcl()` — Stub (logs and returns true, does not send)**

Called by `CentraLiteKeyPad`, `GreatStarKeyPad`, and `AlertmeKeyPad` to send:
- `General.ZclConfigureReporting` — attribute reporting setup
- `General.ZclWriteAttributes` / `ZclWriteAttributesNoResponse` — attribute writes
- `General.ZclReadAttributes` — attribute reads
- `IasZone.ZoneEnrollResponse` — zone enrollment
- `IasAce.PanelStatusChanged`, `IasAce.ArmResponse`, `IasAce.BypassResponse` — alarm panel responses
- `General.ZclDefaultResponse` — default responses

The implementation needs to:
1. Serialize the `ProtocMessage` into a raw ZCL payload (using Arcus protoc serialization)
2. Build a zsmartsystems `ZclCommand` (or use raw frame sending via `ZigBeeNetworkManager`)
3. Send via the network manager
4. Return `Observable<Boolean>` with the result

Drivers subscribe with `.subscribe(RxIris.SWALLOW_ALL)` (fire-and-forget), so the return
value matters less than actually transmitting the frame.

**`zclmsp()` — Stub (logs and returns true, does not send)**

Called by `CentraLiteKeyPad` and `GreatStarKeyPad` for manufacturer-specific commands:
- Manufacturer 0x104E, cluster 0xFC04, command 0x00 (chime)

The implementation needs to:
1. Build a manufacturer-specific ZCL frame with the provided command ID + raw data
2. Send via the network manager
3. Return `Observable<Boolean>`

### Lower Priority Gaps

**`addScheduledPoll()`** — Logs "not yet fully implemented". No current callers (commented
out in `ReflexDriverHubContext`). Would need to schedule periodic attribute reads for
battery-powered devices.

**`send(Address, Protocol.Message)`** — The interface signature takes a Z-Wave
`Protocol.Message` type, which is a copy-paste error from `ZWaveLocalProcessing`. No
current callers. Would need an interface change to accept `ZigbeeMessage.Protocol` instead.

**`ReflexDriverHubContext.zigbeeSend()`** — Commented out at line 259. This is the path for
reflex-generated ZigBee sends. Uncommenting requires a working `send()` with the correct
type signature.

### Unused Database Tables

The schema defines 4 tables that have no DAO support:

| Table | Purpose | Why It Matters |
|-------|---------|----------------|
| `zigbee_profile` | Profile-to-node mapping | Would allow persisting which ZigBee profiles a device supports |
| `zigbee_endpoint` | Endpoint details (device type, ZCL version, manufacturer name, model, power source) | Would allow persisting device introspection data instead of querying live |
| `zigbee_cluster` | Cluster-to-endpoint mapping | Would allow persisting supported clusters |
| `zigbee_attribute` | Attribute values (last known value as BLOB) | Would allow caching attribute state across restarts |

These could be populated during device discovery/interview to provide richer device
metadata without requiring live queries to sleeping devices. Not strictly required for
basic operation.

### Other Minor Items

- `ZigbeeController.hubDeregistered()` — Empty with `//TODO: Anything?` comment. May need
  to factory-reset the ZigBee network or remove all nodes when the hub is deregistered.
- `ZBPairing.onZBEvent()` — Empty listener implementation.
- `ZBConfig` — All values are hardcoded constants. Could be made configurable via
  `ZBDao` config table.

## Build & Verification

```bash
# Set Java 11 (required)
export JAVA_HOME=/path/to/java-11

# Module build
./gradlew :agent:arcus-zigbee-controller:compileJava

# Verify dependent module
./gradlew :agent:arcus-reflex-controller:compileJava

# Full agent build (includes Guice wiring check)
./gradlew :agent:arcus-agent:compileJava
```

All three pass as of the initial commit.

## Key Design Decisions

- **zsmartsystems as engine**: Unlike Z-Wave's custom protocol engine (~138 files), the
  ZigBee controller delegates to `ZigBeeNetworkManager`, keeping the implementation at ~30
  files.
- **Dual address maps**: ZigBee nodes have permanent 64-bit IEEE addresses and changing
  16-bit NWK addresses. `ZBNetwork` maintains both plus a `ProtocolDeviceId` map.
- **Protocol address**: Uses EUI-64 as 8-byte `ProtocolDeviceId` (little-endian, matching
  `KitUtil.zigbeeIdToProtocolId`), hub bridge address `ZIGB`.
