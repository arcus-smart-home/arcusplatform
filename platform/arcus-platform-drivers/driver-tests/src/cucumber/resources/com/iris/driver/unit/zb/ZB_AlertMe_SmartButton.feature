@Zigbee @AlertMe @Button
Feature: Zigbee AlertMe Smart Button Driver Test

    These scenarios test the functionality of the Zigbee AlertMe Smart Button driver.

    Background:
        Given the ZB_AlertMe_SmartButton.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform.
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be [ 'base', 'dev', 'devadv', 'devconn', 'devpow', 'but', 'temp' ]
            And the message's dev:devtypehint attribute should be Button
            And the message's devadv:drivername attribute should be ZBAlertMeSmartButton
            And the message's devadv:driverversion attribute should be 1.0
        Then both busses should be empty


############################################################
# Driver Lifecycle Tests
############################################################

    @basic @added
    Scenario: Device added
        When the device is added
        Then the capability devpow:source should be BATTERY
            And the capability devpow:linecapable should be false
            And the capability devpow:backupbatterycapable should be false
            And the capability devpow:sourcechanged should be recent
            And the capability but:state should be RELEASED
            And the capability but:statechanged should be recent
        # driver should send Mode Change message when added
        Then the driver should send 0x00F0 0xFA
            And with payload 0, 1
        Then protocol bus should be empty

    @basic @connected @timeout
    Scenario: Device connected
        When the device is connected
        # driver should send Hello message when connected
        Then the driver should send 0x00F6 0xFC
        Then the driver should set timeout at 10 minutes


############################################################
# General Driver Tests
############################################################

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set 
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    | remarks                         |
          | Button                   | name can be set                 |
          | "Light Button"           | name with spaces can be set     |
          | "Tom's Button"           | name with apostrophe can be set |
          | "Bob & Sue's Button"     | name with ampersand can be set  |


############################################################
# Button Tests
############################################################

    # setAttributes('but')
    @button
    Scenario Outline: Setting Device button attributes
        When a base:SetAttributes command with the value of but:state <newState> is placed on the platform bus
        Then the capability but:state should be <newState>

        Examples:
          | newState |
          | PRESSED  |
          | RELEASED |


############################################################
# AlertMe Heartbeat Tests
############################################################

    @heartbeat @signal
    Scenario Outline: Device sends Heartbeat Message with LQI
        Given the capability devconn:signal is 10
        When the device response with 240 251 
            # status(1byte), timer(4bytes), battery(2bytes), temperature(2bytes), RSSI(1byte), LQI(1byte), Switch Mask(1byte), Switch State(1byte)
            And with payload 8, 0,0,0,0, 0,0, 0,0, 0, <lqi>, 0, 0
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the message's devconn:signal attribute should be <signal>
        # should send Stop Polling after heartbeat received
        Then the driver should send 0x00F0 0xFD

        Examples:
          | lqi  | signal | remarks        |
          | 0x00 |      0 |   0/255 =   0% |
          | 0x03 |      1 |   3/255 =   1% |
          | 0x7F |     49 | 127/255 =  49% |
          | 0x80 |     50 | 128/255 =  50% |
          | 0xFF |    100 | 255/255 = 100% |


    @heartbeat @battery
    Scenario Outline: Device sends Heartbeat Message with Battery Level
        Given the capability devpow:battery is 90
        When the device response with 240 251
            # status(1byte), timer(4bytes), battery(2bytes), temperature(2bytes), RSSI(1byte), LQI(1byte), Switch Mask(1byte), Switch State(1byte)
            And with payload 1, 0,0,0,0, <lsb>,<msb>, 0,0, 0, 0, 0, 0
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the message's devpow:battery attribute should be <pcnt>
        # should send Stop Polling after heartbeat received
        Then the driver should send 0x00F0 0xFD

        Examples:
          | msb  | lsb  | pcnt | remarks                             |
          | 0x08 | 0x34 |    0 | 2100 mV =   0% of 2.1V - 3.0V range |
          | 0x09 | 0xF6 |   50 | 2550 mV =  50% of 2.1V - 3.0V range |
          | 0x0B | 0xB8 |  100 | 3000 mV = 100% of 2.1V - 3.0V range |
          | 0x07 | 0xD0 |    0 | 2000 mV =   0% of 2.1V - 3.0V range |
          | 0x0C | 0x1C |  100 | 3100 mV = 100% of 2.1V - 3.0V range |

    @heartbeat @temperature
    Scenario Outline: Device sends Heartbeat Message with Temperature
        Given the capability temp:temperature is 20
        When the device response with 240 251
            # status(1byte), timer(4bytes), battery(2bytes), temperature(2bytes), RSSI(1byte), LQI(1byte), Switch Mask(1byte), Switch State(1byte)
            And with payload 2, 0,0,0,0, 0,0, <lsb>,<msb>, 0, 0, 0, 0
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the message's temp:temperature attribute should be <temp>
            And the capability temp:temperature should be <temp>
        # should send Stop Polling after heartbeat received
        Then the driver should send 0x00F0 0xFD

        Examples:
          | msb  | lsb  | temp    | remarks                |
          | 0x01 | 0x60 | 22.0    | 0x016 = 22, 0 = 0      |
          | 0x01 | 0x61 | 22.0625 | 0x016 = 22, 1 = 0.0625 |
          | 0x01 | 0x62 | 22.125  | 0x016 = 22, 2 = 0.125  |
          | 0x00 | 0xF8 | 15.5    | 0x00F = 15, 8 = 0.5    |
          | 0x02 | 0x0F | 32.9375 | 0x020 = 32, F = 0.9375 |


############################################################
# AlertMe Button Cluster Tests
############################################################

    Scenario Outline: Device sends Button Cluster 0xF3 message
        When the device response with 0xF3 <msgId>
            And send to driver
        Then the capability but:state should be <buttonState>
            And the capability but:statechanged should be recent

        Examples:
          | msgId | buttonState | remarks         |
          | 1     | PRESSED     | button PRESSED  |
          | 0     | RELEASED    | button RELEASED |


############################################################
# AlertMe Hello Response Tests
############################################################

# TODO: Add support for Cluster 246
# 20:47:31.732 [main] WARN  c.i.d.u.c.zb.ZigbeeCommandBuilder - Unsupported Zigbee cluster 246
# 20:47:31.732 [main] DEBUG c.i.d.u.c.zb.ZigbeeCommandBuilder - Look under AlertMe
    @helloResponse
    Scenario: Device sends Hello Response with device info
        When the device response with 0xF6 0xFE
            # Node:0x000A, MfgId:0x0123, DvcType:0x4455, AppRel:0x03, AppVer:1.2, HW Ver:3.4
            And with payload 0x0A,0x00, 0,0,0,0,0,0,0,0,0x23,0x01,0x55,0x44,0x03,0x12,0x04,0x03
            And send to driver
        Then the capability devadv:firmwareVersion should be 1.2.0.3

