# Build System

The Arcus platform uses Gradle 7.6.4 with a multi-project build spanning ~86 subprojects. Builds target Java 8 or 11 (auto-detected). Docker images are built via Gradle tasks wrapping shell scripts.

---

## Table of Contents

- [Quick Reference](#quick-reference)
- [Project Structure](#project-structure)
- [Gradle Configuration Files](#gradle-configuration-files)
- [Dependency Management](#dependency-management)
- [Code Generation Pipeline](#code-generation-pipeline)
- [Docker Build System](#docker-build-system)
- [Agent Distribution](#agent-distribution)
- [CI/CD](#cicd)
- [Version Management](#version-management)
- [Testing](#testing)
- [Hub Deployment](#hub-deployment)

---

## Quick Reference

```bash
# Build everything
./gradlew build

# Run tests
./gradlew test

# Start infrastructure (ZooKeeper, Kafka, Cassandra) + schema migration
./gradlew :platform:arcus-khakis:startPlatform

# Start a platform service
./gradlew :platform:arcus-containers:driver-services:startService

# Stop a platform service
./gradlew :platform:arcus-containers:driver-services:stopService

# Build Docker images
./gradlew :platform:arcus-khakis:distDocker

# Run Oculus debug tool
./gradlew :tools:oculus:run

# Generate capability docs
./gradlew :tools:arcus-captools:generateDoc

# List all tasks
./gradlew tasks
```

---

## Project Structure

### settings.gradle

Four major sections with ~86 subprojects:

| Section | Projects | Description |
|---------|----------|-------------|
| `common/` | 14 | Shared libraries: messaging, model, protocol, drivers, metrics |
| `platform/` | 46 | Cloud services: bridges, subsystems, rules, video, voice |
| `agent/` | 18 | Hub agent: controllers, HAL, router, gateway |
| `tools/` | 3 | Oculus, eye-kat, arcus-captools |
| `khakis/` | 1 | Docker infrastructure orchestration |

### Root build.gradle

Applied to all subprojects:
- **Group:** `com.arcussmarthome`
- **Java:** Source/target compatibility auto-detected (Java 8 or 11; fails fast on unsupported versions)
- **Repositories:** Maven Central, JCenter (optional `mavenLocal` via `use_maven_local=true`)
- **Shared dependencies:** SLF4J, Dropwizard Metrics, JUnit 4.13.2, Logback (test), Eclipse annotations
- **Global exclusions:** commons-logging, findbugs annotations, log4j, duplicate metrics-core
- **Forced versions:** Jackson 2.18.6, Guice 4.0 (conflict resolution)
- **Caching:** Dynamic versions cached for 60 minutes

---

## Gradle Configuration Files

All custom build logic lives in `gradle/`:

| File | Purpose |
|------|---------|
| `buildscript.gradle` | Build classpath: Shadow 5.1.0, Docker, Grgit 4.1.1, JMH 0.6.8, SpotBugs 4.8.0 |
| `dependencies.gradle` | Centralized version catalog — 100+ libraries in an `ext.libraries` map |
| `subproject.gradle` | Applied to all subprojects: Java, Maven, Docker, Eclipse, SpotBugs, system test source set |
| `application.gradle` | Executable app setup: JVM opts, start scripts, manifest, `createApplicationInfo` task |
| `container.gradle` | Docker containerization: extends application, adds `distDocker`/`tagDocker`/`pushDocker` tasks |
| `deploy.gradle` | Agent SSH deployment to hubs via Ant SCP/SSH tasks |
| `version.gradle` | Git-based versioning: `bump_major`, `bump_minor`, `bump_patch`, `tag_release` tasks |
| `release.gradle` | Release branch management: `branchRelease`, `tagRelease` |
| `jaxb.gradle` | JAXB `xjc` code generation from XML schemas |
| `groovy.gradle` | Groovy plugin (minimal) |
| `cucumber.gradle` | BDD test support (minimal) |
| `snapshot.gradle` | Snapshot publishing configuration |

### application.gradle Details

Applied to all executable services. Sets up:

```groovy
defaultJvmOpts = [
    '-Dsun.net.inetaddr.ttl=10',
    '-Djdk.tls.ephemeralDHKeySize=2048',
    '-Djava.security.egd=file:/dev/./urandom',
    '-Dio.netty.leakDetectionLevel=disabled',
    '-Dio.netty.buffer.bytebuf.checkAccessible=false',
    '-XX:StringTableSize=1000003',
    "-Xms${jvmMem}m", "-Xmx${jvmMem}m",
    // ... crypto and NUMA optimization flags
]
```

Generates `META-INF/application.properties` with version, name, and build timestamp.

### container.gradle Details

Extends `application.gradle` for Docker-based services:

- **Base image:** `arcus/java`
- **Container naming:** `{project-name}.arcus`
- **Version:** `{major}.{minor}.{patch}` (no qualifier)
- **Docker links:** zookeeper, kafka, cassandra
- **Key tasks:**

| Task | Description |
|------|-------------|
| `uberDist` | Creates shared lib structure with SHA-1 dedup |
| `distDocker` | Builds Docker image |
| `tagDocker` | Tags image with optional registry prefix |
| `pushDocker` | Pushes to container registry |
| `startService` | Starts Docker container |
| `stopService` | Stops Docker container |
| `runDocker` | Runs container in foreground |

---

## Dependency Management

Centralized in `gradle/dependencies.gradle` as an `ext.libraries` map. Subprojects reference libraries by name.

### Key Library Versions

| Library | Version | Notes |
|---------|---------|-------|
| Netty | 4.1.128.Final | Overridable via `netty_override_version` property |
| tcnative | 2.0.75.Final | Overridable via `tcnative_override_version` property |
| Groovy | 2.5.15 | Driver DSL runtime |
| Guice | 4.0 | DI framework (forced to resolve conflicts) |
| Cassandra Driver | 3.11.5 | Shaded JAR |
| Kafka | 2.8.2 | Scala 2.12 |
| ZooKeeper | 3.8.4 | |
| Jackson | 2.18.6 | |
| SLF4J | 2.0.17 | |
| Logback | 1.3.14 | |
| Dropwizard Metrics | 4.2.30 | |
| Guava | 33.4.0-jre | |
| zsmartsystems ZigBee | 1.2.4 | |
| SQLite4Java | 1.0.392 | Agent embedded DB |
| JUnit | 4.13.2 | |
| EasyMock | 3.3 | Primary test mocking |
| Mockito | 1.10.19 | Alternative test mocking |
| Cucumber | 2.4.0 | BDD driver tests |
| Handlebars | 2.2.2 | Code gen templates |
| ANTLR 4 | 4.5 | IRP parser |

### Dependency Overrides

```bash
# Override Netty version at build time
./gradlew build -Pnetty_override_version=4.1.100.Final
```

---

## Code Generation Pipeline

The build runs multiple code generators before `compileJava`. All generated code lands in `src/generated/java/` and is excluded from version control.

### 1. Protocol Generation (IRP files)

**Source:** `common/arcus-protocol/src/main/irp/*.irp`
**Generator:** `common/arcus-protoc/` (ANTLR 4 parser)

Compiles Iris Protocol (IRP) definitions into Java message classes:

```
generateZWaveZigbeeSource
  ├── generateZigbeeZclSource    (zcl-messages.irp)
  ├── generateZigbeeZdpSource    (zdp-messages.irp, depends on ZCL)
  ├── generateZigbeeAmeSource    (ame-messages.irp, depends on ZDP)
  ├── generateZigbeeMessageSource (zb-protocol-messages.irp, depends on AME)
  └── generateZWaveMessageSource  (zwave-messages.irp, depends on ZigBee)
```

Order matters — each step may reference types from the previous.

### 2. Capability Generation (XML definitions)

**Source:** `common/arcus-model/src/main/resources/capability/`, `service/`, `type/`
**Generator:** `common/arcus-model/capability-generator/`

Reads capability XML and produces:
- Java capability interfaces with attribute constants
- Platform message/event classes (`platform-messages`)
- Client model classes (`platform-client`, published as `platform-client-all.jar` shadow JAR)

### 3. IPCD Protocol Generation

**Source:** `common/arcus-protocol/src/main/resources/definition/ipcd/`
**Generator:** `common/protocol-generator/` (JAXB/XJC + Handlebars)

XML schema → JAXB Java bindings for the IP-connected device protocol.

### 4. Groovy Driver Bindings

**Source:** Generated protocol classes
**Generator:** Part of `arcus-protoc`

Produces Groovy DSL binding stubs (`groovy-bindings/`) that driver authors use in `.driver` files.

### 5. Driver Compilation

**Source:** `platform/arcus-containers/driver-services/src/main/resources/*.driver`
**Generator:** `common/arcus-drivers/reflex-generator/` (Shadow JAR)

Compiles `.driver` files into bytecode and reflex databases:

```
compileDrivers
  ├── generateDriversReflexDB    (ReflexGenerator with Groovy compiler)
  └── generateDriversJar         (package compiled classes)
```

The reflex generator uses `groovy.dump.bytecode=true` to produce class files from driver scripts.

### 6. Client Code Generation (arcus-captools)

**Source:** Capability XML definitions
**Generators:** HTML, JavaScript/Backbone, Swift, Objective-C

See [tools.md](tools.md#arcus-captools) for details.

---

## Docker Build System

### Infrastructure (Khakis)

`khakis/build.gradle` wraps shell scripts for Docker orchestration:

```bash
# Build infrastructure images (ZooKeeper, Kafka, Cassandra, base Java)
./gradlew :platform:arcus-khakis:distDocker

# Start infrastructure + run schema migrations
./gradlew :platform:arcus-khakis:startPlatform

# Stop infrastructure
./gradlew :platform:arcus-khakis:stopPlatform
```

### Infrastructure Containers

| Container | Base | Key Ports |
|-----------|------|-----------|
| `arcus/java` | Temurin JDK 8 (Debian Bullseye) | — (base image) |
| `arcus/zookeeper` | arcus/java | 2181, 2888, 3888 |
| `arcus/kafka` | arcus/java (Kafka 2.8.2, Scala 2.12) | 9092 |
| `arcus/cassandra` | arcus/java (Cassandra 4.0.15) | 9042, 9160, 7000, 7199 |

### Service Containers

Each service in `platform/arcus-containers/` that applies `container.gradle` gets Docker tasks:

```bash
# Build and run a specific service
./gradlew :platform:arcus-containers:driver-services:distDocker
./gradlew :platform:arcus-containers:driver-services:startService

# Or run directly (foreground)
./gradlew :platform:arcus-containers:driver-services:runDocker
```

Docker images are tagged as `arcus/{service-name}:{major}.{minor}.{patch}`.

### Registry Publishing

```bash
# Tag for registry
./gradlew tagDocker -PDOCKER_PREFIX_OVERRIDE=ghcr.io/myorg

# Push to registry
./gradlew pushDocker
```

---

## Agent Distribution

### Build

The agent targets **Java 8** (toolchain in `agent/build.gradle`). Build with:

```bash
# Requires a Java 8 JDK (e.g. temurin-8-jdk)
JAVA_HOME=/usr/lib/jvm/temurin-8-jdk-amd64 \
  ./gradlew :agent:arcus-agent:arcus-agent-hub-v2:distTar
```

### Building with Closed-Source Controller Jars

The original Iris hub firmware included closed-source controller jars for ZigBee, Z-Wave, Sercomm cameras, Hue, and 4G. These are not part of the open-source repository but can be extracted from a working hub and included in the build.

1. **Extract jars from a hub** — Copy the `iris2-*-controller-*.jar` files from a running hub's `/data/agent/libs/` directory.

2. **Build with external jars:**

```bash
JAVA_HOME=/usr/lib/jvm/temurin-8-jdk-amd64 \
  ./gradlew :agent:arcus-agent:arcus-agent-hub-v2:distTar \
  -Pexternal_jars_dir=/path/to/extracted/hub/libs
```

This includes the iris2 jars in the distribution and excludes the open-source stub replacements (`arcus-zigbee-controller`, `arcus-zw-controller`).

### Netty 4.0/4.1 Compatibility

The iris2 jars were compiled against Netty 4.0. The open-source agent uses Netty 4.1. Several compatibility shims bridge this gap:

- **`agent/gradle/netty-compat.gradle`** — ASM build-time patching of `netty-buffer` to restore `ByteBufProcessor` bridge methods removed in 4.1
- **`UartEpollChannel`** — Backward-compat constructor accepting `Socket` (4.0) in addition to `LinuxSocket` (4.1)
- **`UartOioChannel`** — Added `isInputShutdown()`/`shutdownInput()` required by Netty 4.1
- **`com.netflix.governator.annotations.WarmUp`** — Stub annotation so `IrisLifecycleManager` can discover `@WarmUp` methods on iris2 classes (which reference Governator, not the Arcus replacement)

### ARM32 Native Libraries

The hub runs on ARM32 (ARMv7) hardware. Two Netty native libraries are cross-compiled for this platform:

- **epoll** — Netty transport for efficient I/O (packaged as a jar in `libs/`)
- **tcnative** — Netty SSL via BoringSSL (packaged as a `.so` in `lib/`)

Pre-built binaries are checked into the repo. To rebuild (e.g. after bumping Netty or tcnative versions):

```bash
./gradlew :agent:arcus-hal:arcus-hal-hub-v2:buildNettyArm32

# Without Docker cache (clean rebuild):
./gradlew :agent:arcus-hal:arcus-hal-hub-v2:buildNettyArm32 -Pno_docker_cache
```

This requires Docker with `linux/arm/v7` platform support (QEMU binfmt_misc). The task:
1. Builds BoringSSL and the Netty JNI libraries in an ARM32 Debian container
2. Packages the epoll `.so` into `libs/netty-transport-native-epoll-{version}-linux-arm_32.jar`
3. Copies the tcnative `.so` to `agent/arcus-hal/hub-v2/src/dist/main/lib/`

The Dockerfiles are in `agent/arcus-hal/hub-v2/docker/`:
- `Dockerfile.netty-arm32` — BoringSSL static build (self-contained, no system OpenSSL dependency)
- `Dockerfile.netty-arm32-openssl` — Dynamic linking against system OpenSSL

**Note:** When building with `-Pexternal_jars_dir` (iris2 jars), the ARM32 epoll native jar is automatically excluded from the distribution. The iris2 zigbee controller's `EpollSerialTransport` is incompatible with Netty 4.1 epoll on UART file descriptors.

### Output Structure

```
arcus-agent-hub-v2-{VERSION}/
├── bin/iris-agent     # Startup script
├── conf/              # logback.xml, sounds/, voice/, agent.version
├── libs/              # JAR dependencies (+ patched netty-buffer)
└── lib/               # Native libraries (.so files for tcnative, apr)
```

### Deploying to a Hub

1. Copy the tarball to the hub and extract to `/data/agent/`
2. Delete the CDS archive (caches old bytecode): `rm -f /data/agent/agent.jsa`
3. Restart the agent: `/etc/init.d/irisinitd restart`

### Simulated Mode

Run the agent locally without hub hardware:

```bash
cd agent
export IRIS_AGENT_HUBV2_FAKE=true
export IRIS_AGENT_HUBV2_DATADIR=~/.hub-simulated
export IRIS_GATEWAY_URI=wss://localhost:8082/hub/1.0
export ZWAVE_DISABLE=true
export ZIGBEE_DISABLE=true
export FOURG_DISABLE=true
./run-agent.sh
```

---

## CI/CD

### GitHub Actions (`.github/workflows/`)

**test.yml** — Runs on every push:
```yaml
- Set up JDK 8 (Corretto)
- Setup Gradle (gradle-build-action v2)
- ./gradlew test --no-daemon
```

**release.yml** — Triggered on version tags (`v*`):
```yaml
1. Run full test suite
2. Build JARs: ./gradlew jar
3. Build & push Docker images: bash khakis/bin/release.sh
   - Registry: GHCR (GitHub Container Registry)
   - Naming: arcus--{service-name}:{version}
```

---

## Version Management

### Version Properties

Each subsystem has its own `version.properties`:

```properties
# Example: khakis/version.properties
major=2026
minor=2
patch=1
qualifier=
```

Computed version: `2026.2.1` (or `2026.2.1-SNAPSHOT` with qualifier)

Docker images strip the qualifier: `2026.2.1`

### Version Tasks

```bash
./gradlew bump_major    # Increment major, reset minor/patch
./gradlew bump_minor    # Increment minor, reset patch
./gradlew bump_patch    # Increment patch
./gradlew tag_release   # Git tag current version
```

### Release Workflow

```bash
./gradlew branchRelease   # Create release-{major}.{minor} branch
./gradlew tagRelease      # Tag and bump version on both branches
```

On a build server (`CI` env var set), version changes are auto-pushed to the remote.

---

## Testing

### Unit Tests

```bash
./gradlew test                                    # All tests
./gradlew :common:arcus-common:test               # Single module
```

Test logging outputs: `STARTED`, `PASSED`, `FAILED`, `SKIPPED`

### System Tests

Separate source set at `src/system-test/java` with its own classpath:

```bash
# Assumes platform is already running
./gradlew systemTestRunner

# Full setup (starts platform, runs tests, stops)
./gradlew individualSystemTest
```

### Code Coverage (optional)

```bash
./gradlew test jacocoTestReport -Puse_jacoco=true
```

### Static Analysis (optional)

```bash
./gradlew build -Peyeris_owasp=true   # SpotBugs + OWASP dependency check
```

---

## Hub Deployment

`gradle/deploy.gradle` provides SSH-based deployment to physical hubs:

```bash
./gradlew :agent:arcus-agent:hub-v2:deploy \
  -Pdeploy_host=10.0.0.4 \
  -Pdeploy_password='kz58!~Eb.RZ?+bqb'
```

This uploads the distribution archive, clears the old agent, unpacks, and reboots the hub.

---

## Build Properties Reference

| Property | Description | Default |
|----------|-------------|---------|
| `use_maven_local` | Use local Maven repository | `false` |
| `use_jacoco` | Enable code coverage | `false` |
| `eyeris_owasp` | Enable OWASP dependency checking + SpotBugs | unset |
| `netty_override_version` | Override Netty version | `4.1.128.Final` |
| `tcnative_override_version` | Override Tomcat Native version | — |
| `cpus`, `mem`, `instances` | Docker resource allocation | — |
| `exposePort` | Ports to expose on Docker container | — |
| `deploy_host` | Hub IP for SSH deployment | — |
| `deploy_password` | Hub SSH password | — |

| Environment Variable | Description |
|---------------------|-------------|
| `CI` | Build server flag (enables Gradle Enterprise scan, auto-push) |
| `LOCAL_PROPS_HOME` | Directory for environment-specific `.properties` files |
| `DOCKER_PREFIX_OVERRIDE` | Docker registry prefix |
| `REGISTRY_NAME` | Docker registry hostname |
| `REGISTRY_SEPERATOR` | Registry path separator (`/` or `--`) |
