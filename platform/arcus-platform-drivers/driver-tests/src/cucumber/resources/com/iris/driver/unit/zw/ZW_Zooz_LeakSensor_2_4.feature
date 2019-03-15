@ZWave @Zooz @leak
Feature: ZWave Zooz Leak Sensor Driver Test

These scenarios test the functionality of the ZWave Zooz Leak Sensor driver

    Background:
    Given the ZW_Zooz_LeakSensor_2_4.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform. 
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'leakh2o']
        And the message's dev:devtypehint attribute should be Water Leak
        And the message's devadv:drivername attribute should be ZWaveZoozLeakSensorDriver 
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
        Then the driver should set timeout at 190 minutes

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set 
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Device                   |
          | "Leak Sensor"            |
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
# Generic Leak Tests
############################################################

    @leak
    Scenario Outline: Device reports a state change via a 'sensor_binary' report
        Given the capability leakh2o:state is <prev_state>
        # Note: this command is defined in ZWaveCommandClasses.json as 'sensor binary v2' so you must send 'sensor_binary_v2' report to test
        # even though driver implements 'onZWaveMessage.sensor_binary.report', without the _v2
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
        # Note: this command is defined in ZWaveCommandClasses.json as 'sensor binary v2' so you must send 'sensor_binary_v2' report to test
        # even though driver implements 'onZWaveMessage.sensor_binary.report', without the _v2
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
    Scenario Outline: Device reports a state change via a 'basic' report
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


    @leak
    Scenario Outline: Device reports a state change via a 'alarm' report
        Given the capability leakh2o:state is <prev_state>
        When the device response with alarm report
            And with parameter alarmtype <atype>
            And with parameter alarmlevel <alevel>
            And with parameter notificationstatus <nstatus>
            And with parameter notificationtype <ntype>
            And with parameter event <event>
            And send to driver
        Then the platform attribute leakh2o:state should change to <new_state>
            And the capability leakh2o:statechanged should be recent
            And the driver should place a base:ValueChange message on the platform bus
        
        Examples:
          | prev_state | atype | alevel | nstatus | ntype | event | new_state |
          | SAFE       | 0     | 0      | -1      | 5     | 2     | LEAK      |
          | LEAK       | 0     | 0      | -1      | 5     | 0     | SAFE      |

    @leak
    Scenario Outline: Device reports an invalid state change via a 'alarm' report
        Given the capability leakh2o:state is <prev_state>
        When the device response with alarm report
            And with parameter alarmtype <atype>
            And with parameter alarmlevel <alevel>
            And with parameter notificationstatus <nstatus>
            And with parameter notificationtype <ntype>
            And with parameter event <event>
            And send to driver
        Then the platform attribute leakh2o:state should be <prev_state>
        
        Examples:
          | prev_state | atype | alevel | nstatus | ntype | event |
          | SAFE       | 0     | 0      | -1      | 5     | 1     |
          | LEAK       | 0     | 0      | -1      | 5     | 1     |
          | SAFE       | 0     | 0      | -1      | 6     | 2     |
          | LEAK       | 0     | 0      | -1      | 0     | 0     |
          | SAFE       | 0     | 0      |  0      | 5     | 2     |
          | LEAK       | 0     | 0      |  1      | 5     | 0     |
          | SAFE       | 0     | 1      | -1      | 5     | 2     |
          | LEAK       | 0     | 2      | -1      | 5     | 0     |
          | SAFE       | 1     | 0      | -1      | 5     | 2     |
          | LEAK       | 2     | 0      | -1      | 5     | 0     |


############################################################
# Zooz Leak Sensor Driver Specific Tests
############################################################

