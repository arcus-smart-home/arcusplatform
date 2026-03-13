@ZWave @Aeotec @contact
Feature: ZWave Aeotec Door Window Sensor 6 Driver Test

These scenarios test the functionality of the ZWave Aeotec Door/Window Sensor 6 (ZW112-A) driver

    Background:
    Given the ZW_Aeotec_DoorWindowSensor6.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform.
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'cont']
        And the message's dev:devtypehint attribute should be Contact
        And the message's devadv:drivername attribute should be ZWAeotecDoorWindowSensor6Driver
        And the message's devadv:driverversion attribute should be 2.4
        And the message's devpow:source attribute should be BATTERY
        And the message's devpow:linecapable attribute should be false
        And the message's devpow:backupbatterycapable attribute should be false
        And the message's cont:usehint attribute should be UNKNOWN
    Then both busses should be empty


############################################################
# Generic Driver Tests
############################################################

    @basic @added
    Scenario: Make sure any "time of change" attributes are defaulted when the device is first Added
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the capability cont:contactchanged should be recent

    @basic @connected @timeout
    Scenario: Make sure the offline timeout is set each time the device Connects
        When the device is connected
        Then the driver should set timeout at 360 minutes

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Device                   |
          | "My Device"              |
          | "Tom's Door"             |
          | "Bob & Sue's Window"     |


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
          | prev_level | level-arg | battery-attr | remarks                            |
          |  50        |  -1       |   0          |                                    |
          |  50        |   0       |  50          | Driver assumes zero is invalid     |
          |  50        |   1       |   1          |                                    |
          |  70        |  50       |  50          |                                    |
          | 100        |  99       |  99          |                                    |
          | 100        | 100       | 100          |                                    |
          |  90        | 101       |  90          | Driver assumes over 100 is invalid |

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


############################################################
# Contact Tests via sensor_binary report
############################################################

    @contact
    Scenario Outline: Device reports a state change via a 'sensor_binary' report
        Given the capability cont:contact is <prev_state>
        When the device response with sensor_binary_v2 report
            And with parameter value <value>
            And send to driver
        Then the platform attribute cont:contact should change to <new_state>
            And the capability cont:contactchanged should be recent
            And the driver should place a base:ValueChange message on the platform bus

        Examples:
          | prev_state | value | new_state  |
          | CLOSED     |  -1   | OPENED     |
          | CLOSED     | 255   | OPENED     |
          | OPENED     |   0   | CLOSED     |

    @contact
    Scenario Outline: Device reports an invalid state change via a 'sensor_binary' report
        Given the capability cont:contact is <prev_state>
        When the device response with sensor_binary_v2 report
            And with parameter value <value>
            And send to driver
        Then the platform attribute cont:contact should be <prev_state>

        Examples:
          | prev_state | value |
          | CLOSED     |   1   |
          | CLOSED     |  15   |
          | OPENED     |   1   |
          | OPENED     | 127   |


############################################################
# Contact Tests via basic report
############################################################

    @contact
    Scenario Outline: Device reports a state change via a 'basic' report
        Given the capability cont:contact is <prev_state>
        When the device response with basic report
            And with parameter value <value>
            And send to driver
        Then the platform attribute cont:contact should change to <new_state>
            And the capability cont:contactchanged should be recent
            And the driver should place a base:ValueChange message on the platform bus

        Examples:
          | prev_state | value | new_state  |
          | CLOSED     |  -1   | OPENED     |
          | CLOSED     | 255   | OPENED     |
          | OPENED     |   0   | CLOSED     |

    @contact
    Scenario Outline: Device reports invalid state change via a 'basic' report
        Given the capability cont:contact is <prev_state>
        When the device response with basic report
            And with parameter value <value>
            And send to driver
        Then the platform attribute cont:contact should be <prev_state>

        Examples:
          | prev_state | value |
          | CLOSED     |   1   |
          | CLOSED     |  15   |
          | OPENED     |   1   |
          | OPENED     | 127   |


############################################################
# Contact Tests via alarm/notification report
############################################################

    @contact
    Scenario: Device reports contact opened via notification
        Given the capability cont:contact is CLOSED
        When the device response with alarm report
            And with parameter alarmtype 0
            And with parameter alarmlevel 0
            And with parameter notificationstatus -1
            And with parameter notificationtype 6
            And with parameter event 22
            And send to driver
        Then the platform attribute cont:contact should change to OPENED
            And the capability cont:contactchanged should be recent
            And the driver should place a base:ValueChange message on the platform bus

    @contact
    Scenario: Device reports contact closed via notification
        Given the capability cont:contact is OPENED
        When the device response with alarm report
            And with parameter alarmtype 0
            And with parameter alarmlevel 0
            And with parameter notificationstatus -1
            And with parameter notificationtype 6
            And with parameter event 23
            And send to driver
        Then the platform attribute cont:contact should change to CLOSED
            And the driver should place a base:ValueChange message on the platform bus
