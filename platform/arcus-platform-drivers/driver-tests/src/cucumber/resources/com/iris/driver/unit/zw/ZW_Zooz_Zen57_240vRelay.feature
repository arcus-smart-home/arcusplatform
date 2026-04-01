@ZWave @Zooz @switch
Feature: ZWave Zooz ZEN57 240V XS Relay Driver Test

These scenarios test the functionality of the ZWave Zooz ZEN57 240V XS Relay driver

    Background:
    Given the ZW_Zooz_Zen57_240vRelay.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform.
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's dev:devtypehint attribute should be Switch
        And the message's devadv:drivername attribute should be ZWZoozZen57240vRelayDriver
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
            And the capability swit:statechanged should be recent

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
          | "Bob & Sue's Pool Pump"  |


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
# Switch Binary Report Tests
############################################################

    @switch
    Scenario: Device reports switch ON via switch binary report
        Given the capability swit:state is OFF
        When the device response with switch_binary report
            And with parameter value -1
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability swit:state should be ON
            And the capability swit:statechanged should be recent

    @switch
    Scenario: Device reports switch OFF via switch binary report
        Given the capability swit:state is ON
        When the device response with switch_binary report
            And with parameter value 0
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability swit:state should be OFF
            And the capability swit:statechanged should be recent

    @switch
    Scenario: Device reports switch ON via basic report
        Given the capability swit:state is OFF
        When the device response with basic report
            And with parameter value -1
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability swit:state should be ON
            And the capability swit:statechanged should be recent

    @switch
    Scenario: Device reports switch OFF via basic report
        Given the capability swit:state is ON
        When the device response with basic report
            And with parameter value 0
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability swit:state should be OFF
            And the capability swit:statechanged should be recent

    @switch
    Scenario: Device reports switch ON via basic set
        Given the capability swit:state is OFF
        When the device response with basic set
            And with parameter value -1
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability swit:state should be ON
            And the capability swit:statechanged should be recent

    @switch
    Scenario: Device reports switch OFF via basic set
        Given the capability swit:state is ON
        When the device response with basic set
            And with parameter value 0
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability swit:state should be OFF
            And the capability swit:statechanged should be recent
