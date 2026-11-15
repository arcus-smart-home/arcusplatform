@Zigbee @SmartThings @Samjin @Contact @SamjinMulti
Feature: Test of the Samjin SmartThings Multipurpose Sensor ZigBee driver

    These scenarios test the functionality of the Samjin SmartThings Multipurpose Sensor (IM6001-MPP01) ZigBee driver.

    Background:
        Given the ZB_SmartThings_SamjinMultipurposeSensor.driver has been initialized
            And the device has endpoint 1

    @basic
    Scenario: Driver reports capabilities to platform.
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be [ 'base', 'dev', 'devadv', 'devconn', 'devpow', 'cont', 'temp', 'tilt', 'devota', 'ident' ]
            And the message's dev:devtypehint attribute should be Contact
            And the message's dev:productId attribute should be 1d49d8
            And the message's devadv:drivername attribute should be ZBSmartThingsSamjinMultipurposeSensor
            And the message's devadv:driverversion attribute should be 1.0
            And the message's devpow:source attribute should be BATTERY
            And the message's devpow:linecapable attribute should be false
            And the message's devpow:backupbatterycapable attribute should be false
            And the message's cont:contact attribute should be CLOSED
            And the message's cont:usehint attribute should be UNKNOWN
            And the message's tilt:tiltstate attribute should be FLAT
        Then both busses should be empty

    @basic @timeout
    Scenario: Offline timeout
        Then the driver should set timeout at 195 minutes

############################################################
# General Driver Tests
############################################################

    @basic @added
    Scenario: Device added
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the capability cont:contactchanged should be recent
            And the capability tilt:tiltstatechanged should be recent
            # cluster bindings, including the accelerometer cluster
            And the driver should send control bind
            And the driver should send iaszone zclWriteAttributes
            And with attribute ATTR_IAS_CIE_ADDRESS value 1
            And the driver should schedule event doWriteIasCie in 5000 ms

    @basic @connected
    Scenario: Device connected
        When the device is connected
        Then the driver should send pollcontrol setLongPollInterval
            And the driver should send pollcontrol setShortPollInterval
            And the driver should send power zclReadAttributes
            And with payload 0x21, 0x00
            And the driver should send temperaturemeasurement zclReadAttributes
            And with payload 0x00, 0x00
            And the driver should send 0x0B05 zclReadAttributes
            And with payload 0x1C, 0x01
            # Zone Status alone, for the contact reflexes
            And the driver should send iaszone zclReadAttributes
            And with payload 0x02, 0x00
            # Zone State and CIE Address without Zone Status, so the response reaches the platform
            And the driver should send iaszone zclReadAttributes
            And with payload 0x00, 0x00, 0x10, 0x00
            And the driver should defer event CnfgPwrRpt
            And the driver should defer event CnfgTempRpt
            And the driver should defer event CnfgPollCrtl
            And the driver should defer event CnfgAccelThreshold
            And the driver should defer event CnfgAccelRpt

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Contact                  |
          | "Front Door"             |
          | "Mom & Dad's Room"       |


############################################################
# Configuration Tests
############################################################

    Scenario: Event CnfgPwrRpt
        Given the driver variable CNFG_PWR_RPT is 0
        When event CnfgPwrRpt trigger
        # BatteryPercentageRemaining, uint8, min 30 s, max 21600 s, change 1
        Then the driver should send 0x0001 0x06
            And with payload 0x00, 0x21, 0x00, 0x20, 0x1E, 0x00, 0x60, 0x54, 0x01
            And the driver should schedule event CnfgPwrRpt in 10000 ms
            And the driver variable CNFG_PWR_RPT should be 1

    Scenario: Event CnfgTempRpt
        Given the driver variable CNFG_TEMP_RPT is 0
        When event CnfgTempRpt trigger
        # MeasuredValue, int16, min 30 s, max 600 s, change 100
        Then the driver should send 0x0402 0x06
            And with payload 0x00, 0x00, 0x00, 0x29, 0x1E, 0x00, 0x58, 0x02, 0x64, 0x00
            And the driver should schedule event CnfgTempRpt in 10000 ms

    Scenario: Event CnfgPollCrtl
        Given the driver variable CNFG_POLL_CTRL is 0
        When event CnfgPollCrtl trigger
        # Checkin Interval, uint32, 480 quarter-seconds
        Then the driver should send 0x0020 0x02
            And with payload 0x00, 0x00, 0x23, 0xE0, 0x01, 0x00, 0x00

    Scenario: Event CnfgAccelThreshold
        Given the driver variable CNFG_ACCEL_THRESHOLD is 0
        When event CnfgAccelThreshold trigger
        # Motion Threshold Multiplier, uint8, 0x14, with Samjin's manufacturer code
        Then the driver should send 0xFC02 0x02
            And with manufacturer code 0x1241
            And with payload 0x00, 0x00, 0x20, 0x14
            And the driver should schedule event CnfgAccelThreshold in 10000 ms

    Scenario: Event CnfgAccelRpt
        Given the driver variable CNFG_ACCEL_RPT is 0
        When event CnfgAccelRpt trigger
        # acceleration (bitmap8) and the three axes (int16), min 0 s, max 3600 s, change 1, with Samjin's manufacturer code
        Then the driver should send 0xFC02 0x06
            And with manufacturer code 0x1241
            And with payload 0x00, 0x10, 0x00, 0x18, 0x00, 0x00, 0x10, 0x0E, 0x00, 0x12, 0x00, 0x29, 0x00, 0x00, 0x10, 0x0E, 0x01, 0x00, 0x00, 0x13, 0x00, 0x29, 0x00, 0x00, 0x10, 0x0E, 0x01, 0x00, 0x00, 0x14, 0x00, 0x29, 0x00, 0x00, 0x10, 0x0E, 0x01, 0x00
            And the driver should schedule event CnfgAccelRpt in 10000 ms

    Scenario Outline: Configuration stops retrying once the device accepts it
        Given the driver variable <varName> is 1
        When the device response with <cluster> <command>
            And with payload <payload>
            And send to driver
        Then the driver variable <varName> should be <varVal>

    Examples:
      | cluster | command | varName              | payload | varVal | remarks                                  |
      | 0x0001  | 0x07    | CNFG_PWR_RPT         |      0  |    -1  | Power configure reporting, success       |
      | 0x0001  | 0x07    | CNFG_PWR_RPT         |    0x86 |     1  | Power configure reporting, failure       |
      | 0x0402  | 0x07    | CNFG_TEMP_RPT        |      0  |    -1  | Temperature configure reporting, success |
      | 0x0402  | 0x07    | CNFG_TEMP_RPT        |    0x86 |     1  | Temperature configure reporting, failure |
      | 0x0020  | 0x04    | CNFG_POLL_CTRL       |      0  |    -1  | Poll Control write, success              |
      | 0x0020  | 0x04    | CNFG_POLL_CTRL       |    0x86 |     1  | Poll Control write, failure              |
      | 0xFC02  | 0x04    | CNFG_ACCEL_THRESHOLD |      0  |    -1  | Accelerometer threshold write, success   |
      | 0xFC02  | 0x04    | CNFG_ACCEL_THRESHOLD |    0x86 |     1  | Accelerometer threshold write, failure   |
      | 0xFC02  | 0x07    | CNFG_ACCEL_RPT       |      0  |    -1  | Accelerometer configure reporting, success |
      | 0xFC02  | 0x07    | CNFG_ACCEL_RPT       |    0x86 |     1  | Accelerometer configure reporting, failure |


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
    | prevState | zoneStatus | contact |
    | OPENED    | 0          | CLOSED  |
    | CLOSED    | 1          | OPENED  |

    # The battery bit's reflex emits devpow:battery 0 from the hub; the platform-side
    # reflex context drops emitted attributes, so only the contact is checked here
    @IASZone
    Scenario Outline: Zone status battery bit doesn't upset the contact
        Given the capability cont:contact is <prevState>
         When the device response with iaszone zclreportattributes
          And with parameter ATTR_ZONE_STATUS <zoneStatus>
          And send to driver
         Then the capability cont:contact should be <contact>

    Examples:
    | prevState | zoneStatus | contact | remarks          |
    | OPENED    | 8          | CLOSED  | battery          |
    | CLOSED    | 9          | OPENED  | alarm1 + battery |

    @IASZone
    Scenario: iaszone zone enroll request
         When the device response with iaszone zoneenrollrequest
          And with parameter zoneType 21
          And with parameter manufacturerCode 4673
          And send to driver
         Then the driver should send iaszone zoneEnrollResponse
          And with parameter enrollResponseCode 0x00
          And with parameter zoneId 0x00

    @IASZone
    Scenario: CIE address written, so enrollment starts
        Given the driver variable writeIasCieCnt is 2
         When the device response with iaszone zclwriteattributesresponse
          And with payload 0
          And send to driver
         Then the driver variable writeIasCieCnt should be -1
          And the driver should defer event doZoneEnroll

    @IASZone
    Scenario: Zone enrollment sends a response and reads the Zone State back
        Given the driver variable zoneEnrollCnt is 0
         When event doZoneEnroll trigger
         Then the driver should send iaszone zoneEnrollResponse
          And with parameter enrollResponseCode 0x00
          And with parameter zoneId 0x00
          And the driver should send iaszone zclReadAttributes
          And with payload 0x00, 0x00
          And the driver should schedule event doZoneEnroll in 5000 ms
          And the driver variable zoneEnrollCnt should be 1

    @IASZone
    Scenario: Enrolled Zone State stops the enrollment retries
        Given the driver variable zoneEnrollCnt is 3
            And the driver variable writeIasCieCnt is 2
         When the device response with iaszone zclreadattributesresponse
          And with parameter ATTR_ZONE_STATE 1
          And send to driver
         Then the driver variable zoneEnrollCnt should be -1
          And the driver variable writeIasCieCnt should be -1
          And protocol message count is 0

    @IASZone
    Scenario: Zone State not enrolled sends an enroll response
        Given the driver variable zoneEnrollCnt is -1
         When the device response with iaszone zclreadattributesresponse
          And with parameter ATTR_ZONE_STATE 0
          And send to driver
         Then the driver should send iaszone zoneEnrollResponse
          And with parameter zoneId 0x00


############################################################
# Power Cluster Tests
############################################################

    # BatteryPercentageRemaining is in half-percent units; values from the Edge driver's test
    @Power
    Scenario Outline: Battery percentage remaining
        Given the capability devpow:battery is 90
        When the device response with power <messageType>
            And with payload <payload>
            And send to driver
        Then the capability devpow:battery should be <battery>

    Examples:
      | messageType               | payload                      | battery | remarks       |
      | zclreportattributes       | 0x21, 0x00, 0x20, 0xC8       | 100     | 200           |
      | zclreportattributes       | 0x21, 0x00, 0x20, 0x96       | 75      | 150           |
      | zclreportattributes       | 0x21, 0x00, 0x20, 0x78       | 60      | 120           |
      | zclreportattributes       | 0x21, 0x00, 0x20, 0x61       | 49      | 97            |
      | zclreportattributes       | 0x21, 0x00, 0x20, 0x33       | 26      | 51            |
      | zclreportattributes       | 0x21, 0x00, 0x20, 0x01       | 1       | 1             |
      | zclreportattributes       | 0x21, 0x00, 0x20, 0x00       | 0       | 0             |
      | zclreportattributes       | 0x21, 0x00, 0x20, 0xFE       | 100     | 254, over 200 |
      | zclreportattributes       | 0x21, 0x00, 0x20, 0xFF       | 90      | 255 = invalid |
      | zclreadattributesresponse | 0x21, 0x00, 0x00, 0x20, 0x61 | 49      | 97            |


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
      | zclreadattributesresponse | 2444  | 24.44  |
      | zclreportattributes       | 2556  | 25.56  |
      | zclreportattributes       | -150  | -1.5   |


############################################################
# Accelerometer Cluster (0xFC02) Tests
############################################################

    # The 0x0013 axis sets Tilt: UPRIGHT above 900, FLAT below 100, unchanged in between
    @Tilt
    Scenario Outline: Axis report changes the tilt state
        Given the capability tilt:tiltstate is <prevState>
        When the device response with 0xFC02 0x0A
            And with header flags 0x0A
            And with manufacturer code 0x1241
            And with payload <payload>
            And send to driver
        Then the capability tilt:tiltstate should be <tilt>
            And the capability tilt:tiltstatechanged should be recent

    Examples:
      | prevState | payload                        | tilt    | remarks |
      | FLAT      | 0x13, 0x00, 0x29, 0xE8, 0x03   | UPRIGHT | 1000    |
      | FLAT      | 0x13, 0x00, 0x29, 0x18, 0xFC   | UPRIGHT | -1000   |
      | FLAT      | 0x13, 0x00, 0x29, 0x85, 0x03   | UPRIGHT | 901     |
      | UPRIGHT   | 0x13, 0x00, 0x29, 0x32, 0x00   | FLAT    | 50      |
      | UPRIGHT   | 0x13, 0x00, 0x29, 0xCE, 0xFF   | FLAT    | -50     |
      | UPRIGHT   | 0x13, 0x00, 0x29, 0x63, 0x00   | FLAT    | 99      |

    @Tilt
    Scenario Outline: Axis report between the thresholds leaves the tilt state alone
        Given the capability tilt:tiltstate is <prevState>
        When the device response with 0xFC02 0x0A
            And with header flags 0x0A
            And with manufacturer code 0x1241
            And with payload <payload>
            And send to driver
        Then the capability tilt:tiltstate should be <prevState>

    Examples:
      | prevState | payload                        | remarks |
      | FLAT      | 0x13, 0x00, 0x29, 0x84, 0x03   | 900     |
      | UPRIGHT   | 0x13, 0x00, 0x29, 0x64, 0x00   | 100     |
      | UPRIGHT   | 0x13, 0x00, 0x29, 0xF4, 0x01   | 500     |
      | FLAT      | 0x13, 0x00, 0x29, 0xF4, 0x01   | 500     |

    @Tilt
    Scenario Outline: Only the 0x0013 axis sets the tilt state
        Given the capability tilt:tiltstate is <prevState>
        When the device response with 0xFC02 0x0A
            And with header flags 0x0A
            And with manufacturer code 0x1241
            And with payload 0x10, 0x00, 0x18, 0x01, 0x12, 0x00, 0x29, <x>, 0x13, 0x00, 0x29, <y>, 0x14, 0x00, 0x29, <z>
            And send to driver
        Then the capability tilt:tiltstate should be <tilt>

    Examples:
      | prevState | x          | y          | z          | tilt    |
      | FLAT      | 0x00, 0x00 | 0xE8, 0x03 | 0x00, 0x00 | UPRIGHT |
      | UPRIGHT   | 0xE8, 0x03 | 0x00, 0x00 | 0xE8, 0x03 | FLAT    |

    @Tilt
    Scenario: Axis read response changes the tilt state
        Given the capability tilt:tiltstate is FLAT
        When the device response with 0xFC02 0x01
            And with header flags 0x0A
            And with manufacturer code 0x1241
            And with payload 0x13, 0x00, 0x00, 0x29, 0xE8, 0x03
            And send to driver
        Then the capability tilt:tiltstate should be UPRIGHT

    # acceleration (vibration) is only logged
    @Tilt
    Scenario Outline: Acceleration report changes nothing
        Given the capability tilt:tiltstate is FLAT
            And the capability cont:contact is CLOSED
        When the device response with 0xFC02 0x0A
            And with header flags 0x0A
            And with manufacturer code 0x1241
            And with payload 0x10, 0x00, 0x18, <value>
            And send to driver
        Then the capability tilt:tiltstate should be FLAT
            And the capability cont:contact should be CLOSED
            And the platform bus should be empty

    Examples:
      | value |
      | 0x01  |
      | 0x00  |

    @Tilt
    Scenario Outline: Tilt sets the contact state when tiltActivatesContact is set
        Given the device has tag tiltActivatesContact
            And the device has tag <tag>
            And the capability tilt:tiltstate is <prevTilt>
            And the capability cont:contact is <prevContact>
        When the device response with 0xFC02 0x0A
            And with header flags 0x0A
            And with manufacturer code 0x1241
            And with payload 0x13, 0x00, 0x29, <axis>
            And send to driver
        Then the capability tilt:tiltstate should be <tilt>
            And the capability cont:contact should be <contact>
            And the capability cont:contactchanged should be recent

    Examples:
      | tag             | prevTilt | prevContact | axis       | tilt    | contact |
      | other           | FLAT     | CLOSED      | 0xE8, 0x03 | UPRIGHT | OPENED  |
      | other           | UPRIGHT  | OPENED      | 0x32, 0x00 | FLAT    | CLOSED  |
      | closedOnUpright | FLAT     | OPENED      | 0xE8, 0x03 | UPRIGHT | CLOSED  |
      | closedOnUpright | UPRIGHT  | CLOSED      | 0x32, 0x00 | FLAT    | OPENED  |

    @Tilt
    Scenario: Tilt leaves the contact state alone without tiltActivatesContact
        Given the device has tag closedOnUpright
            And the capability tilt:tiltstate is FLAT
            And the capability cont:contact is OPENED
        When the device response with 0xFC02 0x0A
            And with header flags 0x0A
            And with manufacturer code 0x1241
            And with payload 0x13, 0x00, 0x29, 0xE8, 0x03
            And send to driver
        Then the capability tilt:tiltstate should be UPRIGHT
            And the capability cont:contact should be OPENED

    @Tilt
    Scenario Outline: Adding tags resyncs the contact only with tiltActivatesContact
        Given the capability tilt:tiltstate is UPRIGHT
            And the capability cont:contact is CLOSED
        When a base:AddTags command with argument tags of list value ["<tag>"] is placed on the platform bus
        Then the capability cont:contact should be <contact>

    Examples:
      | tag                  | contact |
      | tiltActivatesContact | OPENED  |
      | other                | CLOSED  |

    @Tilt
    Scenario: Removing closedOnUpright resyncs the contact
        Given the device has tag tiltActivatesContact
            And the device has tag closedOnUpright
            And the capability tilt:tiltstate is UPRIGHT
            And the capability cont:contact is CLOSED
        When a base:RemoveTags command with argument tags of list value ["closedOnUpright"] is placed on the platform bus
        Then the capability cont:contact should be OPENED


############################################################
# Diagnostics Cluster Tests
############################################################

    Scenario Outline: diagnostics read / report response
        When the device response with diagnostics <messageType>
            And with parameter ATTR_LAST_MESSAGE_LQI <lqi>
            And send to driver
        Then the capability devconn:signal should be <signal>

    Examples:
      | messageType               | lqi | signal |
      | zclreadattributesresponse | 51  | 20     |
      | zclreportattributes       | 102 | 40     |


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
      | cluster | command | remarks                                 |
      | 0x0020  | 0x0B    | CLUSTER_POLL_CONTROL,  CMD_DFLT_RSP     |
      | 0x0500  | 0x0B    | CLUSTER_IAS_ZONE,      CMD_DFLT_RSP     |
      | 0xFC02  | 0x0B    | CLUSTER_ACCELEROMETER, CMD_DFLT_RSP     |
