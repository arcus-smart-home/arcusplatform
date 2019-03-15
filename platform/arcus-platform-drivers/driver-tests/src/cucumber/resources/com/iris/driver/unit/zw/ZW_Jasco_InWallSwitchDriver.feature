@ZWave 
Feature: Binary switch capability for the ZWJascoInWallSwitchDriver

	These scenarios test the functionality of the Jasco In-wall switch driver.

	Background:
		Given the ZW_Jasco_InWallSwitch.driver has been initialized
@version		
	Scenario: Device reports state when first connected
		When the device is connected
		Then the driver should place a base:ValueChange message on the platform bus
		Then the driver should send switch_binary get
			And the driver should poll switch_binary.get every 120 seconds
			And the driver should send configuration get 
			And the driver should send configuration get 
			And the driver should set timeout
			And the driver should send version get
		Then both busses should be empty
				
	Scenario: Driver reports capabilities to platform. 
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'swit', 'indicator' ]
			And the message's dev:devtypehint attribute should be Switch
			And the message's devadv:drivername attribute should be ZWJascoInWallSwitchDriver 
			And the message's devadv:driverversion attribute should be 1.0
			And the message's devpow:source attribute should be LINE
			And the message's devpow:linecapable attribute should be true		
		#Then both busses should be empty
				
	Scenario: Platform turns on switch via attribute change. 
		When a base:SetAttributes command with the value of swit:state ON is placed on the platform bus
		Then the driver should send switch_binary set 
			And with parameter value -1		
		#Then both busses should be empty

	Scenario: Platform turns off switch via attribute change. 
		When a base:SetAttributes command with the value of swit:state OFF is placed on the platform bus
		Then the driver should send switch_binary set 
			And with parameter value 0
		#Then both busses should be empty
			
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

@version		  
	Scenario Outline: Version Report
		When the device response with version report
			And with parameter protocol <protocol>
			And with parameter sub-protocol <subprotocol>
			And with parameter firmware_0_version <ver>
			And with parameter firmware_0_sub_version <subver>
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the platform attribute devadv:firmwareVersion should change to <version>
			
		Examples:
			| protocol	| subprotocol | ver | subver 	| version 				|
			|    4     	|    51       |  3  |   15   	|  4.51.3.15			|
			|	255				|		255				| 255	|	255			|	255.255.255.255 |
				
