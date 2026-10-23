# arcus-alarm

The alarm notification service and its supporting code. This used to be split
between this module (call tree DAO) and the standalone `alarm-service`
container; the container was merged into `subsystem-service`, which installs
`AlarmServiceModule` and hosts the service on the platform bus.

The alarm *state machine* (arming, triggering, incident lifecycle) lives in
`platform/arcus-subsystems` and the incident services in
`platform/arcus-containers/subsystem-service`. This module handles what happens
after an alert is raised:

* `AddAlarmHandler` — runs the notification strategy for a new alert
* `CancelAlertHandler` — stops notifications when an incident is cancelled;
  the incident service waits on this response before marking the platform
  side of an incident complete
* `IvrNotificationAcknowledgedHandler` — stops the sequential call tree when
  someone acknowledges an IVR call (sent by `ivr-callback-server`)
* `BasicNotificationStrategy` — notify the account owner only
* `PremiumNotificationStrategy` — smoke/CO/water notify the whole call tree in
  parallel; security/panic walk the call tree sequentially until acknowledged
