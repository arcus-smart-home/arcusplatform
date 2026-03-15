@ZWave @Ecolink @Tilt
Feature: ZWave Ecolink Tilt Sensor 2.5 Driver Test

These scenarios test the functionality of the ZWave Ecolink TILT-ZWAVE2.5-ECO Garage Door Tilt Sensor driver

    Background:
    Given the ZW_Ecolink_TiltSensor_2_5.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform.
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'devsettings', 'tilt', 'cont']
        And the message's dev:devtypehint attribute should be Tilt
        And the message's devadv:drivername attribute should be ZWEcolinkTiltSensor25Driver
        And the message's devadv:driverversion attribute should be 2.5
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
            And the capability tilt:tiltstate should be UPRIGHT
            And the capability tilt:tiltstatechanged should be recent
            And the capability cont:contact should be OPENED
            And the capability cont:contactchanged should be recent
            And the capability cont:usehint should be UNKNOWN

    @basic @connected @timeout
    Scenario: Make sure the offline timeout is set each time the device Connects
        When the device is connected
        Then the driver should set timeout at 190 minutes

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Device                   |
          | "My Device"              |
          | "Tom's Door"             |
          | "Bob & Sue's Garage"     |


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
          | prev_level | level-arg | battery-attr | remarks                                            |
          |  50        |  -1       |   0          |                                                    |
          |  50        |   0       |  50          | Driver assumes zero is invalid                     |
          |  50        |   1       |   1          |                                                    |
          |  70        |  50       |  50          |                                                    |
          | 100        |  99       |  99          |                                                    |
          | 100        | 100       | 100          |                                                    |
          |  90        | 101       | 100          | Device sometimes reports over 100, but driver caps |

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
# Tilt Sensor Tests - Basic Report
############################################################

    @tilt
    Scenario Outline: Device reports tilt state change via basic report
        Given the capability tilt:tiltstate is <prev_state>
        When the device response with basic report
            And with parameter value <value>
            And send to driver
        Then the platform attribute tilt:tiltstate should change to <new_state>
            And the capability tilt:tiltstatechanged should be recent
            And the driver should place a base:ValueChange message on the platform bus

        Examples:
          | prev_state | value | new_state | remarks                              |
          | UPRIGHT    |  -1   | FLAT      | 0xFF means horizontal                |
          | FLAT       |   0   | UPRIGHT   | 0x00 means vertical                  |


    @tilt
    Scenario Outline: Device reports tilt state change via basic report with contact state (default)
        Given the capability tilt:tiltstate is <prev_state>
        When the device response with basic report
            And with parameter value <value>
            And send to driver
        Then the platform attribute tilt:tiltstate should change to <new_state>
            And the capability cont:contact should be <contact>
            And the capability cont:contactchanged should be recent
            And the driver should place a base:ValueChange message on the platform bus

        Examples:
          | prev_state | value | new_state | contact |
          | UPRIGHT    |  -1   | FLAT      | CLOSED  |
          | FLAT       |   0   | UPRIGHT   | OPENED  |


############################################################
# Tilt Sensor Tests - Sensor Binary Report
############################################################

    @tilt
    Scenario Outline: Device reports tilt state change via sensor binary report
        Given the capability tilt:tiltstate is <prev_state>
        # Note: command class is 'sensor binary v2' in ZWaveCommandClasses.json, must use sensor_binary_v2
        When the device response with sensor_binary_v2 report
            And with parameter value <value>
            And send to driver
        Then the platform attribute tilt:tiltstate should change to <new_state>
            And the capability tilt:tiltstatechanged should be recent
            And the driver should place a base:ValueChange message on the platform bus

        Examples:
          | prev_state | value | new_state |
          | UPRIGHT    |  -1   | FLAT      |
          | FLAT       |   0   | UPRIGHT   |


############################################################
# Tilt Sensor Tests - Notification/Alarm Report
############################################################

    @tilt @notification
    Scenario: Device reports tilt via notification - door open (upright)
        Given the capability tilt:tiltstate is FLAT
        When the device response with alarm report
            And with parameter alarmtype 0
            And with parameter alarmlevel 0
            And with parameter notificationstatus -1
            And with parameter notificationtype 6
            And with parameter event 22
            And send to driver
        Then the platform attribute tilt:tiltstate should change to UPRIGHT
            And the capability tilt:tiltstatechanged should be recent
            And the capability cont:contact should be OPENED
            And the capability cont:contactchanged should be recent
            And the driver should place a base:ValueChange message on the platform bus

    @tilt @notification
    Scenario: Device reports tilt via notification - door closed (flat)
        Given the capability tilt:tiltstate is UPRIGHT
        When the device response with alarm report
            And with parameter alarmtype 0
            And with parameter alarmlevel 0
            And with parameter notificationstatus -1
            And with parameter notificationtype 6
            And with parameter event 23
            And send to driver
        Then the platform attribute tilt:tiltstate should change to FLAT
            And the capability tilt:tiltstatechanged should be recent
            And the capability cont:contact should be CLOSED
            And the capability cont:contactchanged should be recent
            And the driver should place a base:ValueChange message on the platform bus

    @tilt @notification
    Scenario: Device reports tamper via notification
        Given the capability tilt:tiltstate is FLAT
        When the device response with alarm report
            And with parameter alarmtype 0
            And with parameter alarmlevel -1
            And with parameter notificationstatus -1
            And with parameter notificationtype 7
            And with parameter event 3
            And send to driver
        Then the platform attribute tilt:tiltstate should be FLAT


############################################################
# Wake Up Notification Tests
############################################################

    @wakeup
    Scenario: Device sends Wake Up Notification
        When the device response with wake_up notification
            And send to driver
        Then protocol bus should be empty
