@ZWave
Feature: Lower-level driver test example

	These scenarios test the functionality of the generic ZWave switch driver using lower-level,
	message-based steps.

	Background:
		Given the ZZW_Generic_Switch.driver has been initialized
		
	Scenario: Device reports state when first connected
		When a switch binary first connects to the platform
		Then the driver should send switch_binary get
				
	Scenario: Driver reports capabilities to platform. 
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devconn', 'devpow', 'swit']
			And the message's dev:devtypehint attribute should be swit
			And the message's devadv:drivername attribute should be ZZWGenericSwitchDriver 
			And the message's devadv:driverversion attribute should be 3.0
			And the message's devpow:source attribute should be LINE
			And the message's devpow:linecapable attribute should be true		
		Then both busses should be empty
				
	Scenario: Platform turns on switch via attribute change. 
		When a base:SetAttributes command with the value of swit:state ON is placed on the platform bus
		Then the driver should send switch_binary set 
			And with parameter value -1		

	Scenario: Platform turns off switch via attribute change. 
		When a base:SetAttributes command with the value of swit:state OFF is placed on the platform bus
		Then the driver should send switch_binary set 
			And with parameter value 0				
		
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
		  			