# arcus-alarm
Common code shared between the subsystem that runs the alarm state machine and the alarm service that manages the tracking of an alarm incident

# Notes

Despite it's name, this service doesn't seem to be critical to the alarm actually working. For example, you shouldn't have a problem with arming, disarming, and triggering the alarm when this service is not running. It appears that this logic is actually handled by the alarm subsystem, in platform/arcus-subsystems/. Perhaps this was intended for ProMonitoring?

# Messages handled

* AddAlarmHandler
* CancelAlertHandler
* IvrNotificationAcknowledgedHandler
