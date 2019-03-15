@Zigbee @DoorLock @Yale @PushButton @Deadbolt @YRD216
Feature: Test of the Yale Real Living 216 Push Button Deadbolt ZigBee driver

    These scenarios test the functionality of the Yale Real Living 216 Push Button Deadbolt ZigBee driver.

    Background:
        Given the ZB_Yale_216_Pushbutton_Deadbolt_2_8.driver has been initialized
            And the device has endpoint 1

    @basic
    Scenario: Driver reports capabilities to platform.
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be [ 'base', 'dev', 'devadv', 'devconn', 'devpow', 'doorlock', 'devota', 'ident' ]
            And the message's dev:devtypehint attribute should be Lock
            And the message's devadv:drivername attribute should be ZBYale216PushButtonDeadbolt 
            And the message's devadv:driverversion attribute should be 2.8
            And the message's devpow:source attribute should be BATTERY
            And the message's devpow:linecapable attribute should be false
            And the message's devpow:backupbatterycapable attribute should be false
            And the message's doorlock:type attribute should be DEADBOLT
            And the message's doorlock:supportsBuzzIn attribute should be true
            And the message's doorlock:supportsInvalidPin attribute should be false
            And the message's doorlock:numPinsSupported attribute should be 249
        Then both busses should be empty

############################################################
# General Driver Tests
############################################################

    @basic @added
    Scenario: Device added
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the capability doorlock:lockstatechanged should be recent

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set 
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Door                     |
          | "Front Door"             |
          | "Bill's Door"            |
          | "Mom & Dad's House"      |


############################################################
# Power Cluster Tests
############################################################

    @Power
    Scenario Outline: Power Read / Report
        Given the capability devpow:battery is 90
        When the device response with power <messageType> 
            And with parameter ATTR_BATTERY_VOLTAGE_PERCENT_REMAINING <halfPcnt>
            And send to driver
        # Then the capability devpow:battery should be <battery>
        Then the driver should place a base:ValueChange message on the platform bus
            And the message's devpow:battery attribute numeric value should be within delta 1.1 of <battery>

    Examples:
      | messageType               | halfPcnt | battery | remark                                                          |
      | zclreadattributesresponse | 0        | 0       | we accept 0% since device may be able to xmit, just not operate |
      | zclreadattributesresponse | 10       | 5       |                                                                 |
      | zclreportattributes       | 50       | 25      |                                                                 |
      | zclreportattributes       | 100      | 50      |                                                                 |
      | zclreportattributes       | 200      | 100     |                                                                 |
      | zclreportattributes       | 201      | 100     |                                                                 |
      | zclreportattributes       | 255      | 90      | 0xFF = unknown, so leave previous value                         |
      | zclreportattributes       | 0xff     | 90      | 0xFF = unknown, so leave previous value                         |
      | zclreportattributes       | -1       | 90      | 0xFF = unknown, so leave previous value                         |


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


############################################################
# Alarms Cluster Tests
############################################################

    @alarms
    Scenario: Locked device sends alarm message with code 0 = Deadbolt Jammed
        Given the capability doorlock:lockstate is LOCKING
        When the device response with alarms alarm
            And with parameter alarmCode 0
            And with parameter clusterId 257
            And send to driver
        # state remains LOCKING because setting to UNLOCKED might trigger scenes
        Then the capability doorlock:lockstate should be LOCKING 

    @alarms
    Scenario: Locked device sends alarm message with code 6 = Forced Open
        Given the capability doorlock:lockstate is LOCKED
        When the device response with alarms alarm
            And with parameter alarmCode 6
            And with parameter clusterId 257
        # state remains LOCKED because setting to UNLOCKED might trigger scenes
        Then the capability doorlock:lockstate should be LOCKED 

    @alarms
    Scenario: Locked device sends alarm message with code 4 = Wrong PIN Limit
        Given the capability doorlock:lockstate is LOCKED
        When the device response with alarms alarm
            And with parameter alarmCode 4
            And with parameter clusterId 257
            And send to driver


############################################################
# Doorlock Capability Tests
############################################################

    @doorlock
    Scenario: Make sure driver has implemented onDoorLock.ClearAllPins
        When a doorlock:ClearAllPins command is placed on the platform bus
        Then the driver should send doorlock clearAllPinCodes
        Then the driver should place a doorlock:ClearAllPinsResponse message on the platform bus

    # cannot test, attempted use of scheduler (in GenericZigbeeDoorLock.doBuzzIn) within test framework causes null pointer exception
    @Ignore
    @doorlock
    Scenario: Doing a Buzz-In on a LOCKED door should return a response with 'unocked' = TRUE
        Given the capability doorlock:lockstate is LOCKED
        When a doorlock:BuzzIn command is placed on the platform bus
        Then the driver should send doorlock unlockDoor
        Then the driver should place a doorlock:BuzzInResponse message on the platform bus
            And the message's unlocked attribute should be true

    @doorlock
    Scenario: Doing a Buzz-In on an UNLOCKED door should return a response with 'unocked' = FALSE (door was already unlocked)
        Given the capability doorlock:lockstate is UNLOCKED
        When a doorlock:BuzzIn command is placed on the platform bus
        Then the driver should place a doorlock:BuzzInResponse message on the platform bus
            And the message's unlocked attribute should be false

    
############################################################
# Doorlock Cluster Tests
############################################################

    @doorlock
    Scenario Outline: Device responding with FAILURE to lock door request should change lockstate back to UNLOCKED
        Given the capability doorlock:lockstate is LOCKING
        When the device response with doorlock lockdoorresponse
            And with parameter status <status>
            And send to driver
        # state should be set back to UNLOCKED (should not trigger any UNLOCK scenes since transitioning from LOCKING, not UNLOCKING)
        Then the capability doorlock:lockstate should be UNLOCKED

    Examples:
      | status |
      |   -1   |
      |    1   |

    @doorlock
    Scenario Outline: Device responding with FAILURE to unlock door request should change lockstate back to LOCKED
        Given the capability doorlock:lockstate is UNLOCKING
        When the device response with doorlock unlockdoorresponse
            And with parameter status <status>
            And send to driver
        # state should be set back to LOCKED (should not trigger any LOCK scenes since transitioning from UNLOCKING, not LOCKING)
        Then the capability doorlock:lockstate should be LOCKED

    Examples:
      | status |
      |   -1   |
      |    1   |

    @doorlock
    Scenario: Device responding with SUCCESS to Set PIN Code
        Given the capability doorlock:slots is { '0':'1234','1':'2345' }
            And the driver variable CUR_PENDING_SLOT_TO_ADD is 1
        When the device response with doorlock setpincoderesponse
            And with parameter status 0
            And send to driver
        Then the driver should place a doorlock:PersonAuthorized message on the platform bus
            And the message's personId attribute should be 2345
            And the message's slot attribute should be 1

    @doorlock
    Scenario: Device responding with FAIL to Set PIN Code
        Given the capability doorlock:slots is { '0':'1234','1':'2345','2':'RESERVED' }
            And the driver variable CUR_PENDING_SLOT_TO_ADD is 1
        When the device response with doorlock setpincoderesponse
            And with parameter status 1
            And send to driver
        Then the driver should place a doorlock:PinOperationFailed message on the platform bus
            And the message's personId attribute should be 2345
            And the message's slot attribute should be 1

    @doorlock
    Scenario: Device responding with SUCCESS to Clear PIN Code
        Given the capability doorlock:slots is { '0':'1234','1':'2345' }
            And the driver variable CUR_PENDING_SLOT_TO_REMOVE is 1
        When the device response with doorlock clearpincoderesponse
            And with parameter status 0
            And send to driver
        Then the driver should place a doorlock:PersonDeauthorized message on the platform bus
            And the message's personId attribute should be 2345
            And the message's slot attribute should be 1

    @doorlock
    Scenario: Device responding with FAIL to Clear PIN Code
        Given the capability doorlock:slots is { '0':'1234','1':'2345','2':'RESERVED' }
            And the driver variable CUR_PENDING_SLOT_TO_REMOVE is 1
        When the device response with doorlock clearpincoderesponse
            And with parameter status 1
            And send to driver
        Then the driver should place a doorlock:PinOperationFailed message on the platform bus
            And the message's personId attribute should be 2345
            And the message's slot attribute should be 1

    @doorlock
    Scenario: Device responding with SUCCESS to Clear All PIN Codes
        Given the capability doorlock:slots is { '0':'1234','1':'2345' }
        When the device response with doorlock clearallpincodesresponse
            And with parameter status 0
            And send to driver
        Then the capability doorlock:slots should be [:]
        Then the driver should place a doorlock:AllPinsCleared message on the platform bus

    @doorlock
    Scenario: Device responding with FAIL to Clear PIN Code
        Given the capability doorlock:slots is { '0':'1234','1':'2345','2':'RESERVED' }
        When the device response with doorlock clearallpincodesresponse
            And with parameter status 1
            And send to driver

    @doorlock
    Scenario Outline: Door Lock State read / report
        Given the capability doorlock:lockstate is <prevstate>
        When the device response with doorlock <messageType>
            And with parameter ATTR_LOCK_STATE <value>
            And send to driver
        Then the capability doorlock:lockstate should be <newstate>

    Examples:
      | messageType               | prevstate | value |  newstate | remark               |
      | zclreadattributesresponse |  UNLOCKED |    0  |  UNLOCKED | 0 = NOT FULLY LOCKED |
      | zclreadattributesresponse |  UNLOCKED |    1  |    LOCKED | 1 = LOCKED           |
      | zclreadattributesresponse |  UNLOCKED |    2  |  UNLOCKED | 2 = UNLOCKED         |
      | zclreadattributesresponse |  UNLOCKED |  255  |  UNLOCKED | 255 = UNKNOWN        |
      | zclreadattributesresponse |   LOCKING |    0  |   LOCKING | 0 = NOT FULLY LOCKED |
      | zclreadattributesresponse |   LOCKING |    1  |    LOCKED | 1 = LOCKED           |
      | zclreadattributesresponse |   LOCKING |    2  |  UNLOCKED | 2 = UNLOCKED         |
      | zclreadattributesresponse |   LOCKING |  255  |   LOCKING | 255 = UNKNOWN        |
      | zclreadattributesresponse |    LOCKED |    0  |    LOCKED | 0 = NOT FULLY LOCKED |
      | zclreadattributesresponse |    LOCKED |    1  |    LOCKED | 1 = LOCKED           |
      | zclreadattributesresponse |    LOCKED |    2  |  UNLOCKED | 2 = UNLOCKED         |
      | zclreadattributesresponse |    LOCKED |  255  |    LOCKED | 255 = UNKNOWN        |
      | zclreadattributesresponse | UNLOCKING |    0  | UNLOCKING | 0 = NOT FULLY LOCKED |
      | zclreadattributesresponse | UNLOCKING |    1  |    LOCKED | 1 = LOCKED           |
      | zclreadattributesresponse | UNLOCKING |    2  |  UNLOCKED | 2 = UNLOCKED         |
      | zclreadattributesresponse | UNLOCKING |  255  | UNLOCKING | 255 = UNKNOWN        |

      | zclreportattributes       |  UNLOCKED |    1  |    LOCKED | 1 = LOCKED           |
      | zclreportattributes       |  UNLOCKED |    2  |  UNLOCKED | 2 = UNLOCKED         |
      | zclreportattributes       |    LOCKED |    1  |    LOCKED | 1 = LOCKED           |
      | zclreportattributes       |    LOCKED |    2  |  UNLOCKED | 2 = UNLOCKED         |


    @doorlock @progevent
    Scenario: device sends Programming Event Notification - Pin Code Added at Lock
        Given the capability doorlock:slots is { '0':'1234','1':'2345' }
        When the device response with doorlock programmingeventnotification
            And with parameter source 0
            And with parameter code 2
            And with parameter userId 2
            And send to driver
        Then the driver should place a doorlock:PinAddedAtLock message on the platform bus

    @doorlock @progevent
    Scenario: device sends Programming Event Notification - Pin Code Added by Iris
        Given the capability doorlock:slots is { '0':'1234','1':'2345' }
        When the device response with doorlock programmingeventnotification
            And with parameter source 1
            And with parameter code 2
            And with parameter userId 1
            And send to driver
        Then the driver should place a doorlock:PersonAuthorized message on the platform bus
            And the message's personId attribute should be 2345
            And the message's slot attribute should be 1
    
    @doorlock @progevent
    Scenario: device sends Programming Event Notification - Pin Code unknown to Iris Deleted at Lock
        Given the capability doorlock:slots is { '0':'1234','1':'2345' }
        When the device response with doorlock programmingeventnotification
            And with parameter source 0
            And with parameter code 3
            And with parameter userId 2
            And send to driver
        Then the driver should place a doorlock:PinRemovedAtLock message on the platform bus

    @doorlock @progevent
    Scenario: device sends Programming Event Notification - Pin Code known to Iris Deleted at Lock
        Given the capability doorlock:slots is { '0':'1234','1':'2345','2':'RESERVED' }
        When the device response with doorlock programmingeventnotification
            And with parameter source 0
            And with parameter code 3
            # test using RESERVED because cannot setActor in test framework 
            And with parameter userId 2
            And send to driver
        Then the driver should place a doorlock:PinRemovedAtLock message on the platform bus

    @doorlock @progevent
    Scenario: device sends Programming Event Notification - Pin Code Deleted by Iris
        Given the capability doorlock:slots is { '0':'1234','1':'2345' }
        When the device response with doorlock programmingeventnotification
            And with parameter source 1
            And with parameter code 3
            And with parameter userId 1
            And send to driver
        Then the driver should place a doorlock:PersonDeauthorized message on the platform bus
            And the message's personId attribute should be 2345
            And the message's slot attribute should be 1
    
    @doorlock @progevent
    Scenario: device sends Programming Event Notification - Pin Code known to Iris Changed at Lock
        Given the capability doorlock:slots is { '0':'1234','1':'2345','2':'RESERVED' }
        When the device response with doorlock programmingeventnotification
            And with parameter source 0
            And with parameter code 4
            # test using RESERVED because cannot setActor in test framework 
            And with parameter userId 2
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus 


    @doorlock @opevent
    Scenario Outline: device sends Operation Event Notification - Locked at lock
        Given the capability doorlock:lockstate is UNLOCKED
            And the capability doorlock:slots is { '0':'1234','1':'2345','2':'RESERVED' }
        When the device response with doorlock operationeventnotification
            And with parameter source 0
            And with parameter code <code>
            And with parameter userId <user>
            And send to driver
        Then the capability doorlock:lockstate should be LOCKED
            And the capability doorlock:lockstatechanged should be recent 

    Examples:
      | code | user | remark         |
      |   1  |   1  | Lock           |
      |   7  |  -1  | One-Touch Lock |
      |   8  |  -1  | Key Lock       |
      |  11  |  -1  | Schedule Lock  |
      |  13  |  -1  | Manual Lock    |

    @doorlock @opevent
    Scenario Outline: device sends Operation Event Notification - Locked by Iris
        Given the capability doorlock:lockstate is UNLOCKED
            And the capability doorlock:slots is { '0':'1234','1':'2345','2':'RESERVED' }
        When the device response with doorlock operationeventnotification
            And with parameter source 1
            And with parameter code <code>
            And with parameter userId -1
            And send to driver
        Then the capability doorlock:lockstate should be LOCKED
            And the capability doorlock:lockstatechanged should be recent 

    Examples:
      | code | remark         |
      |   1  | Lock           |
      |  11  | Schedule Lock  |

    @doorlock @opevent
    Scenario Outline: device sends Operation Event Notification - Unlocked at lock
        Given the capability doorlock:lockstate is LOCKED
            And the capability doorlock:slots is { '0':'1234','1':'2345','2':'RESERVED' }
        When the device response with doorlock operationeventnotification
            And with parameter source 0
            And with parameter code <code>
            And with parameter userId <user>
            And send to driver
        Then the capability doorlock:lockstate should be UNLOCKED
            And the capability doorlock:lockstatechanged should be recent 

    Examples:
      | code | user | remark           |
      |   2  |   1  | Unlock           |
      |   9  |  -1  | Key Unlock       |
      |  12  |  -1  | Schedule Unlock  |
      |  14  |  -1  | Manual Unlock    |

    @doorlock @opevent
    Scenario Outline: device sends Operation Event Notification - Unlocked by Iris
        Given the capability doorlock:lockstate is LOCKED
            And the capability doorlock:slots is { '0':'1234','1':'2345','2':'RESERVED' }
        When the device response with doorlock operationeventnotification
            And with parameter source 1
            And with parameter code <code>
            And with parameter userId -1
            And send to driver
        Then the capability doorlock:lockstate should be UNLOCKED
            And the capability doorlock:lockstatechanged should be recent 

    Examples:
      | code | remark           |
      |   2  | Unlock           |
      |  12  | Schedule Unlock  |

