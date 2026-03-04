# Tools — Development & Debugging Utilities (`/tools`)

The `tools/` directory contains desktop and CLI utilities for developing, debugging, and code-generating across the Arcus platform.

---

## Overview

| Tool | Type | Purpose |
|------|------|---------|
| [Oculus](#oculus) | Swing desktop app | Admin/debug UI — browse devices, hubs, rules, subsystems, trigger pairing |
| [eye-kat](#eye-kat) | CLI | Kafka topic viewer — filter and tail platform/protocol/log messages |
| [arcus-captools](#arcus-captools) | CLI code generator | Generate client code (HTML docs, JS Backbone, Swift, Obj-C) from capability XML |
| [hubdebug](#hubdebug) | Documentation + keys | SSH debug access for physical Iris hubs |

---

## Oculus

**Location:** `tools/oculus/`
**Main class:** `com.iris.oculus.Main`
**Run:** `./gradlew :tools:oculus:run`

Oculus is a Swing-based admin and debugging GUI that connects to the Arcus platform via WebSocket (same client-bridge API used by mobile apps). It provides full read/write access to devices, hubs, accounts, rules, scenes, and subsystems.

### CLI Arguments

```
-p, --prompt-login       Show login screen (don't try auto-login)
-s, --skip-login         Skip login, show UI as if logged in
--credentials            Login with JSON credentials:
                           session|{host}|{token}
                           token|{host}|{token}
-c, --config             Path to config file (default: ~/.oculus/oculus.properties)
```

### Feature Modules

Each module appears as a tab in the main window:

| Tab | Description |
|-----|-------------|
| Dashboard | System status overview |
| Devices | Browse, inspect, and control devices — view attributes, send commands |
| Hubs | Hub selection, status, ZigBee/Z-Wave diagnostics |
| Products | Product catalog browser |
| Rules | View and edit automation rules |
| Scenes | Smart scene management |
| Video | Video recording/playback |
| Person | User account management |
| Place | Place/location management |
| Account | Account settings |
| Subsystems | Subsystem state inspection (security, safety, climate, etc.) |
| Scheduler | Scheduled event management |
| Behaviors | Behavior/automation configuration |
| Incidents | Alarm incident viewer |
| Pairing Devices | Device pairing wizard |
| Capability | Capability definition browser |

### Architecture

- MVC with Guice-injected controllers and sections
- Async communication via Arcus client library (same WebSocket API as mobile apps)
- Custom `SwingExecutorService` for thread-safe UI updates
- Menu bar: Session, Hubs, Devices, Places, Services, Windows

---

## eye-kat

**Location:** `tools/eye-kat/`
**Main class:** `com.iris.tools.kat.Main`
**Run:** `./gradlew :tools:eye-kat:run` or build and use the script directly

A CLI tool for tailing and filtering Kafka topics. Essential for debugging platform message flow, protocol traffic, and log streams.

### Setup

Ensure `kafka.eyeris` resolves (add to `/etc/hosts` or use `-b` to override):

```bash
./gradlew :tools:eye-kat:jar
./tools/eye-kat/build/scripts/eye-kat -t platform
```

### CLI Arguments

```
-t, --topics       Topics to subscribe to (required, comma-separated)
                     platform, analytics, irisLog,
                     protocol_todrivers, protocol_fromhub
-s, --start        Start offset (default: earliest)
                     'earliest', 'latest',
                     YYYY-MM-DDTHH:MM:SS, or relative: -30m, +2d, -1h30m
-e, --end          End offset (default: never)
                     Same formats as --start, plus 'now'
-b, --broker       Kafka broker (default: kafka.eyeris:9092)
--broker-overrides  Comma-separated broker addresses (for port-forwarding)
-p, --places       Filter by place UUID (comma-separated)
-a, --addresses    Filter by address with wildcards (source/dest/actor)
-f, --format       Output: 'summary' (default) or 'json'
```

### Examples

```bash
# Tail platform messages from last 10 minutes
eye-kat -t platform --start -10m

# Protocol messages in a time window
eye-kat -t protocol_todrivers --start "2024-01-15T14:00:00" --end "2024-01-15T15:00:00"

# Filter by place
eye-kat -t platform -p 12345678-1234-1234-1234-123456789012

# Filter by address pattern, JSON output
eye-kat -t platform -a "DRIV:*" -f json

# Multiple topics with broker override
eye-kat -t platform,irisLog --start -30m -b localhost:9092
```

### Output Formats

**Summary (default):**
```
<timestamp>  from:<source> to:<destination> type:<type> attributes:<payload>
```

**JSON:**
```json
{"timestamp":"...","source":"...","destination":"...","type":"...","payload":{...}}
```

---

## arcus-captools

**Location:** `tools/arcus-captools/`
**Run:** `./gradlew :tools:arcus-captools:installDist`

Code generators that read capability and service XML definitions and produce typed client code for multiple platforms. Each generator is a separate main class.

### Generators

#### HTML Documentation
**Main class:** `com.iris.capability.generator.html.HtmlGenerator`

Generates styled HTML documentation for all capabilities with attributes, commands, and events.

```bash
-i, --input     Path to capability XMLs (required)
-s, --services  Path to service XMLs (optional)
-o, --output    Output directory (required)
```

```bash
./gradlew :tools:arcus-captools:generateDoc
```

#### JavaScript (Backbone.js)
**Main class:** `com.iris.capability.generator.js.BackboneGenerator`

Generates Backbone Model and View classes with JSDoc, validation, and AMD module support. Outputs an `i2-capabilities` npm package.

```bash
-i, --input   Path to capability XMLs (required)
-o, --output  Output directory (required)
```

```bash
./gradlew :tools:arcus-captools:generateJSSource
```

#### Swift (iOS)
**Main class:** `com.iris.capability.generator.swift.SwiftGenerator`

Generates Swift classes for iOS apps. Template-driven — different templates produce capability classes, event handlers, or legacy compatibility layers.

```bash
-i, --input      Path to capability/service XMLs (required)
-t, --template   Handlebars template name (required)
-o, --output     Output directory (required)
```

Templates in `swift/`:
- `swift/capability/capability_swift.hbs` — Capability classes
- `swift/capability/capability_swift_events.hbs` — Event handlers
- `swift/service/service_swift.hbs` — Service classes
- `swift/model/model_swift.hbs` — Model classes

#### Objective-C (iOS)
**Main class:** `com.iris.capability.generator.objc.ObjCGenerator`

Same arguments as Swift. Templates in `objc/`:
- `objc/capability/capability_h.hbs` — Header files
- `objc/capability/capability_m.hbs` — Implementation files
- `objc/model/model_h.hbs` — Model headers

### Template System

All generators use Handlebars templates. Templates live alongside each generator's source code and are loaded from the classpath. Adding a new output language means writing new templates and a thin `Generator` subclass.

---

## hubdebug

**Location:** `tools/hubdebug/`

Documentation and encrypted SSH keys for accessing physical Iris hubs. Not a runnable tool — it's a reference directory.

### Contents

```
hub_debug_keys _and_ssh_README.txt   # Setup instructions
v2_hub_debug_keys_part1-4.zip        # Encrypted keys for v2 hubs
v3_hub_debug_keys/                   # Keys for v3 hubs
```

### Creating a Debug USB Dongle

Format a USB stick as FAT32 and place these files at the root:

| File | Required | Purpose |
|------|----------|---------|
| `{hubID}.dbg` | Yes | Encrypted debug key for your hub (e.g., `LWD-2226.dbg`) |
| `{hubID}.cfg` | No | Configuration overrides (gateway URI, logging) |
| `hubOS.bin` | No | Firmware image to install on boot |

### Example `.cfg` for Local Development

```
IRIS_GATEWAY_URI = wss://10.0.0.4:8082/hub/1.0
IRIS_AGENT_GATEWAY_ALLOW_LOCAL = true
IRIS_AGENT_REFLEX_LOGGING = y
```

### SSH Access

1. Copy your hub's `.dbg` file to the USB stick
2. Plug into hub USB port, reboot
3. Wait ~1 minute, find hub IP from router DHCP table
4. `ssh root@<hub-ip>`

**Passwords:**
- v2 hubs: `kz58!~Eb.RZ?+bqb`
- v3 hubs: `zm{[*f6gB5X($]R9`

### Key Hub Paths

| Path | Purpose |
|------|---------|
| `/data/iris/db/iris.db` | SQLite hub database |
| `/data/` | Hub config and data root |
| `/etc/init.d/irisagent` | Agent startup script |

### Common Debug Commands

```bash
killall -9 irisagentd && killall -9 java   # Kill agent
/etc/init.d/irisagent restart               # Restart agent
sqlite3 /data/iris/db/iris.db ".tables"     # Inspect DB
```

### Configuration Parameters

| Parameter | Description |
|-----------|-------------|
| `IRIS_GATEWAY_URI` | Hub bridge WebSocket URL |
| `IRIS_AGENT_LOGTYPE` | Set to `dev` for verbose logging (incompatible with `ALLOW_LOCAL`) |
| `IRIS_AGENT_GATEWAY_ALLOW_LOCAL` | Allow connection to local dev platform |
| `IRIS_AGENT_REFLEX_LOGGING` | Enable reflex driver logging (`y`) |
| `IRIS_LOGGING_STREAMLVL` | Log stream level (`DEBUG`, etc.) |
| `IRIS_LOGGING_STREAMEND` | Log stream end time (Unix epoch ms) |
