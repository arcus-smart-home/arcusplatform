@ZWave @Blinds 
Feature: ZWave Springs BR1 Remote Test

	These scenarios test the functionality of the ZWave Springs Basic Remote driver.
	
	Background:
		Given the ZW_Springs_BRZ1_Remote.driver has been initialized

	Scenario: Driver reports capabilities to platform.
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
	    And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn']
	    And the message's dev:devtypehint attribute should be Accessory
	    And the message's devadv:drivername attribute should be ZWSpringsBRZ1RemoteDriver
	    And the message's devadv:driverversion attribute should be 1.0
	    And the message's devpow:source attribute should be BATTERY																							
	    And the message's devpow:linecapable attribute should be false	
	
	Scenario: Device connected
		When the device connects to the platform
		Then the driver should place a base:ValueChange message on the platform bus 
		Then the driver should send Battery get
		Then the driver should send Wake_Up set
			And with parameter seconds1 0
			And with parameter seconds2 0xA8
			And with parameter seconds3 0xC0
			And with parameter node 1
		Then the driver should set timeout at 37 hours
		
			Scenario: Device reports battery level
		When the device response with battery report 
			And with parameter level 30
			And send to driver 
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability devpow:battery should be 30
		Then both busses should be empty
		
		Scenario: Device sends scene notification
		When the device response with central_scene notification
			And with parameter sequencenumber 1
			And with parameter scenenumber 2
			And send to driver
#nothing happens
		Then both busses should be empty