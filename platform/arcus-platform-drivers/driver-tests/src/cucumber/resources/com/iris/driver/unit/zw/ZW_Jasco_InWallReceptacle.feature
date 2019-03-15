@ZWave
Feature: Binary switch capability for the ZWJascoInWallReceptacle driver

	These scenarios test the functionality of the Jasco In-wall Receptacle driver.

	Background:
		Given the ZW_Jasco_InWallReceptacle.driver has been initialized
		
	Scenario: Driver reports capabilities to platform. 
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'swit', 'indicator']
			And the message's dev:devtypehint attribute should be Switch
			And the message's devadv:drivername attribute should be ZWJascoInWallReceptacleDriver 
			And the message's devadv:driverversion attribute should be 1.0
			And the message's devpow:source attribute should be LINE
			And the message's devpow:linecapable attribute should be true		
		Then both busses should be empty
				
	Scenario: Platform turns on Receptacle via attribute change. 
		When a base:SetAttributes command with the value of swit:state ON is placed on the platform bus
		Then the driver should send switch_binary set 
			And with parameter value -1
		#Then both busses should be empty

	Scenario: Platform turns off Receptacle via attribute change. 
		When a base:SetAttributes command with the value of swit:state OFF is placed on the platform bus
		Then the driver should send switch_binary set 
			And with parameter value 0
		#Then both busses should be empty
						
	Scenario Outline: Receptacle state changes
		When the device response with switch_binary report 
			And with parameter value <value>
			And send to driver
		Then the platform attribute swit:state should change to <status>
			And the driver should place a base:ValueChange message on the platform bus
		#Then both busses should be empty
		
		Examples:
			| value | status |
			| -1    | ON     |
			| 0     | OFF    |   

	Scenario: Device connected
		When the device is connected
		#When the device connects to the platform
		Then the driver should place a base:ValueChange message on the platform bus 

