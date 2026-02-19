# Java 11 Upgrade Notes

## Build Command

```bash
./gradlew build \
  -x :platform:arcus-containers:voice-service:build \
  -x :platform:arcus-containers:voice-service:generateProto \
  -Dorg.gradle.java.home=/Users/andrew/Library/Java/JavaVirtualMachines/azul-15.0.10/Contents/Home
```

Result: **BUILD SUCCESSFUL** - 568 tasks

## Architecture

- **Platform modules** → Java 11 (sourceCompatibility/targetCompatibility '11')
- **Agent modules** → Java 8 via Gradle toolchain (keeps `sun.misc.SharedSecrets` for serial port/watchdog hardware access)
- **Common modules** → Compiled targeting Java 8 bytecode, tests run on build JDK (15)
- **Tools modules** → Java 11 (depend on platform modules, Gradle variant resolution requires matching)
- **Docker base image** → `temurin-11-jre` (was `temurin-8-jre`)

## Files Changed

### Build Configuration

| File | Change |
|------|--------|
| `gradle/buildscript.gradle` | SpotBugs plugin 2.0.1 → 4.8.0 |
| `gradle/subproject.gradle` | Added `spotbugs { ignoreFailures = true }` |
| `gradle/dependencies.gradle` | Gson 2.3.1 → 2.10.1, Groovy 2.5.8 → 2.5.15 |
| `platform/build.gradle` | Added subprojects block: afterEvaluate sets Java 11 for java-plugin projects |
| `platform/arcus-platform-drivers/driver-tests/build.gradle` | Java 8 → 11 |
| `agent/build.gradle` | Added subprojects block: Java 8 toolchain via `JavaLanguageVersion.of(8)` |
| `common/build.gradle` | Added `jaxb_api`, `javax_annotation_api` deps (removed from JDK 9+) |
| `common/protocol-generator/build.gradle` | Added `jaxb_api`, `jaxb_impl` compile deps |
| `common/arcus-protocol/build.gradle` | Added JAXB + javax.activation to `generator` configuration |
| `common/arcus-drivers/groovy-bindings/build.gradle` | Added JAXB + javax.activation to `generator` configuration |
| `tools/eye-kat/build.gradle` | Java 8 → 11 |
| `tools/oculus/build.gradle` | Java 8 → 11 |
| `tools/arcus-captools/build.gradle` | Java 8 → 11 |
| `khakis/arcus-java/Dockerfile` | `temurin-8-jre` → `temurin-11-jre` |
| `platform/arcus-containers/notification-services/build.gradle` | Excluded PowerMock test (incompatible with JDK 12+) |

### Source Code

| File | Change |
|------|--------|
| `common/arcus-client/src/main/java/com/iris/gson/GsonFactory.java` | Replaced `IrisObjectTypeAdapterFactory` registration with `builder.setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)` |
| `common/arcus-client/src/main/java/com/iris/gson/IrisObjectTypeAdapterFactory.java` | Removed reflection hack; `install()` is now a no-op; kept as TypeAdapterFactory for backward compat |
| `common/arcus-client/src/main/java/com/iris/gson/IrisObjectTypeAdapter.java` | Replaced internal `com.google.gson.internal.LinkedTreeMap` with `java.util.LinkedHashMap` |
| `common/arcus-reflection/src/main/java/com/iris/reflection/ArgumentResolverFactoryChain.java` | Explicit type parameters `<I, R>` (JDK 15 stricter type inference) |
| `common/arcus-drivers/drivers-common/src/main/java/com/iris/driver/handler/ContextualEventHandlers.java` | Explicit local variable type for nested wildcard inference |

### Test Code

| File | Change |
|------|--------|
| `common/arcus-client/src/test/java/com/iris/gson/TestIrisObjectTypeAdapter.java` | Uses `setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)` instead of factory |
| `common/arcus-common/src/test/java/com/iris/resource/classpath/TestClassPathJarResource.java` | Uses Guava class instead of `java.lang.String` (JDK 9+ uses `jrt:` protocol for system classes) |

## Dependency Upgrades

| Dependency | Old | New | Reason |
|------------|-----|-----|--------|
| SpotBugs Gradle plugin | 2.0.1 | 4.8.0 | Incompatible with Gradle 6.9 worker API |
| Gson | 2.3.1 | 2.10.1 | Old version required reflection hack (`Field.getDeclaredField("modifiers")`) removed in JDK 12+ |
| Groovy | 2.5.8 | 2.5.15 | 2.5.8 doesn't support JDK 15 (`InvokerHelper` init failure) |

## Known Limitations

1. **voice-service excluded from build** — protoc 3.5.1 has no Apple Silicon (aarch64) binary. Pre-existing issue, not related to Java upgrade. Fix: upgrade protoc to 3.21+.

2. **MailgunEmailProviderTest excluded** — PowerMock 1.7.3 is fundamentally incompatible with JDK 12+ (Objenesis reflection issues). Only test in the project using PowerMock. Fix: upgrade to PowerMock 2.0+ with Mockito 2.x, or rewrite test without PowerMock.

3. **SpotBugs set to ignoreFailures** — 1829 violations reported in arcus-protocol alone. These are pre-existing and unrelated to the upgrade.

4. **Agent stays on Java 8** — Uses `sun.misc.SharedSecrets` and `sun.nio.ch.FileChannelImpl.open()` in `UartNative.java` and `WatchdogNative.java` for serial port and watchdog hardware access on embedded Linux hubs. These APIs have no public replacement.

## JDKs Used

- Build JDK: Azul Zulu 15.0.10 (`/Users/andrew/Library/Java/JavaVirtualMachines/azul-15.0.10/Contents/Home`)
- Agent toolchain: Amazon Corretto 8 (`/Library/Java/JavaVirtualMachines/amazon-corretto-8.jdk/Contents/Home`)
- Also available: JBR 17

## Next Steps

- [ ] Install JDK 11 and verify build/tests pass on the target JDK
- [ ] Upgrade protoc for voice-service Apple Silicon support
- [ ] Upgrade PowerMock 1.7.3 → 2.0+ (requires Mockito 1.x → 2.x)
- [ ] Address SpotBugs violations or configure exclusion filters
- [ ] Consider upgrading Gradle 6.9 → 7.x+ for better toolchain support
