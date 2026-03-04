# Common — Shared Libraries (`/common`)

Shared between the platform and agent. Contains the core messaging framework, code generators, capability/protocol definitions, and the Groovy driver DSL bindings.

## Modules

| Module | Used By | Purpose |
|--------|---------|---------|
| `arcus-common` | Both | Core messaging: `Message`, `PlatformMessage`, `Address` types, `CapabilityRegistry`, JSON utils, Guice bootstrap with custom lifecycle (`IrisLifecycleManager`) |
| `arcus-client` | Platform | Client-facing data models: `Account`, `Person`, `Place`, `Device`, `Hub`, `MobileDevice`, `MessageConstants` |
| `arcus-model` | Both | Root module; hosts capability/service/type XML definitions and the code generators |
| `arcus-model/capability-generator` | Build only | Reads capability XML → generates Java via Handlebars templates |
| `arcus-model/platform-messages` | Platform | Generated platform message/event classes (uses "platform" template) |
| `arcus-model/platform-client` | External/Tools | Standalone client library; published as `platform-client-all.jar` (shadowJar) for Oculus, Android SDK |
| `arcus-model/model-query` | Platform | ANTLR 4 query DSL for model inspection |
| `arcus-protocol` | Both | IRP and XML definitions for Zigbee (ZCL/ZDP/AME), Z-Wave, IPCD, and hub protocols |
| `arcus-protoc` | Build only | IRP compiler: ANTLR 4 parser → Java message classes + Groovy binding stubs |
| `arcus-protoc-runtime` | Both | Netty `ByteBuf` serialization/deserialization support for protoc-generated code |
| `protocol-generator` | Build only | Generates IPCD model classes from XML definitions via JAXB/XJC + Handlebars |
| `arcus-drivers/drivers-common` | Agent | Base classes and protocols shared by all device drivers |
| `arcus-drivers/groovy-bindings` | Agent | Generated Groovy DSL bindings for driver authors (Zigbee ZCL/ZDP/AME, IPCD) |
| `arcus-drivers/reflex-generator` | Agent | Generates hub-local reflex (rule) code |
| `arcus-reflection` | Platform | `MethodInvoker`, `MethodDiscoverer`, `ArgumentResolverFactory` — used by service message dispatch |
| `arcus-metrics` | Both | Dropwizard Metrics + HDR Histogram wrappers |
| `arcus-billing` | Platform | JAXB-generated billing webhook models |

---

## Code Generation Pipeline

The build runs a multi-stage code generation pipeline before compiling Java:

```
Capability/Service XMLs          IRP files (Zigbee/Z-Wave)      IPCD XML definitions
  (arcus-model/src/main/           (arcus-protocol/src/main/      (arcus-protocol/src/
   resources/capability/,           irp/*.irp)                      main/resources/
   service/, type/)                                                  definition/ipcd/)
        │                                 │                                │
        ▼                                 ▼                                ▼
 capability-generator            arcus-protoc (ANTLR 4)         protocol-generator
 (Handlebars templates)          Java + Binding + Naming         (JAXB/XJC + Handlebars)
        │                                 │                                │
   ┌────┴────┐                   ┌────────┴──────┐                        │
   ▼         ▼                   ▼               ▼                        ▼
platform-  platform-       arcus-protocol   groovy-bindings         IPCD model
messages   client          (Zigbee/Z-Wave   (Driver DSL             classes
(platform  (external       Java classes)    bindings)
 events)    SDK)
```

All generated code lands in `src/generated/java/` within each module and is excluded from version control.

---

## Capability & Service Definitions

Device capabilities (switch, dimmer, lock, thermostat, etc.) and platform services are defined as XML and code-generated into Java.

**Capability XML format:**
```xml
<c:capability name="Switch" namespace="swit" enhances="Device" version="1.0">
  <c:attributes>
    <c:attribute name="state" type="enum" values="ON,OFF" writable="true"/>
    <c:attribute name="statechanged" type="timestamp"/>
  </c:attributes>
</c:capability>
```

Definition files live in:
- `arcus-model/src/main/resources/capability/` — 100+ device capability XMLs
- `arcus-model/src/main/resources/service/` — 20+ platform service XMLs
- `arcus-model/src/main/resources/type/` — 100+ shared type XMLs

The generator produces bean classes, capability interfaces with attribute constants, and event/response classes. At runtime, `StaticDefinitionRegistry` provides a `CapabilityRegistry` for lookups.

### Adding a New Device Capability

1. Create `arcus-model/src/main/resources/capability/myfeature.xml`
2. Run `./gradlew :common:arcus-model:platform-client:generateSource :common:arcus-model:platform-messages:generateSource`
3. Generated bean and interface classes appear in `src/generated/java/`
4. Reference from platform services and/or driver DSL

---

## Protocol Definitions

**IRP files** (`arcus-protocol/src/main/irp/`) define the binary structures for Zigbee and Z-Wave in a custom DSL parsed by ANTLR 4:
- `zcl-messages.irp` — Zigbee Cluster Library
- `zdp-messages.irp` — Zigbee Device Profile
- `ame-messages.irp` — AlertMe clusters
- `zwave-messages.irp` — Z-Wave

`arcus-protoc` compiles these into Java message classes (in `arcus-protocol`) and Groovy binding classes (in `groovy-bindings`) that driver authors use in the Groovy DSL.

**IPCD definitions** (`arcus-protocol/src/main/resources/definition/ipcd/`) use XML:
- `ipcd-definition.xml` — full IPCD protocol spec (GetDeviceInfo, SetParameterValues, firmware update, etc.)
- `ipcd-aos.xml` — A.O. Smith adapter mapping device-specific params to generic IPCD

---

## Core Classes

- **`Message` / `PlatformMessage`** — abstract message with builder; carries source/destination `Address`, correlation ID, TTL, place ID, population, actor
- **`Address`** — 8 concrete subtypes: `DeviceDriverAddress`, `PlatformServiceAddress`, `HubAddress`, `ClientAddress`, `BridgeAddress`, `BroadcastAddress`, etc.
- **`CapabilityRegistry`** — runtime lookup of capability definitions and attribute metadata
- **`MethodInvoker` / `ArgumentResolverFactory`** — reflection-based message dispatch used throughout platform services
