@ZWave @Schlage @lock @BE468
Feature: ZWave Schlage Keypad Deadbolt BE468 Driver Test

    These scenarios test the functionality of the ZWave Schlage Keypad Deadbolt driver.
    
    Background:
        Given the ZW_Schlage_Touch_Deadbolt468_1_1.driver has been initialized
    
    @basic
    Scenario: Driver reports capabilities to platform. 
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'doorlock']
            And the message's dev:devtypehint attribute should be Lock
            And the message's devadv:drivername attribute should be ZWaveSchlageTouchDeadbolt468Driver 
            And the message's devadv:driverversion attribute should be 1.1
            And the message's devpow:source attribute should be BATTERY
            And the message's devpow:linecapable attribute should be false
            And the message's devpow:backupbatterycapable attribute should be false
            And the message's doorlock:type attribute should be DEADBOLT
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
        When the device response with alarm report 
            And with parameter notificationType 6
            And with parameter event <type>
            # we don't have slots setup for testing so set parameter1 to -1
            And with parameter parameter1 -1
            And send to driver
        Then the platform attribute doorlock:lockstate should change to <state>
            And the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty

        Examples:
          | prev-state | type | state    | remarks                                 |
          | UNLOCKED   |   1  | LOCKED   | Locked from inside or outside by key    |
          | UNLOCKED   |   5  | LOCKED   | Locked from outside                     |
          | UNLOCKED   |   9  | LOCKED   | Auto Relocked                           |
          | LOCKED     |   2  | UNLOCKED | Unlocked from inside or outside by key  |
          | LOCKED     |   6  | UNLOCKED | Unlocked from outside                   |

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
