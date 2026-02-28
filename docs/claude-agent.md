# Agent — Hub/Gateway Software (`/agent`)

The agent runs on Iris Hub v2/v3 hardware, managing ZigBee and Z-Wave radios, executing local device reflexes, handling security alarms, and maintaining a persistent WebSocket connection to the Arcus cloud platform. It is organized as 13+ Gradle submodules with a Guice plugin architecture.

---

## Table of Contents

- [Module Overview](#module-overview)
- [Dependency Graph](#dependency-graph)
- [Key External Dependencies](#key-external-dependencies)
- [Startup and Lifecycle](#startup-and-lifecycle)
- [Message Routing](#message-routing)
- [Cloud Gateway](#cloud-gateway)
- [Hub Controller](#hub-controller)
- [Reflex Controller](#reflex-controller)
- [Z-Wave Controller](#z-wave-controller)
- [Alarm Controller](#alarm-controller)
- [Hardware Abstraction Layer](#hardware-abstraction-layer)
- [Database and Storage](#database-and-storage)
- [Configuration System](#configuration-system)
- [Spy Controller](#spy-controller)
- [Key Design Patterns](#key-design-patterns)
- [Message Flow Examples](#message-flow-examples)
- [Running Locally](#running-the-agent-locally-simulated-mode)
- [Build Output](#build-output)
- [Known Missing Components](#known-missing-components-closed-source)

---

## Module Overview

| Module | Purpose |
|--------|---------|
| `arcus-agent` | Main entry point (`IrisAgent`); loads config, bootstraps Guice |
| `arcus-system` | Core services: lifecycle, config, DB, storage, addressing, HAL facade, metrics, SSL, logging, watchdog |
| `arcus-hal/api` | Hardware Abstraction Layer interfaces (`IrisHal`, `IrisHalInternal`, `LEDState`, `SounderMode`) |
| `arcus-hal/common` | Shared HAL base implementations |
| `arcus-hal/hub-v2` | Hub v2/v3 HAL: LED, buzzer, reset button, battery, WiFi, watchdog, OS calls |
| `arcus-hal/simulated` | Mock HAL for desktop testing (no real I/O) |
| `arcus-gateway` | Netty WebSocket client — hub-to-platform connectivity with dual-interface failover |
| `arcus-router` | Async message routing with address-based dispatch (`LinkedTransferQueue`) |
| `arcus-hub-controller` | Hub device attributes, capabilities, and top-level message dispatch |
| `arcus-reflex-controller` | Local rule/automation execution (reflexes) — see [claude-reflexes.md](claude-reflexes.md) |
| `arcus-alarm-controller` | Hub-local security/safety alarm state machine |
| `arcus-zigbee-controller` | ZigBee protocol (zsmartsystems 1.2.4) — stub, under development |
| `arcus-zw-controller` | Z-Wave protocol (Z/IP engine) |
| `arcus-os` | OS abstraction via JNA + Netty epoll (serial ports, watchdog) |
| `arcus-spy-controller` | Diagnostic message snooping (enabled via `IRIS_HUB_SPY_ACTIVE`) |
| `arcus-test-agent` | Shared test utilities and mocks |

---

## Dependency Graph

```
arcus-agent (entry point)
 +-- arcus-system (core foundation)
 +-- arcus-hub-controller
 |    +-- arcus-router --> arcus-system
 |    +-- arcus-reflex-controller
 |    |    +-- arcus-zw-controller
 |    |    +-- arcus-zigbee-controller
 |    +-- arcus-alarm-controller --> arcus-reflex-controller
 |    +-- arcus-zw-controller --> arcus-os
 |    +-- arcus-zigbee-controller --> arcus-os
 +-- arcus-hal:arcus-hal-api

arcus-gateway
 +-- arcus-system
 +-- arcus-router

arcus-spy-controller
 +-- arcus-system
 +-- arcus-router
```

## Key External Dependencies

| Library | Purpose |
|---------|---------|
| Netty | Async I/O (WebSocket, epoll) |
| SQLite4Java | Embedded database with platform-specific native bindings |
| Google Guice | Dependency injection (PRODUCTION stage) |
| RxJava | Reactive streams (Z-Wave scenes, reflex processing) |
| GSON | JSON serialization for platform messages |
| BouncyCastle | Cryptography and TLS |
| zsmartsystems ZigBee (v1.2.4) | ZigBee network manager and Ember EZSP dongle support |

---

## Startup and Lifecycle

### Entry Point

**`IrisAgent.java`** (`arcus-agent/.../com/iris/agent/IrisAgent.java`)

The agent's `main()` method:

1. Initializes logging (`IrisAgentLogging.setupInitialLogging()`) — selects DEV or STDOUT mode based on `IRIS_AGENT_LOGTYPE` env var
2. Starts the `WatchdogService` and registers a 5-minute startup timeout
3. Calls `BootUtils.initialize()` with the base path (first arg) and config directories (remaining args)
4. Blocks on `IrisHal.waitForShutdown()` until the agent is told to exit

### Bootstrap Sequence

**`BootUtils.java`** (`arcus-system/.../boot/BootUtils.java`)

The bootstrap builds a Guice injector from multiple module sources:

1. `IrisHal.start(basePath, configs)` — Initialize hardware abstraction and load config files
2. Collect bootstrap modules and application modules from the HAL
3. Create Guice injector in `PRODUCTION` stage with:
   - `LifecycleModule` (always first)
   - `AgentConfigurationProvider`
   - HAL bootstrap modules
   - HAL application modules
   - `MessagesModule`, `ProtocolMessagesModule`, `GsonModule`, `AgentModule`
4. `Bootstrap.bootstrap()` — Create injector
5. `IrisLifecycleManager.start()` — Trigger `@PostConstruct` and `@WarmUp` lifecycle phases on all bound services
6. Register JVM shutdown hook to transition to `SHUTTING_DOWN`

### Lifecycle States

**`LifeCycleService.java`** (`arcus-system/.../lifecycle/LifeCycleService.java`)

```
INITIAL --> STARTING_UP --> STARTED --> CONNECTING --> CONNECTED --> AUTHORIZED --> SHUTTING_DOWN
```

All controllers implement `LifeCycleListener` and receive callbacks on state transitions. The `LifeCycleService` uses synchronization locks and notifies all registered listeners. Reset modes (`FACTORY`, `SOFT`) are also handled here.

### Lifecycle Annotations

Services use Guice lifecycle annotations to participate in startup:

- `@PostConstruct` — Called during injector creation; used for initial setup and port registration
- `@WarmUp` — Called after all `@PostConstruct` methods; used for starting background threads, opening connections
- `@PreDestroy` — Called during shutdown; used for cleanup

---

## Message Routing

**`Router.java`** (`arcus-router/.../router/Router.java`)

The Router is the agent's internal message bus. All controllers communicate through it using address-based routing.

### Message Types

| Type | Description |
|------|-------------|
| `PLATFORM` | `PlatformMessage` — service-level commands and responses |
| `PROTOCOL` | `ProtocolMessage` — raw device protocol frames (ZigBee ZCL, Z-Wave commands) |
| `CUSTOM` | Generic object messages for internal signaling |
| `POISON` | Shutdown/disconnect signals |

### Port Types

| Port | Description |
|------|-------------|
| `ServicePort` | Binds to a service address (e.g., "hub", "alarm", "reflex"); receives platform messages |
| `BridgePort` | Binds to a protocol bridge address (e.g., "zigbee", "zwave"); receives protocol messages |
| `InjectingPort` | One-way port that can inject messages into the router |
| `SnoopingPort` | Receives all messages (used by Gateway, Alarm, Spy) |

### Dispatch Logic

The router runs a single `MessageDispatcher` thread consuming from a `LinkedTransferQueue`:

1. Look up port by service ID from the message's destination address
2. Look up port by protocol ID from the message's destination address
3. If the message was forwarded to a specific port, broadcast to all snoopers
4. Custom messages are routed to a single named port

### Port Handler Interface

```java
public interface PortHandler {
    Object recv(Port port, PlatformMessage msg);   // Returns response body or null
    void recv(Port port, ProtocolMessage msg);      // Protocol frame delivery
    void recv(Port port, Object msg);               // Custom message delivery
}
```

The `SnoopingPortHandler` extends this with `isInterestedIn(Message)` filtering, allowing selective message monitoring.

### Controller Registration

Each controller registers with the router during `@PostConstruct`:

```java
// Service port (receives platform messages addressed to "hub")
this.port = router.connect("hub-ctrl", HubAddressUtils.service("hub"), this);

// Bridge port (receives protocol messages for zigbee devices)
this.port = router.connect("zigb", HubAddressUtils.bridge("zigbee", "ZBIG"), this);

// Snooping port (sees all messages — used by gateway, alarm controller)
this.port = router.gateway("gtwy", handler, ADDRESS, portHandler);
```

---

## Cloud Gateway

**`Gateway.java`** (`arcus-gateway/.../gateway/Gateway.java`)

The Gateway manages the persistent connection between the hub agent and the Arcus cloud platform.

### Protocol

- **WebSocket Secure (WSS)**: Default URI `wss://bh.irisbylowes.com/hub/1.0` (configurable via `iris.gateway.uri`)
- **Message Format**: JSON-serialized `PlatformMessage` and `ProtocolMessage`, with optional GZIP compression for binary frames
- **TLS**: Mutual TLS with pinned hub certificates (BouncyCastle or OpenSSL provider)

### Dual-Interface Connectivity

The gateway supports primary and secondary network interfaces for failover:

| Parameter | Primary | Secondary |
|-----------|---------|-----------|
| Initial connect delay | 0s | 60s |
| Initial backoff | 0s | 9s |
| Backoff delay | 1s | 1s |
| Backoff factor | 2.0x | 2.0x |
| Max backoff | 90s | 90s |

If the primary connection is lost, the secondary takes over after a 10-second delay.

### Connection Lifecycle

1. **CONNECTING** — Netty event loop created, WebSocket handshake initiated
   - Connect timeout: 90s (`iris.gateway.timeout.connect`)
   - SSL handshake timeout: 90s (`iris.gateway.timeout.ssl.handshake`)
2. **CONNECTED** — WebSocket established, hub sends `hub:connected` event with network attributes
3. **AUTHORIZED** — Hub receives `hub:registered` with account/place IDs, then authorization confirmation
4. **Operational** — Messages flow bidirectionally; queued outbound messages are flushed

### Health Monitoring

| Mechanism | Interval / Threshold | Behavior |
|-----------|---------------------|----------|
| Ping/Pong | Every 5 seconds | Detect broken connections |
| Idle timeout | 17 seconds no platform message | Force reconnect |
| Connection report | Every 15 minutes | Log connection status |
| Auth timeout | 10 minutes connected but unauthorized | Force reconnect |
| Force reboot | 30 minutes no connection | Watchdog reboots hub (configurable via `IRIS_AGENT_UNCONN_REBOOT_TIME`) |

### Message Buffering

**`GatewayOutboundQueue.java`** — Queues both platform and protocol messages while disconnected. All queued messages are flushed when the connection is authorized.

### Log Shipping

**`LogSender`** — Buffers log entries in memory (1024 capacity) and ships them to the cloud every 1 second over the primary connection. Includes full stack traces.

### Supporting Classes

| Class | Purpose |
|-------|---------|
| `GatewayNetty.java` | Netty transport setup (event loop, SSL context) |
| `GatewayConnection.java` | Individual WebSocket connection management |
| `GatewayHandler.java` | Message encode/decode pipeline |
| `GatewayDns.java` | DNS resolution |
| `GatewayNetworkChecker.java` | Network interface change detection |
| `GatewayPokeHandler.java` | Keepalive mechanism |

---

## Hub Controller

**`HubController.java`** (`arcus-hub-controller/.../controller/hub/HubController.java`)

The Hub Controller is the top-level controller that represents the hub as a device in the Arcus system. It manages hub attributes, orchestrates pairing, and delegates protocol-specific messages to the appropriate sub-controllers.

### Responsibilities

- **Hub attributes**: Serial number, MAC address, firmware version, hardware model, manufacturing info
- **Pairing orchestration**: Forwards pairing/unpairing requests to ZigBee and Z-Wave controllers
- **LED management**: Sets LED state based on lifecycle, connection status, and pairing mode
- **Hub capabilities**: Exposes the hub as a device with a rich set of capabilities
- **Message dispatch**: Routes incoming messages to the correct sub-controller by type

### Hub Capabilities

```
HubCapability              HubAdvancedCapability      HubConnectionCapability
HubMetricsCapability       HubNetworkCapability       HubPowerCapability
HubVolumeCapability        HubZigbeeCapability        HubZwaveCapability
HubChimeCapability         HubSoundsCapability        HubBackupCapability
HubDebugCapability         Hub4gCapability            HubReflexCapability
HubAlarmCapability         HubKitCapability           HubWiFiCapability (if supported)
HubButtonCapability (if supported)
```

### Handler Classes

The Hub Controller delegates to specialized singleton handlers:

| Handler | Responsibility |
|---------|---------------|
| `ConfigHandler` | Hub configuration management |
| `MetricsHandler` | Metrics collection and reporting |
| `FirmwareUpdateHandler` | OTA firmware updates |
| `LoggingHandler` | Remote logging configuration |
| `BackupHandler` | Hub backup/restore operations |
| `SoundHandler` | Audio/chime playback |
| `PowerHandler` | Power state management |
| `DebugHandler` | Debug and diagnostic operations |
| `WirelessHandler` | WiFi/network management (if supported) |
| `ButtonIrisHandler` | Physical button press handling (if supported) |
| `HubAttributeReporter` | Attribute delta reporting to platform |

### Hub State

The hub operates in one of three modes: `NORMAL`, `PAIRING`, or `UNPAIRING`.

### Reset Handling

On startup, the Hub Controller checks for factory reset flag files, soft reset flag files, and database corruption logs, triggering the appropriate recovery path.

---

## Reflex Controller

**`ReflexController.java`** (`arcus-reflex-controller/.../reflex/ReflexController.java`)

The Reflex Controller enables hub-local device processing. It intercepts device messages at the router level and, when possible, handles them locally without requiring cloud connectivity. See [claude-reflexes.md](claude-reflexes.md) for full details on the reflex system.

### Local Processing Decision Gate

```
Device message arrives
       |
       v
ReflexController.recv()
       |
       v
Find ReflexProcessor for device address
       |
       v
processor.handle(msg) --> true:  consumed locally, not forwarded
                      --> false: forwarded to Gateway --> Cloud
```

This is the clean boundary between local and cloud processing.

### Driver Types

**Reflex Drivers** (compiled from Groovy DSL):
- Groovy drivers with `reflex {}` blocks are compiled into `ReflexDriverDefinition` by the platform
- Limited to pattern-matching actions (attribute sets, protocol sends, forwards)
- Definitions downloaded from platform during periodic sync (every 90s with exponential backoff)
- Stored compressed (GZIP, base64) in the local database via `ReflexDao`

**Builtin Java Hub Drivers** (hand-coded):
- Extend `AbstractHubDriver` or `AbstractZigbeeHubDriver`
- Registered in `HubDrivers.java` factory map
- Full protocol handling with state management
- Currently 3 drivers: `CentraLiteKeyPad`, `GreatStarKeyPad`, `AlertmeKeyPad`

### Cloud Sync

The controller periodically syncs with the cloud to:
- Download updated reflex driver definitions
- Receive device state and PIN codes for user authentication
- Track driver hash for change detection
- Manage driver lifecycle states: `INITIAL`, `CONNECTED`, `DISCONNECTED`, `REMOVED`

### Local Processing Interfaces

Protocol-specific local processing is delegated through interfaces:

```java
ZigbeeLocalProcessing  // ZigBee local processing
ZWaveLocalProcessing   // Z-Wave local processing
```

Disabled entirely via `IRIS_AGENT_DISABLE_LOCAL_PROCESSING`.

---

## Z-Wave Controller

**`ZWaveController.java`** (`arcus-zw-controller/.../zwave/ZWaveController.java`)

The Z-Wave Controller manages the Z-Wave radio and all Z-Wave devices connected to the hub. It uses a Z/IP (Z-Wave over IP) engine to communicate with the Z-Wave chip.

### Architecture

```
Z-Wave Radio (ttyO1)
       |
   Z/IP Engine (ZWServices)
       |
   ZWNetwork (node topology)
       |
   ZWaveController (PortHandler)
       |
   Router --> Reflex Controller / Gateway
```

### Key Classes

| Class | Role |
|-------|------|
| `ZWaveController` | Main controller: `PortHandler`, `LifeCycleListener`, `ZWEventListener` |
| `ZWNetwork` | Network topology: Home ID, node maps (`id2node`, `devid2node`) |
| `ZWRouter` | Routes decoded Z-Wave commands to handlers |
| `ZWaveEngine` | Core Z-Wave engine wrapper |
| `ZWNode` | Individual device representation |
| `ZWDao` | SQLite persistence for nodes and network state |
| `Bootstrapper` | Network initialization sequence |
| `Pairing` | Device inclusion/exclusion |

Port registration: `router.connect("zwav", HubAddressUtils.bridge("zwave", "ZWAV"), this)`

### Network Management

`ZWNetwork` maintains:
- **Home ID**: Unique network identifier (`Long.MIN_VALUE` sentinel = uninitialized)
- **Node maps**: `id2node` (int -> ZWNode), `devid2node` (ProtocolDeviceId -> ZWNode)
- **Gateway node**: The hub's own Z-Wave node ID

Implements `ZWCmdHandler` for processing decoded Z-Wave commands. Sends low-level commands: learn mode, basic get, node list, associations.

### Command System

Z-Wave uses a hierarchical command class / command ID structure:

- `AbstractZCmd`, `AbstractByteCmd` — Base command types
- `CmdClasses` — Command class registry
- `ZWDecoder` — Binary command decoding
- `ZWBuilders`, `AssociationBuilders`, `NetInclusionBuilders` — Command construction

### Protocol Message Types

| Type | Description |
|------|-------------|
| `Protocol.Command` | Single Z-Wave command |
| `Protocol.OrderedCommands` | Sequence of ordered commands |
| `Protocol.DelayedCommands` | Commands with inter-command delays |
| `Protocol.NodeInfo` | Node information (informational only) |
| `Protocol.SetOfflineTimeout` | Configure offline detection timeout |
| `Protocol.SetSchedule` | Schedule support |

### Event System

```java
enum ZWEventType {
    BOOTSTRAPPED, GONE_ONLINE, GONE_OFFLINE, NODE_ADDED,
    NODE_REMOVED, NODE_COMMAND, HEARD_FROM, OFFLINE_TIMEOUT
}
```

`ZWEventDispatcher` (singleton) uses `CopyOnWriteArraySet` for thread-safe listener management.

### Pairing

```java
Pairing.startPairing(int timeoutSecs)   // Enter inclusion mode
Pairing.stopPairing()                    // Exit inclusion mode
Pairing.startRemoval(int timeoutSecs)    // Enter exclusion mode
Pairing.stopRemoval()                    // Exit exclusion mode
```

Uses `ZWScheduler` for automatic timeout management.

### Scene Support

Optional RxJava-based scene handling (disabled via `IRIS_SCENE_ZWAVE_DISABLE`). Supports Z-Wave scene activation and deactivation commands.

---

## Alarm Controller

**`AlarmController.java`** (`arcus-alarm-controller/.../alarm/AlarmController.java`)

The Alarm Controller implements hub-local security and safety alarm processing. It runs entirely on the hub, enabling alarm functionality even when the cloud connection is down.

### Architecture

Registers as a `SnoopingPortHandler` on service address "alarm", intercepting device messages relevant to alarm state.

### Alarm Types

| Class | Type |
|-------|------|
| `AlarmSecurity` | Armed/disarmed security system |
| `AlarmSmoke` | Smoke detector coordination |
| `AlarmCo` | Carbon monoxide detection |
| `AlarmWater` | Water leak detection |
| `AlarmPanic` | Panic button handling |

All extend `AbstractAlarm` (or `AbstractSafetyAlarm` for smoke/CO/water).

### State Machine

Alarm states are priority-ordered (0-7):

```
Priority  AlertState      AlarmState
0         INACTIVE        INACTIVE
1         DISARMED        READY
2         ARMING          READY
3         READY           READY
4         PENDING_CLEAR   CLEARING
5         CLEARING        CLEARING
6         PREALERT        PREALERT
7         ALERT           ALERTING
```

State transitions are event-driven via `AlarmEvents.Event`. The controller processes events through the alarm state machine and updates the active alert state.

### Hardware Integration

When alarms trigger:
- LED state updated via `IrisHal` (red flash patterns)
- Sounder activated via `IrisHal.setSounderMode()`
- State reported to platform with 1-hour TTL

### Integration with Reflex Controller

Listens via `ReflexLocalProcessing.Listener` to receive device online/offline notifications and coordinate alarm state with local reflex processing.

---

## Hardware Abstraction Layer

**`IrisHal.java`** (`arcus-system/.../hal/IrisHal.java`)

The HAL provides a static facade for all hardware access, with platform-specific implementations.

### Sub-modules

| Module | Purpose |
|--------|---------|
| `arcus-hal/api` | Public API: `IrisHal`, `IrisHalInternal`, `LEDState`, `SounderMode`, `Model` |
| `arcus-hal/common` | Shared base implementation |
| `arcus-hal/hub-v2` | Real hardware impl for Iris v2/v3 hubs (~44KB) |
| `arcus-hal/simulated` | No-op implementation for development |

### Core API

```java
// Lifecycle
IrisHal.start(File base, Set<File> configs)
IrisHal.shutdown()
IrisHal.waitForShutdown()
IrisHal.restart()

// Hardware info
IrisHal.getHubId()           // Hub identifier (derived from MAC)
IrisHal.getSerialNumber()
IrisHal.getMacAddress()
IrisHal.getModel()           // IH200 or IH300

// Hardware control
IrisHal.setLedState(LEDState state)       // RED, GREEN, BLUE, OFF, BLINK_*
IrisHal.setSounderMode(SounderMode mode)
IrisHal.resetZigbeeChip()

// Capabilities
IrisHal.isBatteryPowered()
IrisHal.hasWirelessSupport()
IrisHal.hasButtonSupport()

// OS detection
IrisHal.getOperatingSystemType()  // LINUX, MAC, WINDOWS, UNKNOWN
```

### Hub v2 Hardware Controllers

| Controller | Purpose |
|-----------|---------|
| `LEDControl` | LED control via sysfs |
| `WirelessControl` | WiFi/network management |
| `ButtonIrisControl` | Button GPIO handling |
| `BatteryControl` | Battery monitoring |
| `WatchdogControl` | Hardware watchdog timer |

### Serial Port Mapping (Iris Hub)

```
/dev/ttyO0 - Console
/dev/ttyO1 - Z-Wave radio
/dev/ttyO2 - ZigBee radio (Ember EZSP)
```

---

## Database and Storage

### SQLite Database

**`DbService.java`** (`arcus-system/.../db/DbService.java`)

The agent uses SQLite4Java for local persistence with platform-specific native bindings.

| Feature | Detail |
|---------|--------|
| Threading | Single-threaded workers by default (configurable via `IRIS_DB_DISABLE_MULTITHREADED`) |
| Journal mode | WAL (Write-Ahead Logging) enabled by default |
| Worker pool | `SynchronousQueue` for task submission |
| DB types | File-based and in-memory |
| Schema | Versioned with migration scripts (e.g., `/sql/config.sql`, `/sql/update-config-1.sql`) |
| Named DBs | `DbService.get(String name)` for per-module isolation |
| Backup | SQLite backup API support |

### Storage Service

**`StorageService.java`** — URI-based file mapping with change monitoring:
- Maps URIs to file paths (e.g., `tmp://` protocol)
- Watches files for changes at configurable intervals
- Notifies listeners on content changes

---

## Configuration System

**`ConfigService.java`** (`arcus-system/.../config/ConfigService.java`)

### Priority Hierarchy (highest to lowest)

1. **Runtime database** (ConfigService SQLite table)
2. **System properties** (Java `-D` flags)
3. **Environment variables** (`IRIS_*` converted to `iris.*`, underscores to dots)
4. **Configuration files** (`*.conf` from config directories)
5. **Default values** (specified in code)

### API

```java
ConfigService.get(String key)                           // String or null
ConfigService.get(String key, Class<T> type, T def)     // With type conversion
ConfigService.supplier(String key, Class<T> type, T def) // Lazy supplier
ConfigService.put(String key, T value)                  // Write through to DB
```

### Key Configuration Properties

| Property | Description | Default |
|----------|-------------|---------|
| `iris.gateway.uri` | Cloud gateway WebSocket URL | `wss://bh.irisbylowes.com/hub/1.0` |
| `iris.gateway.timeout.connect` | Connection timeout (ms) | 90000 |
| `iris.gateway.timeout.ssl.handshake` | SSL handshake timeout (ms) | 90000 |
| `iris.gateway.provider` | Netty provider | `nio` (or `epoll`) |
| `iris.gateway.ssl.provider` | SSL provider | `jdk` (or `openssl`) |

### Key Environment Variables

| Variable | Description |
|----------|-------------|
| `IRIS_GATEWAY_URI` | Override gateway URI |
| `IRIS_AGENT_LOGTYPE` | Logging mode (`DEV` or `STDOUT`) |
| `IRIS_AGENT_UNCONN_REBOOT_TIME` | Minutes before forced reboot on no connection (default: 30) |
| `IRIS_AGENT_DISABLE_LOCAL_PROCESSING` | Disable reflex local processing |
| `IRIS_SCENE_ZWAVE_DISABLE` | Disable Z-Wave scene support |
| `IRIS_HUB_SPY_ACTIVE` | Enable diagnostic spy controller |
| `IRIS_DB_DISABLE_MULTITHREADED` | Force single-threaded DB |

---

## Spy Controller

**`SpyController.java`** (`arcus-spy-controller/.../controller/spy/SpyController.java`)

Diagnostic message snooper, only active when `IRIS_HUB_SPY_ACTIVE` is set.

- Registers as `SnoopingPortHandler` (passthrough — does not consume messages)
- Logs all `PlatformMessage` and `ProtocolMessage` traffic
- Uses `SpyService` / `SpyStore` for diagnostic data collection

---

## Key Design Patterns

### 1. Port Handler Pattern

All controllers implement `PortHandler` and register with the Router:

```java
// Controller registers at @PostConstruct
this.port = router.connect(portId, address, this);

// Router dispatches to controller
Object recv(Port port, PlatformMessage msg);   // Platform commands
void recv(Port port, ProtocolMessage msg);      // Device protocol frames
void recv(Port port, Object msg);               // Internal signals
```

### 2. Snooping Pattern

Controllers that need visibility into all messages (Gateway, Alarm, Reflex, Spy) implement `SnoopingPortHandler`. The router broadcasts all forwarded messages to snoopers, enabling cross-cutting concerns without tight coupling.

### 3. Lifecycle Listener Pattern

All controllers implement `LifeCycleListener`:

```java
void lifeCycleStateChanged(LifeCycle oldState, LifeCycle newState);
```

### 4. Event Dispatcher Pattern

Both ZigBee and Z-Wave controllers use the same pattern:
- Singleton `EventDispatcher` with `CopyOnWriteArraySet<EventListener>`
- Domain-specific event types (node added/removed, online/offline, command received)
- Thread-safe listener registration and dispatch

### 5. Singleton Service Locator

Protocol controllers use lazy-loading service locators (`ZBServices`, `ZWServices`) to manage subsystem singletons (Network, Driver, OfflineService), avoiding circular dependencies during Guice initialization.

### 6. Exponential Backoff

Used throughout for resilient connectivity:
- Gateway primary/secondary connection retry
- Reflex controller cloud sync
- Configurable initial delay, factor, and maximum via `Backoff`/`Backoffs` classes

---

## Message Flow Examples

### Device Message: Cloud Processing

```
ZigBee Radio
  --> ZigbeeController.recv(ProtocolMessage)
    --> port.send(protocolMessage)        [inject into router]
      --> Router dispatches to snoopers
        --> ReflexController: processor.handle(msg) returns false (not handled locally)
        --> Gateway: queue/send to cloud via WSS
          --> Cloud platform processes with Groovy driver
            --> Response comes back via WSS
              --> Gateway injects into router
                --> Router dispatches to ZigbeeController
                  --> ZigbeeController sends command to radio
```

### Device Message: Local Reflex Processing

```
Z-Wave Radio
  --> ZWaveController.recv(ProtocolMessage)
    --> port.send(protocolMessage)        [inject into router]
      --> Router dispatches to snoopers
        --> ReflexController: processor.handle(msg) returns true (handled locally)
          --> ReflexProcessor executes reflex actions:
            --> SetAttribute (update device state)
            --> SendProtocol (send response to device via controller)
          --> Message NOT forwarded to Gateway
```

### Pairing Flow

```
Cloud platform sends PairingRequest
  --> Gateway receives via WSS
    --> Router dispatches to HubController (service "hub")
      --> HubController.handlePairingRequest()
        --> ZigbeeController.startPairing(timeout)
        --> ZWaveController.startPairing(timeout)
          --> Radio enters inclusion mode
            --> Device joins network
              --> NodeAddedEvent dispatched
                --> Controller sends AddDeviceRequest to platform
                  --> Platform creates device record
```

### Alarm Trigger

```
Contact sensor opens (ZigBee IAS Zone status change)
  --> ZigbeeController receives protocol message
    --> Router dispatches to snoopers
      --> ReflexController processes locally (set contact attribute)
      --> AlarmController intercepts message
        --> AlarmSecurity processes trigger event
          --> State: READY --> PREALERT --> ALERT
            --> IrisHal.setLedState(BLINK_RED)
            --> IrisHal.setSounderMode(ALARM)
            --> Report alarm state to platform (TTL: 1 hour)
```

---

## Running the Agent Locally (Simulated Mode)

```bash
cd agent
# Key environment variables (see run-agent.sh for full list):
export IRIS_AGENT_HUBV2_FAKE=true
export IRIS_AGENT_HUBV2_DATADIR=~/.hub-simulated
export IRIS_GATEWAY_URI=wss://localhost:8082/hub/1.0
export ZWAVE_DISABLE=true
export ZIGBEE_DISABLE=true
export FOURG_DISABLE=true
./run-agent.sh
```

---

## Build Output

`arcus-agent/hub-v2` produces a distribution archive:
```
iris-agent-hub-v2-{VERSION}/
├── bin/iris-agent     # Startup script
├── conf/              # logback.xml, sounds/, voice/, agent.version
├── libs/              # JAR dependencies
└── lib/               # Native libraries (JNA, Netty epoll)
```

---

## Known Missing Components (Closed-Source)

These shipped as pre-compiled JARs in the original Iris platform and would need reimplementation:
- `arcus-4g-controller` — Cellular backup radio
- `arcus-hue-controller` — Philips Hue bridge integration
- `arcus-sercomm-controller` — Camera integration
