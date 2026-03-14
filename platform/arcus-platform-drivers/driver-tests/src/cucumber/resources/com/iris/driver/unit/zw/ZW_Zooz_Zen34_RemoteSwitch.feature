@ZWave @Zooz @button
Feature: ZWave Zooz ZEN34 Remote Switch Driver Test

These scenarios test the functionality of the ZWave Zooz ZEN34 Remote Switch driver

    Background:
    Given the ZW_Zooz_Zen34_RemoteSwitch.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform.
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's dev:devtypehint attribute should be Button
        And the message's devadv:drivername attribute should be ZWZoozZen34RemoteSwitchDriver
        And the message's devadv:driverversion attribute should be 2.4
        And the message's devpow:source attribute should be BATTERY
        And the message's devpow:linecapable attribute should be false
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
        Then the driver should set timeout at 37 hours

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Device                   |
          | "My Device"              |
          | "Tom's Remote"           |
          | "Bob & Sue's Switch"     |


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
          | prev_level | level-arg | battery-attr | remarks                            |
          |  50        |  -1       |   0          |                                    |
          |  50        |   0       |  50          | Driver assumes zero is invalid     |
          |  50        |   1       |   1          |                                    |
          |  70        |  50       |  50          |                                    |
          | 100        |  99       |  99          |                                    |
          | 100        | 100       | 100          |                                    |
          |  90        | 101       |  90          | Driver assumes over 100 is invalid |

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
# Central Scene Tests
############################################################

    @button
    Scenario: Device reports upper paddle single tap via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 1
            And with parameter properties1 0
            And with parameter scenenumber 1
            And send to driver
        Then the platform attribute but:state.up should change to PRESSED
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Device reports upper paddle released via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 2
            And with parameter properties1 1
            And with parameter scenenumber 1
            And send to driver
        Then the platform attribute but:state.up should change to RELEASED
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Device reports lower paddle single tap via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 3
            And with parameter properties1 0
            And with parameter scenenumber 2
            And send to driver
        Then the platform attribute but:state.down should change to PRESSED
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Device reports lower paddle released via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 4
            And with parameter properties1 1
            And with parameter scenenumber 2
            And send to driver
        Then the platform attribute but:state.down should change to RELEASED
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Device reports upper paddle double tap via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 5
            And with parameter properties1 3
            And with parameter scenenumber 1
            And send to driver
        Then the platform attribute but:state.up should change to PRESSED
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Device reports lower paddle held via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 6
            And with parameter properties1 2
            And with parameter scenenumber 2
            And send to driver
        Then the platform attribute but:state.down should change to PRESSED
            And the driver should place a base:ValueChange message on the platform bus


############################################################
# Wake Up Tests
############################################################

    Scenario: Device sends wake up notification
        When the device response with wake_up notification
            And send to driver
        Then the driver should send Battery get
            And the driver should send Wake_Up no_more_information
