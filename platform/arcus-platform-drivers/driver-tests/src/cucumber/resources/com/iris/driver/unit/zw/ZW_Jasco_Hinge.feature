@ZWave @hinge
Feature: ZWave Jasco Hinge Driver Test

	These scenarios test the functionality of the ZWave Jasco Hinge Sensor driver.
	
	Background:
		Given the ZW_Jasco_HingePinDoorSensor.driver has been initialized
	
	Scenario: Driver reports capabilities to platform. 
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'cont']
			And the message's dev:devtypehint attribute should be Contact
			And the message's devadv:drivername attribute should be ZWJascoHingePinDoorSensorDriver 
			And the message's devadv:driverversion attribute should be 1.0
			And the message's devpow:source attribute should be BATTERY
			And the message's devpow:linecapable attribute should be false		
			And the message's cont:usehint attribute should be DOOR
		Then both busses should be empty

	Scenario: Device associated
		When the device is added
#The base:GetAttributes is required in the test harness or the base:ValueChange message
#does not get placed on the platform bus.
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability cont:contact should be OPENED
			And the capability cont:contactchanged should be recent
			And the capability devpow:sourcechanged should be recent
		Then both busses should be empty
 	
 	Scenario: Device connected
 		When the device is connected
 		Then the driver should place a base:ValueChange message on the platform bus
 			And the capability devconn:state should be ONLINE
 			And the capability devconn:lastchange should be recent
		Then the driver should set timeout at 190 minutes
		Then the driver should send Version get
		Then the driver should send Basic get
		Then the driver should poll Basic.get every 1 hour
		Then the driver should send Wake_Up set
			And with parameter seconds1 0
			And with parameter seconds2 0x1
			And with parameter seconds3 0xC2
			And with parameter node 1			
		Then the driver should poll Battery.get every 24 hours
		Then the driver should send Battery get
		Then the driver should schedule event config_complete in 2 seconds
		Then both busses should be empty
			
	Scenario: Device reports battery level
		When the device response with battery report 
			And with parameter level 57
			And send to driver 
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability devpow:battery should be 57
		Then both busses should be empty
	
	Scenario: Device sends a notification (alarm)
		When the device response with alarm report
			And with parameter alarmtype 0
			And with parameter alarmlevel 0
			And with parameter notificationstatus -1
			And with parameter notificationtype 6
			And with parameter event 22
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability cont:contact should be OPENED
			And the capability cont:contactchanged should be recent
		When the device response with alarm report
			And with parameter alarmtype 0
			And with parameter alarmlevel 0
			And with parameter notificationstatus -1
			And with parameter notificationtype 6
			And with parameter event 23
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability cont:contact should be CLOSED
			And the capability cont:contactchanged should be recent
			
		Scenario Outline: Device sends an invalid notification
			When the device response with alarm report
				And with parameter alarmtype <alarmtype>
				And with parameter alarmlevel <alarmlevel>
				And with parameter notificationstatus <notificationstatus>
				And with parameter notificationtype <notificationtype>
				And with parameter event <event>
				And send to driver
			#Then Nothing should happen
			Then both busses should be empty
			
			Examples:
			| alarmtype	| alarmlevel	| notificationstatus	| notificationtype	| event	  |
			|		1				|			0				|				-1						|				6						|		23		|
			|		0				|			2				|				-1						|				6						|		23		|
			|		0				|			0				|				 0						|				6						|		23		|
			|		0				|			0				|				-1						|				6						|		8		  |
			|		0				|			0				|				-1						|				7						|		23		|

#Jasco Security Sensors can be configured to send either a notification, basic set, or basic report.  Driver should handle any.						

	Scenario: Device sends basic report
		When the device response with basic report
			And with parameter value -1
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability cont:contact should be OPENED
			And the capability cont:contactchanged should be recent
		When the device response with basic report
			And with payload 0
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability cont:contact should be CLOSED
			And the capability cont:contactchanged should be recent
		Then both busses should be empty

		Scenario: Device sends basic set
		When the device response with basic set
			And with parameter value -1
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability cont:contact should be OPENED
			And the capability cont:contactchanged should be recent
		When the device response with basic set
			And with payload 0
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability cont:contact should be CLOSED
			And the capability cont:contactchanged should be recent
		Then both busses should be empty

#It doesn't send, but it says it supports binary sensor, and we should handle if it does
	Scenario: Device sends binary sensor report
		When the device response with sensor_binary report
			And with parameter sensorvalue -1
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability cont:contact should be OPENED
			And the capability cont:contactchanged should be recent
		When the device response with sensor_binary report
			And with payload 0
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability cont:contact should be CLOSED
			And the capability cont:contactchanged should be recent
		Then both busses should be empty

#Early Jasco sensors forget to wake up if time set >8 minutes
	Scenario: Device responds with old version
		When the device response with version report
			And with parameter protocol 4
			And with parameter sub-protocol 18
			And with parameter firmware_0_version 1
			And with parameter firmware_0_sub_version 23
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability devadv:firmwareVersion should be 4.18.1.23
		Then both busses should be empty
		
	Scenario: Device responds with new version
		When the device response with version report
			And with parameter protocol 4
			And with parameter sub-protocol 18
			And with parameter firmware_0_version 5
			And with parameter firmware_0_sub_version 14
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability devadv:firmwareVersion should be 4.18.5.14
		Then the driver should send Wake_Up set
			And with parameter seconds1 0
			And with parameter seconds2 0x0E
			And with parameter seconds3 0x10
			And with parameter node 1			
		Then both busses should be empty
		
	Scenario: Ask again if unknown version
		When the device response with version report
			And with parameter protocol 3
			And with parameter sub_protocol 6
			And with parameter firmware_0_version 4
			And with parameter firmware_0_sub_version 2
			And send to driver
		Then the driver should send version get
			And both busses should be empty
		
			