# Scheduler Service — Time-Based Automation (`scheduler-service`)

The scheduler service manages recurring and one-shot scheduled commands across the Arcus platform. It supports weekly schedules with absolute times or sunrise/sunset-relative times, organized into mutually exclusive groups. Execution uses partition-based time bucketing for horizontal scalability.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Capabilities](#capabilities)
- [Schedule Model](#schedule-model)
- [Execution Pipeline](#execution-pipeline)
- [Time Bucketing and Partitions](#time-bucketing-and-partitions)
- [Sunrise/Sunset and Timezone Support](#sunrisesunset-and-timezone-support)
- [Schedule Lifecycle](#schedule-lifecycle)
- [Grouping and Mutual Exclusion](#grouping-and-mutual-exclusion)
- [Persistence](#persistence)
- [Configuration](#configuration)
- [Metrics](#metrics)
- [Key Files](#key-files)

---

## Architecture Overview

```
User / Subsystem
    │  ScheduleWeeklyCommand, FireCommand, etc.
    ▼
┌─────────────────────────────────────────────┐
│         SchedulerCapabilityService           │
│  (listens to platform bus, routes requests)  │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│     SchedulerCapabilityDispatcher            │
│  (per-scheduler, single-threaded)            │
│                                              │
│  WeeklyScheduleRequestHandler                │
│  ├─ addTimeOfDayCommand()                    │
│  ├─ syncNextFireTime()                       │
│  └─ updateRelativeTime() (sunrise/sunset)    │
└──────────────────┬──────────────────────────┘
                   │ save + schedule
          ┌────────┴────────┐
          ▼                 ▼
┌──────────────┐  ┌─────────────────────┐
│ Cassandra    │  │ EventSchedulerService│
│ (scheduler,  │  │ (time bucketing)     │
│  sched_event)│  │                      │
└──────────────┘  └─────────┬───────────┘
                            │ at scheduled time
                            ▼
                  ┌─────────────────────┐
                  │ ScheduledEvent       │
                  │ → fire target command│
                  │ → recalculate next   │
                  └─────────────────────┘
```

**Service container:** `platform/arcus-containers/scheduler-service/`
**Platform library:** `platform/arcus-lib/src/main/java/com/iris/platform/scheduler/`
**Common time utilities:** `common/arcus-common/src/main/java/com/iris/common/time/`, `common/arcus-common/src/main/java/com/iris/common/sunrise/`

### Guice Modules

| Module | Purpose |
|--------|---------|
| `SchedulerServiceModule` | Bootstrap entry point |
| `KafkaModule` | Event messaging |
| `CassandraPlaceDAOModule` | Place data access |
| `SchedulerDaoModule` | Scheduler/schedule persistence |
| `PlacePopulationCacheModule` | Population caching |
| `CassandraResourceBundleDAOModule` | Resource bundles |

---

## Capabilities

### Scheduler Capability (`scheduler` namespace)

Each scheduler is associated with a **target** (device or subsystem) and manages one or more schedules for that target.

**Attributes (all read-only):**

| Attribute | Type | Description |
|-----------|------|-------------|
| `placeId` | string | Associated place |
| `target` | string | Target device/subsystem address |
| `nextFireTime` | timestamp | Next scheduled execution across all schedules |
| `nextFireSchedule` | string | Schedule ID that fires next |
| `lastFireTime` | timestamp | Last execution timestamp |
| `lastFireSchedule` | string | Last executed schedule ID |
| `commands` | map | Map of available commands |

**Methods:**

| Method | Parameters | Description |
|--------|-----------|-------------|
| `FireCommand` | `commandId` | Fire a command immediately (for testing) |
| `AddWeeklySchedule` | `id`, `group?` | Create a new weekly schedule |
| `Delete` | — | Delete scheduler and all its schedules |
| `RecalculateSchedule` | — | Recalculate next fire time |

### Schedule Capability (`sched` namespace)

Each schedule belongs to a scheduler and contains the actual timed commands.

**Attributes:**

| Attribute | Type | Access | Description |
|-----------|------|--------|-------------|
| `group` | string | read-only | Scheduling group (mutual exclusion) |
| `enabled` | boolean | read-write | Enable/disable this schedule |
| `nextFireTime` | timestamp | read-only | Next fire time for this schedule |
| `nextFireCommand` | string | read-only | Next command to execute |
| `lastFireTime` | timestamp | read-only | Last execution |
| `lastFireCommand` | string | read-only | Last command executed |
| `lastFireMessageType` | string | read-only | Type of message sent |
| `lastFireAttributes` | map | read-only | Command attributes sent |

**Methods:**

| Method | Parameters | Description |
|--------|-----------|-------------|
| `Delete` | — | Delete this schedule |
| `DeleteCommand` | `commandId` | Delete a specific command |

---

## Schedule Model

### TimeOfDayCommand

The primary scheduled command type. Each command defines when and what to fire:

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique command ID (UUID) |
| `scheduleId` | string | Parent schedule ID |
| `mode` | enum | `ABSOLUTE`, `SUNRISE`, or `SUNSET` |
| `time` | string | `HH:MM` format (set directly for ABSOLUTE, calculated for SUNRISE/SUNSET) |
| `offsetMinutes` | int | Minutes offset from sunrise/sunset (0 for ABSOLUTE) |
| `days` | set | Days of week: `MON`, `TUE`, `WED`, `THU`, `FRI`, `SAT`, `SUN` |
| `messageType` | string | Message type to send (e.g., `base:SetAttributes`) |
| `attributes` | map | Command parameters / attribute values |

### Weekly Schedule

Extends Schedule with day-based organization:

- Commands stored per day (`mon`, `tue`, `wed`, `thu`, `fri`, `sat`, `sun`)
- Commands sorted by time within each day
- Maximum 8-day lookahead when calculating next fire time

---

## Execution Pipeline

### 1. Command Scheduling

```
ScheduleWeeklyCommand request arrives
  → TimeOfDayScheduledCommand validation
  → WeeklyScheduleRequestHandler.addTimeOfDayCommand()
  → Command added to appropriate day lists
  → Relative times calculated if SUNRISE/SUNSET mode
  → commit() saves model to Cassandra
  → syncNextFireTime() calculates next fire
  → EventSchedulerService.fireEventAt() schedules in time bucket
```

### 2. Next Fire Calculation

`syncNextFireTime(currentTime)`:

1. If schedule disabled → clear `nextFireTime` and `nextFireCommand`
2. Start from current day in the place's timezone
3. For each day (up to 8 days forward):
   - Get commands for this day of week
   - Calculate relative times if needed (sunrise/sunset)
   - Sort commands by time
   - Find first command where time > current time
   - If found: set `nextFireTime` and `nextFireCommand`, return
4. If no future command found → clear fire time attributes

### 3. Event Execution

When the scheduled time arrives:

1. `PartitionSchedulerJob` processes the time bucket for its partition
2. `EventSchedulerJob` loads commands within the bucket
3. At scheduled time: dispatch fires `ScheduledEvent`
4. `SchedulerCapabilityService` listener receives the event
5. Retrieves command details from scheduler model
6. Fires target command via platform message bus
7. Updates `lastFireTime` and `lastFireCommand`
8. Calls `handler.fire()` to calculate next execution
9. Recalculates `nextFireTime`
10. Reschedules if another execution is pending

### 4. Persistence and Commit

`SchedulerCapabilityDispatcher.commit()`:

1. Save scheduler model to `CassandraSchedulerModelDao`
2. If `nextFireTime` changed:
   - If null → `schedulerService.cancelEvent()` to unschedule
   - If new → `schedulerService.fireEventAt()` to schedule
   - If modified → `schedulerService.rescheduleEventAt()`
3. Broadcast `EVENT_VALUE_CHANGE` or `EVENT_ADDED` to platform bus

---

## Time Bucketing and Partitions

Scheduled events are organized into **time buckets** for efficient execution across service instances.

### Architecture

- Default bucket size: **60 seconds** (configurable: `scheduler.windowSizeSec`)
- Scheduling horizon: **600 seconds** / 10 minutes (configurable: `scheduler.horizonSec`)
- Only commands within the horizon are loaded into memory
- Past-due commands execute immediately
- Partitions assigned to instances based on place partitioning

### Partition Offset

Each partition tracks its progress through time:

```java
class PartitionOffset {
    int partitionId;
    Date timeBucket;        // Bucket-granularity timestamp
    long bucketSizeMs;      // Bucket duration

    Date getOffset();       // Current offset time
    Date getNextOffset();   // Next bucket boundary
}
```

### ScheduleDao Operations

```java
ScheduledCommand schedule(placeId, schedulerAddress, scheduledTime, validForMs)
ScheduledCommand reschedule(command, newFireTime, validForMs)
void unschedule(placeId, schedulerAddress, scheduledTime)
Stream<ScheduledCommand> streamByPartitionOffset(offset)
PartitionOffset completeOffset(offset)    // Mark bucket as processed
```

### Expiration

- Scheduled commands stored with `expiresAt` timestamp
- Default expiration: 24 hours (`scheduler.defaultExpirationTimeSec`)
- Expired commands dropped without execution (metric incremented)
- Prevents stale commands from firing after long outages

---

## Sunrise/Sunset and Timezone Support

### Timezone Handling

- Timezone sourced from the Place model (`tzId`, `tzName`, `tzOffset`, `tzUsesDst`)
- Applied to all schedule calculations
- Place timezone change triggers recalculation of all enabled schedules
- Falls back to server default timezone if place timezone missing

### Sunrise/Sunset Calculations

`SunriseSunsetCalc` interface (`common/arcus-common/src/main/java/com/iris/common/sunrise/`):

```java
SunriseSunsetInfo calculateSunriseSunset(Calendar day, GeoLocation location, ZENITH zenith)
```

**ZENITH options:**

| Zenith | Angle | Description |
|--------|-------|-------------|
| `OFFICIAL` | 90° 50' | Default — standard sunrise/sunset |
| `CIVIL` | 96° | Civil twilight |
| `NAUTICAL` | 102° | Nautical twilight |
| `ASTRONOMICAL` | 108° | Astronomical twilight |

- Requires `GeoLocation` (latitude/longitude) from Place model
- Validation error if location missing for SUNRISE/SUNSET mode commands
- Relative times recalculated daily — `time` attribute updated to the calculated absolute time

### TimeOfDay Utilities

`TimeOfDay` class (`common/arcus-common/src/main/java/com/iris/common/time/TimeOfDay.java`):

```java
static TimeOfDay fromString("HH:MM:SS")
Calendar next(Calendar from)    // Next occurrence of this time
Calendar on(Calendar day)       // This time on given day
```

`DayOfWeek` enum (`common/arcus-common/src/main/java/com/iris/common/time/DayOfWeek.java`):

```java
enum DayOfWeek { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }
static DayOfWeek fromAbbr("MON")    // Parse abbreviation
static int toCalendar(DayOfWeek)    // Convert to java.util.Calendar constant
```

---

## Schedule Lifecycle

### Creation

```
AddWeeklySchedule(id, group?)
  → Creates empty schedule with day lists
  → Schedule initially disabled
  → User must add commands and enable
```

### Adding Commands

```
ScheduleWeeklyCommand(scheduleId, days, time/mode, messageType, attributes)
  → Validates required fields
  → For ABSOLUTE: time required, offsetMinutes must be 0
  → For SUNRISE/SUNSET: offsetMinutes required, time must be null
  → Adds command to each specified day
  → Calculates next fire time
```

### Enabling/Disabling

- Enabling recalculates `nextFireTime` and schedules the next event
- Disabling clears `nextFireTime` and cancels the pending event
- Group mutual exclusion: enabling one schedule disables others in the same group

### Modification

```
UpdateWeeklyCommand(commandId, changes)
  → Updates command attributes
  → Recalculates relative times if mode changed
  → Recalculates next fire time
```

### Deletion

```
DeleteCommand(commandId)
  → Removes command from all day lists
  → Recalculates next fire time

Delete() on schedule
  → Removes schedule and all commands
  → Cancels pending events

Delete() on scheduler
  → Removes scheduler, all schedules, all commands
  → Cancels all pending events
```

### Timezone/Location Changes

When the Place model's timezone or geolocation changes:
- All enabled schedules recalculated with new timezone
- Sunrise/sunset values updated if applicable
- Next fire times adjusted

---

## Grouping and Mutual Exclusion

Schedules can be organized into **groups**. Only one schedule per group can be enabled at a time.

- Group stored in schedule's `group` attribute (defaults to schedule ID)
- Enabling a schedule automatically disables other schedules in the same group
- Use case: weekday vs. weekend schedules for the same device — user can switch between them without conflicting commands

---

## Persistence

### Cassandra Tables

| Table | Partition Key | Purpose |
|-------|--------------|---------|
| `scheduler` | `id` | Scheduler model storage (attributes, target, schedules) |
| `scheduled_event` | `partitionId`, `timeBucket` | Commands awaiting execution, with automatic expiration |
| `scheduler_offset` | `partitionId` | Partition progress tracking (completed time buckets) |
| `scheduler_address_index` | address | Bidirectional address lookups |

### DAOs

**SchedulerModelDao:**

```java
List<ModelEntity> listByPlace(UUID placeId, boolean includeWeekdays)
ModelEntity findByAddress(Address address)
ModelEntity findOrCreateByTarget(UUID placeId, Address target)
ModelEntity save(ModelEntity entity)
void deleteByAddress(Address address)
void updateAttributes(Address address, Map<String, Object> attributes)
```

Implementation: `CassandraSchedulerModelDao`

**ScheduleDao:**

```java
long getTimeBucketDurationMs()
List<PartitionOffset> listPartitionOffsets()
ScheduledCommand schedule(placeId, address, time, validForMs)
ScheduledCommand reschedule(command, newFireTime, validForMs)
void unschedule(placeId, address, time)
Stream<ScheduledCommand> streamByPartitionOffset(offset)
PartitionOffset completeOffset(offset)
```

Implementation: `CassandraScheduleDao`

---

## Configuration

### SchedulerConfig

| Property | Default | Description |
|----------|---------|-------------|
| `scheduler.windowSizeSec` | `60` | Time bucket duration (seconds) |
| `scheduler.horizonSec` | `600` | Scheduling lookahead window (10 min) |
| `scheduler.schedulingThreadPoolSize` | `5` | Partition scheduler threads |
| `scheduler.dispatchThreadPoolSize` | `40` | Command dispatch threads |
| `scheduler.defaultExpirationTimeSec` | `86400` | Command expiration (24 hours) |
| `scheduler.sanity.check` | `false` | Check for past-due events on startup |

### SchedulerServiceConfig

| Property | Default | Description |
|----------|---------|-------------|
| `platform.service.threads.max` | `20` | Listener threads |
| `platform.service.threads.keepAliveMs` | `10000` | Thread pool keepalive |

### Health Check

- TCP health check on port `9005`

---

## Metrics

Published via Codahale/Metrics to the metrics topic:

| Metric | Type | Description |
|--------|------|-------------|
| `scheduler.partition.scheduled` | Counter | Partitions scheduled for processing |
| `scheduler.partition.completed` | Counter | Partitions fully processed |
| `scheduler.partition.errors` | Counter | Partition processing errors |
| `scheduler.partition.schedule.time` | Timer | Time to schedule a partition |
| `scheduler.command.scheduled` | Counter | Commands loaded into memory |
| `scheduler.command.sent` | Counter | Commands successfully executed |
| `scheduler.command.expired` | Counter | Expired commands dropped |
| `scheduler.command.error` | Counter | Command dispatch errors |
| `scheduler.command.rescheduled` | Counter | Past-due commands rescheduled |
| `scheduler.partition.count` | Gauge | Active partition count |
| `scheduler.partition.pending` | Gauge | Pending partition jobs |

---

## Key Files

### Service Container (`platform/arcus-containers/scheduler-service/`)

| File | Description |
|------|-------------|
| `.../SchedulerServiceModule.java` | Bootstrap module |
| `.../SchedulerCapabilityService.java` | Main service — routes requests, dispatches events |
| `.../SchedulerCapabilityDispatcher.java` | Per-scheduler dispatcher (single-threaded) |
| `.../handlers/SchedulerRequestHandler.java` | Core request handling (fire, add, delete) |
| `.../handlers/WeeklyScheduleRequestHandler.java` | Weekly schedule operations (add/update/delete commands, calculate next fire) |
| `.../SchedulerServiceConfig.java` | Service thread pool configuration |
| `src/dist/conf/scheduler-service.properties` | Service properties |

### Platform Library (`platform/arcus-lib/.../scheduler/`)

| File | Description |
|------|-------------|
| `.../SchedulerConfig.java` | Time bucketing and horizon configuration |
| `.../SchedulerModelDao.java` | Scheduler model persistence interface |
| `.../ScheduleDao.java` | Scheduled event persistence interface |
| `.../cassandra/CassandraSchedulerModelDao.java` | Cassandra model DAO |
| `.../cassandra/CassandraScheduleDao.java` | Cassandra event DAO |
| `.../SchedulerMetrics.java` | Metrics collection |
| `.../PartitionSchedulerJob.java` | Partition-level scheduling job |
| `.../EventSchedulerJob.java` | Event-level scheduling job |
| `.../PlatformEventSchedulerService.java` | Time-based event execution |

### Common Utilities

| File | Description |
|------|-------------|
| `common/arcus-common/.../time/TimeOfDay.java` | Immutable time-of-day representation |
| `common/arcus-common/.../time/DayOfWeek.java` | Day-of-week enum with Calendar conversion |
| `common/arcus-common/.../sunrise/SunriseSunsetCalc.java` | Sunrise/sunset calculation interface |
| `common/arcus-common/.../sunrise/GeoLocation.java` | Latitude/longitude container |

### Capability Definitions

| File | Description |
|------|-------------|
| `common/arcus-model/src/main/resources/capability/scheduler.xml` | Scheduler capability definition |
| `common/arcus-model/src/main/resources/capability/schedule.xml` | Schedule capability definition |
