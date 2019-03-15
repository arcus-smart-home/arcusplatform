@Zigbee @NYCE @Tilt
Feature: Test of the Nyce Tilt Sensor driver

    These scenarios test the functionality of the Nyce Tilt Sensor driver.

    Background:
        Given the ZB_Nyce_TiltSensor.driver has been initialized
            And the device has endpoint 1

    @basic
    Scenario: Driver reports capabilities to platform.
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be [ 'base', 'dev', 'devadv', 'devconn', 'devpow', 'tilt', 'cont' ]
            And the message's dev:devtypehint attribute should be Tilt
            And the message's devadv:drivername attribute should be ZBNyceTiltSensor 
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
            And the capability tilt:tiltstate should be UPRIGHT
            And the capability tilt:tiltstatechanged should be recent
            And the capability cont:usehint should be UNKNOWN
            And the capability cont:contact should be OPENED
            And the capability cont:contactchanged should be recent
        # driver should set poll control intervals for config
        Then the driver should send pollcontrol setLongPollInterval
        Then the driver should send pollcontrol setShortPollInterval
        # driver should try to write IAS CIE Address when device is added
        Then the driver should send IasZone zclWriteAttributes


    @basic @connected @timeout
    Scenario: Device connected
        When the device is connected
        # driver should always set offline timeout first
        Then the driver should set timeout at 135 minutes
        # driver should set poll control intervals for config
        Then the driver should send pollcontrol setLongPollInterval
        Then the driver should send pollcontrol setShortPollInterval
        # driver should read current values from device
        Then the driver should send basic zclReadAttributes
        Then the driver should send power zclReadAttributes
        Then the driver should send IasZone zclReadAttributes
        # driver should configure reporting
        Then the driver should send power 0x06
        Then the driver should send PollControl 0x02
        # driver should restore poll control intervals after config
        Then the driver should send pollcontrol setLongPollInterval
        Then the driver should send pollcontrol setShortPollInterval
        Then the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty 


############################################################
# General Driver Tests
############################################################

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set 
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    | remarks                         |
          | Door                     | name can be set                 |
          | "Garage Door"            | name with spaces can be set     |
          | "Sue's Jewelry Box"      | name with apostrophe can be set |
          | "TV & Games Cabinet"     | name with ampersand can be set  |


############################################################
# ZigBee IAS Zone Cluster Tests
############################################################

    # onEvent doWriteIasCie
    @IASZone
     Scenario: doWriteIasCie 
         Given the driver variable writeIasCieCnt is 0
        When event doWriteIasCie trigger 
        Then the driver should send IasZone zclWriteAttributes

    # onEvent doZoneEnroll
    @IASZone
    Scenario: doZoneEnroll
         Given the driver variable zoneEnrollCnt is 0
        When event doZoneEnroll trigger 
        Then the driver should send IasZone zoneEnrollResponse
            And with payload 0, 0xFF

    # iaszone.zclwriteattributesresponse
    @IASZone
    Scenario Outline: Zone write attribute response
        When the device response with iaszone zclwriteattributesresponse
            And with payload <payload>
            And send to driver
            
        Examples:
          | payload |
          | -1      |
          | 0       |

    # onZigbeeMessage.Zcl.iaszone.zoneenrollrequest
    @IASZone
    Scenario: IAS Zone enroll request
        When the device response with iaszone zoneenrollrequest 
            And with parameter zoneType 1
            And with parameter manufacturerCode 1
            And send to driver
        Then the driver should send iaszone zoneEnrollResponse
            And with parameter enrollResponseCode 0x00
            And with parameter zoneId 0x00


    # onZigbeeMessage.Zcl.iaszone.zclreadattributesresponse
    # onZigbeeMessage.Zcl.iaszone.zclreportattributes
    @IASZone
    Scenario Outline: IAS Zone read/report attribute
        Given the capability tilt:tiltstate is <prevstate>
            And the capability base:tags is [ <tags> ]
        When the device response with iaszone <messageType> 
            And with parameter ATTR_ZONE_STATUS <zoneStatus>
            And send to driver
        Then the capability tilt:tiltstate should be <state> 
            And the capability tilt:tiltstatechanged should be recent
            And the capability cont:contact should be <contact>
            And the capability cont:contactchanged should be recent
            And the driver should place a base:ValueChange message on the platform bus 

        Examples:
          | messageType               | prevstate | tags            | zoneStatus | state   | contact | remarks    |
          | zclreadattributesresponse | FLAT      |                 | 0          | UPRIGHT | OPENED  |            |
          | zclreportattributes       | FLAT      |                 | 0          | UPRIGHT | OPENED  |            |
          | zclreadattributesresponse | UPRIGHT   |                 | 1          | FLAT    | CLOSED  | Alarm1 bit |
          | zclreportattributes       | UPRIGHT   |                 | 1          | FLAT    | CLOSED  | Alarm1 bit |
          | zclreadattributesresponse | FLAT      | closedOnUpright | 0          | UPRIGHT | CLOSED  |            |
          | zclreportattributes       | FLAT      | closedOnUpright | 0          | UPRIGHT | CLOSED  |            |
          | zclreadattributesresponse | UPRIGHT   | closedOnUpright | 1          | FLAT    | OPENED  | Alarm1 bit |
          | zclreportattributes       | UPRIGHT   | closedOnUpright | 1          | FLAT    | OPENED  | Alarm1 bit |


    # onZigbeeMessage.Zcl.iaszone.zonestatuschangenotification
    @IASZone
    Scenario Outline: IAS Zone status change notification with no delay specified
        Given the capability tilt:tiltstate is <prevstate>
            And the capability devpow:battery is 50
            And the capability base:tags is [ <tags> ]
        When the device response with iaszone zonestatuschangenotification 
            And with parameter zoneStatus <zoneStatus>
            And send to driver
        Then the capability tilt:tiltstate should be <state> 
            And the capability tilt:tiltstatechanged should be recent 
            And the capability cont:contact should be <contact>
            And the capability cont:contactchanged should be recent
            And the capability devpow:battery should be <batt>
            And the driver should place a base:ValueChange message on the platform bus 

        Examples:
          | prevstate | tags            | zoneStatus | state   | contact | batt | remarks                 |
          | FLAT      |                 | 0          | UPRIGHT | OPENED  | 50   |                         |
          | UPRIGHT   |                 | 1          | FLAT    | CLOSED  | 50   | Alarm1 bit              |
          | FLAT      |                 | 8          | UPRIGHT | OPENED  |  0   | low battery             |
          | UPRIGHT   |                 | 9          | FLAT    | CLOSED  |  0   | Alarm1 bit, low battery |
          | FLAT      | closedOnUpright | 0          | UPRIGHT | CLOSED  | 50   |                         |
          | UPRIGHT   | closedOnUpright | 1          | FLAT    | OPENED  | 50   | Alarm1 bit              |
          | FLAT      | closedOnUpright | 8          | UPRIGHT | CLOSED  |  0   | low battery             |
          | UPRIGHT   | closedOnUpright | 9          | FLAT    | OPENED  |  0   | Alarm1 bit, low battery |


    @IASZone
    Scenario Outline: IAS Zone status change notification with delay longer than 30 seconds (120 1/4 seconds)
        Given the capability tilt:tiltstate is <prevstate>
            And the capability cont:contact is <contact>
            And the capability devpow:battery is 50
            And the capability base:tags is [ <tags> ]
        When the device response with iaszone zonestatuschangenotification 
            And with payload <zoneStatus>, 0, 0, 1, 121, 0
            And send to driver
        Then the capability tilt:tiltstate should be <state>
            And the capability cont:contact should be <contact>
            And the capability devpow:battery should be <batt>
            And the driver should send IasZone zclReadAttributes

        Examples:
          | prevstate | tags            | zoneStatus | state    | contact | batt | remarks                                          |
          | FLAT      |                 | 0          | FLAT     | CLOSED  | 50   | ignore and read current setting                  |
          | UPRIGHT   |                 | 1          | UPRIGHT  | OPENED  | 50   | ignore and read current setting                  |
          | FLAT      |                 | 8          | FLAT     | CLOSED  |  0   | ignore and read current setting, but low battery |
          | UPRIGHT   |                 | 9          | UPRIGHT  | OPENED  |  0   | ignore and read current setting, but low battery |
          | FLAT      | closedOnUpright | 0          | FLAT     | OPENED  | 50   | ignore and read current setting                  |
          | UPRIGHT   | closedOnUpright | 1          | UPRIGHT  | CLOSED  | 50   | ignore and read current setting                  |
          | FLAT      | closedOnUpright | 8          | FLAT     | OPENED  |  0   | ignore and read current setting, but low battery |
          | UPRIGHT   | closedOnUpright | 9          | UPRIGHT  | CLOSED  |  0   | ignore and read current setting, but low battery |


    @IASZone
    Scenario Outline: IAS Zone status change notification with delay 30 seconds or less (120 1/4 seconds)
        Given the capability tilt:tiltstate is <prevstate>
            And the capability devpow:battery is 50
            And the capability base:tags is [ <tags> ]
        When the device response with iaszone zonestatuschangenotification 
            And with payload <zoneStatus>, 0, 0, 1, 120, 0
            And send to driver
        Then the capability tilt:tiltstate should be <state> 
            And the capability tilt:tiltstatechanged should be recent 
            And the capability cont:contact should be <contact>
            And the capability cont:contactchanged should be recent
            And the capability devpow:battery should be <batt>

        Examples:
          | prevstate | tags            | zoneStatus | state    | contact | batt | remarks                            |
          | FLAT      |                 | 0          | UPRIGHT  | OPENED  | 50   | process status change              |
          | UPRIGHT   |                 | 1          | FLAT     | CLOSED  | 50   | process status change              |
          | FLAT      |                 | 8          | UPRIGHT  | OPENED  |  0   | process status change, low battery |
          | UPRIGHT   |                 | 9          | FLAT     | CLOSED  |  0   | process status change, low battery |
          | FLAT      | closedOnUpright | 0          | UPRIGHT  | CLOSED  | 50   | process status change              |
          | UPRIGHT   | closedOnUpright | 1          | FLAT     | OPENED  | 50   | process status change              |
          | FLAT      | closedOnUpright | 8          | UPRIGHT  | CLOSED  |  0   | process status change, low battery |
          | UPRIGHT   | closedOnUpright | 9          | FLAT     | OPENED  |  0   | process status change, low battery |


############################################################
# ZigBee Power Cluster Tests
############################################################

    # power.zclreadattributesresponse / zclreportattributes
    @Battery
    Scenario Outline: Power Read / Report
        Given the capability devpow:battery is 90
        When the device response with power <messageType> 
            And with parameter ATTR_BATTERY_VOLTAGE <voltage>
            And with parameter ATTR_BATTERY_VOLTAGE_MIN_THRESHOLD 21
            And send to driver
        # Then the capability devpow:battery should be <battery>
        Then the driver should place a base:ValueChange message on the platform bus
            And the message's devpow:battery attribute numeric value should be within delta 1 of <battery>
    
        Examples:
          | messageType               | voltage | battery |
          | zclreadattributesresponse | 30      | 100     |
          | zclreportattributes       | 20      | 0       |
          | zclreportattributes       | 21      | 0       |
          | zclreportattributes       | 22      | 11      |
          | zclreportattributes       | 25      | 44      |
          | zclreportattributes       | 26      | 55      |
          | zclreportattributes       | 29      | 88      |
          | zclreportattributes       | 30      | 100     |
          | zclreportattributes       | 31      | 100     |


############################################################
# ZigBee Poll Control Cluster Tests
############################################################

    # pollcontrol.zclreadattributesresponse / zclreportattributes
    @PollControl
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

    # pollcontrol.CheckIn
    @PollControl
    Scenario: poll control check in
        When the device response with pollcontrol checkin
            And send to driver
        Then the driver should send pollcontrol checkinresponse    


############################################################
# ZigBee Ack/Response Message Tests
############################################################

    # onZigbeeMessage(Zigbee.TYPE_ZCL) 
    Scenario Outline: default ZigbeeMessage processing
        When the device response with <cluster> <command>
            And send to driver
    
    Examples:
      | cluster | command | remarks                                |
      | 0x0020  | 0x0B    | CLUSTER_POLL_CONTROL, CMD_DFLT_RSP     |
      | 0x0500  | 0x0B    | CLUSTER_IAS_ZONE,     CMD_DFLT_RSP     |

