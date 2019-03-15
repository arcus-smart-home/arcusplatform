@Zigbee @AlertMe @Motion23
Feature: Zigbee AlertMe Motion Sensor Driver Test

    These scenarios test the functionality of the Zigbee AlertMe Motion Sensor driver.

    Background:
        Given the ZB_AlertMe_MotionSensor_2_3.driver has been initialized
            And the device has endpoint 2

    @basic
    Scenario: Driver reports capabilities to platform.
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be [ 'base', 'dev', 'devadv', 'devconn', 'devpow', 'mot', 'temp' ]
            And the message's dev:devtypehint attribute should be Motion
            And the message's devadv:drivername attribute should be ZB_AlertMe_MotionSensor
            And the message's devadv:driverversion attribute should be 2.3
            And the message's devpow:source attribute should be BATTERY
            And the message's devpow:linecapable attribute should be false
            And the message's devpow:backupbatterycapable attribute should be false
            And the message's mot:motion attribute should be NONE
        Then both busses should be empty


############################################################
# Driver Lifecycle Tests
############################################################

    @basic @added
    Scenario: Device added
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the capability mot:motionchanged should be recent


############################################################
# General Driver Tests
############################################################

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set 
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    | remarks                         |
          | Motion                   | name can be set                 |
          | "Living Room"            | name with spaces can be set     |
          | "Tom's Room"             | name with apostrophe can be set |
          | "TV & Game Room"         | name with ampersand can be set  |


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
# Tamper Message Tests
############################################################

    Scenario Outline: process tamper report with cluster 242
        When the device response with 242 <messageId>
            And send to driver

        Examples:
          | messageId |
          | 1         |
          | 0         |

