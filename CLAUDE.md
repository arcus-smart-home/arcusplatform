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
| [claude-driver-model.md](docs/claude-driver-model.md) | Driver DSL, lifecycle events, protocol sections (Zigbee/Z-Wave/IPCD/MOCK), compilation |
| [claude-driver-execution.md](docs/claude-driver-execution.md) | Platform-side execution model: threading, pairing, upgrade, request flow |
| [claude-reflexes.md](docs/claude-reflexes.md) | Reflex system: concept, matches, actions, hub-side processing, sync |
| [claude-agent.md](docs/claude-agent.md) | Hub agent modules, startup flow, simulated mode |
| [claude-khakis.md](docs/claude-khakis.md) | Docker infrastructure containers, Gradle tasks, shell scripts |

---

## Build System

**Gradle 6.9** via `./gradlew` wrapper. Parallel builds are enabled by default.

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

- **Language:** Java (upgrading from 8 → 11; see below)
- **Build:** Gradle 6.9
- **Message Bus:** Apache Kafka 2.4.0
- **Coordination:** Apache ZooKeeper 3.5.7
- **Database:** Apache Cassandra (datastax driver 3.9.0)
- **Networking:** Netty, Jetty
- **DI/IoC:** Google Guice 4.0 + Netflix Governator
- **Device Protocols:** Zigbee (ZSmartSystems 1.2.4), Z-Wave (OpenHAB 2.5.0)
- **Serialization:** Jackson, GSON, JAXB
- **Device Drivers:** Groovy DSL
- **Testing:** JUnit, Cassandra Unit

---

## Active Work: Java 8 → 11 Upgrade (`upgrade-java` branch)

The `upgrade-java` branch is migrating from Java 8 to Java 11. Key changes so far:

- Replaced `javax.xml.bind` (removed in Java 11) with Jakarta EE equivalents (`jakarta.xml.bind`)
- Added explicit JAXB runtime dependencies to modules that previously relied on the JDK-bundled version
- Fixed JAXB/XJC code generation in `protocol-generator` and `capability-generator`
- Updated Gradle buildscript dependencies for JAXB tooling compatibility
- Added `bindings.xjb` schema binding file for XJC configuration

When working on this upgrade:
- Use `jakarta.xml.bind.*` imports, not `javax.xml.bind.*`
- Modules needing JAXB must explicitly declare it in `build.gradle` (no longer implicit from JDK)
- The `jaxb.gradle` script provides the shared XJC task configuration

---

## Dependency Management

All dependency versions are declared centrally in `gradle/dependencies.gradle` via the `ext.libraries` map. Reference them in subproject `build.gradle` files as:

```groovy
dependencies {
    compile libraries.cassandraDriver
    compile libraries.jackson_databind
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
