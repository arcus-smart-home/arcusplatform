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

### Cucumber BDD Tests

Cucumber BDD tests are the primary acceptance test framework for device drivers. Feature files written in Gherkin describe driver behavior — how it responds to device messages, platform commands, and lifecycle events. Step definitions in Groovy map the Gherkin to the Arcus driver test infrastructure.

#### Test Case Architecture

**Base class:** `AbstractDriverTestCase` (`platform/arcus-platform-drivers/driver-tests/src/test/java/com/iris/driver/unit/cucumber/AbstractDriverTestCase.java`)

Extends `IrisMockTestCase` with:

| Component | Description |
|-----------|-------------|
| `GroovyDriverFactory` | Loads and compiles `.driver` files from classpath |
| `InMemoryPlatformMessageBus` | Captures platform messages (ValueChange, GetAttributesResponse, etc.) |
| `InMemoryProtocolMessageBus` | Captures protocol messages (Z-Wave/Zigbee/IPCD commands to device) |
| `CapturingSchedulerContext` | Captures scheduled/deferred events with delay and data |
| `PinManagementContext` | Mock PIN validation (valid/invalid) |
| `DeviceDAO` (mock) | Captures device state saves |
| `PlatformDeviceDriverContext` | Driver execution context wrapping device + driver |
| `DefaultDriverExecutor` | Executes driver message handlers |

**Driver path resolution** — searches in order:

1. `src/test/resources/` — test-only drivers
2. `../../arcus-containers/driver-services/src/main/resources/` — production drivers
3. `build/drivers/` — compiled drivers

**Driver initialization flow:**

1. `GroovyDriverFactory.load()` compiles the `.driver` file with Arcus Groovy customizers
2. Device created from driver's base attributes (caps, protocol, drivername, vendor, model)
3. Device marked `ONLINE` and `PRESENT` to suppress spurious `ValueChange` events
4. `PlatformDeviceDriverContext` created wrapping device + driver
5. `DefaultDriverExecutor` created for message dispatch
6. `DeviceDAO.save()` and `DeviceDAO.updateDriverState()` mocked to capture mutations

#### Protocol-Specific Test Cases

Each protocol has a test case subclass and a command builder:

| Protocol | Test Case | Command Builder | Notes |
|----------|-----------|----------------|-------|
| Z-Wave | `ZWaveDriverTestCase` | `ZWaveCommandBuilder` | Uses `ZWaveAllCommandClasses` for command lookup. Default node ID 10. |
| Zigbee | `ZigbeeDriverTestCase` | `ZigbeeCommandBuilder` | Supports ZCL clusters by name, decimal, or hex (`onoff`, `6`, `0x0006`). Handles attribute read/report/write responses. |
| IPCD | `IpcdDriverTestCase` | `IpcdCommandBuilder` | Simulates events (`onValueChange`), responses (`GetParameterValuesResponse`), and reports. |

**ZWaveDriverTestCase** validates outbound commands by:
- Deserializing command class and command from protocol message
- Matching command class name (e.g., `switch_binary`) and command name (e.g., `report`)
- Checking send parameters as byte values

**ZigbeeDriverTestCase** validates outbound messages by:
- Deserializing ZCL message from protocol payload
- Matching cluster ID and message ID (by name or number)
- Checking ZCL data types (8/16/32/64-bit, float, string, IEEE address)
- Supporting `checkReadAttributes()` and `checkWriteAttribute()` for attribute-level validation

**IpcdDriverTestCase** validates outbound commands by:
- Converting `IpcdCommand` to a map
- Matching command name (e.g., `SetParameterValues`, `GetDeviceInfo`)
- Checking parameter values in the command's value map

#### MockGroovyDriverModule

**Location:** `platform/arcus-platform-drivers/driver-tests/src/test/java/com/iris/driver/unit/cucumber/MockGroovyDriverModule.java`

Extends `GroovyDriverModule` to provide test doubles:

| Binding | Description |
|---------|-------------|
| `CapturingSchedulerContext` | Captures all `defer()`, `scheduleIn()`, `scheduleRepeating()`, `cancel()` calls into a queue of `CapturedScheduledEvent` objects |
| `PinManagementContext` | EasyMock mock for `validatePin()` |
| `OnScheduledClosure` | Binds `onEvent` in driver Groovy environment |
| `Scheduler` property | Bound to capturing scheduler |
| `PinManagement` property | Bound to mock pin manager |

`CapturedScheduledEvent` stores: method name, event name, delay (ms), max retries, and data. Test steps poll these to assert scheduled behavior.

#### Step Definitions

**Location:** `platform/arcus-platform-drivers/driver-tests/src/test/java/com/iris/driver/unit/CucumberTestSteps.groovy`

Groovy-based step definitions using Cucumber's `EN` mixin. Protocol-specific test contexts are selected by tags:

```groovy
Before("@Zigbee") { context = new ZigbeeDriverTestCase(); context.setUp() }
Before("@ZWave")  { context = new ZWaveDriverTestCase();  context.setUp() }
Before("@IPCD")   { context = new IpcdDriverTestCase();   context.setUp() }
After             { context.tearDown() }
```

#### Gherkin Step Reference

**Setup Steps (Given):**

| Step | Description |
|------|-------------|
| `Given the <file>.driver has been initialized` | Load and initialize driver from file |
| `Given the capability <ns>:<attr> is <value>` | Set attribute value (fires ValueChange) |
| `Given the driver attribute <ns>:<attr> is <value>` | Set attribute without ValueChange |
| `Given the driver variable <name> is <value>` | Set driver variable (number, boolean, null, JSON, date) |
| `Given the time driver variable <name> is <N> <units> ago` | Set time variable relative to now |
| `Given the device has endpoint <id>` | Set Zigbee endpoint |
| `Given the device has tag <tag>` | Add tag to device |
| `Given the pin <pin> is valid` | Mock PIN validation → success |
| `Given the pin <pin> is invalid` | Mock PIN validation → failure |

**Trigger Steps (When):**

| Step | Description |
|------|-------------|
| `When the device is added` | Fire device added lifecycle event |
| `When the device is connected` | Fire device connected event |
| `When the device is disconnected` | Fire device disconnected event |
| `When the device is removed` | Fire device removed event |
| `When event <name> triggers` | Fire named scheduled event |
| `When event <name> triggers with <json>` | Fire scheduled event with data payload |
| `When a <cmd> command is placed on the platform bus` | Send platform command (no args) |
| `When a <cmd> command with the value of <ns>:<attr> <value> is placed on the platform bus` | Send command with attribute |
| `When the capability method <cmd>` | Begin building capability command (must follow with `And send to driver`) |
| `With capability <ns>:<attr> is <value>` | Add attribute to pending command |

**Protocol message simulation (When):**

| Step | Description |
|------|-------------|
| `When the device response with <type> <subType>` | Begin building inbound protocol message |
| `And with parameter <name> <value>` | Add parameter to message |
| `And with payload <bytes>` | Set raw payload (comma-separated hex/decimal) |
| `And with header flags <byte>` | Set ZCL flags |
| `And with manufacturer code <int>` | Set manufacturer code |
| `And with endpoint <id>` | Set Zigbee endpoint |
| `And send to driver` | Dispatch the built message to the driver |

**Assertion Steps (Then):**

| Step | Description |
|------|-------------|
| `Then the driver should place a <msgType> message on the platform bus` | Assert platform message sent |
| `Then the driver should not place a <msgType> message on the platform bus` | Assert platform message NOT sent |
| `And the message's <ns>:<attr> attribute should be <value>` | Check attribute in last platform message |
| `And the message's <ns>:<attr> attribute list should be [<list>]` | Check list attribute (order-independent) |
| `And the message's <ns>:<attr> attribute numeric value should be within delta <d> of <v>` | Fuzzy numeric check |
| `Then the capability <ns>:<attr> should be <value>` | Assert device attribute value |
| `Then the numeric capability <ns>:<attr> should be within <pct>% of <value>` | Percentage-based fuzzy check |
| `Then the driver variable <name> should be <value>` | Assert driver variable value |
| `Then the driver should send <type> <subType>` | Assert outbound protocol message |
| `And with parameter <name> <value>` | Check parameter in outbound message |
| `Then the driver should set timeout at <N> <units>` | Assert offline timeout |
| `Then the driver should poll <type>.<subType> every <N> <units>` | Assert polling schedule |
| `Then the driver should schedule event <name>` | Assert event scheduled |
| `Then the driver should schedule event <name> in <delay> <units>` | Assert event with delay |
| `Then the driver should schedule event <name> in <delay> <units> with <data>` | Assert event with delay and data |
| `Then the driver should schedule event <name> every <N> <units> <reps> times` | Assert repeating event |
| `Then the driver should cancel event <name>` | Assert event cancellation |
| `Then there should be no more scheduled events` | Assert no pending events |
| `Then the platform bus should be empty` | Assert no pending platform messages |
| `Then the protocol bus should be empty` | Assert no pending protocol messages |
| `Then both busses should be empty` | Assert both buses empty |
| `Then protocol message count is <N>` | Assert exact protocol message count |
| `Then nothing should happen` | Assert no messages and no events |

#### Feature File Structure

Feature files use this pattern:

```gherkin
@Protocol @DeviceTag
Feature: Descriptive feature name

  Background:
    Given the <driver-file>.driver has been initialized

  Scenario: Single test case
    When ...
    Then ...

  Scenario Outline: Parameterized test
    When the device response with <type> <subType>
      And with parameter value <value>
      And send to driver
    Then the capability <ns>:<attr> should be <expected>

    Examples:
      | type          | subType | value | expected |
      | switch_binary | report  | -1    | ON       |
      | switch_binary | report  | 0     | OFF      |
```

**Tags:**

| Tag | Purpose |
|-----|---------|
| `@ZWave`, `@Zigbee`, `@IPCD` | **Required** — selects protocol test case |
| `@Ignore` | Skip scenario |
| Device-specific: `@Jasco500`, `@AlertMe`, `@GreatStar`, etc. | Filter by device manufacturer/model |

#### Example: Z-Wave Switch Test

```gherkin
@ZWave @Jasco500
Feature: Unit Tests for the ZWJasco14288SwitchDriver

  Background:
    Given the ZW_Jasco_14288_InWallReceptacle.driver has been initialized

  Scenario: Driver reports capabilities to platform
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
      And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'swit', 'indicator']
      And the message's dev:devtypehint attribute should be Switch
      And the message's devadv:drivername attribute should be ZWJasco14288InWallReceptacleDriver
      And the message's devpow:source attribute should be LINE
    Then both busses should be empty

  Scenario: Device associated
    When the device is added
    Then the driver should send configuration set
      And with parameter param 3
      And with parameter size 1
      And with parameter val1 1
    Then both busses should be empty

  Scenario: Switch value changed
    When the device response with switch_binary report
      And with parameter value -1
      And send to driver
    Then the platform attribute swit:state should change to ON
    Then both busses should be empty

  Scenario Outline: Indicator value
    Given the capability indicator:indicator is <before>
    When the device response with switch_binary report
      And with parameter value <value>
      And send to driver
    Then the platform attribute indicator:indicator should change to <after>

    Examples:
      | before | value | after |
      | ON     | -1    | OFF   |
      | ON     | 0     | ON    |
      | OFF    | -1    | ON    |
```

#### Example: Zigbee Sensor Test

```gherkin
@Zigbee @AlertMe @Pendant
Feature: Zigbee AlertMe Care Pendant Driver Test

  Background:
    Given the ZB_AlertMe_CarePendant.driver has been initialized

  Scenario: Device connected while Present
    Given the capability pres:presence is PRESENT
    When the device is connected
    Then the driver should send 0x00F6 0xFC
    Then the driver should set timeout at 10 minutes

  Scenario Outline: Device sends Heartbeat with LQI
    Given the driver variable targetHelpState is -1
    When the device response with 240 251
      And with payload 8, 0,0,0,0, 0,0, 0,0, 0, <lqi>, 0, 0
      And send to driver
    Then the driver should place a base:ValueChange message on the platform bus
      And the message's devconn:signal attribute should be <signal>

    Examples:
      | lqi  | signal |
      | 0x00 |      0 |
      | 0x7F |     49 |
      | 0xFF |    100 |
```

#### Example: IPCD Plug Test

```gherkin
@IPCD @GreatStar
Feature: IPCD GreatStar Indoor Plug Driver Test

  Background:
    Given the IPCD_GreatStar_Indoor_Plug_2_12.driver has been initialized

  Scenario: Device Added
    When the device is added
    Then the driver should schedule event callGPV in 5000 milliseconds
    Then the driver should schedule event setReport in 10000 milliseconds
    Then the driver should send GetDeviceInfo command

  Scenario Outline: Platform turns on/off switch
    When a base:SetAttributes command with the value of swit:state <request> is placed on the platform bus
    Then protocol message count is 1
    Then the driver should send SetParameterValues command
      And with parameter switch.state <command>

    Examples:
      | request | command |
      | ON      | ON      |
      | OFF     | OFF     |

  Scenario: Handle GetParameterValuesResponse
    When the device sends response GetParameterValuesResponse
      And with parameter switch.state ON
      And send to driver
    Then the capability swit:state should be ON
```

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
# Run all driver features
./gradlew :platform:arcus-platform-drivers:driver-tests:cucumber

# Run only Z-Wave tests
./gradlew :platform:arcus-platform-drivers:driver-tests:cucumber -Ptags="@ZWave"

# Run specific feature files
./gradlew :platform:arcus-platform-drivers:driver-tests:cucumber -Pfeatures="zw/ZW_Jasco*.feature"

# Run excluding ignored tests (default behavior)
./gradlew :platform:arcus-platform-drivers:driver-tests:cucumber -Ptags="~@Ignore"
```

**Gradle task configuration:**

```groovy
configurations {
    cucumberRuntime { extendsFrom testImplementation, testRuntimeOnly }
}

task cucumber(type: JavaExec) {
    mainClass = "cucumber.api.cli.Main"
    classpath = configurations.cucumberRuntime + sourceSets.main.output + sourceSets.test.output
    args = cukeArgs   // --glue, --plugin, --tags, feature paths
}
```

**Report outputs:**
- HTML: `build/reports/test-results/cucumber/`
- JSON: `build/reports/test-results/cucumber.json`
- JUnit XML: `build/reports/test-results/cucumber.xml`

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
