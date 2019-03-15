@ZWave @lock @DoorLock @Kwikset @910 @plus
Feature: ZWave Stanley Black & Decker Deadbolt Driver Test

    These scenarios test the functionality of the Z-Wave Stanley Black & Decker (Kwikset) 910 5-Button Deadbolt, 500 Series

    Background:
        Given the ZW_SBD_Deadbolt910_500series_2_7.driver has been initialized

    Scenario: Driver reports capabilities to platform. 
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'doorlock']
            And the message's dev:devtypehint attribute should be Lock
            And the message's devadv:drivername attribute should be ZWSBDDeadbolt910ZwavePlus 
            And the message's devadv:driverversion attribute should be 2.7
            And the message's devpow:source attribute should be BATTERY
            And the message's devpow:linecapable attribute should be false
            And the message's devpow:backupbatterycapable attribute should be false
            And the message's doorlock:type attribute should be DEADBOLT
            And the message's doorlock:supportsBuzzIn attribute should be true
            And the message's doorlock:supportsInvalidPin attribute should be true
            And the message's doorlock:numPinsSupported attribute should be 30
            And the capability devadv:errors should be [:]
        Then both busses should be empty


############################################################
# Generic Driver Tests
############################################################

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set 
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Deadbolt                 |
          | "Front Door"             |
          | "Tom's Door"             |
          | "Bob & Sue's Door"       |


############################################################
# Generic ZWave Driver Tests
############################################################

    Scenario: Make sure driver handles ZWave Plus Info Reports
        When the device response with zwaveplus_info report
            And with parameter zwaveversion 5
            And with parameter roletype 6
            And with parameter nodetype 2
            And send to driver
        Then protocol bus should be empty

    Scenario: Make sure driver handles Device Reset Locally Notification
        When the device response with device_reset_locally notification
            And send to driver
        Then protocol bus should be empty

    Scenario Outline: Device reports battery level
        Given the capability devpow:battery is <prev_level>
        When the device response with battery report 
            And with parameter level <level-arg>
            And send to driver 
        Then the platform attribute devpow:battery should change to <battery-attr>
            And the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty

        Examples:
          | prev_level | level-arg | battery-attr |
          |  50        |  -1       |   0          |
          |  50        |   0       |   0          |
          |  50        |   1       |   1          |
          |  70        |  50       |  50          |
          | 100        |  99       |  99          |
          | 100        | 100       | 100          |


############################################################
# ZWave Doorlock Life-Cycle Tests
############################################################

    @basic @added
    Scenario: Make sure any "time of change" attributes are defaulted when the device is first Added
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the capability doorlock:lockstatechanged should be recent

    @basic @connected @timeout
    Scenario: Make sure the offline timeout is set each time the device Connects
        When the device is connected
        Then the driver should set timeout at 735 minutes
        Then the driver should send battery get
        Then the driver should poll battery.get every 24 hours
        Then the driver should send door_lock operation_get
        Then the driver should send door_lock.operation_get every 4 hours


############################################################
# ZWave Doorlock Tests
############################################################

    Scenario Outline: Door Lock reports alarm state of bolt extended
        Given the capability doorlock:lockstate is UNLOCKED
            And the capability devadv:errors is {"WARN_JAM":"Door lock may be jammed"}
         When the device response with alarm report 
            And with parameter alarmtype <alarmType>
            And send to driver
        Then the platform attribute doorlock:lockstate should change to <state>
            And the capability devadv:errors should be [:]
            And the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty

        Examples:
        | alarmType | state    | remarks        |
        |  0x12     | LOCKED   | Locked, Keypad |
        |  0x15     | LOCKED   | Locked, Manual |
        |  0x1B     | LOCKED   | AutoLocked     |
        |  0x18     | LOCKED   | Locked, ZWave  |
       
    Scenario Outline: Door Lock reports alarm state of bolt retracted
        Given the capability doorlock:lockstate is LOCKED
            And the capability devadv:errors is {"WARN_JAM":"Door lock may be jammed"}
        When the device response with alarm report 
            And with parameter alarmtype <alarmType>
            And send to driver
        Then the platform attribute doorlock:lockstate should change to <state>
            And the capability devadv:errors should be [:]
            And the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty

        Examples:
        | alarmType | state    | remarks          |
        |  0x16     | UNLOCKED | Unlocked, ZWave  |
        |  0x19     | UNLOCKED | Unlocked, ZWave  |
        
    Scenario Outline: Door Lock reports alarm state of bolt retracted from Keypad
        Given the capability doorlock:lockstate is LOCKED
            And the capability devadv:errors is {"WARN_JAM":"Door lock may be jammed"}
        When the device response with alarm report 
            And with parameter alarmtype 0x13
            And send to driver
        Then the platform attribute doorlock:lockstate should change to UNLOCKED
            And the capability devadv:errors should be [:]
            And the driver should place a doorlock:PinUsed message on the platform bus
            And the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty


    Scenario Outline: Door Lock reports operation states
        Given the capability doorlock:lockstate is <prev-state>
        When the device response with door_lock operation_report 
            And with parameter doorlockmode <doorlockmode>
            And send to driver
        Then the platform attribute doorlock:lockstate should change to <state>
            And the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty

        Examples:
        | doorlockmode     | prev-state | state    |
        | -1               | UNLOCKED   | LOCKED   |
        |  0               | LOCKED     | UNLOCKED |
        |  1               | LOCKED     | UNLOCKED |
        | 16               | UNLOCKED   | LOCKED   |
        | 17               | UNLOCKED   | LOCKED   |
        | 32               | LOCKED     | UNLOCKED |
        | 33               | LOCKED     | UNLOCKED |

    @jam
    Scenario Outline: Door Lock reports alarm state of bolt jammed while locking
        Given the capability doorlock:lockstate is LOCKING
        When the device response with alarm report 
            And with parameter notificationType 6
            And with parameter alarmtype <alarmType>
            And send to driver
        #don't change the lockstate on a jam
        Then the platform attribute doorlock:lockstate should be LOCKING
            And the driver should place a base:ValueChange message on the platform bus
            And the capability devadv:errors should be [WARN_JAM:Door lock may be jammed]
        Then both busses should be empty

        Examples:
        | alarmType | remarks       |
        |  0x1A     | Jam, Autolock |
        |  0x11     | Jam, Keypad   |
        |  0x17     | Jam, ZWave    |

    Scenario: Setting Unlocked device to Unlocked should not change state
        Given the capability doorlock:lockstate is UNLOCKED
            And the capability devadv:errors is {"WARN_JAM":"Door lock may be jammed"}
        When a base:SetAttributes command with the value of doorlock:lockstate UNLOCKED is placed on the platform bus
        Then the driver should place a EmptyMessage message on the platform bus
        Then the driver should send door_lock operation_set 
            And with parameter mode 0
        Then the capability doorlock:lockstate should be UNLOCKED
            # error should not clear until device reports back that it is unlocked
            And the capability devadv:errors should be [WARN_JAM:Door lock may be jammed]
        Then the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty
        
    Scenario: Setting locked device to locked should not change state
        Given the capability doorlock:lockstate is LOCKED
            And the capability devadv:errors is {"WARN_JAM":"Door lock may be jammed"}
        When a base:SetAttributes command with the value of doorlock:lockstate LOCKED is placed on the platform bus
        Then the driver should place a EmptyMessage message on the platform bus
        Then the driver should send door_lock operation_set 
            And with parameter mode -1
        Then the capability doorlock:lockstate should be LOCKED
            # error should not clear until device reports back that it is locked
            And the capability devadv:errors should be [WARN_JAM:Door lock may be jammed]
        Then the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty
