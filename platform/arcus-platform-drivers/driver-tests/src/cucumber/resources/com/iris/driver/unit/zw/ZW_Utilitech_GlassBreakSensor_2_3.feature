@ZWave @glass
Feature: ZWave Utilitech Glass Break Sensor Reflex Driver Test

	These scenarios test the functionality of the Utilitech Glass Break Sensor ZWave driver.

	Background:
		Given the ZW_Utilitech_Glass_Break_Sensor_2_3.driver has been initialized
				
	Scenario: Driver reports capabilities to platform. 
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'glass']
			And the message's dev:devtypehint attribute should be Glass Break
			And the message's devadv:drivername attribute should be ZWaveUtilitechGlassBreakSensorDriver
			And the message's devadv:driverversion attribute should be 2.3
			And the message's devpow:source attribute should be BATTERY
			And the message's devpow:linecapable attribute should be false
			And the message's glass:break attribute should be SAFE		
		Then nothing else should happen
		
	Scenario: Device is added
		When the device is added
		Then the capability devpow:sourcechanged should be recent
			And the capability glass:breakchanged should be recent
		Then nothing else should happen

	Scenario: Device connected
		When the device is connected
		Then the driver should place a base:ValueChange message on the platform bus
#		Then the driver should set timeout at 135 minutes
		Then the driver should send battery get
		Then the driver should send version get
		Then the driver should send sensor_alarm get
			And with parameter sensortype 7
		Then nothing else should happen

#Needs test harness update
@Ignore
	Scenario Outline: Sensor reports alarm state
	 Given the driver attribute glass:break is <before>
		When the device response with sensor_alarm report
			And with parameter sensortype <type>
			And with parameter sensorstate <sensorstate>
			And send to driver
		Then the capability glass:break should be <status>
			And the capability glass:breakchanged should be recent
			And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty

		Examples:
		  | before   | type | sensorstate | status   |
		  | DETECTED |  0   |     0    	  | SAFE     |
		  | SAFE     |  0   |    -1 	    | DETECTED |
		  | DETECTED |  7   |     0    	  | SAFE     |
		  | SAFE     |  7   |    -1 	    | DETECTED |
		
	Scenario Outline: Sensor reports invalid alarm state
	 Given the driver attribute glass:break is <before>
		When the device response with sensor_alarm report
			And with parameter sensortype <type>
			And with parameter sensorstate <sensorstate>
			And send to driver
		Then the capability glass:break should be <status>
#			And the capability glass:breakchanged should not be recent
#			And the driver should not place a base:ValueChange message on the platform bus
		Then both busses should be empty

		Examples:
		  | before   | type | sensorstate | status   |
		  | DETECTED |  1   |     0    	  | DETECTED |
		  | DETECTED |  5   |    -1 	    | DETECTED |
		  | SAFE     |  0   |     4    	  | SAFE     |
		  | SAFE     |  7   |    -2 	    | SAFE		 |
		
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