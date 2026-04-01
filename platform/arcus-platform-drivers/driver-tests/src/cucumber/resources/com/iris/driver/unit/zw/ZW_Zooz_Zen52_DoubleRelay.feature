@ZWave @Zooz @switch
Feature: ZWave Zooz ZEN52 Double Relay Driver Test

These scenarios test the functionality of the ZWave Zooz ZEN52 Double Relay driver

    Background:
    Given the ZW_Zooz_Zen52_DoubleRelay.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform.
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's dev:devtypehint attribute should be Switch
        And the message's devadv:drivername attribute should be ZWZoozZen52DoubleRelayDriver
        And the message's devadv:driverversion attribute should be 1.0
        And the message's devpow:source attribute should be LINE
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
          | "Tom's Relay"            |
          | "Bob & Sue's Garage"     |


############################################################
# Generic ZWave Driver Tests
############################################################

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
# Multi-Channel Switch Binary Report Tests
############################################################

    @switch
    Scenario: Device reports relay 1 ON via multi-channel encapsulated switch binary report
        Given the capability swit:state.relay1 is OFF
        When the device response with multi_channel encapsulate
            And with parameter source 1
            And with parameter command_class 37
            And with parameter command 3
            And with parameter parameter1 -1
            And send to driver
        Then the capability swit:state.relay1 should be ON
            And the driver should place a base:ValueChange message on the platform bus

    @switch
    Scenario: Device reports relay 1 OFF via multi-channel encapsulated switch binary report
        Given the capability swit:state.relay1 is ON
        When the device response with multi_channel encapsulate
            And with parameter source 1
            And with parameter command_class 37
            And with parameter command 3
            And with parameter parameter1 0
            And send to driver
        Then the capability swit:state.relay1 should be OFF
            And the driver should place a base:ValueChange message on the platform bus

    @switch
    Scenario: Device reports relay 2 ON via multi-channel encapsulated switch binary report
        Given the capability swit:state.relay2 is OFF
        When the device response with multi_channel encapsulate
            And with parameter source 2
            And with parameter command_class 37
            And with parameter command 3
            And with parameter parameter1 -1
            And send to driver
        Then the capability swit:state.relay2 should be ON
            And the driver should place a base:ValueChange message on the platform bus

    @switch
    Scenario: Device reports relay 2 OFF via multi-channel encapsulated switch binary report
        Given the capability swit:state.relay2 is ON
        When the device response with multi_channel encapsulate
            And with parameter source 2
            And with parameter command_class 37
            And with parameter command 3
            And with parameter parameter1 0
            And send to driver
        Then the capability swit:state.relay2 should be OFF
            And the driver should place a base:ValueChange message on the platform bus

    @switch
    Scenario: Root switch binary reports are ignored (ambiguous on multi-relay devices)
        Given the capability swit:state.relay1 is OFF
        When the device response with switch_binary report
            And with parameter value -1
            And send to driver
        Then the capability swit:state.relay1 should be OFF

    @switch
    Scenario: Root basic reports are ignored (ambiguous on multi-relay devices)
        Given the capability swit:state.relay1 is OFF
        When the device response with basic report
            And with parameter value -1
            And send to driver
        Then the capability swit:state.relay1 should be OFF
