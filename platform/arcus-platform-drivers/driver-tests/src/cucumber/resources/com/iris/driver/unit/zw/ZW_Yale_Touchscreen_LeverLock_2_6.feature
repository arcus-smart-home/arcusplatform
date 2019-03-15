@ZWave @Yale @lock @YRL220
Feature: ZWave Yale Touchscreen Levellock Driver Test

    These scenarios test the functionality of the ZWave Yale Touchscreen Levellock driver.

    Background:
        Given the ZW_Yale_Touchscreen_LeverLock_2_6.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform. 
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'doorlock']
            And the message's dev:devtypehint attribute should be Lock
            And the message's devadv:drivername attribute should be ZWYaleTouchScreenLeverLock 
            And the message's devadv:driverversion attribute should be 2.6
            And the message's devpow:source attribute should be BATTERY
            And the message's devpow:linecapable attribute should be false
            And the message's devpow:backupbatterycapable attribute should be false
            And the message's doorlock:type attribute should be LEVERLOCK
        Then both busses should be empty


############################################################
# Generic Driver Tests
############################################################

    @basic @added
    Scenario: Make sure any "time of change" attributes are defaulted when the device is first Added
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the capability doorlock:lockstatechanged should be recent

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
# Generic ZWave Door Lock Tests
############################################################

    Scenario Outline: Door Lock reports alarm state of bolt extended
        Given the capability doorlock:lockstate is <prev-state>
            And the capability doorlock:slots is { '0':'1234','1':'2345','2':'RESERVED' }
        When the device response with alarm report 
            And with parameter alarmtype <type>
            And with parameter alarmlevel <level>
            And send to driver
        Then the platform attribute doorlock:lockstate should change to <state>
            And the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty

        Examples:
          | prev-state | type | level | state    | remarks                                 |
          | UNLOCKED   | 0x15 | -1    | LOCKED   | Manual Lock                             |
          | UNLOCKED   | 0x18 | -1    | LOCKED   | ZWave Command to Operate Lock           |
          | UNLOCKED   | 0x1B | -1    | LOCKED   | Auto Lock Operate Locked                |
          | LOCKED     | 0x16 | -1    | UNLOCKED | Manual Unlock                           |
          | LOCKED     | 0x19 | -1    | UNLOCKED | ZWave Command to Operate Unlock         |

    Scenario: Door Lock reports unlocked from keypad with user
        Given the capability doorlock:lockstate is LOCKED
            And the capability doorlock:slots is { '0':'1234','1':'2345','2':'RESERVED' }
        When the device response with alarm report 
            And with parameter alarmtype 0x13
            And with parameter alarmlevel 1
            And send to driver
        Then the platform attribute doorlock:lockstate should change to UNLOCKED
            And the driver should place a doorlock:PinUsed message on the platform bus
            And the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty

    Scenario: Door Lock reports locked from keypad
        Given the capability doorlock:lockstate is UNLOCKED
            And the capability doorlock:slots is { '0':'1234','1':'2345','2':'RESERVED' }
        When the device response with alarm report 
            And with parameter alarmtype 0x12
            And with parameter alarmlevel -1
            And send to driver
        Then the platform attribute doorlock:lockstate should change to LOCKED
            And the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty

    Scenario: Door Lock reports alarm state of bolt jammed while locking
        Given the capability doorlock:lockstate is LOCKING
        When the device response with alarm report 
            And with parameter alarmtype 9
            And with parameter alarmlevel 0
            And send to driver
        Then the platform attribute doorlock:lockstate should be LOCKING
            And the capability devadv:errors should be [WARN_JAM:Door lock may be jammed]

    Scenario: Door Lock reports alarm state of bolt jammed while unlocking
        Given the capability doorlock:lockstate is UNLOCKING
        When the device response with alarm report 
            And with parameter alarmtype 9
            And with parameter alarmlevel 0
            And send to driver
        Then the platform attribute doorlock:lockstate should be UNLOCKING
            And the capability devadv:errors should be [WARN_JAM:Door lock may be jammed]

    Scenario Outline: Door Lock reports operation state
        When the device response with door_lock operation_report 
            And with parameter doorlockmode <doorlockmode>
            And send to driver
        Then the platform attribute doorlock:lockstate should change to <state>
            And the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty

        Examples:
          | doorlockmode  | state    | remarks                                             |
          | -1            | LOCKED   |                                                     |
          |  0            | UNLOCKED | door unsecured                                      |
          |  1            | UNLOCKED | door unsecured with timeout                         |
          | 16            | LOCKED   | door unsecured for inside door handle               |
          | 17            | LOCKED   | door unsecured for inside door handle with timeout  |
          | 32            | UNLOCKED | door unsecured for outside door handle              |
          | 33            | UNLOCKED | door unsecured for outside door handle with timeout |
