@ZWave @lock @SBD
Feature: ZWave Stanley Black & Decker Deadbolt Driver Test

    These scenarios test the functionality of the ZWave Stanley Black & Decker Deadbolt driver using lower-level,
    message-based steps.

	Background:
		Given the ZW_SBD_Deadbolt_2_6.driver has been initialized
				
	Scenario: Driver reports capabilities to platform. 
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'doorlock']
			And the message's dev:devtypehint attribute should be Lock
			And the message's devadv:drivername attribute should be ZWSBDDeadbolt 
			And the message's devadv:driverversion attribute should be 2.6
			And the message's devpow:source attribute should be BATTERY
			And the message's devpow:linecapable attribute should be false
			And the message's doorlock:type attribute should be DEADBOLT
			And the capability devadv:errors should be [:]
		Then both busses should be empty
	
	Scenario Outline: Door Lock reports alarm state of bolt extended
    Given the capability doorlock:lockstate is UNLOCKED
      And the capability devadv:errors is {"WARN_JAM":"Door lock may be jammed"}
 		When the device response with alarm report 
			And with parameter alarmtype <alarmType>
			And send to driver
		Then the platform attribute doorlock:lockstate should change to <state>
		  And the capability devadv:errors should be [:]
			And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty

		Examples:
		| alarmType	| state    | remarks |
		|  0x12     | LOCKED   | Locked, Keypad |
		|  0x15     | LOCKED   | Locked, Manual |
		|  0x1B     | LOCKED   | AutoLocked     |
		|  0x18     | LOCKED   | Locked, ZWave  |

@jam		
	Scenario Outline: Door Lock reports alarm state of bolt retracted
    Given the capability doorlock:lockstate is LOCKED
      And the capability devadv:errors is {"WARN_JAM":"Door lock may be jammed"}
 		When the device response with alarm report 
			And with parameter alarmtype <alarmType>
			And send to driver
		Then the platform attribute doorlock:lockstate should change to <state>
		  And the capability devadv:errors should be [:]
			And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty

		Examples:
		| alarmType	| state    | remarks |
		|  0x16     | UNLOCKED | Unlocked, ZWave  |
		|  0x19     | UNLOCKED | Unlocked, ZWave  |
		
	Scenario Outline: Door Lock reports alarm state of bolt retracted from Keypad
    Given the capability doorlock:lockstate is LOCKED
      And the capability devadv:errors is {"WARN_JAM":"Door lock may be jammed"}
 		When the device response with alarm report 
			And with parameter alarmtype 0x13
			And send to driver
		Then the platform attribute doorlock:lockstate should change to UNLOCKED
		  And the capability devadv:errors should be [:]
		  And the driver should place a doorlock:PinUsed message on the platform bus
			And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty


	Scenario Outline: Door Lock reports operation states
		When the device response with door_lock operation_report 
			And with parameter doorlockmode <doorlockmode>
			And send to driver
		Then the platform attribute doorlock:lockstate should change to <state>
			And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty

		Examples:
		| doorlockmode 	| state    |
		| -1           	| LOCKED   |
		|  0						|	UNLOCKED |
		|  1           	| UNLOCKED |
		| 16           	| LOCKED   |
		| 17           	| LOCKED   |
		| 32           	| UNLOCKED |
		| 33           	| UNLOCKED |

@jam
    Scenario Outline: Door Lock reports alarm state of bolt jammed while locking
        Given the capability doorlock:lockstate is LOCKING
        When the device response with alarm report 
            And with parameter notificationType 6
            And with parameter alarmtype <alarmType>
            And send to driver
        #don't change the lockstate on a jam
        Then the platform attribute doorlock:lockstate should be LOCKING
            And the driver should place a base:ValueChange message on the platform bus
            And the capability devadv:errors should be [WARN_JAM:Door lock may be jammed]
        Then both busses should be empty

		Examples:
		| alarmType	| remarks       |
		|  0x1A     | Jam, Autolock |
		|  0x11     | Jam, Keypad   |
		|  0x17     | Jam, ZWave    |
		
		
	Scenario Outline: Door Lock reports battery
		Given the capability devpow:battery is 25
		#needs a non-zero initialization because otherwise a base:ValueChange will not be pushed
		When the device response with battery report 
			And with parameter level <level>
			And send to driver 
		Then the platform attribute devpow:battery should change to <battery>
			And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty
		Examples:
		| level | battery |
		| -1    | 0       |
		| 0     | 0       |
		| 54    | 54      |
		| 100   | 100     |
@unjam		
    Scenario: Set Attributes on Doorlock
    Given the capability doorlock:lockstate is UNLOCKED
      And the capability devadv:errors is {"WARN_JAM":"Door lock may be jammed"}
		When a base:SetAttributes command with the value of doorlock:lockstate UNLOCKED is placed on the platform bus
		Then the driver should place a EmptyMessage message on the platform bus
		Then the driver should send door_lock operation_set 
			And with parameter mode 0
		# setting Unlocked device to Unlocked should not change state
    	Then the capability doorlock:lockstate should be UNLOCKED
    	    # error should not clear until device reports unlocked
    	    And the capability devadv:errors should be [WARN_JAM:Door lock may be jammed]
		Then the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty
		
@jam		
    Scenario: Set Attributes on Doorlock
    Given the capability doorlock:lockstate is LOCKED
      And the capability devadv:errors is {"WARN_JAM":"Door lock may be jammed"}
		When a base:SetAttributes command with the value of doorlock:lockstate LOCKED is placed on the platform bus
		Then the driver should place a EmptyMessage message on the platform bus
		Then the driver should send door_lock operation_set 
			And with parameter mode -1
		# setting locked device to locked should not change state
    	Then the capability doorlock:lockstate should be LOCKED
    	    # error should not clear until device reports locked
    	    And the capability devadv:errors should be [WARN_JAM:Door lock may be jammed]
		Then the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty
