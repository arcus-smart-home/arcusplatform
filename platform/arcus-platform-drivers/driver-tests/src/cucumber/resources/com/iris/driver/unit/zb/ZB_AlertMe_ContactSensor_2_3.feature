@Zigbee @AlertMe @Contact
Feature: Zigbee AlertMe Contact Sensor Driver Test

    These scenarios test the functionality of the Zigbee AlertMe Contact Sensor driver.
    
    Background:
        Given the ZB_AlertMe_ContactSensor_2_3.driver has been initialized
            And the device has endpoint 2
    

    @basic
    Scenario: Driver reports capabilities to platform.
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be [ 'base', 'dev', 'devadv', 'devconn', 'devpow', 'cont', 'temp' ]
            And the message's dev:devtypehint attribute should be Contact
            And the message's devadv:drivername attribute should be ZBAlertMeContactSensor 
            And the message's devadv:driverversion attribute should be 2.3
            And the message's devpow:source attribute should be BATTERY
            And the message's devpow:linecapable attribute should be false
            And the message's devpow:backupbatterycapable attribute should be false
            And the message's cont:contact attribute should be CLOSED
            And the message's cont:usehint attribute should be UNKNOWN
        Then both busses should be empty


############################################################
# General Driver Tests
############################################################

    @basic @added
    Scenario: Device added
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the capability cont:contactchanged should be recent
        # driver should send Hello Request
# test harness no longer seems to support these steps (maybe because inside reflex now)
#        Then the driver should send 0x00F6 0xFC
        # driver should send a Mode Change to NORMAL, with flag Set HNF
#        Then the driver should send 0x00F0 0xFA
#         And with payload 0, 1

    @basic @connected
    Scenario: Device connected
        When the device is connected
        # driver should send a Stop Polling
# test harness no longer seems to support this step (maybe because inside reflex now)
#        Then the driver should send 0x00F0 0xFD

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

    @UseHint
    Scenario Outline: Make sure driver allows Contact Use Hint to be set 
        When a base:SetAttributes command with the value of cont:usehint <value> is placed on the platform bus
        Then the platform attribute cont:usehint should change to <value>

        Examples:
          | value   |
          | DOOR    |
          | WINDOW  |
          | OTHER   |

############################################################
# AlertMe Hello Response Tests
############################################################

# 20:47:31.732 [main] WARN  c.i.d.u.c.zb.ZigbeeCommandBuilder - Unsupported Zigbee cluster 246
# 20:47:31.732 [main] DEBUG c.i.d.u.c.zb.ZigbeeCommandBuilder - Look under AlertMe
    @helloResponse
    Scenario: Device sends Hello Response with device info
        When the device response with 0xF6 0xFE
            # Node:0x000A, MfgId:0x0123, DvcType:0x4455, AppRel:0x03, AppVer:1.2, HW Ver:3.4
            And with payload 0x0A,0x00, 0,0,0,0,0,0,0,0,0x23,0x01,0x55,0x44,0x03,0x12,0x04,0x03
            And send to driver
        Then the capability devadv:firmwareVersion should be 1.2.0.3


############################################################
# IAS Zone Cluster Tests
############################################################

# requires test harness to send message with profile 0xC216, not 0x0104
@Ignore        
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
    
    
# requires test harness to send message with profile 0xC216, not 0x0104
@Ignore        
    @IASZone
    Scenario: iaszone zone enroll request
         When the device response with iaszone zoneenrollrequest 
          And with parameter zoneType 1
          And with parameter manufacturerCode 1
          And send to driver
         Then the driver should send iaszone zoneEnrollResponse
          And with parameter enrollResponseCode 0x00
          And with parameter zoneId 0xFF
    

# requires test harness to send message with profile 0xC216, not 0x0104
@Ignore        
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
# Lifesign Tests
############################################################

# requires test harness to send message with profile 0xC216, not 0x0104
@Ignore
    @Lifesign
    Scenario: process battery, temperature, LQI and contact report with cluster 240
       Given the capability cont:contact is CLOSED
        When the device response with 240 251
         And with payload 15, 0, 0, 0, 0,  0, 11, 48, 2, 0,  100, 3, 1
         And send to driver
        Then the capability devpow:battery should be 79
         And the capability temp:temperature should be 35.0
         And the capability devconn:signal should be 39
         And the capability cont:contact should be OPENED
         And the capability cont:contactchanged should be recent
         And the driver should place a base:ValueChange message on the platform bus
    
