@ZWave @lock
Feature: ZWave Schlage Touch Deadbolt Driver Test

	These scenarios test the functionality of the ZWave Schlage Touch Deadbolt driver.

	Background:
		Given the ZW_Schlage_Touch_Deadbolt.driver has been initialized
				
	Scenario: Driver reports capabilities to platform. 
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'doorlock']
			And the message's dev:devtypehint attribute should be Lock
			And the message's devadv:drivername attribute should be ZWaveSchlageTouchDeadboltDriver 
			And the message's devadv:driverversion attribute should be 1.0
			And the message's devpow:source attribute should be BATTERY
			And the message's devpow:linecapable attribute should be false
			And the message's doorlock:type attribute should be DEADBOLT
		Then both busses should be empty
	
	Scenario Outline: Device reports battery level
		Given the capability devpow:battery is 25
		#needs a non-zero initialization becuase otherwise a base:ValueChange will not be pushed
		When the device response with battery report 
			And with parameter level <level-arg> 
			And send to driver
		Then the platform attribute devpow:battery should change to <battery-attr>
			And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty

		Examples:
		  | level-arg | battery-attr |
		  | -1        | 0            |
		  | 0         | 0            |
		  | 1         | 1            |
		  | 100       | 100          |

	Scenario: Device connected
		When the device connects to the platform
		Then the driver should place a base:ValueChange message on the platform bus 