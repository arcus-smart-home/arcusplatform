@ZWave @Zooz @dimmer
Feature: ZWave Zooz ZEN77 S2 Dimmer Switch Driver Test

These scenarios test the functionality of the ZWave Zooz ZEN77 S2 Dimmer Switch driver

    Background:
    Given the ZW_Zooz_Zen77_DimmerSwitch.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform.
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's dev:devtypehint attribute should be Dimmer
        And the message's devadv:drivername attribute should be ZWZoozZen77DimmerSwitchDriver
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
            And the capability swit:statechanged should be recent

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
          | "Tom's Dimmer"           |
          | "Bob & Sue's Light"      |


############################################################
# Dimmer Tests
############################################################

    @dimmer
    Scenario: Device reports multilevel switch at 50%
        When the device response with switch_multilevel report
            And with parameter value 50
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability dim:brightness should be 50
            And the capability swit:state should be ON

    @dimmer
    Scenario: Device reports multilevel switch at 0 (OFF)
        When the device response with switch_multilevel report
            And with parameter value 0
            And send to driver
        Then the capability swit:state should be OFF

    @dimmer
    Scenario: Device reports multilevel switch at 99 (full brightness)
        When the device response with switch_multilevel report
            And with parameter value 99
            And send to driver
        Then the capability dim:brightness should be 100
            And the capability swit:state should be ON


############################################################
# Central Scene Tests (scene control must be enabled via param 13)
############################################################

    @button
    Scenario: Device reports upper paddle single tap via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 1
            And with parameter properties1 0
            And with parameter scenenumber 2
            And send to driver
        Then the capability but:state.up should be PRESSED
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Device reports upper paddle released via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 2
            And with parameter properties1 1
            And with parameter scenenumber 2
            And send to driver
        Then the capability but:state.up should be RELEASED
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Device reports lower paddle single tap via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 3
            And with parameter properties1 0
            And with parameter scenenumber 1
            And send to driver
        Then the capability but:state.down should be PRESSED
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Device reports lower paddle released via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 4
            And with parameter properties1 1
            And with parameter scenenumber 1
            And send to driver
        Then the capability but:state.down should be RELEASED
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Device reports upper paddle double tap via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 5
            And with parameter properties1 3
            And with parameter scenenumber 2
            And send to driver
        Then the capability but:state.up should be PRESSED
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Device reports lower paddle held via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 6
            And with parameter properties1 2
            And with parameter scenenumber 1
            And send to driver
        Then the capability but:state.down should be PRESSED
            And the driver should place a base:ValueChange message on the platform bus


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

    @basic
    Scenario: Device reports basic report ON via local change
        When the device response with basic report
            And with parameter value 75
            And send to driver
        Then the capability dim:brightness should be 75
            And the capability swit:state should be ON

    @basic
    Scenario: Device reports basic report OFF via local change
        When the device response with basic report
            And with parameter value 0
            And send to driver
        Then the capability swit:state should be OFF
