# Arcus Platform - Claude Code Guide

## Project Overview

Arcus Platform is an open-source home automation and IoT platform based on the Iris by Lowe's codebase. It provides backend microservices, a hub/gateway agent, device drivers, rules engine, video streaming, and voice assistant integrations (Alexa, Google).

## Repository Structure

```
/agent       - Hub/gateway software running on Iris hub hardware
/common      - Shared libraries for both platform and agent
/platform    - Backend microservices (~24 containerized services)
/tools       - Development and debugging tools (Oculus desktop test client)
/khakis      - Docker container configurations (Kafka, Cassandra, Zookeeper)
/gradle      - Shared Gradle build scripts and dependency management
/docs        - Project documentation
/libs        - Local libraries not on Maven Central
```

## Detailed Documentation

Topic-specific docs live in `/docs/`:

| File | Contents |
|------|----------|
| [claude-platform.md](docs/claude-platform.md) | Microservices, shared libraries, message flow, Cassandra schema |
| [claude-common.md](docs/claude-common.md) | Shared libraries, code generation pipeline, capability/protocol definitions |
| [claude-driver-model.md](docs/claude-driver-model.md) | Driver DSL, lifecycle events, protocol sections (Zigbee/Z-Wave/IPCD/MOCK), compilation, adding new devices |
| [claude-driver-execution.md](docs/claude-driver-execution.md) | Platform-side execution model: threading, pairing, upgrade, request flow |
| [claude-reflexes.md](docs/claude-reflexes.md) | Reflex system: concept, matches, actions, hub-side processing, sync |
| [claude-agent.md](docs/claude-agent.md) | Hub agent modules, startup flow, simulated mode |
| [claude-build.md](docs/claude-build.md) | Gradle build system, dependency management, code generation, Docker, CI/CD |
| [claude-khakis.md](docs/claude-khakis.md) | Docker infrastructure containers, Gradle tasks, shell scripts |
| [claude-hub-bridge.md](docs/claude-hub-bridge.md) | Hub-to-cloud WebSocket gateway, TLS, session management, message routing |
| [claude-subsystems.md](docs/claude-subsystems.md) | Subsystem framework, all 16 subsystems, event handling, persistence |
| [claude-rules.md](docs/claude-rules.md) | Rule engine: triggers, conditions, actions, templates, scenes |
| [claude-scheduler.md](docs/claude-scheduler.md) | Scheduler service: weekly schedules, sunrise/sunset, time bucketing |
| [claude-testing.md](docs/claude-testing.md) | Testing infrastructure, base classes, Cucumber BDD, mock framework |
| [claude-tools.md](docs/claude-tools.md) | Oculus, eye-kat, arcus-captools, hubdebug |

---

## Build System

**Gradle 7.6.4** via `./gradlew` wrapper. Parallel builds are enabled by default.

Key Gradle scripts in `/gradle/`:
- `dependencies.gradle` - Centralized dependency versions for all subprojects
- `subproject.gradle` - Common plugins and config applied to all subprojects
- `buildscript.gradle` - Buildscript classpath dependencies
- `jaxb.gradle` - JAXB/XJC code generation setup
- `container.gradle` - Docker image building

### Common Build Commands

```bash
./gradlew jar                              # Build all JARs
./gradlew test                             # Run all unit tests
./gradlew test --no-daemon                 # Run tests (CI mode)
./gradlew :some:module:test                # Test a specific module
./gradlew :tools:oculus:run               # Launch desktop test client
./gradlew :platform:arcus-khakis:startPlatform  # Start Docker infrastructure
```

---

## Technology Stack

- **Language:** Java 8 or 11 (auto-detected)
- **Build:** Gradle 7.6.4
- **Message Bus:** Apache Kafka 2.8.2
- **Coordination:** Apache ZooKeeper 3.8.4
- **Database:** Apache Cassandra (datastax driver 3.11.5)
- **Networking:** Netty 4.1.128, Jetty
- **DI/IoC:** Google Guice 4.0 + custom lifecycle (`IrisLifecycleManager`)
- **Device Protocols:** Zigbee (ZSmartSystems 1.2.4), Z-Wave (OpenHAB 2.5.0)
- **Serialization:** Jackson 2.18.6, GSON, JAXB
- **Device Drivers:** Groovy 2.5.15 DSL
- **Testing:** JUnit 4.13.2, EasyMock, Cucumber

---

## Java 8 / 11 Compatibility

The build supports both Java 8 and Java 11. The root `build.gradle` fails fast on unsupported JDK versions. Source/target compatibility is auto-detected based on the running JDK.

Key Java 11 considerations:
- JAXB is no longer bundled in the JDK — modules that need it declare explicit dependencies
- `javax.xml.bind.*` imports are used (not Jakarta EE) with standalone JAXB 2.2.7 runtime
- The `jaxb.gradle` script provides the shared XJC task configuration
- Some JVM intrinsic flags (SHA, diagnostic) are version-gated in `application.gradle`

---

## Dependency Management

All dependency versions are declared centrally in `gradle/dependencies.gradle` via the `ext.libraries` map. Reference them in subproject `build.gradle` files as:

```groovy
dependencies {
    implementation libraries.cassandraDriver
    implementation libraries.jacksonDatabind
}
```

Do not hardcode version strings in individual `build.gradle` files — add new dependencies to `dependencies.gradle` first.

---

## CI/CD

- **GitHub Actions `test.yml`:** Runs `./gradlew test --no-daemon` on every push (Java Corretto)
- **GitHub Actions `release.yml`:** Triggered by `v*` tags; builds and pushes Docker images to GitHub Container Registry (ghcr.io)

---

## Running Locally

Requires Docker with at least 10 GB RAM and 25 GB disk. Minimum required services:

1. `arcus/zookeeper` + `arcus/kafka` + `arcus/cassandra` (infrastructure)
2. `client-bridge` — WebSocket entry point
3. `hub-bridge` — Hub communication
4. `platform-services` — Account/device registry
5. `subsystem-service` — Business logic
6. `driver-services` — Device driver management
7. `rule-service` + `scheduler-service`

Start infrastructure:
```bash
./gradlew :platform:arcus-khakis:startPlatform
```

See [docs/claude-khakis.md](docs/claude-khakis.md) for full infrastructure details and [docs/claude-agent.md](docs/claude-agent.md) for running the hub agent locally.
