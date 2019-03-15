@Zigbee @Sylvania @Smart @bulb @FlexIndoor
Feature: Test of the Sylvania Smart+ Flex Indoor RGBW Light Strip driver
    
    These scenarios test the functionality of the Sylvania Smart+ Flex Indoor RGBW Light Strip driver.

    Background:
        Given the ZB_Sylvania_Smart_Flex_Indoor_RGBW_2_10.driver has been initialized
        And the device has endpoint 1
    
    
    Scenario: Driver reports capabilities to platform.
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devconn', 'devpow', 'dim', 'swit', 'light', 'colortemp', 'color', 'devota', 'ident']
            And the message's dev:devtypehint attribute should be Light
            And the message's devadv:drivername attribute should be ZBSylvaniaSmartFlexIndoorRGBWStrip 
            And the message's devadv:driverversion attribute should be 2.10
            And the message's devpow:source attribute should be LINE
            And the message's devpow:linecapable attribute should be true        
            And the message's devpow:backupbatterycapable attribute should be false        
            And the message's swit:state attribute should be ON        
            And the message's dim:brightness attribute should be 100        
            And the message's light:colormode attribute should be COLORTEMP        
            And the message's colortemp:colortemp attribute should be 2700        
            And the message's colortemp:mincolortemp attribute should be 2700        
            And the message's colortemp:maxcolortemp attribute should be 6500        
            And the message's color:hue attribute should be 120        
            And the message's color:saturation attribute should be 100        
        Then both busses should be empty


############################################################
# General Driver Tests
############################################################

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set 
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Light                    |
          | "Front Room"             |
          | "Mom & Dad's Room"       |


############################################################
# Driver Lifecycle Tests
############################################################

    @basic @added
    Scenario: Device added
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the capability swit:statechanged should be recent

    Scenario: Device connected for first time
        When the device is connected
        Then the driver should set timeout
        # driver restores bulb to default level
        Then the driver should send level moveToLevel
    
    @reconnect
    Scenario: Device disconnected while OFF should be restored to OFF state when reconnected
        Given the capability swit:state is OFF
        When the device is connected
        Then the driver should set timeout
        # driver restores bulb to default level
        Then the driver should send onoff OFF


############################################################
# Dimmer Tests
############################################################

    # Setting just the Dimmer.brightness to 1-100 will adjust the brightness to that value and turn the device ON, whatever state it was previously in
    @dimProc
    Scenario Outline: Client sets brightness attribute of 1-100, and no switch state
        Given the capability dim:brightness is <init-brightness>
            And the capability swit:state is <init-state>
            And the driver variable targetLevel is <init-brightness>
            And the driver variable targetState is <init-state>
        When a base:SetAttributes command with the value of dim:brightness <to-brightness> is placed on the platform bus
        Then the driver should send level moveToLevel
        Then protocol bus should be empty
        Then the driver variable targetLevel should be <target-brightness>
        Then the driver variable targetState should be ON
        
        Examples:
          | init-brightness | init-state | to-brightness | target-brightness | remarks                           |
          | 10              | ON         | 50            | 50                | adjust ON device to level 50      |
          | 10              | OFF        | 50            | 50                | turn OFF device to ON at level 50 |
    
    # Setting just the Switch.state to OFF, will turn the device off and leave the brightness setting at whatever brightness it was previously
    @dimProc
    Scenario Outline: Client sets swit:state to OFF with no dim:brightness setting
        Given the capability dim:brightness is <init-brightness>
            And the capability swit:state is <init-state>
            And the driver variable targetLevel is <init-brightness>
            And the driver variable targetState is <init-state>
        When a base:SetAttributes command with the value of swit:state OFF is placed on the platform bus
        Then the driver should send onoff off
        Then protocol bus should be empty
        Then the driver variable targetLevel should be <init-brightness>
        Then the driver variable targetState should be OFF
    
        Examples:
          | init-brightness | init-state | remarks                                         |
          | 10              | OFF        | turn OFF device to OFF (to make sure it is OFF) |
          | 10              | ON         | turn ON device to OFF                           |
    
    # Setting just the Switch.state to ON, will turn the device on at the current Dimmer.brightness (which should be non-zero, but if it is somehow zero then the brightness will default to 100)
    @dimProc
    Scenario Outline: Client sets swit:state to ON with no dim:brightness setting
        Given the capability dim:brightness is <init-brightness>
            And the capability swit:state is <init-state>
            And the driver variable targetLevel is <init-brightness>
            And the driver variable targetState is <init-state>
        When a base:SetAttributes command with the value of swit:state ON is placed on the platform bus
        Then the driver should send level moveToLevel
        Then protocol bus should be empty
        Then the driver variable targetLevel should be <target-brightness>
        Then the driver variable targetState should be ON
    
        Examples:
          | init-brightness | init-state | target-brightness | remarks                                      |
          | 10              | OFF        | 10                | turn OFF device to ON at prev level          |
          | 10              | ON         | 10                | turn ON device to ON (to make sure it is ON) |
    
    # Setting the Switch.state to OFF and Dimmer.brightness to 1-100 will turn the device OFF and set the driver brightness to the specified brightness so that value is used as the default when turned back ON
    @dimProc
    Scenario Outline: Client sets swit:state to OFF with a dim:brightness
        Given the capability dim:brightness is <init-brightness>
            And the capability swit:state is <init-state>
            And the driver variable targetLevel is <init-brightness>
            And the driver variable targetState is <init-state>
        When the capability method base:SetAttributes
             And with capability swit:state is OFF
             And with capability dim:brightness is <to-brightness>
             And send to driver
        Then the driver should send onoff off
        Then protocol bus should be empty
        Then the driver variable targetLevel should be <target-brightness>
        Then the driver variable targetState should be OFF
    
        Examples:
          | init-brightness | init-state | to-brightness | target-brightness | remarks                                          |
          | 100             | OFF        | 50            | 50                | turn OFF device to OFF with new default ON level |
          | 100             | ON         | 50            | 50                | turn ON device to OFF with new default ON level  |
    
    # Setting the Switch.state to ON and Dimmer.brightness to 1-100 will set the device to the specified brightness first and then turn the device ON (after a short delay)
    @dimProc
    Scenario Outline: Client sets swit:state to ON with a dim:brightness
        Given the capability dim:brightness is <init-brightness>
            And the capability swit:state is <init-state>
            And the driver variable targetLevel is <init-brightness>
            And the driver variable targetState is <init-state>
        When the capability method base:SetAttributes
             And with capability swit:state is ON
             And with capability dim:brightness is <to-brightness>
             And send to driver
        Then the driver should send level moveToLevel
        Then protocol bus should be empty
        Then the driver variable targetLevel should be <target-brightness>
        Then the driver variable targetState should be ON
    
        Examples:
          | init-brightness | init-state | to-brightness | target-brightness | remarks                              |
          | 10              | OFF        | 50            | 50                | turn OFF device to ON at a new level |
          | 10              | ON         | 50            | 50                | turn ON device to ON at a new level  |
    
    
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

    
############################################################
# Identify Cluster Tests
############################################################

    Scenario: identify
        When the capability method ident:Identify
            And send to driver
        Then the driver should send identify identifyCmd    


############################################################
# ZigBee Ack/Response Message Tests
############################################################

    Scenario Outline: default ZigbeeMessage processing
        When the device response with <cluster> <command>
            And send to driver

        Examples:
          | cluster | command | remarks                                      |
          | 0x0006  | 0x07    | CLUSTER_ON_OFF,        CMD_CNFG_RPT_RSP      |
          | 0x0006  | 0x09    | CLUSTER_ON_OFF,        CMD_READ_RPT_CNFG_RSP |
          | 0x0008  | 0x07    | CLUSTER_LEVEL_CONTROL, CMD_CNFG_RPT_RSP      |
          | 0x0008  | 0x09    | CLUSTER_LEVEL_CONTROL, CMD_READ_RPT_CNFG_RSP |
          | 0x0300  | 0x07    | CLUSTER_COLOR_CONTROL, CMD_CNFG_RPT_RSP      |
          | 0x0300  | 0x09    | CLUSTER_COLOR_CONTROL, CMD_READ_RPT_CNFG_RSP |
          | 0x0001  | 0x07    | CLUSTER_IDENTIFY,      CMD_RPT_ATTR          |
          | 0x0B05  | 0x07    | CLUSTER_DIAGNOSTICS,   CMD_CNFG_RPT_RSP      |

