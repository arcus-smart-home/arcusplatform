@ZWave @Jarden
Feature: ZWave Jarden Smoke Detector Driver Test

	These scenarios test the functionality of the ZWave Jarden Smoke Detector driver using lower-level,
	message-based steps.

	Background:
		Given the ZW_Jarden_SmokeDetector_2_3.driver has been initialized
				
	Scenario: Driver reports capabilities to platform. 
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'smoke', 'test']
			And the message's dev:devtypehint attribute should be Smoke/CO
			And the message's devadv:drivername attribute should be ZWJardenSmokeDetector 
			And the message's devadv:driverversion attribute should be 2.3
			And the message's devpow:source attribute should be BATTERY
			And the message's devpow:linecapable attribute should be false		
		Then both busses should be empty

	Scenario: Smoke Alarm reports alarm detected
		When the device response with alarm report
			And with parameter alarmtype 1
			And with parameter alarmlevel -1
			And send to driver
		Then the platform attribute smoke:smoke should change to DETECTED
			And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty

	Scenario: Smoke Alarm reports alarm safe
		When the device response with alarm report
			And with parameter alarmtype 1
			And with parameter alarmlevel 0
			And send to driver
		Then the platform attribute smoke:smoke should change to SAFE
		Then both busses should be empty
	
	Scenario Outline: Device reports battery level
		When the device response with battery report 
			And with parameter level <level-arg>
			And send to driver  
		Then the platform attribute devpow:battery should change to <battery-attr>
			And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty

		Examples:
		  | level-arg | battery-attr |
		  | -1        | 0            |
		  | 1         | 1            |
		  | 54        | 54           |
		  | 100       | 100          |

	Scenario: Device connected
		When the device is connected
		#When the device connects to the platform
		  