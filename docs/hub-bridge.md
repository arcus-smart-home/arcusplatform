# Hub Bridge — Hub-to-Cloud Gateway (`hub-bridge`)

The hub-bridge is the WebSocket gateway that connects physical Iris hubs to the Arcus cloud platform. It handles mutual TLS authentication, hub registration, session management, message routing, and heartbeat monitoring. Built on Netty, it supports thousands of concurrent hub connections.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Netty Pipeline](#netty-pipeline)
- [TLS and Certificate Handling](#tls-and-certificate-handling)
- [Hub Session Management](#hub-session-management)
- [Hub Registration Flow](#hub-registration-flow)
- [Message Routing and Filtering](#message-routing-and-filtering)
- [Kafka Integration](#kafka-integration)
- [Connection State and Heartbeats](#connection-state-and-heartbeats)
- [Configuration](#configuration)
- [Key Files](#key-files)

---

## Architecture Overview

```
Hub (WiFi/3G)
    │
    │  WebSocket (wss://...:8082/hub/1.0)
    │  Mutual TLS
    ▼
┌──────────────────────────────────────────┐
│            Hub Bridge Service            │
│                                          │
│  Netty Server                            │
│  ├─ TLS (mutual auth, blacklist)         │
│  ├─ HTTP codec → WebSocket upgrade       │
│  ├─ Idle detection → ping/pong           │
│  └─ Binary WebSocket frame handler       │
│                                          │
│  Session Layer                           │
│  ├─ HubSession (state machine)           │
│  ├─ HubSessionRegistry (heartbeat)       │
│  └─ Partition assignment                 │
│                                          │
│  Message Routing                         │
│  ├─ HubMessageFilter (authz)            │
│  ├─ Direct handlers (registration)       │
│  └─ Bus listeners (platform/protocol)    │
└──────────────┬───────────────────────────┘
               │ Kafka
               ▼
        Platform Services
```

**Main class:** `com.iris.hubcom.server.HubServer`
**Module:** `platform/arcus-containers/hub-bridge/`
**Shared networking:** `platform/bridge-common/`
**Session management:** `platform/arcus-hubsession/`

The HubServer extends `BridgeServer` (from bridge-common) and loads these Guice modules:

| Module | Purpose |
|--------|---------|
| `HubServerModule` | Hub-specific bindings (filters, handlers, TLS) |
| `ClusterAwareServerModule` | Cluster awareness |
| `KafkaModule` | Kafka messaging |
| `HubBlacklistDAOModule` | Hub certificate blacklist |
| `CassandraResourceBundleDAOModule` | Cassandra persistence |
| `PopulationAwareFirmwareModule` | Firmware upgrade management |
| `MetricsTopicReporterBuilderModule` | Metrics reporting |
| `HttpHealthCheckModule` | Health checks (port 9082) |

---

## Netty Pipeline

`Bridge10ChannelInitializer` configures the Netty pipeline for each new socket:

| Order | Handler | Purpose |
|-------|---------|---------|
| 1 | `SslHandler` | Mutual TLS with client certificate validation |
| 2 | `HttpRequestDecoder` | Parse HTTP 1.1 requests |
| 3 | `HttpResponseEncoder` | Encode HTTP responses |
| 4 | `HttpObjectAggregator` | Aggregate chunked HTTP into complete requests |
| 5 | `IdleStateHandler` | Detect read/write idle for keep-alive |
| 6 | `ChunkedWriteHandler` | Handle large data (removed after WS upgrade) |
| 7 | `Binary10WebSocketServerHandler` | WebSocket frame handling |

### WebSocket Handshake

1. Hub sends HTTP GET to upgrade path (`hub/1.0`)
2. `WebSocketUpgradeResponder` creates `WebSocketServerHandshaker`
3. Protocol upgrade completes, chunked handler removed from pipeline
4. `HubSession` created and stored in channel attributes
5. Session listeners notified of connection

### Keep-Alive

- `IdleStateHandler` fires `IdleStateEvent` on writer idle
- Server sends WebSocket ping frame
- If no pong within `web.socket.pong.timeout` (default 30s), connection closed
- `PingPong` class tracks timestamps per channel

---

## TLS and Certificate Handling

Hub-bridge uses mutual TLS — both server and hub present certificates.

### Trust Manager

`HubTrustManagerFactoryImpl` wraps the JDK X509TrustManager with `BlackListTrustManager`:

1. Check if client certificate chain exists (reject if missing and mutual auth required)
2. Look up certificate serial number in `HubBlacklistDAO`
3. If blacklisted → reject with `CertificateException`
4. Delegate to JDK TrustManager for chain validation

### Certificate → Hub ID Mapping

`HubClientFactory` extracts the hub ID from the client certificate:

1. Parse CN (Common Name) from certificate principal
2. Pattern: `CN=ih200-<MAC_ADDRESS>` (v2) or `CN=ih30x-<MAC_ADDRESS>` (v3)
3. Convert MAC address to hub ID via `HubID.fromMac()`
4. Return authenticated `HubClient` with hub ID

### TLS Configuration

```properties
tls.server=true
tls.server.ciphers=TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,...
tls.server.protocols=TLSv1.2
tls.need.client.auth=true
tls.max.concurrent.handshakes=500
```

`InstrumentedSSLEngine` and `SslMetrics` track handshake performance and enforce the concurrent handshake limit.

---

## Hub Session Management

### Session States

`HubSession` extends `DefaultSessionImpl` with a state machine:

```
CONNECTED → PENDING_REG_ACK → REGISTERED → AUTHORIZED
```

| State | Description |
|-------|-------------|
| `CONNECTED` | Socket established, waiting for hub connect event |
| `PENDING_REG_ACK` | Registration request sent, awaiting acknowledgment |
| `REGISTERED` | Hub acknowledged registration |
| `AUTHORIZED` | Fully authorized — can send/receive all messages |

### Unauthorized Reasons

When a hub is not `AUTHORIZED`, a reason is tracked:

| Reason | Description |
|--------|-------------|
| `BELOW_MIN_FW` | Firmware version below minimum supported |
| `UNREGISTERED` | Hub not yet registered in system |
| `REGISTERING` | Registration in progress |
| `ORPHANED` | Hub connected without account/place record |
| `INVALID_ACCOUNT` | Account mismatch |
| `HANDSHAKING` | Initial handshake in progress |
| `BANNED_CELL` | 3G cellular backup disabled for this hub |
| `UNAUTHENTICATED` | No valid client certificate |

### Session Data

Each `HubSession` stores:

- Hub ID (from certificate CN)
- Connection type (WiFi vs 3G cellular)
- SIM ID (for cellular connections)
- Firmware version, agent version, hardware version
- Active place ID
- Assigned platform partition
- Last state change timestamp

### Session Registry

`HubSessionRegistry` extends `DefaultSessionRegistryImpl`:

- Maintains registry of active sessions by client token
- Runs periodic heartbeat task (configurable interval, default 1s)
- Batches connected hubs per partition (batch size: 1000)
- Rotates through partitions each heartbeat cycle (`hub.heartbeat.partitionsPerHeartbeat=4`)
- Tracks hubs on cellular backup, persists timing info at intervals (`hub.cellbackup.dump.interval.mins=5`)

### Session Metrics

`HubSessionMetrics` publishes:

- Total connected hubs
- Hubs on cellular backup
- Per-state counts (connected, pending, registered, authorized)
- Per-firmware-version breakdowns
- Unauthorized reason breakdowns

---

## Hub Registration Flow

### Step 1: Hub Connects

`HubConnectedHandler` processes `MSG_HUB_CONNECTED_EVENT`:

1. **Extract metadata** — connection type, agent/firmware/hardware version, SIM ID
2. **Blacklist check** — if hub on disallowed cellular, set `BANNED_CELL` and return
3. **New hub?** Create record in HubDAO with default name "Smart Hub"
4. **Firmware check** — if below minimum, set `BELOW_MIN_FW` and return
5. **Orphaned hub?** Hub with no account but has returned with account ID
6. **Existing hub?** Update version fields, check for account mismatch
7. **Firmware upgrade?** Consult `HubRegistrationRegistry.upgradeIfNeeded()` — send upgrade message if needed
8. **Assign partition** — based on place ID or hub ID

### Step 2: Hub Registers

`HubRegisteredResponseHandler` processes `MSG_HUB_REGISTERED_RESPONSE`:

1. Mark session as `AUTHORIZED`
2. Load hub model from database
3. Broadcast `base:Added` event to platform
4. Set partition assignment

### Step 3: Authorization

The `authorized()` method on `DirectMessageHandler`:

1. Set place from hub model
2. Transition session to `AUTHORIZED` state
3. Clear unauthorized reason
4. Send `MSG_HUB_AUTHORIZED_EVENT` back to hub
5. Send timezone and external IP to hub via `SET_ATTRIBUTES`

---

## Message Routing and Filtering

### Message Filter

`HubMessageFilter` (implemented by `DefaultHubMessageFilterImpl`) gates all messages:

**Inbound (hub → platform):**

- Source address hub ID must match session hub ID
- Only `AUTHORIZED` hubs can send most messages
- Always allowed regardless of state:
  - `MSG_HUB_CONNECTED_EVENT`
  - Firmware update responses
  - Error responses
  - Responses to admin addresses
- Allowed during `PENDING_REG_ACK`: `MSG_HUB_REGISTERED_RESPONSE`

**Outbound (platform → hub):**

- Messages with no place ID pass through
- Admin address messages bypass place filtering
- Place ID must match session's active place (prevents cross-contamination)
- Admin-only messages checked against whitelist

### Message Handler

`HubMessageHandler` processes four message types from the hub:

| Type | Handling |
|------|----------|
| `PLATFORM` | Deserialize, filter, timestamp → platform bus |
| `PROTOCOL` | Filter by place → protocol bus |
| `LOG` | Route to hub logger with place ID |
| `METRICS` | Tagged → Kafka metrics topic; Aggregated → HdrHistogram collection |

### Direct Message Handlers

Some messages are handled directly in the hub-bridge rather than forwarded to the bus:

| Handler | Message | Purpose |
|---------|---------|---------|
| `HubConnectedHandler` | `MSG_HUB_CONNECTED_EVENT` | Hub registration flow |
| `HubRegisteredResponseHandler` | `MSG_HUB_REGISTERED_RESPONSE` | Complete registration |
| `HubFirmwareUpgradeProcessEventHandler` | Firmware upgrade events | Track firmware upgrades |
| `HubFirmwareUpdateResponseHandler` | Firmware update response | Handle update results |

### Admin Message Restrictions

Configured via properties:

```properties
hub.bridge.admin.addresses=SERV:hub:
hub.bridge.admin.only.messages=hub:GetLogs,hub4g:*,hubdebug:*
```

Only service-level hub addresses can send admin-only messages (e.g., log retrieval, debug commands, 4G management).

---

## Kafka Integration

### Bus Architecture

The hub-bridge is both a Kafka producer (inbound hub messages) and consumer (outbound platform messages).

**Inbound (hub → Kafka):**

1. Hub sends message via WebSocket
2. `HubMessageHandler` deserializes
3. Message placed on platform or protocol bus (Kafka topics)
4. Platform services consume asynchronously

**Outbound (Kafka → hub):**

1. Platform service publishes to platform/protocol bus
2. Hub-bridge consumes from Kafka topics
3. Listener checks session filter, finds matching hub session
4. Message sent to hub via WebSocket

### Bus Listeners

**`HubPlatformBusListener`:**
- Subscribes to platform messages destined for hubs
- Validates active place matches message place
- Wraps in `HubMessage` container → sends via WebSocket

**`HubProtocolBusListener`:**
- Subscribes to protocol messages
- Wraps in `HubMessage` container → sends via WebSocket

### Kafka Configuration

```properties
kafka.group=hub-bridge
bridge.name=hub
partition.assignment=ALL         # Listen to all partitions
kafka.offsets.transient=true     # Don't save offsets (always start at latest)
```

---

## Connection State and Heartbeats

### Online/Offline Detection

`HubConnectionSessionListener` broadcasts state changes:

| Transition | Event |
|------------|-------|
| Socket connects (during registration) | `HubConnectionCapability.STATE_HANDSHAKE` |
| Session reaches `AUTHORIZED` | `HubConnectionCapability.STATE_ONLINE` |
| Socket disconnects | `HubCapability.HubDisconnectedEvent` + `HubRegistrationRegistry.offline()` |

### Heartbeat Mechanism

The `HubSessionRegistry` runs a scheduled heartbeat:

1. Every 1 second (configurable via `hub.heartbeat.intervalMs`)
2. Collects connected hub IDs per partition
3. Sends `HeartbeatMessage` with connected hub list
4. Batches of 1000 hubs per message
5. Rotates through partition subset each cycle
6. Hub-service uses heartbeats to reconcile connection state

### Attribute Updates on Authorization

When a hub becomes authorized, `HubConnectionSessionListener.onAuthorized()`:

- Retrieves timezone from place configuration
- Retrieves external IP from channel socket address
- Sends `SET_ATTRIBUTES` to hub with `HubCapability.ATTR_TZ` and `HubNetworkCapability.ATTR_EXTERNALIP`

---

## Configuration

### hub-bridge.properties

| Property | Default | Description |
|----------|---------|-------------|
| `web.socket.path` | `hub/1.0` | WebSocket upgrade path |
| `web.socket.maxFrameSizeBytes` | `1048576` | Max WebSocket frame size (1 MB) |
| `web.socket.pong.timeout` | `30` | Seconds to wait for pong response |
| `bridge.name` | `hub` | Bridge identifier |
| `port` | `8082` | Server listen port |
| `healthcheck.port` | `9082` | Health check HTTP port |
| `tls.need.client.auth` | `true` | Require client certificates |
| `tls.max.concurrent.handshakes` | `500` | Max concurrent TLS handshakes |
| `kafka.group` | `hub-bridge` | Kafka consumer group |
| `partition.assignment` | `ALL` | Listen to all Kafka partitions |
| `kafka.offsets.transient` | `true` | Don't persist consumer offsets |
| `hub.heartbeat.intervalMs` | `1000` | Heartbeat interval |
| `hub.heartbeat.partitionsPerHeartbeat` | `4` | Partitions per heartbeat cycle |
| `hub.cellbackup.dump.interval.mins` | `5` | Cellular backup stats interval |
| `hub.bridge.admin.addresses` | `SERV:hub:` | Admin address whitelist |
| `hub.bridge.admin.only.messages` | (see above) | Admin-only message types |

---

## Key Files

### Hub Bridge Service

| File | Description |
|------|-------------|
| `platform/arcus-containers/hub-bridge/.../HubServer.java` | Main class, Guice module loading |
| `platform/arcus-containers/hub-bridge/.../HubServerModule.java` | Guice bindings for all hub-bridge components |
| `platform/arcus-containers/hub-bridge/.../message/HubMessageHandler.java` | Inbound message dispatcher (platform/protocol/log/metrics) |
| `platform/arcus-containers/hub-bridge/.../message/HubConnectedHandler.java` | Hub registration initiation |
| `platform/arcus-containers/hub-bridge/.../message/HubRegisteredResponseHandler.java` | Registration completion |
| `platform/arcus-containers/hub-bridge/.../authz/DefaultHubMessageFilterImpl.java` | Message authorization filter |
| `platform/arcus-containers/hub-bridge/src/dist/conf/hub-bridge.properties` | Configuration |

### Hub Session

| File | Description |
|------|-------------|
| `platform/arcus-hubsession/.../session/HubSession.java` | Session state machine |
| `platform/arcus-hubsession/.../session/HubSessionRegistry.java` | Session registry with heartbeat |
| `platform/arcus-hubsession/.../session/HubSessionMetrics.java` | Connection metrics |
| `platform/arcus-hubsession/.../session/HubClientFactory.java` | Certificate → HubClient mapping |
| `platform/arcus-hubsession/.../ssl/HubTrustManagerFactoryImpl.java` | TLS trust manager with blacklist |
| `platform/arcus-hubsession/.../ssl/BlackListTrustManager.java` | Certificate blacklist enforcement |

### Bridge Common

| File | Description |
|------|-------------|
| `platform/bridge-common/.../netty/Bridge10ChannelInitializer.java` | Netty pipeline setup |
| `platform/bridge-common/.../netty/BaseWebSocketServerHandler.java` | WebSocket handshake and frame handling |
| `platform/bridge-common/.../server/BridgeServer.java` | Base server application class |
| `platform/bridge-common/.../server/ServerRunner.java` | Netty bootstrap and lifecycle |
| `platform/bridge-common/.../session/Session.java` | Session interface |
| `platform/bridge-common/.../session/SessionRegistry.java` | Session registry interface |
