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

    # Scene control (param 13) belongs to the sceneControl device setting. Writing it
    # on connect would undo the user's choice every time the dimmer reconnects.
    @basic @connected @scene
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

    # Firmware 10.20 and later honor the Multilevel Switch Set duration. 0xFF asks for the
    # dimmer's own ramp (parameters 28 and 29), which a duration of 0 (instant) would override.
    @dimmer @ramp
    Scenario Outline: Brightness and on/off changes ask for the dimmer's own ramp
        Given the capability swit:state is <init-state>
            And the capability dim:brightness is 50
        When a base:SetAttributes command with the value of <attr> <value> is placed on the platform bus
        Then the driver should send switch_multilevel set
            And the command payload should be <payload>

        Examples:
          | init-state | attr           | value | payload | remarks                  |
          | OFF        | dim:brightness | 30    | 30,0xFF | brightness turns it on   |
          | OFF        | swit:state     | ON    | 50,0xFF | on at the previous level |
          | ON         | swit:state     | OFF   | 0,0xFF  | off                      |

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
# Repeated ON Tests (skipRepeatOn)
# The ZEN77 reports every change on its own, so an ON it has
# already reported isn't sent again.
############################################################

    @dimmer @repeat
    Scenario Outline: Turning ON a dimmer that has reported it is ON at that brightness only reads the level
        Given the capability dim:brightness is 50
            And the capability swit:state is ON
            And the driver variable targetlevel is 50
            And the driver variable targetstate is ON
        When a base:SetAttributes command with the value of <attribute> <value> is placed on the platform bus
        Then the driver should place a EmptyMessage message on the platform bus
        Then the driver should send switch_multilevel get
        Then protocol bus should be empty
            And there should be no more scheduled events

        Examples:
          | attribute      | value |
          | swit:state     | ON    |
          | dim:brightness | 50    |

    # Only a brightness the dimmer has confirmed is skipped. Its last report is stale when it
    # never confirmed the last command.
    @dimmer @repeat
    Scenario Outline: Turning ON a dimmer that hasn't confirmed that brightness sends it
        Given the capability dim:brightness is 50
            And the capability swit:state is <state>
            And the driver variable targetlevel is <target-level>
            And the driver variable targetstate is <target-state>
        When a base:SetAttributes command with the value of dim:brightness <level> is placed on the platform bus
        Then the driver should send switch_multilevel set
            And with parameter level <level>

        Examples:
          | state | target-level | target-state | level | remarks                            |
          | ON    | 50           | ON           | 60    | a new brightness                   |
          | ON    | 60           | ON           | 50    | reported 50, but the target is 60  |
          | ON    | 50           | OFF          | 50    | reported 50, but the target is OFF |
          | ON    | 60           | ON           | 60    | the target, but reported 50        |
          | OFF   | 50           | ON           | 50    | the target, but reported OFF       |

    # An ON the dimmer hasn't confirmed yet is sent again, so a lost Set can be retried
    @dimmer @repeat
    Scenario: Turning OFF, then ON twice, before the dimmer reports sends every command
        Given the capability dim:brightness is 50
            And the capability swit:state is ON
            And the driver variable targetlevel is 50
            And the driver variable targetstate is ON
        When a base:SetAttributes command with the value of swit:state OFF is placed on the platform bus
        Then the driver should send switch_multilevel set
            And with parameter level 0
        When a base:SetAttributes command with the value of swit:state ON is placed on the platform bus
        Then the driver should send switch_multilevel set
            And with parameter level 50
        When a base:SetAttributes command with the value of swit:state ON is placed on the platform bus
        Then the driver should send switch_multilevel set
            And with parameter level 50

    @dimmer @repeat
    Scenario: Turning ON again after the dimmer reported reaching the brightness sends nothing
        Given the capability dim:brightness is 10
            And the capability swit:state is OFF
            And the driver variable targetlevel is 10
            And the driver variable targetstate is OFF
        When a base:SetAttributes command with the value of dim:brightness 50 is placed on the platform bus
        Then the driver should send switch_multilevel set
            And with parameter level 50
        When the device response with switch_multilevel report
            And with parameter value 50
            And send to driver
        Then the capability swit:state should be ON
        When the capability method base:SetAttributes
            And with capability swit:state is ON
            And with capability dim:brightness is 50
            And send to driver
        Then the driver should send switch_multilevel get
        Then protocol bus should be empty

    # A report can be lost, so a skipped ON reads the level. If the dimmer is really off, the next ON is sent.
    @dimmer @repeat
    Scenario: An ON after a skipped ON finds the dimmer OFF is sent
        Given the capability dim:brightness is 50
            And the capability swit:state is ON
            And the driver variable targetlevel is 50
            And the driver variable targetstate is ON
        When a base:SetAttributes command with the value of swit:state ON is placed on the platform bus
        Then the driver should send switch_multilevel get
        When the device response with switch_multilevel report
            And with parameter value 0
            And send to driver
        Then the capability swit:state should be OFF
        When a base:SetAttributes command with the value of swit:state ON is placed on the platform bus
        Then the driver should send switch_multilevel set
            And with parameter level 50


############################################################
# Central Scene Tests (scene control must be enabled via param 13)
# The ZEN77 reports the upper paddle as Scene 1 and the lower paddle as Scene 2.
############################################################

    @button
    Scenario: Device reports upper paddle single tap via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 1
            And with parameter properties1 0
            And with parameter scenenumber 1
            And send to driver
        Then the capability but:state.up should be PRESSED
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Device reports upper paddle released via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 2
            And with parameter properties1 1
            And with parameter scenenumber 1
            And send to driver
        Then the capability but:state.up should be RELEASED
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Device reports lower paddle single tap via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 3
            And with parameter properties1 0
            And with parameter scenenumber 2
            And send to driver
        Then the capability but:state.down should be PRESSED
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Device reports lower paddle released via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 4
            And with parameter properties1 1
            And with parameter scenenumber 2
            And send to driver
        Then the capability but:state.down should be RELEASED
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Device reports upper paddle double tap via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 5
            And with parameter properties1 3
            And with parameter scenenumber 1
            And send to driver
        Then the capability but:state.up should be PRESSED
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Device reports lower paddle held via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 6
            And with parameter properties1 2
            And with parameter scenenumber 2
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
