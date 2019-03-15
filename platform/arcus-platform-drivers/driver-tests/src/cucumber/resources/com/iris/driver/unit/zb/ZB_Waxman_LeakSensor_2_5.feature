@Zigbee @Waxman @Leak
Feature: Test of the Waxman Leak Sensor driver

    These scenarios test the functionality of the Waxman Leaksmart Leak sensor.

    Background:
        Given the ZB_Waxman_LeakSensor_2_5.driver has been initialized
            And the device has endpoint 1 

    @basic
    Scenario: Driver reports capabilities to platform.
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be [ 'base', 'dev', 'devadv', 'devconn', 'devpow', 'leakh2o', 'temp', 'devota', 'ident' ]
            And the message's dev:devtypehint attribute should be Water Leak
            And the message's devadv:drivername attribute should be ZBWaxmanLeakSensor 
            And the message's devadv:driverversion attribute should be 2.5
            And the message's devpow:source attribute should be BATTERY
            And the message's devpow:linecapable attribute should be false
            And the message's devpow:backupbatterycapable attribute should be false
            And the message's leakh2o:state attribute should be SAFE
        Then both busses should be empty


############################################################
# General Driver Tests
############################################################

    @basic @added
    Scenario: Device added
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the capability leakh2o:statechanged should be recent

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
            And with parameter ATTR_BATTERY_VOLTAGE_MIN_THRESHOLD 21
            And send to driver
        # Then the capability devpow:battery should be <battery>
        Then the driver should place a base:ValueChange message on the platform bus
            And the message's devpow:battery attribute numeric value should be within delta 1.1 of <battery>
    
    Examples:
      | messageType               | voltage | battery |
      | zclreadattributesresponse | 20      | 0       |
      | zclreadattributesresponse | 21      | 0       |
      | zclreportattributes       | 33      | 50      |
      | zclreportattributes       | 45      | 100     |
      | zclreportattributes       | 46      | 100     |


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
# Temperature Cluster Tests
############################################################

    @Temperature
    Scenario Outline: Temperature measurement attribute reading and reporting
        When the device response with temperaturemeasurement <responseType>
            And with parameter ATTR_MEASURED_VALUE <value>
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the message's temp:temperature attribute numeric value should be within delta 0.01 of <result>

    Examples:
      | responseType              | value | result |
      | zclreadattributesresponse | 2757  | 27.57  |
      | zclreportattributes       | 2757  | 27.57  |


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
      | cluster | command | remarks                                |
      | 0x0001  | 0x07    | CLUSTER_PWR_CNFG,     CMD_CNFG_RPT_RSP |
      | 0x0020  | 0x04    | CLUSTER_POLL_CONTROL, CMD_WRT_ATTR_RSP |
      | 0x0020  | 0x0B    | CLUSTER_POLL_CONTROL, CMD_DFLT_RSP     |
      | 0x0402  | 0x07    | CLUSTER_TEMPERATURE,  CMD_CNFG_RPT_RSP |

   #onZigbeeMessage.Zcl.onoff.zclreadattributesresponse() 
   #onZigbeeMessage.Zcl.onoff.zclreportattributes() 
   @reflex
   Scenario Outline: Device gets appliance leak alerts
      Given the capability leakh2o:state is <previous>
      When the device response with appliancealerts <responseType>
         And with parameter numberAlerts <numberAlerts>
         And with parameter data <data>
         And send to driver
      Then the driver should place a base:ValueChange message on the platform bus
         And the capability leakh2o:state should be <new>
      Then both busses should be empty

         Examples:
         | responseType        | previous | numberAlerts |data                   | new  | 
         | getalertsresponse   | LEAK     | 0            | -127                  | SAFE | 
         | getalertsresponse   | SAFE     | 1            | -127,17,0             | LEAK |
         | alertsnotification  | SAFE     | 1            | -127,17,0             | LEAK |
         | alertsnotification  | LEAK     | 1            | -127,1,0              | SAFE |
         | getalertsresponse   | SAFE     | 2            | -127,17,0,0,0,0       | LEAK |
         | alertsnotification  | SAFE     | 2            | 0,0,0,-127,17,0       | LEAK |
         | getalertsresponse   | SAFE     | 3            | -127,17,0,0,0,0,0,0,0 | LEAK |
         | alertsnotification  | SAFE     | 3            | 0,0,0,-127,17,0,0,0,0 | LEAK |
         | getalertsresponse   | SAFE     | 3            | 0,0,0,0,0,0,-127,17,0 | LEAK |
         