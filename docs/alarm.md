# Alarm State Machine

## Architecture

The alarm system is split across two layers:

- **Hub-side** (`agent/arcus-alarm-controller/`): Runs locally on the hub, processes sensor events in real time via the reflex protocol, controls siren/LED/sounder hardware. The hub is authoritative for per-alarm state.
- **Platform-side** (`platform/arcus-subsystems/.../alarm/`): Mirrors hub state, creates and manages alarm incidents, handles professional monitoring dispatch, user notifications, and automated shutoffs (fans, water valves).

The hub maintains five independent alarm instances that run in parallel:

| Alarm | Hub Class | Trigger Source |
|-------|-----------|---------------|
| Security | `AlarmSecurity` | Contact sensors, motion sensors, glass break, motorized doors |
| Panic | `AlarmPanic` | Keypad panic button, platform panic request |
| Smoke | `AlarmSmoke` | `SmokeCapability.ATTR_SMOKE = DETECTED` |
| CO | `AlarmCo` | `CarbonMonoxideCapability.ATTR_CO = DETECTED` |
| Water | `AlarmWater` | `LeakH2OCapability.ATTR_STATE = LEAK` |

## States

All alarms share this state enum, though not all alarms use all states:

| State | Description | Used By |
|-------|-------------|---------|
| `INACTIVE` | No devices present for this alarm type | All |
| `DISARMED` | Devices present but not armed | Security only |
| `ARMING` | Exit delay countdown | Security only |
| `READY` | Armed and monitoring / safety alarm quiescent | All |
| `PREALERT` | Entry delay countdown | Security only |
| `ALERT` | Alarm firing | All |
| `PENDING_CLEAR` | Disarmed, waiting for platform to acknowledge | All |
| `CLEARING` | Triggers gone, waiting for hardware to clear | Safety only |

### Top-Level Alarm State

The hub computes an overall `alarmState` from the highest-priority active alarm:

| Priority | Per-Alarm State | Overall State |
|----------|----------------|---------------|
| 0–3 | INACTIVE, DISARMED, ARMING, READY | `READY` |
| 4–5 | PENDING_CLEAR, CLEARING | `CLEARING` |
| 6 | PREALERT | `PREALERT` |
| 7 | ALERT | `ALERTING` |

## Security Alarm State Transitions

```
INACTIVE ──(devices added)──────────────────────────── DISARMED
DISARMED ──(ArmRequest)─────────────────────────────── ARMING
ARMING ────(exit delay timer fires)─────────────────── READY
ARMING ────(DisarmRequest)──────────────────────────── DISARMED
READY ─────(contact/glass/door trigger)─────────────── PREALERT
READY ─────(motion count >= sensitivity threshold)──── PREALERT
READY ─────(DisarmRequest)──────────────────────────── DISARMED
PREALERT ──(entry delay timer fires)────────────────── ALERT
PREALERT ──(VerifiedEvent from monitoring)──────────── ALERT (immediate)
PREALERT ──(DisarmRequest)──────────────────────────── PENDING_CLEAR
ALERT ─────(DisarmRequest)──────────────────────────── PENDING_CLEAR
PENDING_CLEAR ─(ClearIncidentRequest from platform)─── DISARMED
```

During the ARMING exit delay, any triggered devices are added to `excludedDevices` rather than causing a false alarm.

## Safety Alarm State Transitions (Smoke, CO, Water, Panic)

Safety alarms have no arming or pre-alert phases:

```
INACTIVE ──(devices added, not triggered)───── READY
INACTIVE ──(devices added, already triggered)── ALERT
READY ─────(trigger event)──────────────────── ALERT
ALERT ─────(DisarmRequest)──────────────────── PENDING_CLEAR
PENDING_CLEAR ─(ClearEvent from platform)───── CLEARING
PENDING_CLEAR ─(new trigger)────────────────── ALERT
CLEARING ──(all triggered devices clear)────── READY
CLEARING ──(new trigger)────────────────────── ALERT
```

## Arm Modes: ON vs PARTIAL

Each mode has its own configuration stored in `SecurityAlarmModeModel`:

| Setting | Description |
|---------|-------------|
| `devices` | Set of device addresses included in this mode |
| `exitDelaySec` | Exit delay in seconds (default 30) |
| `entranceDelaySec` | Entry delay in seconds (default 30) |
| `alarmSensitivityDeviceCount` | Motion sensors required to trigger (default 1) |
| `soundsEnabled` | Whether arming/disarming chimes play |
| `silent` | Whether the siren is silenced |

**ON** includes all configured security devices. **PARTIAL** typically includes only perimeter devices (contacts, glass break) but not interior motion sensors.

### Bypassed Devices

Devices that are triggered or offline at arm time can be bypassed (`ArmBypassed` / `bypassed=true`). Bypassed devices go into `excludedDevices` and their triggers are ignored while armed. When a bypassed device returns to a normal state, it is automatically removed from `excludedDevices`.

## Grace Periods

### Exit Delay (Arming → Ready)

When arming begins, the hub schedules a timer for `exitDelaySec`. During this window:
- The `ARMING_GRACE_EXIT` sound plays
- Keypads receive `BeginArmingRequest(mode, delaySec)`
- Triggered devices during exit are added to `excludedDevices` (not treated as alarms)

### Entry Delay (Pre-Alert → Alert)

When a security trigger fires while armed, the hub enters PREALERT and schedules a timer for `entranceDelaySec`. During this window:
- The `ARMING_GRACE_ENTER` sound plays
- Keypads receive `SoakingRequest(mode, durationSec)`
- The user can disarm to cancel the alarm

A **verified alarm** (from professional monitoring) skips the entry delay and immediately transitions to ALERT.

If the hub is offline, the platform adds a `hubOfflinePrealertBuffer` (default 60s) on top of the entry delay. If the hub doesn't transition within that time, the platform forces the incident to ALERT autonomously.

## Trigger Types

### Security

| Trigger | Capability | Condition |
|---------|-----------|-----------|
| `CONTACT` | `ContactCapability.ATTR_CONTACT` | `OPENED` |
| `MOTION` | `MotionCapability.ATTR_MOTION` | `DETECTED` |
| `GLASS` | `GlassCapability.ATTR_BREAK` | `DETECTED` |
| `DOOR` | `MotorizedDoorCapability.ATTR_DOORSTATE` | `OPEN`, `OPENING`, or `OBSTRUCTION` |
| `PANIC` | `KeyPadCapability.PanicPressedEvent` | Keypad event |

Motion triggers use a sensitivity threshold: the alarm only fires when the count of distinct triggered motion devices reaches `alarmSensitivityDeviceCount`.

### Safety

| Alarm | Capability | Condition |
|-------|-----------|-----------|
| Smoke | `SmokeCapability.ATTR_SMOKE` | `DETECTED` |
| CO | `CarbonMonoxideCapability.ATTR_CO` | `DETECTED` |
| Water | `LeakH2OCapability.ATTR_STATE` | `LEAK` |
| Panic | Keypad `PanicPressedEvent`, platform `PanicRequest`, or rules | — |

## Side Effects on Alert

| Effect | Alarm Types | Implementation |
|--------|-------------|----------------|
| Siren activation | All | `AlertCapability.STATE_ALERTING` sent to siren devices |
| Hub sounder | All | `HubSoundsCapability.PlayToneRequest` |
| Camera recording | Security, Panic | `RecordOnSecurityAdapter` |
| Fan shutoff | Smoke, CO | `FanShutoffAdapter` (if enabled) |
| Water valve shutoff | Water | `AlarmUtil.shutoffValvesIfNeeded()` (if enabled) |

### Siren Priority

When multiple alarms are active, the highest-priority alarm controls the siren sound:

| Priority | Alarm | Triggered Sound | Monitored Sound |
|----------|-------|-----------------|-----------------|
| 1 (highest) | Smoke | `SMOKE_ALARM_TRIGGERED` | `SMOKE_TRIGGERED_MONITORING_NOTIFIED` |
| 2 | CO | `CO_TRIGGERED` | `CO_TRIGGERED_MONITORING_NOTIFIED` |
| 3 | Panic | `PANIC_ALARM` | `PANIC_TRIGGERED_MONITORING_NOTIFIED` |
| 4 | Security | `SECURITY_ALARM_TRIGGERED` | `SECURITY_TRIGGERED_MONITORING_NOTIFIED` |
| 6 (lowest) | Water | `WATER_LEAK_DETECTED` | same |

## Alarm Incidents

### Lifecycle

```
(security trigger) → addPreAlert() → incident created in PREALERT
                   → addAlert()    → incident moves to ALERT
                   → cancel()      → CLEARING (user disarmed)
                   → complete      → ClearIncidentRequest sent to hub
```

Safety alarms skip the pre-alert phase and go directly to `addAlert()`.

Each incident tracks:
- **Triggers**: list of `(alarm, event, source, time)` entries
- **Monitoring state**: `NONE` → `PENDING` → `DISPATCHING` → `DISPATCHED` (or `CANCELLED`/`REFUSED`/`FAILED`)
- **Alert state**, **platform state**, **hub state**

### Professional Monitoring

Monitored alarm types (default): Security, Panic, Smoke, CO. Water is **not** monitored.

When a monitored alarm enters ALERT, the incident is sent to the monitoring center (UCC). The monitoring state progresses through `PENDING` → `DISPATCHING` → `DISPATCHED`. If the user cancels before dispatch completes, the cancel warning message varies by alarm type and monitoring state (managed by `CancelAlarmMessageTable`).

## Hub-Platform Coordination

### Normal Flow

1. Platform sends `HubAlarmCapability.ArmRequest` to hub (device list, delays, sensitivity, mode)
2. Hub processes arming, responds with `ArmResponse`
3. Hub sends periodic `base:Report` with all `hubalarm:*` attributes
4. Platform's `HubAlarmSubsystem.onReport()` translates attributes (e.g., `hubalarm:securityAlertState` → `alarm:alertState:SECURITY`) and protocol addresses to platform addresses, then syncs state

### Hub Reconnect

When the hub reconnects after being offline:
- If platform already cancelled the incident while hub was offline, platform sends `DisarmRequest` to the hub
- If hub is in `PENDING_CLEAR` and platform has already cleared, platform sends `ClearIncidentRequest`
- Missing incident triggers are replayed via `replayIfNecessary()`

## Keypad Integration

Keypads are first-class actors in the alarm flow:

| Keypad Event | Action |
|-------------|--------|
| `ArmPressedEvent` | Arms in the requested mode |
| `DisarmPressedEvent` | Disarms (validates PIN via reflex) |
| `PanicPressedEvent` | Triggers panic alarm immediately |

Platform sends state updates to keypads:

| State | Keypad Command |
|-------|---------------|
| Arming | `BeginArmingRequest(mode, delaySec)` |
| Armed | `ArmedRequest(mode)` |
| Pre-alert | `SoakingRequest(mode, durationSec)` |
| Alert | `AlertingRequest(alarmMode)` |
| Disarmed | `DisarmedRequest()` |

## Alarm Type Comparison

| Aspect | Security | Panic | Smoke | CO | Water |
|--------|----------|-------|-------|----|-------|
| Has arming/disarming | Yes | No | No | No | No |
| Has entry/exit delay | Yes | No | No | No | No |
| Monitored (default) | Yes | Yes | Yes | Yes | No |
| Camera recording | Yes | Yes | No | No | No |
| Fan shutoff | No | No | Yes | Yes | No |
| Valve shutoff | No | No | No | No | Yes |
| Dispatch type | Police | Police | Fire | Fire | — |

## End-to-End Example: Security Alarm

1. User arms via app → `ArmRequest(mode=ON)`
2. Platform sends `ArmRequest` to hub with device list and delays
3. Hub: `DISARMED → ARMING` — exit delay sound plays, keypad shows arming
4. Exit delay expires → Hub: `ARMING → READY` — keypad shows armed
5. Contact sensor opens → Hub: `READY → PREALERT` — entry delay sound plays
6. Entry delay expires → Hub: `PREALERT → ALERT` — siren fires, cameras record
7. Platform creates incident, notifies monitoring center
8. User enters PIN on keypad → Hub: `ALERT → PENDING_CLEAR`
9. Platform cancels incident, sends `ClearIncidentRequest` to hub
10. Hub: `PENDING_CLEAR → DISARMED` — siren silenced, keypad shows disarmed

## Key Files

### Hub Side

| File | Purpose |
|------|---------|
| `agent/arcus-alarm-controller/.../AlarmController.java` | Central coordinator, routes events, drives siren/LED |
| `agent/arcus-alarm-controller/.../AbstractAlarm.java` | Base state machine with all transitions |
| `agent/arcus-alarm-controller/.../AlarmSecurity.java` | Security alarm (full state set) |
| `agent/arcus-alarm-controller/.../AbstractSafetyAlarm.java` | Base for Smoke, CO, Water, Panic |
| `agent/arcus-alarm-controller/.../AlarmEvents.java` | Event classes and trigger enum |
| `agent/arcus-alarm-controller/.../sounds/AlarmSoundConfig.java` | Alarm type × state → sounder mode |

### Platform Side

| File | Purpose |
|------|---------|
| `platform/arcus-subsystems/.../alarm/HubAlarmSubsystem.java` | Main subsystem, syncs hub state, manages incidents |
| `platform/arcus-subsystems/.../alarm/security/SecurityAlarm.java` | Platform security state machine |
| `platform/arcus-subsystems/.../alarm/security/Security*State.java` | Individual security states |
| `platform/arcus-subsystems/.../alarm/generic/AlarmStateMachine.java` | Generic alarm state machine base |
| `platform/arcus-subsystems/.../alarm/subs/AlarmSubsystemState.java` | Top-level subsystem state (drives sirens/keypads) |
| `platform/arcus-subsystems/.../alarm/incident/AlarmIncidentService.java` | Incident lifecycle |
| `platform/arcus-subsystems/.../alarm/CancelAlarmMessageTable.java` | Cancel warning message decision table |
| `platform/arcus-subsystems/.../alarm/KeyPad.java` | Keypad command dispatch |
| `platform/arcus-subsystems/.../alarm/FanShutoffAdapter.java` | Fan shutoff on smoke/CO |
| `platform/arcus-subsystems/.../alarm/RecordOnSecurityAdapter.java` | Camera recording on security/panic |
