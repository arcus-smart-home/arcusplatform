@ZWave
Feature: Contact sensor capability for the ZWSensativeContactSensorDriver

	These scenarios test the functionality of the Sensative Strips contact sensor driver.

	Background:
		Given the ZW_Sensative_ContactSensor.driver has been initialized
				
	Scenario: Driver reports capabilities to platform. 
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'cont' ]
			And the message's dev:devtypehint attribute should be Contact
			And the message's devadv:drivername attribute should be ZWSensativeContactSensorDriver 
			And the message's devadv:driverversion attribute should be 1.0
			And the message's devpow:source attribute should be BATTERY
			And the message's devpow:linecapable attribute should be false		
		#Then both busses should be empty
			
	Scenario Outline: Contact state has changed
		When the device response with alarm report
			And with parameter alarmtype <atype>
			And with parameter alarmlevel <alevel>
			And with parameter notificationstatus <nstatus>
			And with parameter notificationtype <ntype>
			And with parameter event <event>
			And send to driver
		Then the platform attribute cont:contact should change to <state>
			And the driver should place a base:ValueChange message on the platform bus
		
		Examples:
		  | state     | atype | alevel | nstatus | ntype | event |
		  | OPENED    | 0     | 0      | -1      | 6     | 22    |
		  | CLOSED    | 0     |	0      | -1      | 6     | 23    |	
