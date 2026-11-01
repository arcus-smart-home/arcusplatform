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

############################################################
# Indicator CC V3
#
# The keypad only understands V3 indicator objects. Indicator Set V3 payload:
#   [v1 value 0x00][object count][indicator id, property id, value]...
# Ids: 2 disarmed, 9 code rejected, 10 armed stay, 11 armed away, 13 burglar
# alarm, 16 bypass needed, 17 entry delay, 18 exit delay. A countdown uses
# property 6 (minutes) and 7 (seconds), the way zwave-js sends its "timeout".
############################################################

    @indicator
    Scenario: Arming with an exit delay starts the exit delay countdown
        When the capability method keypad:BeginArming
            And attribute delayInS is 30
            And with capability alarmMode is ON
            And send to driver
        Then the driver should send indicator set
            And with payload 0,2,18,6,0,18,7,30
        Then the driver should place a keypad:BeginArmingResponse message on the platform bus
            And the driver should place a base:ValueChange message on the platform bus
            And the message's keypad:alarmState attribute should be ARMING
        Then both busses should be empty

    @indicator
    Scenario: A long exit delay is split into minutes and seconds
        When the capability method keypad:BeginArming
            And attribute delayInS is 90
            And with capability alarmMode is ON
            And send to driver
        Then the driver should send indicator set
            And with payload 0,2,18,6,1,18,7,30
        Then the driver should place a keypad:BeginArmingResponse message on the platform bus
            And the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty

    @indicator
    Scenario Outline: Armed and disarmed states light the matching indicator
        When the capability method <method>
            And with capability <modeArg> is <mode>
            And send to driver
        Then the driver should send indicator set
            And with payload 0,1,<indicator>,1,255
        Then the driver should place a <response> message on the platform bus
            And the driver should place a base:ValueChange message on the platform bus
            And the message's <changed> attribute should be <value>
        Then both busses should be empty

        Examples:
          | method          | modeArg   | mode    | indicator | response                | changed          | value    |
          | keypad:Armed    | alarmMode | ON      | 11        | keypad:ArmedResponse    | keypad:alarmState | ARMED   |
          | keypad:Armed    | alarmMode | PARTIAL | 10        | keypad:ArmedResponse    | keypad:alarmMode  | PARTIAL |
          | keypad:Disarmed | N/A       | N/A     | 2         | keypad:DisarmedResponse | keypad:alarmMode  | OFF     |
          | keypad:Alerting | alarmMode | ON      | 13        | keypad:AlertingResponse | keypad:alarmState | ALERTING |

    @indicator
    Scenario: An entry delay starts the entry delay countdown
        When the capability method keypad:Soaking
            And attribute durationInS is 45
            And with capability alarmMode is ON
            And send to driver
        Then the driver should send indicator set
            And with payload 0,2,17,6,0,17,7,45
        Then the driver should place a keypad:SoakingResponse message on the platform bus
            And the driver should place a base:ValueChange message on the platform bus
            And the message's keypad:alarmState attribute should be SOAKING
        Then both busses should be empty

    @indicator
    Scenario: Disarm without a PIN lights the code-rejected indicator
        When the device response with supervision get
            And with payload 4,6,111,1,1,0,3,0
            And send to driver
        Then the driver should send supervision report
            And with parameter properties 4
        Then the driver should send indicator set
            And with payload 0,1,9,1,255
        Then the driver should place a keypad:InvalidPinEntered message on the platform bus
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
