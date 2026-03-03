@ZWave @Zooz @switch @button
Feature: ZWave Zooz ZEN32 Scene Controller Driver Test

These scenarios test the functionality of the ZWave Zooz ZEN32 Scene Controller driver.
The ZEN32 has a main relay paddle (scene 5) and four small scene buttons (scenes 1-4).

    Background:
    Given the ZW_Zooz_Zen32_SceneController.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform.
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's dev:devtypehint attribute should be Switch
        And the message's devadv:drivername attribute should be ZWZoozZen32SceneControllerDriver
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
        Then the driver should set timeout at 190 minutes

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Device                   |
          | "My Device"              |
          | "Tom's Switch"           |
          | "Bob & Sue's Light"      |


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
# Switch Binary Report Tests (main relay)
############################################################

    @switch
    Scenario: Device reports relay ON via switch binary report
        Given the capability swit:state is OFF
        When the device response with switch_binary report
            And with parameter value -1
            And send to driver
        Then the platform attribute swit:state should change to ON
            And the driver should place a base:ValueChange message on the platform bus

    @switch
    Scenario: Device reports relay OFF via switch binary report
        Given the capability swit:state is ON
        When the device response with switch_binary report
            And with parameter value 0
            And send to driver
        Then the platform attribute swit:state should change to OFF
            And the driver should place a base:ValueChange message on the platform bus

    @switch
    Scenario: Device reports relay ON via basic report
        Given the capability swit:state is OFF
        When the device response with basic report
            And with parameter value -1
            And send to driver
        Then the platform attribute swit:state should change to ON
            And the driver should place a base:ValueChange message on the platform bus

    @switch
    Scenario: Device reports relay OFF via basic report
        Given the capability swit:state is ON
        When the device response with basic report
            And with parameter value 0
            And send to driver
        Then the platform attribute swit:state should change to OFF
            And the driver should place a base:ValueChange message on the platform bus


############################################################
# Central Scene Tests - Small Button 1 (top left, scene 1)
############################################################

    @button
    Scenario: Device reports button 1 single tap via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 1
            And with parameter properties1 0
            And with parameter scenenumber 1
            And send to driver
        Then the capability but:state.button1 should be PRESSED
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Device reports button 1 released via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 2
            And with parameter properties1 1
            And with parameter scenenumber 1
            And send to driver
        Then the capability but:state.button1 should be RELEASED
            And the driver should place a base:ValueChange message on the platform bus


############################################################
# Central Scene Tests - Small Button 2 (top right, scene 2)
############################################################

    @button
    Scenario: Device reports button 2 single tap via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 3
            And with parameter properties1 0
            And with parameter scenenumber 2
            And send to driver
        Then the capability but:state.button2 should be PRESSED
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Device reports button 2 released via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 4
            And with parameter properties1 1
            And with parameter scenenumber 2
            And send to driver
        Then the capability but:state.button2 should be RELEASED
            And the driver should place a base:ValueChange message on the platform bus


############################################################
# Central Scene Tests - Small Button 3 (bottom left, scene 3)
############################################################

    @button
    Scenario: Device reports button 3 single tap via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 5
            And with parameter properties1 0
            And with parameter scenenumber 3
            And send to driver
        Then the capability but:state.button3 should be PRESSED
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Device reports button 3 held via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 6
            And with parameter properties1 2
            And with parameter scenenumber 3
            And send to driver
        Then the capability but:state.button3 should be PRESSED
            And the driver should place a base:ValueChange message on the platform bus


############################################################
# Central Scene Tests - Small Button 4 (bottom right, scene 4)
############################################################

    @button
    Scenario: Device reports button 4 single tap via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 7
            And with parameter properties1 0
            And with parameter scenenumber 4
            And send to driver
        Then the capability but:state.button4 should be PRESSED
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Device reports button 4 double tap via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 8
            And with parameter properties1 3
            And with parameter scenenumber 4
            And send to driver
        Then the capability but:state.button4 should be PRESSED
            And the driver should place a base:ValueChange message on the platform bus


############################################################
# Central Scene Tests - Main Paddle (scene 5)
############################################################

    @button
    Scenario: Device reports main paddle single tap via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 9
            And with parameter properties1 0
            And with parameter scenenumber 5
            And send to driver
        Then the capability but:state.main should be PRESSED
            And the driver should place a base:ValueChange message on the platform bus

    @button
    Scenario: Device reports main paddle released via central scene
        When the device response with central_scene notification
            And with parameter sequencenumber 10
            And with parameter properties1 1
            And with parameter scenenumber 5
            And send to driver
        Then the capability but:state.main should be RELEASED
            And the driver should place a base:ValueChange message on the platform bus
