@ZWave @Ring @motion
Feature: ZWave Ring Alarm Motion Detector Driver Test

These scenarios test the functionality of the ZWave Ring Alarm Motion Detector driver

    Background:
    Given the ZW_Ring_MotionSensor.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform.
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's dev:devtypehint attribute should be Motion
        And the message's devadv:drivername attribute should be ZWRingMotionSensor
        And the message's devadv:driverversion attribute should be 1.0
        And the message's devpow:source attribute should be BATTERY
        And the message's devpow:linecapable attribute should be false
        And the message's devpow:backupbatterycapable attribute should be false
        And the message's mot:motion attribute should be NONE
    Then both busses should be empty


############################################################
# Generic Driver Tests
############################################################

    @basic @added
    Scenario: Make sure any "time of change" attributes are defaulted when the device is first Added
        When the device is added
        Then the capability mot:motionchanged should be recent

    @basic @connected @timeout
    Scenario: Make sure the offline timeout is set each time the device Connects
        When the device is connected
        Then the driver should set timeout at 60 minutes

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Device                   |
          | "My Device"              |
          | "Tom's Sensor"           |
          | "Bob & Sue's Motion"     |


############################################################
# Generic ZWave Driver Tests
############################################################

    Scenario: Device reports battery level
        Given the capability devpow:battery is 50
        When the device response with battery report
            And with parameter level 75
            And send to driver
        Then the platform attribute devpow:battery should change to 75
            And the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty


############################################################
# Motion Detection Tests via Alarm/Notification Reports
############################################################

    @motion
    Scenario: Motion detected via notification report
        Given the capability mot:motion is NONE
        When the device response with alarm report
            And with parameter notificationtype 7
            And with parameter event 8
            And send to driver
        Then the platform attribute mot:motion should change to DETECTED
            And the capability mot:motionchanged should be recent
            And the driver should place a base:ValueChange message on the platform bus

    @motion
    Scenario: Motion idle via notification report
        Given the capability mot:motion is DETECTED
        When the device response with alarm report
            And with parameter notificationtype 7
            And with parameter event 0
            And send to driver
        Then the platform attribute mot:motion should change to NONE
            And the capability mot:motionchanged should be recent
            And the driver should place a base:ValueChange message on the platform bus
