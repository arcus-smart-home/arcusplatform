@ZWave
Feature: ZWave GoCtrl Thermostat Driver Test

These scenarios test the functionality of the ZWave GoCtrl Thermostat driver

	Background:
	Given the ZW_GoCtrl_Thermostat.driver has been initialized
	
	Scenario: Driver reports capabilities to platform. 
	When a base:GetAttributes command is placed on the platform bus
	Then the driver should place a base:GetAttributesResponse message on the platform bus
		And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'temp','humid','therm','clock']
		And the message's dev:devtypehint attribute should be Thermostat
		And the message's devadv:drivername attribute should be ZWGoCtrlThermostat 
		And the message's devadv:driverversion attribute should be 1.0
		# And the message's devpow:source attribute should be BATTERY
		And the message's devpow:linecapable attribute should be true
	Then both busses should be empty
	
	Scenario: Device connected
		When the device is connected
		#When the device connects to the platform
		Then the driver should send configuration get
		Then the driver should send thermostat_mode get