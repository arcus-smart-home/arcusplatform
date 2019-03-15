@ZWave @lock @DoorLock @Generic
Feature: Generic ZWave Doorlock Driver Test

    These scenarios test the functionality of the Generic ZWave Doorlock Driver

    Background:
        Given the ZW_ZZWaveGenericDoorlock_2_12.driver has been initialized

    Scenario: Driver reports capabilities to platform. 
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'doorlock']
            And the message's dev:devtypehint attribute should be Lock
            And the message's devadv:drivername attribute should be ZZWaveGenericDoorlock 
            And the message's devadv:driverversion attribute should be 2.12
            And the message's devpow:source attribute should be BATTERY
            And the message's devpow:linecapable attribute should be false
            And the message's devpow:backupbatterycapable attribute should be false
            And the message's doorlock:type attribute should be OTHER
            And the message's doorlock:supportsBuzzIn attribute should be false
            And the message's doorlock:supportsInvalidPin attribute should be false
            And the message's doorlock:numPinsSupported attribute should be 0
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
        Then the driver should send door_lock.operation_get every 4 hours
        Then the driver should poll battery.get every 24 hours


############################################################
# ZWave Doorlock Tests
############################################################

    Scenario Outline: Door Lock reports alarm state of Locked
        Given the capability doorlock:lockstate is UNLOCKED
         When the device response with alarm report 
            And with parameter notificationType 6
            And with parameter event <event>
            And send to driver
        Then the platform attribute doorlock:lockstate should change to LOCKED
            And the capability devadv:errors should be [:]
            And the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty

        Examples:
        | event | remarks        |
        |  0x01 | Locked, Manual |
        |  0x03 | Locked, ZWave  |
        |  0x05 | Locked, Keypad |

    Scenario Outline: Door Lock reports alarm state of Unlocked
        Given the capability doorlock:lockstate is LOCKED
        When the device response with alarm report 
            And with parameter notificationType 6
            And with parameter event <event>
            And send to driver
        Then the platform attribute doorlock:lockstate should change to UNLOCKED
            And the capability devadv:errors should be [:]
            And the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty

        Examples:
        | event | remarks          |
        |  0x02 | Unlocked, Manual |
        |  0x04 | Unlocked, ZWave  |
        |  0x06 | Unlocked, Keypad |
        

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


    Scenario: Setting Unlocked device to Unlocked should not change state
        Given the capability doorlock:lockstate is UNLOCKED
        When a base:SetAttributes command with the value of doorlock:lockstate UNLOCKED is placed on the platform bus
        Then the driver should place a EmptyMessage message on the platform bus
        Then the driver should send door_lock operation_set 
            And with parameter mode 0
        Then the capability doorlock:lockstate should be UNLOCKED
        
        
    Scenario: Setting locked device to locked should not change state
        Given the capability doorlock:lockstate is LOCKED
        When a base:SetAttributes command with the value of doorlock:lockstate LOCKED is placed on the platform bus
        Then the driver should place a EmptyMessage message on the platform bus
        Then the driver should send door_lock operation_set 
            And with parameter mode -1
        Then the capability doorlock:lockstate should be LOCKED
