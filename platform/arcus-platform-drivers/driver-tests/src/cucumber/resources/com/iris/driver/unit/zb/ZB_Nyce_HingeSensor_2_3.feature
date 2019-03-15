@Zigbee @NYCE @Contact
Feature: Test of the NYCE Hinge Sensor driver

    These scenarios test the functionality of the NYCE Hinge Sensor driver.

    Background:
    Given the ZB_Nyce_HingeSensor_2_3.driver has been initialized
        And the device has endpoint 1

    @basic
    Scenario: Driver reports capabilities to platform.
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be [ 'base', 'dev', 'devadv', 'devconn', 'devpow', 'cont' ]
            And the message's dev:devtypehint attribute should be Contact
            And the message's devadv:drivername attribute should be ZBNyceHingeSensor 
            And the message's devadv:driverversion attribute should be 2.3
            And the message's devpow:source attribute should be BATTERY
            And the message's devpow:linecapable attribute should be false
            And the message's devpow:backupbatterycapable attribute should be false
            And the message's cont:contact attribute should be CLOSED
            And the message's cont:usehint attribute should be DOOR
        Then both busses should be empty


############################################################
# General Driver Tests
############################################################

    @basic @added
    Scenario: Device added
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the capability cont:contactchanged should be recent

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set 
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Contact                  |
          | "Front Door"             |
          | "Bob's Room"             |
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
    Scenario: iaszone zone enroll request
         When the device response with iaszone zoneenrollrequest 
          And with parameter zoneType 1
          And with parameter manufacturerCode 1
          And send to driver
         Then the driver should send iaszone zoneEnrollResponse
          And with parameter enrollResponseCode 0x00
          And with parameter zoneId 0x01
    

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
# Power Cluster Tests
############################################################

    @Power
    Scenario Outline: Power Read / Report
        Given the capability devpow:battery is 90
        When the device response with power <messageType> 
            And with parameter ATTR_BATTERY_VOLTAGE <voltage>
            And with parameter ATTR_BATTERY_VOLTAGE_MIN_THRESHOLD 21
            And send to driver
        # Then the capability devpow:battery should be <battery>
        Then the driver should place a base:ValueChange message on the platform bus
            And the message's devpow:battery attribute numeric value should be within delta 1.1 of <battery>
    
    Examples:
      | messageType               | voltage | battery |
      | zclreadattributesresponse | 20      | 0       |
      | zclreadattributesresponse | 21      | 0       |
      | zclreportattributes       | 25      | 44      |
      | zclreportattributes       | 30      | 100     |
      | zclreportattributes       | 31      | 100     |


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
      | 0x0500  | 0x0B    | CLUSTER_IAS_ZONE,     CMD_DFLT_RSP     |
      | 0x0500  | 0x04    | CLUSTER_IAS_ZONE,     CMD_WRT_ATTR_RSP |


