@IPCD @Genie @Bridge
Feature: IPCD Genie Aladdin Bridge Driver Test

    These scenarios test the functionality of the IPCD Genie Aladdin Bridge Driver.
    
    Background:
        Given the IPCD_Genie_Aladdin_Bridge_2_5.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform.
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'bridge', 'devota', 'wifi']
            And the message's dev:devtypehint attribute should be Genie Aladdin Controller
            And the message's devadv:drivername attribute should be IPCDGenieAladdinBridge
            And the message's devadv:driverversion attribute should be 2.5
            And the message's devpow:source attribute should be LINE
            And the message's devpow:linecapable attribute should be true
            And the message's devpow:backupbatterycapable attribute should be false
            And the message's bridge:numDevicesSupported attribute should be 3
        Then both busses should be empty


############################################################
# Generic Driver Tests
############################################################

    @basic @added
    Scenario: Make sure any "time of change" attributes are defaulted when the device is first Added
        When the device is added
        Then the capability devpow:sourcechanged should be recent

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set 
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

      Examples:
        | value                      |
        | Controller                 |
        | "Garage Controller"        |
        | "Tom's Garage Controller"  |
        | "Garage & Shed Controller" |


############################################################
# OTA Capability Tests
############################################################

    Scenario: Platform starts OTA
        Given the capability devota:status is IDLE
        When the capability method devota:FirmwareUpdate
            And with capability url is http://www.aladdinconnect.net/ota/iris/update
            And with capability priority is URGENT
            And send to driver
        Then the driver should schedule event DeviceOtaCheckTimeout in 900 seconds
        Then the driver should place a devota:FirmwareUpdateResponse message on the platform bus
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability devota:status should be INPROGRESS
            And the capability devota:lastFailReason should be ""
            And the capability devota:lastAttempt should be recent
            And the capability devota:progressPercent should be 0
        Then the driver should send Download command
            And with parameter url http://www.aladdinconnect.net/ota/iris/update

    Scenario Outline: Bad formatted URLs are rejected
        Given the capability devota:status is IDLE
        When the capability method devota:FirmwareUpdate
            And with capability url is <url>
            And with capability priority is URGENT
            And send to driver
        Then the driver should place a devota:FirmwareUpdateResponse message on the platform bus
            And the capability devota:status should be IDLE
        Then the protocol bus should be empty

      Examples:
        | url                                    | remark           |
        | www.aladdinconnect.net/ota/iris/update | missing protocol |

    Scenario: OTA Rejected if OTA in progress
        Given the capability devota:status is INPROGRESS
            #Arbitrary time to confirm unchanged
            And the capability devota:lastAttempt is Wed Oct 12 00:00:00 EDT 2016
        When the capability method devota:FirmwareUpdate
            And with capability url is http://www.aladdinconnect.net/ota/iris/update
            And with capability priority is URGENT
            And send to driver
        Then the driver should place a devota:FirmwareUpdateResponse message on the platform bus
            And the capability devota:status should be INPROGRESS
            And the capability devota:lastAttempt should be Wed Oct 12 00:00:00 EDT 2016
        Then the protocol bus should be empty

    Scenario: Platform cancels OTA
        Given the capability devota:status is INPROGRESS
        When the capability method devota:FirmwareUpdateCancel
            And send to driver
        Then the driver should place a devota:FirmwareUpdateCancelResponse message on the platform bus
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability devota:status should be IDLE
            And the capability devota:lastFailReason should be Cancelled

    Scenario: Platform tries to cancel OTA with none in progress
        Given the capability devota:status is FAILED
        When the capability method devota:FirmwareUpdateCancel
            And send to driver
        Then the driver should place a devota:FirmwareUpdateCancelResponse message on the platform bus
            And the capability devota:status should be FAILED

    Scenario: OnDownloadComplete
        When the device response with event onDownloadComplete
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability devota:progressPercent should be 50

    Scenario: OnDownloadFailed
        When the device response with event onDownloadFailed
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability devota:status should be FAILED
            And the capability devota:lastFailReason should be Download Failed

    Scenario: Process onUpgrade event if OTA In Progress
        Given the capability devota:status is INPROGRESS
        When the device response with event onUpdate
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability devota:status should be COMPLETED
            And the capability devota:progressPercent should be 100
            And the capability devota:lastFailReason should be ""
        Then both busses should be empty

    Scenario: Process onBoot event if OTA In Progress
        Given the capability devota:status is INPROGRESS
        When the device response with event onBoot
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability devota:status should be COMPLETED
            And the capability devota:progressPercent should be 100
            And the capability devota:lastFailReason should be ""
        Then both busses should be empty

    Scenario: Device offline during OTA checks if rebooting
        Given the capability devota:status is INPROGRESS
        When the device is disconnected
        Then the driver should schedule event OfflineCheck in 60 seconds 

    Scenario: Scheduled Check fails if still offline
        Given the capability devota:status is INPROGRESS
        When event OfflineCheck triggers
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability devota:status should be FAILED
            And the capability devota:lastFailReason should be Offline

    Scenario Outline: Device offline does not do anything when not doing OTA
        Given the capability devota:status is <state>
            And the capability devota:lastFailReason is <reason>
        When the device is disconnected
        Then the capability devota:status should be <state>
            And the capability devota:lastFailReason should be <reason>

      Examples:
        | state     | reason          |
        | IDLE      | ""              |
        | FAILED    | Download Failed |
        | COMPLETED | ""              |
