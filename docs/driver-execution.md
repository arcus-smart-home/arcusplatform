# Driver-Services Execution Model

How `driver-services` loads, runs, and manages device drivers at runtime on the platform side.

---

## Component Roles

| Component | Class | Role |
|-----------|-------|------|
| **DeviceService** | `driver-services/.../DeviceService.java` | Device lifecycle: pairing, upgrade, lost, delete |
| **DriverExecutorRegistry** | `PlatformDriverExecutorRegistry.java` | Executor cache; creates, swaps, and evicts per-device executors |
| **DefaultDriverExecutor** | `drivers-common/.../executor/DefaultDriverExecutor.java` | Per-device event queue with borrowed-thread execution |
| **PlatformDeviceDriverContext** | `driver-services/.../PlatformDeviceDriverContext.java` | Attribute state, dirty tracking, Cassandra flush, response dispatch |
| **DeviceDriverImpl** | `drivers-common/.../DeviceDriverImpl.java` | Dispatches messages to DSL handlers and reflex engine |
| **Scheduler** | — | Deferred/named event scheduling, delegating back into the executor |

---

## Threading Model

There is **one `DefaultDriverExecutor` per device**. Execution is single-threaded per device but uses a shared thread pool — a thread is borrowed from the pool while the queue is non-empty and released when it drains.

```
Kafka message or scheduled event arrives
  │
  ▼
executor.fire(event)
  │
  ├─ synchronized(lock):
  │    queue(event)
  │    if running == null → running = currentThread  (lock acquired)
  │    else → return  (event queued; current executor thread will drain it)
  │
  └─ if lock acquired:
       while (event = queue.poll()) {
           synchronized(context) {        ← memory barrier
               DriverExecutors.dispatch(event, executor)
           }
       }
       running = null                     ← release; thread returns to pool
```

**Priority:** Protocol messages (Zigbee/Z-Wave frames) are processed before platform messages (SetAttributes, commands) via a `PriorityQueue` comparator.

**Message blocking:** If a platform message is awaiting a response (`context.hasMessageContext()`), the executor will not dequeue the next platform message until the current one is resolved — but it will still process protocol messages.

---

## Attribute State and Dirty Tracking

`PlatformDeviceDriverContext` splits attributes into two storage buckets:

| Bucket | What goes here | Persisted via |
|--------|---------------|---------------|
| **Device entity fields** | address, id, caps, tags, name, vendor, driverName, driverVersion, protocol, protocolId | `DeviceDAO.save(device)` |
| **Capability attributes** | All `CapabilityName.attributeName` values set in driver handlers | `DeviceDAO.updateDriverState(device, dirtyAttrs, variables)` |

`setAttributeValue(key, value)` marks the key in a `dirtyAttributes` set. `commit()` flushes only dirty attributes, then broadcasts a `ValueChangeEvent` with the changed map and clears the dirty set.

```
Handler sets attribute
  └─ context.setAttributeValue(key, val)
       ├─ updates working copy (Device or AttributeMap)
       └─ dirtyAttributes.add(key)

Handler returns
  └─ DeviceDriverImpl.commit(context)
       ├─ attrBindingHandler.preCommit()   ← auto-timestamps (bind x to y)
       ├─ context.commit()
       │    ├─ DeviceDAO.save(device)           (if device fields dirty)
       │    ├─ DeviceDAO.updateDriverState(...)  (capability attrs + variables)
       │    ├─ broadcast ValueChangeEvent        (dirty attrs only)
       │    └─ dirtyAttributes.clear()
       └─ postCommit()                     ← check connection state change
```

---

## Complete SetAttributes Request Flow

```
1. Client sends SetAttributes via client-bridge → Kafka
2. driver-services receives PlatformMessage on device's driver address
3. executor.fire(message)                    ← enqueued or inline
4. synchronized(context): dispatch(message)
5. DeviceDriverImpl.handlePlatformMessage()
   ├─ context.setMessageContext(message)    ← store for response tracking + timeout
   ├─ reflexes.handle(context, message)?    ← hub-local reflex check first
   │    └─ if handled: context.respondToPlatform(response); commit(); return
   └─ messageHandler.handleEvent(context, message)
        └─ SetAttributesHandler
             ├─ validate each attribute (exists? writable?)
             ├─ dispatch to SetAttributesConsumer by namespace (DSL handler)
             │    └─ driver code runs: context.setAttributeValue(...)
             └─ context.respondToPlatform(SetAttributesResponse or errors)
6. DeviceDriverImpl.commit()
   └─ context.commit() → save to Cassandra, broadcast ValueChangeEvent
7. context.respondToPlatform() → PlatformMessageBus → client-bridge → client
```

**Timeout:** If `respondToPlatform()` is never called within the message TTL, a `PlatformMessageTimeout` deferred event fires on the executor, calling `context.cancel()` which responds with `Errors.requestTimeout()`.

---

## Device Pairing Flow

```
Hub discovers device → sends ADD_DEVICE to hub-bridge → Kafka → driver-services

DeviceService.create(request, protocolAttributes)
  ├─ addsInProgress.putIfAbsent(protocolAddr) ← prevents duplicate concurrent adds
  ├─ DeviceDAO.findByProtocolAddress()        ← check for existing / tombstoned
  ├─ new Device(UUID, protocolAddr, placeId, accountId, state=CREATED)
  ├─ drivers.findDriverFor(population, discoveryAttrs, reflexVersion)
  │    └─ DriverRegistry evaluates matcher predicates → best match or fallback
  ├─ registry.associate(device, driver, initializer)
  │    ├─ PlatformDeviceDriverContext created with driver's base attributes
  │    └─ DefaultDriverExecutor created and cached
  └─ synchronized(executor context):
       ├─ DeviceInitializer.initialize()
       ├─ executor.fire(DriverEvent.ASSOCIATED)  → onAdded() handler runs
       ├─ DeviceDAO.save / broadcast EVENT_ADDED
       └─ executor.fire(CONNECTED or DISCONNECTED)
```

`addsInProgress` lock is released after the sequence; duplicate ADD_DEVICE for the same protocol address is rejected until the first completes.

---

## Driver Upgrade Flow

```
upgradeDriver(address, newDriverId)
  ├─ registry.loadConsumer(address)       ← load current executor
  ├─ drivers.loadDriverById(newDriverId)  ← load new driver
  └─ synchronized(executor context):
       ├─ if same driverId → no-op
       ├─ registry.associate(device, newDriver, initializer)
       │    ├─ old.stop()  ← sets stopped flag; in-flight event completes, no new ones
       │    ├─ preserve old attributes (filter out capabilities being replaced)
       │    ├─ merge new driver's base attributes
       │    └─ new DefaultDriverExecutor created and cached
       ├─ if driver name changed → fire ASSOCIATED event → onAdded()
       ├─ fire CONNECTED / DISCONNECTED
       └─ sendResyncToHub()  ← hub updates reflex definitions
```

---

## Scheduling

`Scheduler.scheduleIn` in the driver DSL schedules a named event that fires back into the device's own executor:

```
driver code: Scheduler.scheduleIn 'PollBattery', 5000

  └─ executor.defer('PollBattery', event, Date(now+5000))
       ├─ namedEvents.put('PollBattery', task)    ← allows cancellation
       └─ scheduler.scheduleAt(task, targetDate)

  When timer fires:
    DeferredEvent.run() → executor.fire(event)   ← re-enters the executor queue
    → synchronized(context): dispatch(event)
    → driver's onEvent('PollBattery') handler runs
```

Calling `Scheduler.scheduleIn` again with the same key cancels the previous pending event.

---

## Error Handling

| Scenario | Behaviour |
|----------|-----------|
| Exception in DSL handler | Logged as warning; `sendError()` responds with error message; `commit()` still called |
| Exception in protocol handler | Logged as warning; swallowed; next event processed |
| No response within TTL | `PlatformMessageTimeout` event fires → `Errors.requestTimeout()` response |
| Device deleted mid-flight | Message dropped; no response sent |
| Device tombstoned | Message dropped; request gets `notFound` error response |

---

## Key Files

| File | Location |
|------|----------|
| `DeviceService.java` | `platform/arcus-containers/driver-services/src/main/java/com/iris/driver/service/` |
| `PlatformDriverExecutorRegistry.java` | `platform/arcus-containers/driver-services/src/main/java/com/iris/driver/platform/` |
| `PlatformDeviceDriverContext.java` | same |
| `DefaultDriverExecutor.java` | `common/arcus-drivers/drivers-common/.../driver/service/executor/` |
| `DriverExecutor.java` | same (interface) |
| `DriverExecutors.java` | same (dispatch utilities) |
| `DeviceDriverImpl.java` | `common/arcus-drivers/drivers-common/.../driver/` |
