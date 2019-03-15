@ZWave
Feature: ZWave Aeon Energy Reader Driver Test

	These scenarios test the functionality of the ZWave Aeon Energy Reader driver.
	
	Background:
		Given the ZW_Aeon_EnergyReader.driver has been initialized
	
	Scenario: Driver reports capabilities to platform.
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'pow']
			And the message's dev:devtypehint attribute should be EnergyMonitor
			And the message's devadv:drivername attribute should be ZWAeonEnergyReader 
			And the message's devadv:driverversion attribute should be 1.0
			And the message's devpow:linecapable attribute should be true
			And the message's pow:wholehome attribute should be true
		Then both busses should be empty	
			
	Scenario: Device connected
		When the device is connected
		#When the device connects to the platform
		Then the driver should place a base:ValueChange message on the platform bus 
			
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
		  | 50        | 50           |
		  | 100       | 100          |
		  
		#  | 0         | 0            | Need to validate with real device
		  			  		
	Scenario Outline: Device reports multi-level energy reading
		When the device response with meter report
			And with parameter Scale <Scale>
			And with parameter Value1 <Value1>
			And with parameter Value2 <Value2>
			And with parameter Value3 <Value3>
			And with parameter Value4 <Value4>
			And send to driver
		Then the platform attribute <type> should change to <reading>
			And the driver should place a base:ValueChange message on the platform bus
			
		Examples:
		  | Scale | Value1 | Value2 | Value3 | Value4 | type              | reading |
		  | 100   | 0      | 0      | 0      | 1      | pow:cumulative    | 1.0     |
		  | 116   | 0      | 0      | 0      | 1      | pow:instantaneous | 0.001   |
		  