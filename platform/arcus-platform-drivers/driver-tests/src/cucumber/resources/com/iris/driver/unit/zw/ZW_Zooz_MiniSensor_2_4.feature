@ZWave @Zooz @motion
Feature: ZWave Zooz Mini Motion and Illumination Sensor Driver Test

These scenarios test the functionality of the ZWave Zooz Mini Motion and Illumination Sensor driver

    Background:
    Given the ZW_Zooz_MiniSensor_2_4.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform. 
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'mot', 'ill']
        And the message's dev:devtypehint attribute should be Motion
        And the message's devadv:drivername attribute should be ZWZoozMiniSensorDriver 
        And the message's devadv:driverversion attribute should be 2.4
        And the message's devpow:source attribute should be BATTERY
        And the message's devpow:linecapable attribute should be false
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
          | "My Device"              |
          | "Tom's Room"             |
          | "Bob & Sue's Room"       |


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
# Generic Motion Tests
############################################################

    @motion
    Scenario Outline: Device reports motion detected via a 'sensor_binary' report
        Given the capability mot:motion is <prev_state>
        # Note: this command is defined in ZWaveCommandClasses.json as 'sensor binary v2' so you must send 'sensor_binary_v2' report to test
        # even though driver implements 'onZWaveMessage.sensor_binary.report', without the _v2
        When the device response with sensor_binary_v2 report
            And with parameter value <value>
            And send to driver
        Then the platform attribute mot:motion should change to <new_state>
            And the capability mot:motionchanged should be recent
            And the driver should place a base:ValueChange message on the platform bus

        Examples:
          | prev_state | value | new_state  |
          | NONE       |  -1   | DETECTED   |
          | NONE       | 255   | DETECTED   |

    @motion
    Scenario Outline: Device reports NO motion via a 'sensor_binary' report
        Given the capability mot:motion is <prev_state>
        # Note: this command is defined in ZWaveCommandClasses.json as 'sensor binary v2' so you must send 'sensor_binary_v2' report to test
        # even though driver implements 'onZWaveMessage.sensor_binary.report', without the _v2
        When the device response with sensor_binary_v2 report
            And with parameter value <value>
            And send to driver
        Then the platform attribute mot:motion should change to <new_state>
            And the driver should place a base:ValueChange message on the platform bus

        Examples:
          | prev_state | value | new_state  |
          | DETECTED   |   0   | NONE       |

    @motion
    Scenario Outline: Device reports invalid value via a 'sensor_binary' report
        Given the capability mot:motion is <prev_state>
        # Note: this command is defined in ZWaveCommandClasses.json as 'sensor binary v2' so you must send 'sensor_binary_v2' report to test
        # even though driver implements 'onZWaveMessage.sensor_binary.report', without the _v2
        When the device response with sensor_binary_v2 report
            And with parameter value <value>
            And send to driver
        Then the platform attribute mot:motion should be <prev_state>

        Examples:
          | prev_state | value |
          | NONE       |   1   |
          | NONE       | 127   |
          | DETECTED   |   1   |
          | DETECTED   | 127   |

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
          | NONE       | 255   | DETECTED   |

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

    @motion
    Scenario Outline: Device reports invalid value via a 'basic' report
        Given the capability mot:motion is <prev_state>
        When the device response with basic report
            And with parameter value <value>
            And send to driver
        Then the platform attribute mot:motion should be <prev_state>

        Examples:
          | prev_state | value |
          | NONE       |   1   |
          | NONE       | 127   |
          | DETECTED   |   1   |
          | DETECTED   | 127   |


############################################################
# Zooz Mini Motion & Illumination Sensor Driver Specific Tests
############################################################

    @illumination
    Scenario Outline: Device reports illumination reading
        When the device response with sensor_multilevel report
            # type 3 is luminance
            And with parameter type 3
            # level is 4{4 bytes} + 8{scale 1 = lux} + 0{precision 0}
            And with parameter level 12
            And with parameter val1 <Value1>
            And with parameter val2 <Value2>
            And with parameter val3 <Value3>
            And with parameter val4 <Value4>
            And send to driver
        Then the platform attribute ill:illuminance should change to <reading>
            And the driver should place a base:ValueChange message on the platform bus
            
        Examples:
          | Value1 | Value2 | Value3 | Value4 | reading | remarks                   |
          | 0      | 0      | 0      | 0      | 0       |                           |
          | 0      | 0      | 0      | 1      | 1       |                           |
          | 0      | 0      | 0      | 128    | 128     |                           |
          | 0      | 0      | 0      | 255    | 255     |                           |
          | 0      | 0      | 1      | 0      | 256     |                           |
          | 0      | 0      | 1      | 1      | 257     |                           |
          | 0      | 1      | 1      | 1      | 65793   |                           |
          | 0      | 1      | 134    | 159    | 99999   |                           |
          | 0      | 1      | 134    | 160    | 100000  | max allowed Lux is 100000 |
          | 0      | 1      | 134    | 161    | 100000  | max allowed Lux is 100000 |
          | 1      | 1      | 1      | 1      | 100000  | max allowed Lux is 100000 |



