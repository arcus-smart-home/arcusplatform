@ZWave @Jasco500 @Jasco14291
Feature: Unit Tests for the500 Series ZWavePlus ZWJasco14291SwitchDriver

	These scenarios test the functionality of the Jasco In-wall switch driver.

	Background:
		Given the ZW_Jasco_14291_InWallSwitch.driver has been initialized

	Scenario: Driver reports capabilities to platform. 
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'swit', 'indicator' ]
			And the message's dev:devtypehint attribute should be Switch
			And the message's devadv:drivername attribute should be ZWJasco14291InWallSwitchDriver 
			And the message's devadv:driverversion attribute should be 1.0
			And the message's devpow:source attribute should be LINE
			And the message's devpow:linecapable attribute should be true		
			And the message's indicator:indicator attribute should be ON
			And the message's indicator:enabled attribute should be true
			And the message's indicator:inverted attribute should be false
			And the message's swit:state attribute should be OFF
			And the message's swit:inverted attribute should be false								
		Then both busses should be empty

Scenario: Device associated
		When the device is added
#The base:GetAttributes is required in the test harness or the base:ValueChange message
#does not get placed on the platform bus.
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability swit:statechanged should be recent
			And the capability devpow:sourcechanged should be recent
		Then the driver should send configuration set
			And with parameter param 3
			And with parameter size 1
			And with parameter val1 0
	Then the driver should send configuration set
			And with parameter param 4
			And with parameter size 1
			And with parameter val1 0
		Then both busses should be empty
 	
	Scenario: Device reports state when first connected
		When the device is connected
		Then the driver should place a base:ValueChange message on the platform bus
		Then the driver should set timeout at 3 hours
		Then the driver should poll switch_binary.get every 1 hour
		Then the driver should send switch_binary get
		Then the driver should send configuration get
#			And with parameter param 3 
		Then the driver should send configuration get
#			And with parameter param 4 
		Then the driver should send version get
		Then both busses should be empty
								
	Scenario: Platform turns on switch via attribute change. 
		When a base:SetAttributes command with the value of swit:state ON is placed on the platform bus
		Then the driver should send switch_binary set 
			And with parameter value -1		
		Then the driver should place a EmptyMessage message on the platform bus		
		Then the driver should send switch_binary get
		Then both busses should be empty

	Scenario: Platform turns off switch via attribute change. 
		When a base:SetAttributes command with the value of swit:state OFF is placed on the platform bus
		Then the driver should send switch_binary set 
			And with parameter value 0
		Then the driver should place a EmptyMessage message on the platform bus		
		Then the driver should send switch_binary get
		Then both busses should be empty
			
	Scenario: Switch value changed
		When the device response with switch_binary report
			And with parameter value -1
			And send to driver
			#driver defaults to inverted (false)
		Then the platform attribute swit:state should change to ON
			And the platform attribute indicator:indicator should change to OFF
		And the driver should place a base:ValueChange message on the platform bus
		When the device response with switch_binary report
			And with parameter value 0
			And send to driver
		Then the platform attribute swit:state should change to OFF
			And the platform attribute indicator:indicator should change to ON
		And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty

	Scenario: Switch value changed indicator inverted
		Given the capability indicator:inverted is true
		When the device response with switch_binary report
			And with parameter value -1
			And send to driver
		Then the platform attribute swit:state should change to ON
			And the platform attribute indicator:indicator should change to ON
		And the driver should place a base:ValueChange message on the platform bus
		When the device response with switch_binary report
			And with parameter value 0
			And send to driver
		Then the platform attribute swit:state should change to OFF
			And the platform attribute indicator:indicator should change to OFF
		And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty

	Scenario: Switch value changed indicator disabled
		Given the capability indicator:enabled is false
		When the device response with switch_binary report
			And with parameter value -1
			And send to driver
		Then the platform attribute swit:state should change to ON
			And the platform attribute indicator:indicator should change to DISABLED
		And the driver should place a base:ValueChange message on the platform bus
		When the device response with switch_binary report
			And with parameter value 0
			And send to driver
		Then the platform attribute swit:state should change to OFF
			And the platform attribute indicator:indicator should change to DISABLED
		And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty


	Scenario Outline: Indicator value
		Given the capability indicator:indicator is <before>
			And the capability indicator:enabled is <isEnabled>
			And the capability indicator:inverted is <isInverted>
		When the device response with switch_binary report
			And with parameter value <value> 
			And send to driver
		Then the platform attribute indicator:indicator should change to <after>
#			And the driver should place a base:ValueChange message on the platform bus
		
		Examples:
		  |before 		| isEnabled | isInverted 	| value | after		 	|
		  | ON    		| true	   	|  false			| -1		|	OFF				|
		  | ON    		| true	   	|  true				| -1		|	ON				|
		  | ON    		| true	   	|  false			|  0		|	ON				|
		  | ON    		| true	   	|  true				|  0		|	OFF				|
		  | ON    		| false	   	|  false			| -1		|	DISABLED	|
		  | ON    		| false	   	|  true				| -1		|	DISABLED	|
		  | ON    		| false	   	|  false			|  0		|	DISABLED	|
		  | ON    		| false	   	|  true				|  0		|	DISABLED	|
		  
	Scenario Outline: Indicator Settings
		Given the capability indicator:inverted is <isInverted>
		When a base:SetAttributes command with the value of indicator:<attribute> <state> is placed on the platform bus
		Then the driver should send configuration set
			And with parameter param 3
			And with parameter val1 <value>
			And send to driver
			And the driver should send configuration get
		Then the driver should place a indicator:SetAttributesResponse message on the platform bus
		
		Examples:
		  | isInverted 	| attribute	| state	| value | 
		  |  false			|	enabled		| false	| 2			|	
		  |  true				|	inverted	| true	|	1			|	
		  |  false			|	enabled		| true	| 0			|	
		  |  true				|	inverted	| false	|	0			|	
		  |  true				|	enabled		| true	| 1			|	
	