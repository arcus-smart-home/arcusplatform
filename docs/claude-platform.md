# Platform — Backend Microservices (`/platform`)

## Containerized Services (`arcus-containers/`)

**Bridge Services** — external entry points:

| Service | Purpose |
|---------|---------|
| `client-bridge` | WebSocket server for iOS/Android/Web clients; REST product catalog/invitation APIs |
| `hub-bridge` | Bidirectional protocol gateway between hub hardware and platform |
| `ipcd-bridge` | IPCD (Iris Protocol for Communication and Devices) edge protocol bridge |

**Core Platform Services:**

| Service | Purpose |
|---------|---------|
| `platform-services` | Account, device, and place registry — core data model |
| `driver-services` | Loads and runs Groovy device drivers; manages device lifecycle |
| `subsystem-service` | Feature subsystems (security, safety, care, climate, water, presence, weather) |
| `rule-service` | Executes user-defined automation rules and triggers |
| `scheduler-service` | Schedules timed events, reminders, and recurring tasks |
| `alarm-service` | Professional monitoring integration and incident tracking |
| `history-service` | Records device state changes and events for historical replay |
| `notification-services` | Multi-channel notifications: SMS (Twilio), email (SendGrid/Mailgun), push (GCM/APNS), XMPP |
| `tag-service` | User-defined device tagging/labeling |
| `metrics-server` | Metrics collection and aggregation (Prometheus-compatible) |

**Voice Assistant Services:**

| Service | Purpose |
|---------|---------|
| `voice-service` | Cloud voice assistant protocol handling |
| `alexa-bridge` | Amazon Alexa skill integration |
| `google-bridge` | Google Home integration |

**Video Services:**

| Service | Purpose |
|---------|---------|
| `video-service` | Core video streaming and recording management |
| `video-streaming-server` | RTMP/live streaming protocol handler |
| `video-recording-server` | Recording storage and retrieval |
| `video-preview-server` | Thumbnail/preview generation |
| `video-download-server` | Video file export/download |
| `video-purge` | Cleanup of expired/deleted video |

**IVR/Billing:**

| Service | Purpose |
|---------|---------|
| `ivr-callback-server` | Interactive Voice Response callbacks |
| `ivr-fallback-server` | IVR fallback routing |
| `billing-callback-server` | Billing webhook event handling |

---

## Key Shared Libraries

| Module | Purpose |
|--------|---------|
| `arcus-lib` | Central framework: Kafka dispatchers, message bus, Cassandra DAOs, Guice wiring, OTA firmware, scene/action catalog |
| `bridge-common` | Netty networking, Shiro auth, protocol handling shared by all bridge services |
| `arcus-prodcat` | Product/device catalog with Lucene search, pairing metadata, capability mappings |
| `arcus-subsystems` | Subsystem state machine implementations (alarm, care, climate, cameras, etc.) |
| `arcus-rules` | Rule compilation and evaluation framework |
| `arcus-alarm` | Alarm state machine, incident tracking, call tree/escalation |
| `arcus-video` | Video recording/streaming management, cloud storage integration |
| `arcus-voice-bridge` | Common code for Alexa/Google integration |
| `arcus-oauth` | OAuth 2.0 authentication and session management |
| `arcus-security` | Shiro-based authentication/authorization with Cassandra-backed DAOs |
| `arcus-subscriptions` | User subscription plans, feature entitlements, billing integration |
| `arcus-hubsession` | WebSocket session management for hub connections |
| `arcus-modelmanager` | Cassandra schema versioning CLI — applies XML-based changelogs to 3 keyspaces |
| `arcus-platform-drivers` | Cucumber/BDD driver test infrastructure (Zigbee, Z-Wave, IPCD command builders) |
| `arcus-test` | Common test fixtures, Cassandra Unit support, Governator test bootstrap |
| `ipcd-common` | IPCD protocol Netty handlers |

---

## Message Flow Pattern

All inter-service communication goes through Kafka — no direct RPC calls between services.

```
Client Request (WebSocket)
  → client-bridge
  → Kafka topic (PlatformMessage with correlation ID)
  → Target service (e.g., driver-services)
      → Handler (subclass of AbstractPlatformMessageHandler)
      → Cassandra write / device command
  → Response published back to Kafka reply topic
  → client-bridge → client
```

**Handler types in `arcus-lib`:**
- `PlatformRequestMessageHandler` — request/response RPC style
- `EventMessageHandler` — one-way event broadcasts
- `ContextualRequestHandler` / `ContextualEventHandler` — require place/account context

**Kafka partitioning:** `KafkaPlatformPartitioner` routes messages by place UUID for consistency.

---

## Cassandra Schema (3 Keyspaces)

Managed by `arcus-modelmanager` using XML changelog files (similar to Liquibase).

| Keyspace | Data |
|----------|------|
| `platform` | Places, people, devices, rules, scenes, scheduler, subsystem state, authorization grants, pairing |
| `video` | Recording metadata, streaming sessions, preview data, cloud storage references |
| `history` | Device attribute change history and event analytics, partitioned by time for retention |

```bash
./gradlew :platform:arcus-modelmanager:run   # Apply schema migrations
```

Changelog files: `arcus-modelmanager/src/dist/{platform,video,history}-resources/changelogs/*.xml`

---

## Device Driver System (overview)

Groovy DSL scripts in `driver-services/src/main/resources/`.

**Naming convention:** `{PROTOCOL}_{Vendor}_{Device}_{Major}_{Minor}.driver`
- `ZB_` — Zigbee (ZSmartSystems)
- `ZW_` — Z-Wave (OpenHAB binding)
- `IPCD_` — Iris Protocol for Communication and Devices
- `MOCK_` — Test/simulator drivers

See [claude-driver-model.md](claude-driver-model.md) and [claude-driver-execution.md](claude-driver-execution.md) for full details.

---

## Notable Schema/Config Files

- `arcus-lib/src/main/resources/schema/scene/scene-catalog.xsd` — Automation scene/action definitions (JAXB-generated)
- `arcus-prodcat/src/main/resources/schema/pairing_1.0.0.xsd` — Device pairing metadata schema
- `arcus-lib/src/main/resources/schema/ota/device-ota-firmware.xsd` — OTA firmware format
- `arcus-lib/src/main/resources/firmware.xsd` — Firmware version definitions
- `notification-services/src/main/resources/*-notification-messages.properties` — Message templates per channel (email, SMS, push, APNS)
