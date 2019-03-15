@ZWave
Feature: ZWave Utilitech Glass Break Sensor Driver Test

	These scenarios test the functionality of the Utilitech Glass Break Sensor ZWave driver.

	Background:
		Given the ZW_Utilitech_Glass_Break_Sensor.driver has been initialized
				
	Scenario: Driver reports capabilities to platform. 
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'glass']
			And the message's dev:devtypehint attribute should be Glass Break
			And the message's devadv:drivername attribute should be ZWaveUtilitechGlassBreakSensorDriver 
			And the message's devadv:driverversion attribute should be 1.0
			And the message's devpow:source attribute should be BATTERY
			And the message's devpow:linecapable attribute should be false		
		Then both busses should be empty

	Scenario Outline: Sensor reports alarm state 
		When the device response with sensor_alarm report
			And with parameter sensortype 7
			And with parameter sensorstate <sensorstate>
			And send to driver
		Then the platform attribute glass:break should change to <status>
			And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty

		Examples:
		  | sensorstate | status   |
		  | 0    	    | SAFE     |
		  | -1 	        | DETECTED |
		
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
		When the device connects to the platform
		Then the driver should place a base:ValueChange message on the platform bus 
		  
