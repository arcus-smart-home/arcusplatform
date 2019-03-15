@ZWave @Utilitech @Leak
Feature: ZWave Utilitech Water Leak Detector Driver Test

    These scenarios test the functionality of the ZWave Utilitech Water Leak Detector driver.

    Background:
        Given the ZW_Utilitech_Leak_Detector_2_3.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform. 
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'leakh2o']
            And the message's dev:devtypehint attribute should be Water Leak
            And the message's devadv:drivername attribute should be ZWaveUtilitechLeakDetectorDriver 
            And the message's devadv:driverversion attribute should be 2.3
            And the message's devpow:source attribute should be BATTERY
            And the message's devpow:linecapable attribute should be false        
            And the message's devpow:backupbatterycapable attribute should be false
            And the message's leakh2o:state attribute should be SAFE
        Then both busses should be empty


############################################################
# General Driver Tests
############################################################

    @basic @added
    Scenario: Device added
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the capability leakh2o:statechanged should be recent

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set 
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Leak                     |
          | "Washing Machine"        |
          | "Bob's Sink"             |
          | "Mom & Dad's Room"       |


############################################################
# Sensor Alarm Tests
############################################################

    Scenario Outline: Device reports SAFE alarm state with 0 or 1 bit errors
        Given the capability leakh2o:state is LEAK
        When the device response with sensor_alarm report
            And with parameter sensortype 5
            And with parameter sensorstate <sensorstate>
            And send to driver
        Then the platform attribute leakh2o:state should change to SAFE
            And the driver should place a base:ValueChange message on the platform bus

        Examples:
          | sensorstate | remark   |
          |       0     | 00000000 |
          |       1     | 00000001 |
          |       2     | 00000010 |
          |       4     | 00000100 |
          |       8     | 00001000 |
          |      16     | 00010000 |
          |      32     | 00100000 |
          |      64     | 01000000 |
          |     128     | 10000000 |

    Scenario Outline: Device reports LEAK alarm state with 0 or 1 bit errors
        Given the capability leakh2o:state is SAFE
        When the device response with sensor_alarm report
            And with parameter sensortype 5
            And with parameter sensorstate <sensorstate>
            And send to driver
        Then the platform attribute leakh2o:state should change to LEAK
            And the driver should place a base:ValueChange message on the platform bus

        Examples:
          | sensorstate | remark   |
          |      -1     | 11111111 |
          |      -2     | 11111110 |
          |      -3     | 11111101 |
          |      -5     | 11111011 |
          |      -9     | 11110111 |
          |     -17     | 11101111 |
          |     -33     | 11011111 |
          |     -65     | 10111111 |
          |     127     | 01111111 |

    Scenario Outline: Device reports undefined alarm state, more than 1 bit error
        Given the capability leakh2o:state is SAFE
        When the device response with sensor_alarm report
            And with parameter sensortype 5
            And with parameter sensorstate <sensorstate>
            And send to driver
        Then the platform attribute leakh2o:state should change to LEAK
            And the driver should place a base:ValueChange message on the platform bus

        Examples:
          | sensorstate | remark   |
          |      -4     | 11111100 |
          |     -16     | 11110000 |
          |      15     | 00001111 |
          |      63     | 00111111 |


############################################################
# Battery Tests
############################################################

    Scenario Outline: Device reports battery level
        Given the capability devpow:battery is 60
        When the device response with battery report 
            And with parameter level <level-arg>
            And send to driver 
        Then the platform attribute devpow:battery should change to <battery-attr>
            And the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty

        Examples:
          | level-arg | battery-attr |
          | -1        |  0           |
          |  1        |  1           |
          |  50       |  50          |
          |  100      |  100         |

