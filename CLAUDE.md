# Arcus Platform

Java IoT platform (Iris by Lowe's fork). Gradle 7.6.4, Java 11, Groovy drivers.

## Repository Layout

- `agent/` — Hub agent (runs on Iris hub hardware)
- `common/` — Shared libraries, model definitions, protocol codegen
- `platform/` — Backend microservices (~24 containers)
- `tools/` — Oculus desktop client, eye-kat, captools
- `khakis/` — Docker infrastructure (Kafka, Cassandra, ZooKeeper)
- `gradle/` — Shared build scripts (`dependencies.gradle` has all versions)
- `docs/` — Documentation (MkDocs site)

## Build Commands

```bash
./gradlew jar                                # Build all
./gradlew test                               # Run all tests
./gradlew :platform:arcus-lib:test           # Test one module
./gradlew :platform:arcus-khakis:startPlatform  # Start Docker infra
```

## Coding Conventions

- **Java 11** — source/target compatibility set in `platform/build.gradle`
- **3-space indentation** (no tabs)
- **SLF4J + Logback** for logging — never use System.out or log4j directly
- **JUnit 4** for tests, **EasyMock** for mocking, **Cucumber** for BDD
- Test classes: `FooTest.java` (not `TestFoo`)
- Drivers are written in **Groovy DSL** (`*.driver` files)

## Dependency Management

All versions in `gradle/dependencies.gradle` via `ext.libraries`. Reference as:

```groovy
dependencies {
    implementation libraries.cassandraDriver
    api project(':common:arcus-common')  // use api for inter-module deps
}
```

Never hardcode versions in subproject `build.gradle` files.

Key configuration scopes (Gradle 7):
- `implementation` / `api` (not `compile`)
- `testImplementation` (not `testCompile`)
- `runtimeOnly` (not `runtime`)

## Architecture Notes

- **Message bus:** Kafka — services communicate via platform messages
- **Database:** Cassandra (no ORM, raw CQL via datastax driver)
- **DI:** Guice 4.0 with custom `IrisLifecycleManager`
- **Networking:** Netty for bridges (client-bridge, hub-bridge)
- **Auth:** Apache Shiro (SHA-256 passwords, Shiro wildcard permissions)
- **Serialization:** Jackson 2.18.6 — avoid GSON for new code

## Key Patterns

- Capabilities are defined in XML (`common/arcus-model/src/main/resources/capability/`)
  and code-generated into Java classes. Don't edit generated files.
- Device drivers use a Groovy DSL — see `docs/driver-model.md`.
- Subsystems extend `BaseSubsystem` — see `docs/subsystems.md`.
- Platform request handlers extend `MessageHandler` and are registered via Guice.

## Detailed Docs

| Topic | File |
|-------|------|
| Platform services | `docs/platform-services.md` |
| Common libraries & codegen | `docs/common.md` |
| Driver DSL | `docs/driver-model.md` |
| Driver execution | `docs/driver-execution.md` |
| Reflex system | `docs/reflexes.md` |
| Hub agent | `docs/agent.md` |
| Build system | `docs/build.md` |
| Docker infra | `docs/khakis.md` |
| Hub bridge | `docs/hub-bridge.md` |
| Subsystems | `docs/subsystems.md` |
| Rules engine | `docs/rules.md` |
| Scheduler | `docs/scheduler.md` |
| Testing | `docs/testing.md` |
| Tools | `docs/tools.md` |
