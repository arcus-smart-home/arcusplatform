@ZWave @Zooz @motion
Feature: ZWave Zooz ZSE18 Motion Sensor Driver Test

These scenarios test the functionality of the ZWave Zooz ZSE18 Motion Sensor driver

    Background:
    Given the ZW_Zooz_Zse18_MotionSensor.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform.
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'mot']
        And the message's dev:devtypehint attribute should be Motion
        And the message's devadv:drivername attribute should be ZWZoozZse18MotionSensorDriver
        And the message's devadv:driverversion attribute should be 2.4
        And the message's devpow:source attribute should be BATTERY
        And the message's devpow:linecapable attribute should be true
        And the message's devpow:backupbatterycapable attribute should be false
    Then both busses should be empty


############################################################
# Generic Driver Tests
############################################################

    @basic @added
    Scenario: Make sure any "time of change" attributes are defaulted when the device is first Added
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the capability mot:motionchanged should be recent

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
          | "Motion Sensor"          |
          | "Tom's Hallway"          |
          | "Bob & Sue's Bedroom"    |


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
# Motion Tests via basic report
############################################################

    @motion
    Scenario Outline: Device reports motion detected via a 'basic' report
        Given the capability mot:motion is <prev_state>
        When the device response with basic report
            And with parameter value <value>
            And send to driver
        Then the platform attribute mot:motion should change to <new_state>
            And the capability mot:motionchanged should be recent
            And the driver should place a base:ValueChange message on the platform bus

        Examples:
          | prev_state | value | new_state  |
          | NONE       |  -1   | DETECTED   |

    @motion
    Scenario Outline: Device reports NO motion detected via a 'basic' report
        Given the capability mot:motion is <prev_state>
        When the device response with basic report
            And with parameter value <value>
            And send to driver
        Then the platform attribute mot:motion should change to <new_state>
            And the driver should place a base:ValueChange message on the platform bus

        Examples:
          | prev_state | value | new_state  |
          | DETECTED   |   0   | NONE       |


############################################################
# Motion Tests via sensor binary report
############################################################

    @motion
    Scenario Outline: Device reports motion detected via a 'sensor_binary' report
        Given the capability mot:motion is <prev_state>
        # Note: command class is 'sensor binary v2' in ZWaveCommandClasses.json, must use sensor_binary_v2
        When the device response with sensor_binary_v2 report
            And with parameter value <value>
            And send to driver
        Then the platform attribute mot:motion should change to <new_state>
            And the capability mot:motionchanged should be recent
            And the driver should place a base:ValueChange message on the platform bus

        Examples:
          | prev_state | value | new_state  |
          | NONE       |  -1   | DETECTED   |

    @motion
    Scenario Outline: Device reports NO motion via a 'sensor_binary' report
        Given the capability mot:motion is <prev_state>
        When the device response with sensor_binary_v2 report
            And with parameter value <value>
            And send to driver
        Then the platform attribute mot:motion should change to <new_state>
            And the driver should place a base:ValueChange message on the platform bus

        Examples:
          | prev_state | value | new_state  |
          | DETECTED   |   0   | NONE       |


############################################################
# Motion Tests via alarm/notification report
############################################################

    @motion
    Scenario: Device reports motion detected via notification
        Given the capability mot:motion is NONE
        When the device response with alarm report
            And with parameter alarmtype 0
            And with parameter alarmlevel 0
            And with parameter notificationstatus -1
            And with parameter notificationtype 7
            And with parameter event 8
            And send to driver
        Then the platform attribute mot:motion should change to DETECTED
            And the capability mot:motionchanged should be recent
            And the driver should place a base:ValueChange message on the platform bus

    @motion
    Scenario: Device reports motion cleared via notification
        Given the capability mot:motion is DETECTED
        When the device response with alarm report
            And with parameter alarmtype 0
            And with parameter alarmlevel 0
            And with parameter notificationstatus -1
            And with parameter notificationtype 7
            And with parameter event 0
            And send to driver
        Then the platform attribute mot:motion should change to NONE
            And the driver should place a base:ValueChange message on the platform bus

    @motion
    Scenario: Device reports tamper via notification - should not change motion state
        Given the capability mot:motion is NONE
        When the device response with alarm report
            And with parameter alarmtype 0
            And with parameter alarmlevel 0
            And with parameter notificationstatus -1
            And with parameter notificationtype 7
            And with parameter event 3
            And send to driver
        Then the platform attribute mot:motion should be NONE


############################################################
# Wake Up Notification Tests
############################################################

    @wakeup
    Scenario: Device sends Wake Up Notification
        When the device response with wake_up notification
            And send to driver
        Then protocol bus should be empty
