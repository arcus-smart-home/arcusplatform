@ZWave @Zooz @switch
Feature: ZWave Zooz ZEN26 In-Wall Switch Driver Test

These scenarios test the functionality of the ZWave Zooz ZEN26 In-Wall Switch driver

    Background:
    Given the ZW_Zooz_Zen26.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform.
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'swit', 'indicator', 'devsettings' ]
        And the message's dev:devtypehint attribute should be Switch
        And the message's devadv:drivername attribute should be ZWZoozZen26Driver
        And the message's devadv:driverversion attribute should be 2.4
        And the message's devpow:source attribute should be LINE
        And the message's devpow:linecapable attribute should be true
        And the message's devpow:backupbatterycapable attribute should be false
        And the message's indicator:indicator attribute should be ON
        And the message's indicator:enabled attribute should be true
        And the message's indicator:enableSupported attribute should be true
        And the message's indicator:inverted attribute should be false
        And the message's swit:state attribute should be OFF
        And the message's swit:inverted attribute should be false
    Then both busses should be empty


############################################################
# Generic Driver Tests
############################################################

    @basic @added
    Scenario: Make sure any "time of change" attributes are defaulted when the device is first Added
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the capability swit:statechanged should be recent
        Then the driver should send configuration set
            And with parameter param 2
            And with parameter size 1
            And with parameter val1 1
        Then the driver should send configuration set
            And with parameter param 1
            And with parameter size 1
            And with parameter val1 0
        Then both busses should be empty

    @basic @connected @timeout
    Scenario: Make sure the offline timeout is set each time the device Connects
        When the device is connected
        Then the driver should send configuration get
        Then the driver should send configuration get
        Then the driver should set timeout at 30 minutes
        Then the driver should poll switch_binary.get every 600 seconds

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Device                   |
          | "My Device"              |
          | "Tom's Switch"           |
          | "Bob & Sue's Switch"     |


############################################################
# Switch Tests
############################################################

    @switch
    Scenario: Platform turns on switch via attribute change
        When a base:SetAttributes command with the value of swit:state ON is placed on the platform bus
        Then the driver should send switch_binary set
            And with parameter value -1

    @switch
    Scenario: Platform turns off switch via attribute change
        When a base:SetAttributes command with the value of swit:state OFF is placed on the platform bus
        Then the driver should send switch_binary set
            And with parameter value 0

    @switch
    Scenario: Device reports switch binary ON
        When the device response with switch_binary report
            And with parameter value -1
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability swit:state should be ON

    @switch
    Scenario: Device reports switch binary OFF
        When the device response with switch_binary report
            And with parameter value 0
            And send to driver
        Then the capability swit:state should be OFF

    @switch
    Scenario: Device reports basic report ON
        When the device response with basic report
            And with parameter value -1
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability swit:state should be ON

    @switch
    Scenario: Device reports basic report OFF
        When the device response with basic report
            And with parameter value 0
            And send to driver
        Then the capability swit:state should be OFF


############################################################
# Indicator Tests
############################################################

    Scenario Outline: Indicator value
        Given the capability indicator:indicator is <before>
            And the capability indicator:enabled is <isEnabled>
            And the capability indicator:inverted is <isInverted>
        When the device response with switch_binary report
            And with parameter value <value>
            And send to driver
        Then the platform attribute indicator:indicator should change to <after>

        Examples:
          |before | isEnabled | isInverted | value | after    |
          | ON    | true      |  false     |  -1   | OFF      |
          | ON    | true      |  true      |  -1   | ON       |
          | ON    | true      |  false     |   0   | ON       |
          | ON    | true      |  true      |   0   | OFF      |
          | ON    | false     |  false     |  -1   | DISABLED |
          | ON    | false     |  true      |  -1   | DISABLED |
          | ON    | false     |  false     |   0   | DISABLED |
          | ON    | false     |  true      |   0   | DISABLED |


############################################################
# Generic ZWave Driver Tests
############################################################

    Scenario: Make sure driver handles Device Reset Locally Notification
        When the device response with device_reset_locally notification
            And send to driver
        Then protocol bus should be empty
