@ZWave @Ring @keypad
Feature: ZWave Ring Alarm Keypad Driver Test

These scenarios test the functionality of the ZWave Ring Alarm Keypad driver

    Background:
    Given the ZW_Ring_Keypad.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform.
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's dev:devtypehint attribute should be KeyPad
        And the message's devadv:drivername attribute should be ZWRingKeypad
        And the message's devadv:driverversion attribute should be 1.0
        And the message's devpow:source attribute should be BATTERY
    Then both busses should be empty


############################################################
# Supervision CC
#
# The keypad wraps Entry Control notifications in Supervision Get:
#   [properties][encapsulated length][0x6F][0x01][seq][data type][event][data length]
# The session id is the low six bits of the properties byte and the Report
# must carry it back unchanged, or the keypad repeats the Get.
############################################################

    @supervision
    Scenario Outline: A Supervision-wrapped Arm Away press is acknowledged with the same session id
        When the device response with supervision get
            And with payload <properties>,6,111,1,<seq>,0,5,0
            And send to driver
        Then the driver should send supervision report
            And with parameter properties <sessionId>
            And with parameter status 255
            And with parameter duration 0
        Then the driver should place a keypad:ArmPressed message on the platform bus
            And the message's mode attribute should be ON
        Then both busses should be empty

        Examples:
          | properties | sessionId | seq |
          | 1          | 1         | 1   |
          | 2          | 2         | 2   |
          | 3          | 3         | 3   |
          | 62         | 62        | 4   |
          | 131        | 3         | 5   |

    @supervision
    Scenario: A Supervision-wrapped Arm Home press is dispatched to the Entry Control handler
        When the device response with supervision get
            And with payload 7,6,111,1,9,0,6,0
            And send to driver
        Then the driver should send supervision report
            And with parameter properties 7
        Then the driver should place a keypad:ArmPressed message on the platform bus
            And the message's mode attribute should be PARTIAL
        Then both busses should be empty

    @supervision
    Scenario: A Supervision Get wrapping another command class is still acknowledged
        When the device response with supervision get
            And with payload 5,3,128,3,99
            And send to driver
        Then the driver should send supervision report
            And with parameter properties 5
            And with parameter status 255
        Then both busses should be empty
