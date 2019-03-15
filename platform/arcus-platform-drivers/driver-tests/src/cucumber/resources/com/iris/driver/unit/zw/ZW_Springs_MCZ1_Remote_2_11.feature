@ZWave @Blinds @MCZ1
Feature: Test ZWave Springs MCZ1 MultiChannel Remote driver

    These scenarios test the functionality of the ZWave Springs MultiChannel Cord Remote driver.

    Background:
        Given the ZW_Springs_MCZ1_Remote_2_11.driver has been initialized

    Scenario: Driver reports capabilities to platform.
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn']
        And the message's dev:devtypehint attribute should be Accessory
        And the message's devadv:drivername attribute should be ZWSpringsMCZ1RemoteDriver
        And the message's devadv:driverversion attribute should be 2.11
        And the message's devpow:source attribute should be BATTERY
        And the message's devpow:linecapable attribute should be false
        And the message's devpow:backupbatterycapable attribute should be false

############################################################
# Generic Driver Tests
############################################################

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set 
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                       |
          | Remote                      |
          | "Shade Remote"              |
          | "Tom's Remote"              |
          | "Kitchen & Basement Remote" |

############################################################
# Connection Tests
############################################################

    Scenario: Device connected
        When the device connects to the platform
        Then the driver should place a base:ValueChange message on the platform bus 
        Then the driver should set timeout at 37 hours
        Then the driver should send Battery get
        Then the driver should send Wake_Up set
            And with parameter seconds1 0
            And with parameter seconds2 0xA8
            And with parameter seconds3 0xC0
            And with parameter node 1

############################################################
# Generic ZWave Driver Tests
############################################################

    Scenario Outline: Device reports battery level
        Given the capability devpow:battery is <prev_level>
        When the device response with battery report 
            And with parameter level <level-arg>
            And send to driver 
        Then the platform attribute devpow:battery should change to <battery-attr>
            And the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty

        Examples:
          | prev_level | level-arg | battery-attr | remarks                                            |
          |  50        |  -1       |   0          |                                                    |
          |  50        |   0       |  50          | Driver assumes zero is invalid                     |
          |  50        |   1       |   1          |                                                    |
          |  70        |  50       |  50          |                                                    |
          | 100        |  99       |  99          |                                                    |
          | 100        | 100       | 100          |                                                    |

    Scenario: Make sure driver handles ZWave Plus Info Reports
        When the device response with zwaveplus_info report
            And with parameter zwaveversion 5
            And with parameter roletype 6
            And with parameter nodetype 2
            And send to driver
        Then protocol bus should be empty

    Scenario: Make sure driver handles Device Reset Locally Notification
        When the device response with device_reset_locally notification
            And send to driver
        Then protocol bus should be empty

