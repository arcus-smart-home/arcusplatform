# Java 11 Upgrade Notes

## Overview

The build automatically detects the JDK version running Gradle and adjusts `sourceCompatibility`/`targetCompatibility`, JVM flags, and test exclusions accordingly. No flags or properties needed — just run Gradle with the desired JDK.

```bash
# Java 8 build
JAVA_HOME=/usr/lib/jvm/temurin-8-jdk-amd64 ./gradlew build

# Java 11 build
JAVA_HOME=/usr/lib/jvm/temurin-11-jdk-amd64 ./gradlew build
```

## Architecture

- **Platform modules** — Targets the JDK running Gradle (8 or 11+)
- **Common modules** — Always targets Java 8 bytecode so agent can consume them
- **Agent modules** — Always Java 8 via Gradle toolchain (`sun.misc.SharedSecrets` for serial/watchdog hardware)
- **Tools modules** — Targets the JDK running Gradle
- **Docker base image** — Parameterized via `ARG JAVA_VERSION`, auto-detected from build host JDK

## How the Gating Works

`gradle/subproject.gradle` sets two ext properties based on `JavaVersion.current()`:

- `isJava8` — true when running on JDK 8
- `javaCompatibility` — `'1.8'` on JDK 8, otherwise the current JDK version string

These control:

| What | JDK 8 | JDK 11+ |
|------|-------|---------|
| `sourceCompatibility` / `targetCompatibility` | `1.8` | Current JDK version |
| JVM intrinsic flags (UseSHA*, UseXmm*, etc.) | Included | Excluded |
| PowerMock test (MailgunEmailProviderTest) | Runs | Excluded |
| Docker plugin workaround (targetCompatibility reset) | No-op | Active |

`common/build.gradle` overrides this to always target Java 8, since agent depends on common libraries.

## Changes Made

### Build Configuration

| File | Change |
|------|--------|
| `gradle/subproject.gradle` | Defines `isJava8`/`javaCompatibility`, uses dynamic `sourceCompatibility` |
| `gradle/application.gradle` | Java 8 JVM intrinsic flags gated behind `JavaVersion.VERSION_1_8` |
| `gradle/container.gradle` | Docker plugin workaround (always runs, harmless on Java 8) |
| `gradle/buildscript.gradle` | SpotBugs plugin 2.0.1 → 4.8.0 |
| `gradle/dependencies.gradle` | Gson 2.3.1 → 2.10.1, Groovy 2.5.8 → 2.5.15, added javax.activation/annotation libs |
| `platform/build.gradle` | `afterEvaluate` overrides compatibility to current JDK when not Java 8 |
| `agent/build.gradle` | Forces Java 8 toolchain via `JavaLanguageVersion.of(8)` |
| `common/build.gradle` | Always targets Java 8; added JAXB/javax.activation deps |
| `common/arcus-protocol/build.gradle` | Added JAXB + javax.activation to `generator` configuration |
| `common/arcus-drivers/groovy-bindings/build.gradle` | Added JAXB + javax.activation to `generator` configuration |
| `common/protocol-generator/build.gradle` | Added JAXB activation + compile deps |
| `platform/.../notification-services/build.gradle` | PowerMock test excluded when `!isJava8` |
| `khakis/arcus-java/Dockerfile` | `ARG JAVA_VERSION=11`, uses `temurin-${JAVA_VERSION}-jre` |
| `khakis/bin/common.sh` | Auto-detects build host JDK, passes `--build-arg JAVA_VERSION` |

### Source Code (backward-compatible with Java 8)

| File | Change |
|------|--------|
| `GsonFactory.java` | Replaced reflection hack with `setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)` |
| `IrisObjectTypeAdapterFactory.java` | `install()` is now a no-op; removed reflection hack |
| `IrisObjectTypeAdapter.java` | `LinkedTreeMap` → `LinkedHashMap` |
| `ArgumentResolverFactoryChain.java` | Explicit type parameters `<I, R>` (stricter inference in JDK 11+) |
| `ContextualEventHandlers.java` | Explicit local variable for nested wildcard inference |
| `TestClassPathJarResource.java` | Uses Guava class instead of `java.lang.String` (JDK 9+ `jrt:` protocol) |

## Dependency Upgrades

| Dependency | Old | New | Reason |
|------------|-----|-----|--------|
| SpotBugs Gradle plugin | 2.0.1 | 4.8.0 | Old version incompatible with Gradle 6.9 worker API |
| Gson | 2.3.1 | 2.10.1 | Old version used `Field.getDeclaredField("modifiers")` hack removed in JDK 12+ |
| Groovy | 2.5.8 | 2.5.15 | 2.5.8 incompatible with JDK 15+ |

## Known Limitations

1. **Gradle 6.9 requires JDK 8 or 11 to run** — JDK 17 is not supported (ASM version too old for class file major version 61). Upgrade to Gradle 7.3+ to use JDK 17.

2. **SpotBugs 4.8.0 requires JDK 11+ to run** — On JDK 8, SpotBugs tasks will fail but `ignoreFailures = true` prevents build breakage.

3. **MailgunEmailProviderTest excluded on JDK 11+** — PowerMock 1.7.3 is incompatible with JDK 12+. Fix: upgrade to PowerMock 2.0+ with Mockito 2.x, or rewrite without PowerMock.

4. **Agent stays on Java 8** — Uses `sun.misc.SharedSecrets` and `sun.nio.ch.FileChannelImpl.open()` in `UartNative.java` and `WatchdogNative.java`. No public replacement exists.

5. **SpotBugs ignoreFailures** — 1829 pre-existing violations. Unrelated to the upgrade.
