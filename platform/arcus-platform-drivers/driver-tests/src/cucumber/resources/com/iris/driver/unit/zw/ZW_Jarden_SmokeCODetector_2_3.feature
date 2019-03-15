@ZWave @Jarden
Feature: ZWave Jarden Smoke and CO Detector Driver Test

	These scenarios test the functionality of the ZWave Jarden Smoke and CO Detector driver.

	Background:
		Given the ZW_Jarden_SmokeCODetector.driver has been initialized
				
	Scenario: Driver reports capabilities to platform. 
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'smoke', 'co', 'test']
			And the message's dev:devtypehint attribute should be Smoke/CO
			And the message's devadv:drivername attribute should be ZWJardenSmokeAndCarbonMonoxideDetectorDriver 
			And the message's devadv:driverversion attribute should be 1.0
			And the message's devpow:source attribute should be BATTERY
			And the message's devpow:linecapable attribute should be false		
		Then both busses should be empty
	
	Scenario Outline: Smoke CO Alarm reports alarm state 
		When the device response with alarm report
			And with parameter alarmtype <alarmtype>
			And with parameter alarmlevel <alarmlevel>
			And send to driver
		Then the platform attribute <attribute> should change to <status>
			And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty

		Examples:
		  | attribute   | alarmtype | alarmlevel | status   |
		  | smoke:smoke | 1         | 0    	     | SAFE     |
		  | smoke:smoke | 1	        | -1 	     | DETECTED |
		  | co:co       | 2         | 0 	     | SAFE     |
		  | co:co       | 2         | -1 	     | DETECTED |

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
		  | 100       | 100          |

	Scenario: Device connected
		When the device is connected
		#When the device connects to the platform
		Then the driver should place a base:ValueChange message on the platform bus 
		  