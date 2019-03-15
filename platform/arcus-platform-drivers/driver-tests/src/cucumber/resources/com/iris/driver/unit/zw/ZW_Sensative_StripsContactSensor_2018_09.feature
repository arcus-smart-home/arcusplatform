@ZWave @Sensative @contact
Feature: Tests for the Sensative Contact sensor driver

    These scenarios test the functionality of the Sensative Strips contact sensor driver.

    Background:
        Given the ZW_Sensative_ContactSensor_2018_09.driver has been initialized

    Scenario: Driver reports capabilities to platform. 
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'cont' ]
            And the message's dev:devtypehint attribute should be Contact
            And the message's devadv:drivername attribute should be ZWSensativeContactSensorDriver 
            And the message's devadv:driverversion attribute should be 2018.09
            And the message's devpow:source attribute should be BATTERY
            And the message's devpow:linecapable attribute should be false        
            And the message's devpow:backupbatterycapable attribute should be false
            And the message's cont:usehint attribute should be DOOR
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
        Then the driver should set timeout at 4380 minutes

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

        Examples:
          | prev_level | level-arg | battery-attr | remarks                           |
          |  50        |  -1       |   0          |                                   |
          |  50        |   0       |  50          | Driver assumes zero is invalid    |
          |  50        |   1       |   1          |                                   |
          |  70        |  50       |  50          |                                   |
          | 100        |  99       |  99          |                                   |
          | 100        | 100       | 100          |                                   |
          |  90        | 101       |  90          | 101 is invalid value, and ignored |


############################################################
# Sensative Contact Sensor Driver Specific Tests
############################################################

    @contact
    Scenario Outline: Contact state has changed
        Given the capability cont:contact is <prev_state>
        When the device response with alarm report
            And with parameter alarmtype <atype>
            And with parameter alarmlevel <alevel>
            And with parameter notificationstatus <nstatus>
            And with parameter notificationtype <ntype>
            And with parameter event <event>
            And send to driver
        Then the platform attribute cont:contact should change to <state>
            And the driver should place a base:ValueChange message on the platform bus

        Examples:
          | prev_state | state     | atype | alevel | nstatus | ntype | event |
          | CLOSED     | OPENED    | 0     | 0      | -1      | 6     | 22    |
          | OPENED     | CLOSED    | 0     |    0   | -1      | 6     | 23    |    

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
