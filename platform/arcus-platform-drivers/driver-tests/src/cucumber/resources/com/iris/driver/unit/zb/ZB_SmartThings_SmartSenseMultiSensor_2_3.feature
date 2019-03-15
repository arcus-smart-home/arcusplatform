@Zigbee @SmartThings @Contact @SmartThingsMulti23
Feature: Test of the SmartThings Smart Sense Multi Sensor ZigBee driver

    These scenarios test the functionality of the SmartThings Smart Sense Multi Sensor ZigBee driver.

    Background:
        Given the ZB_SmartThings_SmartSenseMultiSensor_2_3.driver has been initialized
            And the device has endpoint 1

    @basic
    Scenario: Driver reports capabilities to platform.
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be [ 'base', 'dev', 'devadv', 'devconn', 'devpow', 'cont', 'tilt', 'devota' ]
            And the message's dev:devtypehint attribute should be Contact
            And the message's devadv:drivername attribute should be ZBSmartThingsMultiSensor
            And the message's devadv:driverversion attribute should be 2.3
            And the message's devpow:source attribute should be BATTERY
            And the message's devpow:linecapable attribute should be false
            And the message's devpow:backupbatterycapable attribute should be false
            And the message's cont:contact attribute should be CLOSED
            And the message's cont:usehint attribute should be UNKNOWN
            And the message's tilt:tiltstate attribute should be FLAT
        Then both busses should be empty

############################################################
# General Driver Tests
############################################################

    @basic @added
    Scenario: Device added
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the capability cont:contactchanged should be recent
            And the capability tilt:tiltstatechanged should be recent

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set 
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Contact                  |
          | "Front Door"             |
          | "Back Window"            |
          | "Mom & Dad's Room"       |


############################################################
# Contact Capability Tests
############################################################

    Scenario Outline: Make sure driver allows Contact Use Hint to be set 
        When a base:SetAttributes command with the value of cont:usehint <value> is placed on the platform bus
        Then the platform attribute cont:usehint should change to <value>

        Examples:
          | value   |
          | DOOR    |
          | WINDOW  |
          | OTHER   |


############################################################
# IAS Zone Cluster Tests
############################################################

    @IASZone
    Scenario Outline: Zone attribute reading and reporting
        Given the capability cont:contact is <prevState>
         When the device response with iaszone <messageType> 
          And with parameter ATTR_ZONE_STATUS <zoneStatus>
          And send to driver
         Then the capability cont:contact should be <contact> 
          And the capability cont:contactchanged should be recent 
          And the driver should place a base:ValueChange message on the platform bus 

    Examples:
    | messageType               | prevState | zoneStatus | contact |
    | zclreadattributesresponse | OPENED    | 0          | CLOSED  |
    | zclreportattributes       | OPENED    | 0          | CLOSED  |
    | zclreadattributesresponse | CLOSED    | 1          | OPENED  |
    | zclreportattributes       | CLOSED    | 1          | OPENED  |
    
    
    @IASZone
    Scenario Outline: iaszone zone status change notification
        Given the capability cont:contact is <prevState>
         When the device response with iaszone zonestatuschangenotification 
          And with parameter zoneStatus <zoneStatus>
          And send to driver
         Then the capability cont:contact should be <contact> 
          And the capability cont:contactchanged should be recent 
          And the driver should place a base:ValueChange message on the platform bus 

    Examples:
    | messageType                  | prevState | zoneStatus | contact |
    | zonestatuschangenotification | OPENED    | 0          | CLOSED  |
    | zonestatuschangenotification | CLOSED    | 1          | OPENED  |
    

############################################################
# OTA Cluster Tests
############################################################

    # ota.zclreadattributesresponse
    @OTA
    Scenario: OTA read response
        Given the capability devota:targetVersion is 1
        When the device response with ota zclreadattributesresponse
            And with parameter ATTR_CURRENT_FILE_VERSION 1
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability devota:currentVersion should be 1
            And the capability devota:status should be COMPLETED
    
    # ota.querynextimagerequest
    @OTA
    Scenario: OTA query next image
        Given the capability devota:targetVersion is 1
        When the device response with ota querynextimagerequest
            And with parameter manufacturerCode 1
            And with parameter imageType 1
            And with parameter fileVersion 1
            And with header flags 1
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability devota:currentVersion should be 1
            And the capability devota:status should be COMPLETED
    
    #ota.imageblockrequest / imagePageRequest
    @OTA
    Scenario Outline: OTA image block / page
        Given the capability devota:status is IDLE
        When the device response with ota <messageType>
            And with parameter fileVersion 1
            And with parameter fileOffset 0
            And with header flags 1
            And send to driver 
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability devota:targetVersion should be 1
            And the capability devota:status should be INPROGRESS
    
    Examples:
      | messageType       |
      | imageblockrequest |
      | imagePageRequest  |
    
    
    # ota.upgradeendrequest
    @OTA
    Scenario Outline: OTA upgrade end request
        When the device response with ota upgradeendrequest
            And with parameter status <status>
            And with parameter manufacturerCode 0
            And with parameter imageType 0
            And with parameter fileVersion 0
            And with header flags 1
            And send to driver 
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability devota:status should be <result>

    Examples:
      | status | result    |
      |    0   | COMPLETED |
      |   -1   | FAILED    |


