@ZWave @Aeotec @leak
Feature: ZWave Aeotec Water Sensor 6 Driver Test

These scenarios test the functionality of the ZWave Aeotec Water Sensor 6 (ZW122-A) driver

    Background:
    Given the ZW_Aeotec_WaterSensor6.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform.
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'leakh2o', 'temp']
        And the message's dev:devtypehint attribute should be Water Leak
        And the message's devadv:drivername attribute should be ZWAeotecWaterSensor6Driver
        And the message's devadv:driverversion attribute should be 2.4
        And the message's devpow:source attribute should be BATTERY
        And the message's devpow:linecapable attribute should be false
        And the message's devpow:backupbatterycapable attribute should be false
        And the message's leakh2o:state attribute should be SAFE
    Then both busses should be empty


############################################################
# Generic Driver Tests
############################################################

    @basic @added
    Scenario: Make sure any "time of change" attributes are defaulted when the device is first Added
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the capability leakh2o:statechanged should be recent

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
          | "Water Sensor"           |
          | "Tom's Sink"             |
          | "Bob & Sue's Washer"     |


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
# Leak Tests via sensor_binary report
############################################################

    @leak
    Scenario Outline: Device reports a state change via a 'sensor_binary' report
        Given the capability leakh2o:state is <prev_state>
        When the device response with sensor_binary_v2 report
            And with parameter value <value>
            And send to driver
        Then the platform attribute leakh2o:state should change to <new_state>
            And the capability leakh2o:statechanged should be recent
            And the driver should place a base:ValueChange message on the platform bus

        Examples:
          | prev_state | value | new_state |
          | SAFE       |   -1  | LEAK      |
          | SAFE       |  255  | LEAK      |
          | LEAK       |    0  | SAFE      |

    @leak
    Scenario Outline: Device reports an invalid state change via a 'sensor_binary' report
        Given the capability leakh2o:state is <prev_state>
        When the device response with sensor_binary_v2 report
            And with parameter value <value>
            And send to driver
        Then the platform attribute leakh2o:state should be <prev_state>

        Examples:
          | prev_state | value |
          | SAFE       |    1  |
          | SAFE       |   15  |
          | LEAK       |    1  |
          | LEAK       |  127  |


############################################################
# Leak Tests via basic report
############################################################

    @leak
    Scenario Outline: Device reports a state change via a 'basic' report
        Given the capability leakh2o:state is <prev_state>
        When the device response with basic report
            And with parameter value <value>
            And send to driver
        Then the platform attribute leakh2o:state should change to <new_state>
            And the capability leakh2o:statechanged should be recent
            And the driver should place a base:ValueChange message on the platform bus

        Examples:
          | prev_state | value | new_state |
          | SAFE       |   -1  | LEAK      |
          | SAFE       |  255  | LEAK      |
          | LEAK       |    0  | SAFE      |

    @leak
    Scenario Outline: Device reports invalid state change via a 'basic' report
        Given the capability leakh2o:state is <prev_state>
        When the device response with basic report
            And with parameter value <value>
            And send to driver
        Then the platform attribute leakh2o:state should be <prev_state>

        Examples:
          | prev_state | value |
          | SAFE       |    1  |
          | SAFE       |  127  |
          | LEAK       |    1  |
          | LEAK       |   15  |


############################################################
# Leak Tests via alarm/notification report
############################################################

    @leak
    Scenario: Device reports water leak detected via notification
        Given the capability leakh2o:state is SAFE
        When the device response with alarm report
            And with parameter alarmtype 0
            And with parameter alarmlevel 0
            And with parameter notificationstatus -1
            And with parameter notificationtype 5
            And with parameter event 2
            And send to driver
        Then the platform attribute leakh2o:state should change to LEAK
            And the capability leakh2o:statechanged should be recent
            And the driver should place a base:ValueChange message on the platform bus

    @leak
    Scenario: Device reports water leak cleared via notification
        Given the capability leakh2o:state is LEAK
        When the device response with alarm report
            And with parameter alarmtype 0
            And with parameter alarmlevel 0
            And with parameter notificationstatus -1
            And with parameter notificationtype 5
            And with parameter event 0
            And send to driver
        Then the platform attribute leakh2o:state should change to SAFE
            And the driver should place a base:ValueChange message on the platform bus
