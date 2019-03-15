@ZWave
Feature: Binary switch capability for the GE/Jasco ZW4101 Indoor Switch

	These scenarios test the functionality of the ZW4101 switch driver.

	Background:
		Given the ZW_Jasco_IndoorSwitch.driver has been initialized
		
	Scenario: Device reports state when first connected
		When the device is connected
		#When a switch binary first connects to the platform
		Then the driver should send switch_binary get
				
	Scenario: Driver reports capabilities to platform. 
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'swit']
			And the message's dev:devtypehint attribute should be Switch
			And the message's devadv:drivername attribute should be ZWJascoIndoorSwitchDriver 
			And the message's devadv:driverversion attribute should be 1.0
			And the message's devpow:source attribute should be LINE
			And the message's devpow:linecapable attribute should be true		
		Then both busses should be empty
				
	Scenario Outline: Platform turns switch via attribute change. 
		When a base:SetAttributes command with the value of swit:state <state> is placed on the platform bus
		Then the driver should send switch_binary set
			And with parameter value <value>
		
		Examples:
		  | state | value |
		  | ON    | -1    |
		  | OFF   | 0     |	
			
	Scenario Outline: Switch value changed
		When the device response with switch_binary report
			And with parameter value <value> 
			And send to driver
		Then the platform attribute swit:state should change to <state>
			And the driver should place a base:ValueChange message on the platform bus
		
		Examples:
		  | state | value |
		  | ON    | -1    |
		  | OFF   | 0     |		
@node		  
	Scenario: Node Info
		When the device sends nodeInfo
		Then the driver should send switch_binary get
			And both busses should be empty	
		  	