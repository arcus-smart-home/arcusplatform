@ZWave @Zooz @switch
Feature: ZWave Zooz ZEN14 Outdoor Double Plug Driver Test

These scenarios test the functionality of the ZWave Zooz ZEN14 Outdoor Double Plug driver

    Background:
    Given the ZW_Zooz_Zen14_OutdoorDoublePlug.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform.
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's dev:devtypehint attribute should be Switch
        And the message's devadv:drivername attribute should be ZWZoozZen14OutdoorDoublePlugDriver
        And the message's devadv:driverversion attribute should be 2.4
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
        Then the driver should set timeout at 30 minutes

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Device                   |
          | "My Device"              |
          | "Tom's Plug"             |
          | "Bob & Sue's Patio"      |


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
# Switch Binary Report Tests (root endpoint -> outlet1)
############################################################

    @switch
    Scenario: Device reports outlet 1 ON via root switch binary report
        Given the capability swit:state.outlet1 is OFF
        When the device response with switch_binary report
            And with parameter value -1
            And send to driver
        Then the capability swit:state.outlet1 should be ON
            And the driver should place a base:ValueChange message on the platform bus

    @switch
    Scenario: Device reports outlet 1 OFF via root switch binary report
        Given the capability swit:state.outlet1 is ON
        When the device response with switch_binary report
            And with parameter value 0
            And send to driver
        Then the capability swit:state.outlet1 should be OFF
            And the driver should place a base:ValueChange message on the platform bus

    @switch
    Scenario: Device reports outlet 1 ON via root basic report
        Given the capability swit:state.outlet1 is OFF
        When the device response with basic report
            And with parameter value -1
            And send to driver
        Then the capability swit:state.outlet1 should be ON
            And the driver should place a base:ValueChange message on the platform bus

    @switch
    Scenario: Device reports outlet 1 OFF via root basic report
        Given the capability swit:state.outlet1 is ON
        When the device response with basic report
            And with parameter value 0
            And send to driver
        Then the capability swit:state.outlet1 should be OFF
            And the driver should place a base:ValueChange message on the platform bus

    @switch
    Scenario: Device reports outlet 1 ON via root basic set
        Given the capability swit:state.outlet1 is OFF
        When the device response with basic set
            And with parameter value -1
            And send to driver
        Then the capability swit:state.outlet1 should be ON
            And the driver should place a base:ValueChange message on the platform bus

    @switch
    Scenario: Device reports outlet 1 OFF via root basic set
        Given the capability swit:state.outlet1 is ON
        When the device response with basic set
            And with parameter value 0
            And send to driver
        Then the capability swit:state.outlet1 should be OFF
            And the driver should place a base:ValueChange message on the platform bus
