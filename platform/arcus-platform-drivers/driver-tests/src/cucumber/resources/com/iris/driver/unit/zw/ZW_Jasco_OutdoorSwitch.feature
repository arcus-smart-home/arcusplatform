@ZWave @Jasco @switch
Feature: Binary switch capability for the GE/Jasco ZW4201 Outdoor Switch

	These scenarios test the functionality of the ZW4201 switch driver.

	Background:
		Given the ZW_Jasco_OutdoorSwitch.driver has been initialized
		
	Scenario: Device reports state when first connected
		When the device is connected
		#When a switch binary first connects to the platform
		Then the driver should send switch_binary get
		
	Scenario: Driver reports capabilities to platform. 
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'swit']
			And the message's dev:devtypehint attribute should be Switch
			And the message's devadv:drivername attribute should be ZWJascoOutdoorSwitchDriver 
			And the message's devadv:driverversion attribute should be 1.0
			And the message's devpow:source attribute should be LINE
			And the message's devpow:linecapable attribute should be true		
		Then both busses should be empty
				
	Scenario Outline: Platform turns on switch via attribute change. 
		When a base:SetAttributes command with the value of swit:state ON is placed on the platform bus
		Then the driver should send switch_binary set 
			And with parameter value -1		

	Scenario: Platform turns off switch via attribute change. 
		When a base:SetAttributes command with the value of swit:state OFF is placed on the platform bus
		Then the driver should send switch_binary set 
			And with parameter value 0
		
	Scenario Outline: Switch value changed
		Given the capability swit:state is <before>
		When the device response with switch_binary report
			And with parameter value <value> 
			And send to driver
		Then the platform attribute swit:state should change to <state>
			And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty
		
		Examples:
		  | before | state | value |
		  | OFF    | ON    | -1    |
		  | ON     | OFF   | 0     |
