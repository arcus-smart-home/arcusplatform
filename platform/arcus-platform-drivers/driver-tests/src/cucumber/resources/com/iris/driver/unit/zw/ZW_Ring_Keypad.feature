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
        And the message's mot:motion attribute should be NONE
    Then both busses should be empty


############################################################
# Alarm state
#
# RingKeyPad owns the indicators, so these go to the hub driver. Its behaviour
# is covered by RingKeyPadTest in arcus-reflex-controller.
############################################################

    @forward
    Scenario Outline: Alarm state requests are answered and left to the hub driver
        When the capability method <method>
            And with capability <modeArg> is <mode>
            And send to driver
        Then the driver should place a <response> message on the platform bus

        Examples:
          | method                   | modeArg   | mode | response                         |
          | keypad:Armed             | alarmMode | ON   | keypad:ArmedResponse             |
          | keypad:Disarmed          | N/A       | N/A  | keypad:DisarmedResponse          |
          | keypad:Alerting          | alarmMode | ON   | keypad:AlertingResponse          |
          | keypad:ArmingUnavailable | N/A       | N/A  | keypad:ArmingUnavailableResponse |

############################################################
# Chime
#
# The chime stays in this driver because its settings do.
############################################################

    @indicator
    Scenario: A chime plays the keypad's ding ding sound
        When the capability method keypad:Chime
            And send to driver
        Then the driver should send indicator set
            And with payload 0,1,96,9,80
        Then the driver should place a keypad:ChimeResponse message on the platform bus
        Then both busses should be empty

    @indicator
    Scenario: A chime can ask for one of the keypad's sounds
        When the capability method keypad:Chime
            And attribute sound is 98
            And send to driver
        Then the driver should send indicator set
            And with payload 0,1,98,9,80
        Then the driver should place a keypad:ChimeResponse message on the platform bus
        Then both busses should be empty

    @indicator
    Scenario Outline: A chime can't reach the keypad's other indicators
        When the capability method keypad:Chime
            And attribute sound is <sound>
            And send to driver
        Then the driver should place a Error message on the platform bus
        Then both busses should be empty

        Examples:
          | sound |
          | 13    |
          | 95    |
          | 101   |

############################################################
# Device settings
############################################################

    @settings
    Scenario Outline: Setting a parameter writes it to the keypad and reads it back
        When the capability method devsettings:SetParam
            And attribute paramId is "<paramId>"
            And attribute value is "<value>"
            And send to driver
        Then the driver should send configuration set
            And with payload <setPayload>
        Then the driver should send configuration get
            And with payload <param>
        Then the driver should place a devsettings:SetParamResponse message on the platform bus
            And the message's status attribute should be PENDING
        Then the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty

        Examples:
          | paramId             | value | param | setPayload |
          | sirenVolume         | 4     | 6     | 6,1,4      |
          | proximityDetection  | 0     | 15    | 15,1,0     |
          | ledRampTime         | 255   | 16    | 16,1,255   |
          | securityModeDisplay | 601   | 22    | 22,2,2,89  |

    @settings
    Scenario Outline: The chime uses its settings, which stay in the driver
        When the capability method devsettings:SetParam
            And attribute paramId is "<paramId>"
            And attribute value is "<value>"
            And send to driver
        Then the driver should place a devsettings:SetParamResponse message on the platform bus
            And the message's status attribute should be OK
        Then the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty
        When the capability method keypad:Chime
            And send to driver
        Then the driver should send indicator set
            And with payload <payload>
        Then the driver should place a keypad:ChimeResponse message on the platform bus
        Then both busses should be empty

        Examples:
          | paramId     | value | payload       |
          | chimeTone   | 100   | 0,1,100,9,80  |
          | chimeVolume | 40    | 0,1,96,9,40   |

    @settings
    Scenario Outline: A value the keypad would not accept is refused without touching it
        When the capability method devsettings:SetParam
            And attribute paramId is "<paramId>"
            And attribute value is "<value>"
            And send to driver
        Then the driver should place a devsettings:SetParamResponse message on the platform bus
            And the message's status attribute should be <status>
        Then both busses should be empty

        Examples:
          | paramId     | value | status        |
          | sirenVolume | 11    | INVALID_VALUE |
          | language    | 1     | INVALID_VALUE |
          | chimeTone   | 95    | INVALID_VALUE |
          | heartbeat   | 5     | INVALID_PARAM |
