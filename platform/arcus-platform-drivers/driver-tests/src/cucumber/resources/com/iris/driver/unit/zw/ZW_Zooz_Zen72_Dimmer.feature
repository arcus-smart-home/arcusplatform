@ZWave @Zooz @dimmer @zen72
Feature: ZWave Zooz ZEN72 Dimmer Driver Test

These scenarios test the functionality of the ZWave Zooz ZEN72 Dimmer driver

    Background:
    Given the ZW_Zooz_Zen72_Dimmer.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform.
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's dev:devtypehint attribute should be Dimmer
        And the message's devadv:drivername attribute should be ZWZoozZen72DimmerDriver
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

    # The lifeline (group 1) makes the dimmer report local changes to the hub.
    @basic @connected
    Scenario: Connecting sets the lifeline association
        When the device is connected
        Then the driver should set timeout at 30 minutes
        Then the driver should send association set
            And with parameter group 1
            And with parameter node1 1

    # Every setting belongs to device settings. Writing one on connect would undo the
    # user's choice every time the dimmer reconnects.
    @basic @connected
    Scenario: Connecting does not write any configuration
        When the device is connected
        Then the driver should not send configuration set

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
    Scenario: Setting the brightness sends a multilevel switch set
        When a base:SetAttributes command with the value of dim:brightness 50 is placed on the platform bus
        Then the driver should send switch_multilevel set
            And with parameter level 50

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
# Basic Tests (local changes)
############################################################

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

    # 0xFF means "on" without a level, so the driver asks for the level.
    @basic
    Scenario Outline: Basic 0xFF reads the level instead of guessing it
        When the device response with basic <command>
            And with parameter value 0xFF
            And send to driver
        Then the driver should send switch_multilevel get

        Examples:
          | command |
          | report  |
          | set     |


############################################################
# Central Scene Tests (scene control must be enabled via param 13)
# The upper paddle is Scene 1 and the lower paddle is Scene 2.
############################################################

    @button
    Scenario Outline: Device reports a paddle press via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 1
            And with parameter properties1 <key>
            And with parameter scenenumber <scene>
            And send to driver
        Then the capability but:state.<button> should be PRESSED
            And the driver should place a base:ValueChange message on the platform bus

        Examples:
          | scene | key | button | remarks            |
          | 1     | 0   | up     | upper single tap   |
          | 1     | 3   | up     | upper double tap   |
          | 1     | 2   | up     | upper held         |
          | 2     | 0   | down   | lower single tap   |
          | 2     | 2   | down   | lower held         |

    @button
    Scenario Outline: Device reports a paddle release via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 2
            And with parameter properties1 1
            And with parameter scenenumber <scene>
            And send to driver
        Then the capability but:state.<button> should be RELEASED

        Examples:
          | scene | button |
          | 1     | up     |
          | 2     | down   |


############################################################
# Configuration Report Tests
############################################################

    @config
    Scenario Outline: Paddle orientation report sets swit:inverted
        When the device response with configuration report
            And with parameter param 1
            And with parameter level 1
            And with parameter val1 <value>
            And send to driver
        Then the capability swit:inverted should be <inverted>

        Examples:
          | value | inverted | remarks        |
          | 0     | false    | normal         |
          | 1     | true     | inverted       |
          | 2     | false    | momentary mode |

    @config
    Scenario Outline: LED indicator report sets the indicator attributes
        When the device response with configuration report
            And with parameter param 2
            And with parameter level 1
            And with parameter val1 <value>
            And send to driver
        Then the capability indicator:enabled should be <enabled>

        Examples:
          | value | enabled |
          | 0     | true    |
          | 1     | true    |
          | 2     | false   |


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
