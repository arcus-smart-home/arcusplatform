@ZWave @watervalve @FortrezZ
Feature: Test for ZWave water valve fom FortrezZ

	These scenarios test the functionality of the FortrezZ ZWave Water Valve Driver.

	Background:
		Given the ZW_FortrezZ_WaterValve_2_4.driver has been initialized
		
	Scenario: Driver reports capabilities to platform. 
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devconn', 'devpow', 'valv']
			And the message's dev:devtypehint attribute should be Water Valve
			And the message's devadv:drivername attribute should be ZWFortrezZWaterValveDriver 
			And the message's devadv:driverversion attribute should be 2.4
			And the message's devpow:source attribute should be LINE
			And the message's devpow:linecapable attribute should be true	
			And the message's devpow:backupbatterycapable attribute should be false
			And the message's valv:valvestate attribute should be OPEN	
		Then both busses should be empty
		
	Scenario: Device Added
		When the device is added
#The base:GetAttributes is required in the test harness or the base:ValueChange message
#does not get placed on the platform bus.
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability devpow:sourcechanged should be recent
			And the capability valv:valvestatechanged should be recent	

	Scenario: Device reports state when first connected
		When the device is connected
 		Then the driver should place a base:ValueChange message on the platform bus
 			And the capability devconn:state should be ONLINE
 			And the capability devconn:lastchange should be recent
		Then the driver should set timeout at 1 hr
		Then the driver should send switch_binary get
		Then the driver should poll switch_binary.get every 15 minutes
		Then the driver should send version get 
			And both busses should be empty				
				
	Scenario: Platform turns on valve via attribute change. 
		When a base:SetAttributes command with the value of valv:valvestate OPEN is placed on the platform bus
		Then the driver should send switch_binary set 
			And with parameter value 0
		Then the driver should place a EmptyMessage message on the platform bus
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability valv:valvestate should be OPENING
			And both busses should be empty				
					
	Scenario: Platform turns off valve via attribute change. 
		When a base:SetAttributes command with the value of valv:valvestate CLOSED is placed on the platform bus
		Then the driver should send switch_binary set 
			And with parameter value -1				
		Then the driver should place a EmptyMessage message on the platform bus
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability valv:valvestate should be CLOSING
			And both busses should be empty				
		
	Scenario: Switch value changed
		When the device response with switch_binary report
			And with parameter value -1
			And send to driver
		Then the platform attribute valv:valvestate should change to CLOSED
			And the driver should place a base:ValueChange message on the platform bus
		When the device response with switch_binary report
			And with parameter value 0
			And send to driver
		Then the platform attribute valv:valvestate should change to OPEN
			And the driver should place a base:ValueChange message on the platform bus
			And both busses should be empty				
		
