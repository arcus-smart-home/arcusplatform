# Testing Infrastructure

The Arcus platform uses JUnit 4 with EasyMock as its primary testing stack, augmented with Cucumber BDD for driver acceptance tests. Custom base classes provide Guice-injected test contexts, in-memory message buses, and mock hardware abstraction for hub agent testing.

---

## Table of Contents

- [Frameworks and Dependencies](#frameworks-and-dependencies)
- [Test Base Classes](#test-base-classes)
- [Mock Infrastructure](#mock-infrastructure)
- [In-Memory Message Buses](#in-memory-message-buses)
- [Test Fixtures](#test-fixtures)
- [Driver Testing](#driver-testing)
- [Subsystem Testing](#subsystem-testing)
- [Agent / Hub Testing](#agent--hub-testing)
- [Gradle Test Configuration](#gradle-test-configuration)
- [CI/CD Pipeline](#cicd-pipeline)
- [Testing Patterns](#testing-patterns)
- [Key Locations](#key-locations)

---

## Frameworks and Dependencies

Defined in `gradle/dependencies.gradle`:

| Framework | Version | Purpose |
|-----------|---------|---------|
| JUnit | 4.13 | Core unit testing |
| EasyMock | 3.3 | Primary mocking framework |
| Mockito | 1.10+ | Alternative mocking |
| PowerMock | 1.7.3 | Static/final class mocking |
| Cucumber | 2.4.0 | BDD acceptance tests for drivers |
| Hamcrest | 1.3 | Assertion matchers |
| Cassandra Unit | 2.0.2.2 | Embedded Cassandra (optional) |
| Google Guava TestLib | 19.0 | Guava test utilities |

Cucumber sub-dependencies:
- `cucumber-junit` — JUnit runner and reports
- `cucumber-groovy` (2.0.1) — Groovy step definitions
- `cucumber-java8` — Java 8 lambda step definitions

---

## Test Base Classes

### IrisTestCase

**Location:** `platform/arcus-test/src/main/java/com/iris/test/IrisTestCase.java`

Base class for all platform tests. Extends JUnit `Assert`.

Features:
- **Guice DI** — `@Modules` annotation declares Guice modules for the test
- **Provider methods** — `@Provides` methods expose test services to the injector
- **ServiceLocator** — auto-initializes `ServiceLocator` with the test injector
- **Resource paths** — file system and classpath resource access

```java
@Modules({MyModule.class, TestHelperModule.class})
public class MyTest extends IrisTestCase {

    @Inject
    private MyService service;

    @Provides
    public SomeDependency provideDep() {
        return new TestDep();
    }

    @Test
    public void testSomething() {
        // service is injected, ServiceLocator is initialized
    }
}
```

### IrisMockTestCase

**Location:** `platform/arcus-test/src/main/java/com/iris/test/IrisMockTestCase.java`

Extends `IrisTestCase` with EasyMock integration.

Features:
- **`@Mocks` annotation** — declares service classes to auto-mock
- **Mock lifecycle** — `replay()`, `reset()`, `verify()` methods for all mocks
- **Auto-properties** — sets up mock-friendly properties (e.g., redirect base URLs)

```java
@Modules({MyModule.class})
@Mocks({DeviceDAO.class, PersonDAO.class})
public class MyMockTest extends IrisMockTestCase {

    @Inject
    private DeviceDAO deviceDao;  // Auto-mocked

    @Test
    public void testWithMocks() {
        EasyMock.expect(deviceDao.findById(id)).andReturn(device);
        replay();  // Replays all mocks

        // ... test logic ...

        verify();  // Verifies all expectations
    }
}
```

---

## Mock Infrastructure

### MockModule

**Location:** `platform/arcus-test/src/main/java/com/iris/test/MockModule.java`

Reusable Guice module that auto-creates EasyMock mocks and binds them:

- Integrates with `@Mocks` annotation
- Provides `replay()`, `reset()`, `verify()` for all registered mocks
- Fluent API for adding mock services

### MockExecutorService

**Location:** `platform/arcus-test/src/main/java/com/iris/test/MockExecutorService.java`

Mock `ScheduledExecutorService` for testing time-dependent code:

- Job queue with execution order control
- Support for one-time and repeating jobs
- Manual job execution via `runNext()`
- Delay and scheduling support without actual waiting

### CapturingSchedulerContext

Used in driver tests to capture scheduled events:

- Inspects scheduled events during driver execution
- Verifies time-based driver behavior without real delays

---

## In-Memory Message Buses

**Location:** `platform/arcus-lib/src/main/java/com/iris/core/messaging/memory/`

Drop-in replacements for Kafka-backed message buses, used extensively in tests:

| Class | Replaces |
|-------|----------|
| `InMemoryPlatformMessageBus` | Kafka platform message bus |
| `InMemoryProtocolMessageBus` | Kafka protocol message bus |
| `InMemoryIntraServiceMessageBus` | Kafka intra-service bus |

Provided by `InMemoryMessageModule` for Guice injection.

### Message Capture Pattern

Tests capture sent messages using EasyMock:

```java
protected Capture<MessageBody> responses = EasyMock.newCapture(CaptureType.ALL);
protected Capture<MessageBody> requests = EasyMock.newCapture(CaptureType.ALL);

// After execution:
List<MessageBody> sent = responses.getValues();
assertEquals("dev:SetAttributes", sent.get(0).getMessageType());
```

---

## Test Fixtures

### Fixtures Class

**Location:** `platform/arcus-test/src/main/java/com/iris/messages/model/Fixtures.java`

Central factory for test data:

- `Account` — test accounts with random IDs
- `Place` — test places with defaults
- `Device` — devices with protocol addresses
- Client and device addresses
- Service levels and subscriptions
- Random test data with proper initialization

### Domain-Specific Fixtures

Each subsystem has its own fixture classes in its test directory:

| Fixture Class | Subsystem |
|---------------|-----------|
| `SecurityFixtures` | Security alarm |
| `DoorsNLocksFixtures` | Doors & Locks |
| `CamerasFixtures` | Cameras |
| `ClimateFixtures` | Climate |
| `BehaviorFixtures` | Care behaviors |
| `LawnNGardenFixtures` | Lawn & Garden |

Located in `platform/arcus-subsystems/src/test/java/com/iris/common/subsystem/`.

---

## Driver Testing

### GroovyDriverTestCase

**Location:** `platform/arcus-platform-drivers/driver-tests/src/test/java/com/iris/driver/groovy/GroovyDriverTestCase.java`

Base class for unit testing Groovy-based drivers. Extends `IrisMockTestCase`.

Pre-configured with:
- `GroovyDriverFactory` for loading `.driver` files
- Mock `DeviceDAO`, `PersonDAO`, `PlaceDAO`, `PlacePopulationCacheManager`
- In-memory `PlatformMessageBus` and `ProtocolMessageBus`
- Groovy script engine with compilation customizers
- Scheduler, Control protocol, and ZWave protocol plugins

### Cucumber BDD Tests (AbstractDriverTestCase)

**Location:** `platform/arcus-platform-drivers/driver-tests/src/test/java/com/iris/driver/unit/cucumber/AbstractDriverTestCase.java`

BDD-style driver testing using Cucumber feature files written in Gherkin:

```gherkin
Feature: Aeon Labs Energy Reader

  @ZWave
  Scenario: Device connected
    Given the driver is loaded
    And the device is connected
    Then the driver should set attribute dev:online to true
```

Protocol-specific test case subclasses:

| Class | Protocol |
|-------|----------|
| `ZWaveDriverTestCase` | Z-Wave |
| `ZigbeeDriverTestCase` | Zigbee |
| `IpcdDriverTestCase` | IPCD |

Each provides:
- Driver initialization from Groovy scripts
- Platform and protocol message buses (in-memory)
- Device fixture creation and state management
- `CapturingSchedulerContext` for time-based behavior
- Protocol-specific command builders (`ZWaveCommandBuilder`, `ZigbeeCommandBuilder`, `IpcdCommandBuilder`)
- `PinManagement` mock

### Feature File Organization

```
platform/arcus-platform-drivers/driver-tests/
└── src/cucumber/resources/com/iris/driver/unit/
    ├── zw/                    # Z-Wave driver features
    │   ├── ZW_Aeon_EnergyReader.feature
    │   ├── ZW_Jasco_Switch.feature
    │   └── ...
    ├── zb/                    # Zigbee driver features
    │   ├── ZB_Alertme_SmartPlug.feature
    │   └── ...
    └── ipcd/                  # IPCD driver features
        └── ...
```

### Running Cucumber Tests

```bash
# Via Gradle (custom task)
./gradlew :platform:arcus-platform-drivers:driver-tests:cucumber

# Cucumber args configured in build.gradle
task cucumber(type: JavaExec) {
    main = "cucumber.api.cli.Main"
    classpath = configurations.cucumberRuntime + sourceSets.main.output + sourceSets.test.output
    args = cukeArgs
}
```

Feature files are excluded from the standard `test` task:

```groovy
test {
    exclude '**/*.feature'
}
```

---

## Subsystem Testing

### SubsystemTestCase

**Location:** `platform/arcus-containers/subsystem-service/src/test/java/com/iris/common/subsystem/SubsystemTestCase.java`

Generic base class: `SubsystemTestCase<M extends SubsystemModel>`. Extends `IrisMockTestCase`.

Features:
- `TransactionalModelStore` — in-memory model storage
- Mock `SubsystemContext` with message capture
- Multiple message buses (platform, protocol)
- `Capture` objects for response/request/broadcast assertion

### Pattern

```java
public class MySubsystemTest extends SubsystemTestCase<MySubsystemModel> {

    @Override
    protected MySubsystem createSubsystem() {
        return new MySubsystem();
    }

    @Test
    public void testDeviceAdded() {
        // Add a device model to the store
        Model device = addModel(SecurityFixtures.createMotionSensor());

        // Verify subsystem reacted
        assertEquals(1, model.getDevices().size());
        assertTrue(model.getDevices().contains(device.getAddress()));
    }
}
```

### Specialized Subsystem Tests

Each subsystem has tests with domain-specific fixtures:

- `AlarmSubsystemTestCase` — alarm system tests
- `DoorsNLocksSubsystemTestCase` — door lock tests
- `SecuritySubsystemTestCase` — security tests
- `ClimateSubsystemTestCase` — climate control tests

---

## Agent / Hub Testing

### SystemTestCase

**Location:** `agent/arcus-test-agent/src/main/java/com/iris/agent/test/SystemTestCase.java`

Base class for agent-level system tests:

- `@BeforeClass` / `@AfterClass` for one-time system startup/shutdown
- Creates temporary directories for hub data
- Initializes `IrisHalSimulated` (simulated hardware abstraction)

### AbstractSystemTestCase

**Location:** `agent/arcus-test-agent/src/main/java/com/iris/agent/test/AbstractSystemTestCase.java`

Lower-level infrastructure:

- Starts full Iris agent system with temporary storage
- Uses `IrisHalSimulated` for hardware abstraction (no real ZigBee/Z-Wave)
- Manages `StorageService` with temporary directories
- Simulates hub boot sequence

---

## Gradle Test Configuration

### Standard Test Task

All subprojects inherit the test configuration from `gradle/subproject.gradle`:

```bash
# Run all tests
./gradlew test

# Run tests for a single module
./gradlew :common:arcus-common:test

# Run with code coverage
./gradlew test jacocoTestReport -Puse_jacoco=true
```

Test output logs: `STARTED`, `PASSED`, `FAILED`, `SKIPPED`.

### System Test Source Set

Defined in `gradle/subproject.gradle`, a separate `system-test` source set:

```
src/system-test/java     # System test sources
src/system-test/resources # System test resources
```

```bash
# Run system tests (assumes platform is running)
./gradlew systemTestRunner

# Full setup: start platform, run tests, stop
./gradlew individualSystemTest
```

### Static Analysis (optional)

```bash
./gradlew build -Peyeris_owasp=true   # SpotBugs + OWASP dependency check
```

---

## CI/CD Pipeline

### GitHub Actions (`.github/workflows/test.yml`)

Runs on every push:

```yaml
- Set up JDK 8 (Corretto)
- Setup Gradle (gradle-build-action v2)
- ./gradlew test --no-daemon
```

- **Runner:** Ubuntu latest
- **Java:** Corretto 8
- **Caching:** Gradle build action caches dependencies
- No system tests or Cucumber tests in CI currently

---

## Testing Patterns

### Dependency Injection

All test dependencies are injected via Guice:
- `@Modules` declares which modules to load
- `@Mocks` declares which services to auto-mock
- `@Inject` on fields for dependency injection
- `@Provides` methods for custom test bindings

### EasyMock Lifecycle

Standard pattern across the codebase:

```java
// Setup expectations
EasyMock.expect(dao.findById(id)).andReturn(result);

// Enter replay mode
replay();

// Execute test
service.doSomething(id);

// Verify all expectations met
verify();
```

### Message Testing

1. Set up `Capture<MessageBody>` with `CaptureType.ALL`
2. Execute code that sends messages
3. Assert on `capture.getValues()` — message types, attributes, destinations

### Parameterized Tests

`@RunWith(Parameterized.class)` for data-driven test variations. Common in alarm subsystem, climate control, and handler tests.

### Fixture Pattern

- Central `Fixtures` class for common objects (accounts, places, devices)
- Domain-specific fixture classes per subsystem
- Factory methods return pre-configured model objects

---

## Key Locations

### Test Infrastructure

| Location | Description |
|----------|-------------|
| `platform/arcus-test/src/main/java/com/iris/test/` | Base classes: `IrisTestCase`, `IrisMockTestCase`, `MockModule` |
| `platform/arcus-test/src/main/java/com/iris/test/util/` | Test utilities (`TestUtils`) |
| `platform/arcus-test/src/main/java/com/iris/messages/model/Fixtures.java` | Central test fixture factory |
| `platform/arcus-lib/src/main/java/com/iris/core/messaging/memory/` | In-memory message buses |
| `agent/arcus-test-agent/src/main/java/com/iris/agent/test/` | Agent system test infrastructure |

### Driver Tests

| Location | Description |
|----------|-------------|
| `platform/arcus-platform-drivers/driver-tests/src/test/java/com/iris/driver/groovy/` | `GroovyDriverTestCase` |
| `platform/arcus-platform-drivers/driver-tests/src/test/java/com/iris/driver/unit/cucumber/` | Cucumber test cases (`ZWaveDriverTestCase`, etc.) |
| `platform/arcus-platform-drivers/driver-tests/src/cucumber/resources/com/iris/driver/unit/` | `.feature` files by protocol |

### Subsystem Tests

| Location | Description |
|----------|-------------|
| `platform/arcus-containers/subsystem-service/src/test/java/com/iris/common/subsystem/SubsystemTestCase.java` | Base subsystem test |
| `platform/arcus-subsystems/src/test/java/com/iris/common/subsystem/` | All subsystem tests + fixtures |

### Configuration

| Location | Description |
|----------|-------------|
| `platform/arcus-lib/src/test/resources/test.properties` | Test configuration |
| `platform/bridge-common/src/test/resources/test-security.properties` | Security test config |
