# Agent — Hub/Gateway Software (`/agent`)

The agent runs on Iris Hub v2 hardware, managing local devices and bridging them to the platform via WebSocket. It is organized as 13+ Gradle submodules with a Guice/Governator plugin architecture.

---

## Module Overview

| Module | Purpose |
|--------|---------|
| `arcus-agent` | Main entry point (`IrisAgent`); loads config, bootstraps Guice |
| `arcus-system` | Core services: boot framework, SSL, metrics, HTTP spy, logging |
| `arcus-hal/api` | Hardware Abstraction Layer interfaces |
| `arcus-hal/common` | Shared HAL implementations |
| `arcus-hal/hub-v2` | Hub v2 HAL: LED, buzzer, reset button, OTA firmware, OS calls |
| `arcus-hal/simulated` | Mock HAL for desktop testing |
| `arcus-gateway` | Netty WebSocket client — hub ↔ platform connectivity |
| `arcus-router` | High-performance async message routing (JCTools queues) |
| `arcus-hub-controller` | Hub hardware lifecycle management |
| `arcus-reflex-controller` | Local rule/automation execution (reflexes) — see [claude-reflexes.md](claude-reflexes.md) |
| `arcus-alarm-controller` | Alarm/security state machine |
| `arcus-zigbee-controller` | Zigbee protocol (ZSmartSystems 1.2.4) |
| `arcus-zw-controller` | Z-Wave protocol (OpenHAB binding 2.5.0) |
| `arcus-os` | OS abstraction via JNA + Netty epoll (ARM/Linux) |
| `arcus-spy-controller` | HTTP debugging endpoint |
| `arcus-test-agent` | Shared test doubles and mocks |

---

## Startup Flow

1. `IrisAgent` (entry point) takes a data directory argument (e.g., `~/.hub-simulated/`)
2. Loads `.conf` files from configuration directories
3. `BootUtils.initialize()` bootstraps Guice modules
4. `IrisHal.waitForShutdown()` blocks until shutdown signal

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
- Full Zigbee/Z-Wave controller internals (currently distributed as JARs)
