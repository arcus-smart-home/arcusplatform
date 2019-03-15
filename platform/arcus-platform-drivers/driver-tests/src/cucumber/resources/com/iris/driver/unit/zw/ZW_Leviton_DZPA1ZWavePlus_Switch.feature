@ZWave @Leviton @Switch @DZPA1
Feature: Unit Tests for Leviton DZPA1 ZWave Plus Plug-In Switch

	These scenarios test the functionality of the Leviton switch driver.
	Background:
		Given the ZW_Leviton_DZPA1ZWavePlus_Switch_2_9.driver has been initialized

	Scenario: Driver reports capabilities to platform. 
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'swit' ]
			And the message's dev:devtypehint attribute should be Switch
			And the message's devadv:drivername attribute should be ZWLevitonDZPA1ZWavePlusSwitch
			And the message's devadv:driverversion attribute should be 2.9
			And the message's dev:vendor attribute should be Leviton
			And the message's devpow:source attribute should be LINE
			And the message's devpow:linecapable attribute should be true
		Then both busses should be empty

	@basic @added
	Scenario: Device associated
		When the device is added
#The base:GetAttributes is required in the test harness or the base:ValueChange message
#does not get placed on the platform bus.
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
		Then the driver should place a base:ValueChange message on the platform bus
		And the capability devpow:sourcechanged should be recent
		And the capability swit:statechanged should be recent
		Then both busses should be empty

	@basic @connected
	Scenario: Device reports state when first connected
		When the device is connected
		Then the driver should place a base:ValueChange message on the platform bus
		Then the driver should set timeout at 200 minutes
		Then the driver should poll switch_binary.get every 60 minutes
		Then the driver should send version get
		Then the driver should send switch_binary get
		Then both busses should be empty

	Scenario: Platform turns on switch via attribute change.
		When a base:SetAttributes command with the value of swit:state ON is placed on the platform bus
		Then the driver should send switch_binary set 
			And with parameter value -1		
		Then the driver should place a EmptyMessage message on the platform bus
		Then the driver should schedule event DeferredSwitchBinaryGet
		Then both busses should be empty

	Scenario: Platform turns off switch via attribute change.
		When a base:SetAttributes command with the value of swit:state OFF is placed on the platform bus
		Then the driver should send switch_binary set 
			And with parameter value 0
		Then the driver should place a EmptyMessage message on the platform bus
		Then the driver should schedule event DeferredSwitchBinaryGet
		Then both busses should be empty
			
	Scenario: Switch value changed
		When the device response with switch_binary report
			And with parameter value -1
			And send to driver
		Then the platform attribute swit:state should change to ON
		And the driver should place a base:ValueChange message on the platform bus
		When the device response with switch_binary report
			And with parameter value 0
			And send to driver
		Then the platform attribute swit:state should change to OFF
		And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty

	Scenario: Hail
		When the device sends hail hail
			And send to driver
		Then the driver should schedule event DeferredSwitchBinaryGet
			And both busses should be empty
			
