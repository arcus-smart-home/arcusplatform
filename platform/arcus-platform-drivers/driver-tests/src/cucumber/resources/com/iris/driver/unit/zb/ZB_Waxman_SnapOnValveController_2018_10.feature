@Zigbee @Waxman @Valve @Snapon
Feature: Test of the Waxman Snap-On valve controller driver

    These scenarios test the functionality of the Waxman Snap-On valve controller.

    Background:
        Given the ZB_Waxman_SnapOnValveController_2018_10.driver has been initialized
            And the device has endpoint 1 

    @basic
    Scenario: Driver reports capabilities to platform.
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be [ 'base', 'dev', 'devadv', 'devconn', 'devpow', 'valv', 'devota', 'ident' ]
            And the message's dev:devtypehint attribute should be Water Valve
            And the message's devadv:drivername attribute should be ZBWaxmanSnapOnValveController 
            And the message's devadv:driverversion attribute should be 2018.10
            And the message's devpow:source attribute should be LINE
            And the message's devpow:linecapable attribute should be true
            And the message's devpow:backupbatterycapable attribute should be false
            And the message's valv:valvestate attribute should be OPEN
        Then both busses should be empty


############################################################
# General Driver Tests
############################################################

    @basic @added
    Scenario: Device added
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the capability valv:valvestatechanged should be recent

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set 
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

    Examples:
      | value                    |
      | Water                    |
      | "Bathroom Sink"          |
      | "Sue's Sink"             |
      | "Mom & Dad's Bathroom"   |




############################################################
# Power Cluster Tests
############################################################

    @Power
    Scenario Outline: Power Read / Report
        Given the capability devpow:battery is 90
        When the device response with power <messageType> 
            And with parameter ATTR_BATTERY_VOLTAGE <voltage>
            And with parameter ATTR_BATTERY_VOLTAGE_MIN_THRESHOLD 41
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the message's devpow:battery attribute numeric value should be within delta 1.1 of <battery>
    
    Examples:
      | messageType               | voltage | battery |
      | zclreadattributesresponse | 0       | 0       |
      | zclreadattributesresponse | 41      | 0       |
      | zclreportattributes       | 50      | 47      |
      | zclreportattributes       | 60      | 100     |
      | zclreportattributes       | 61      | 100     |


############################################################
# Poll Control Cluster Tests
############################################################

    @Poll
    Scenario Outline: poll control read / report
        When the device response with pollcontrol <messageType>
        And with parameter ATTR_CHECKIN_INTERVAL 1
        And with parameter ATTR_LONG_POLL_INTERVAL 1
        And with parameter ATTR_SHORT_POLL_INTERVAL 1
        And send to driver

    Examples:
      | messageType               |
      | zclreadattributesresponse |
      | zclreportattributes       |

    @Poll
    Scenario: poll control check in
        When the device response with pollcontrol checkin
            And send to driver
        Then the driver should send pollcontrol checkinresponse    
    
############################################################
# OTA Cluster Tests
############################################################

    @OTA
    Scenario: OTA read response
        Given the capability devota:targetVersion is 1
        When the device response with ota zclreadattributesresponse
            And with parameter ATTR_CURRENT_FILE_VERSION 1
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability devota:currentVersion should be 1
            And the capability devota:status should be COMPLETED
    
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
      | cluster | command | remarks                                |
      | 0x0001  | 0x07    | CLUSTER_PWR_CNFG,     CMD_CNFG_RPT_RSP |
      | 0x0020  | 0x04    | CLUSTER_POLL_CONTROL, CMD_WRT_ATTR_RSP |
      | 0x0020  | 0x0B    | CLUSTER_POLL_CONTROL, CMD_DFLT_RSP     |
      | 0x0402  | 0x07    | CLUSTER_TEMPERATURE,  CMD_CNFG_RPT_RSP |


############################################################
# Valve Message Tests
############################################################

    @onoff
    Scenario Outline: setAttributes for valv
        When the capability method base:SetAttributes 
        And with capability valv:valvestate is <valvestate>
        And send to driver
        Then the driver should place a EmptyMessage message on the platform bus
        Then the driver should send onoff <messageID>
        And the driver should place a base:ValueChange message on the platform bus

    Examples:        
      | valvestate | messageID |
      | OPEN       | 1         |
      | CLOSED     | 0         |

    @onoff
    Scenario Outline: Device sending onoff response
        Given the driver variable obstructed is false
            And the capability valv:valvestate is <previous>
        When the device response with onoff <responseType>
            And with parameter ATTR_ONOFF <on_off>
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability valv:valvestate should be <new>

    Examples:
      | responseType              | previous | on_off | new    | 
      | zclreadattributesresponse | OPEN     | 0      | CLOSED | 
      | zclreportattributes       | OPEN     | 0      | CLOSED | 
      | zclreadattributesresponse | CLOSED   | 1      | OPEN   | 
      | zclreportattributes       | CLOSED   | 1      | OPEN   | 

