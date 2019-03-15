@ZWave @motion
Feature: ZWave Jasco Motion Driver Test

	These scenarios test the functionality of the ZWave Jasco Motion driver.
	
	Background:
		Given the ZW_Jasco_MotionSensor.driver has been initialized
	
	Scenario: Driver reports capabilities to platform. 
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'mot']
			And the message's dev:devtypehint attribute should be Motion
			And the message's devadv:drivername attribute should be ZWJascoMotionSensorDriver 
			And the message's devadv:driverversion attribute should be 1.0
			And the message's devpow:source attribute should be BATTERY
			And the message's devpow:linecapable attribute should be true		
		Then both busses should be empty

	Scenario: Device associated
		When the device is added
#The base:GetAttributes is required in the test harness or the base:ValueChange message
#does not get placed on the platform bus.
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability mot:motion should be NONE
			And the capability mot:motionchanged should be recent
		Then the driver should send configuration set
			And with parameter param 18
			And with parameter size 1
			And with parameter val1 1
		Then both busses should be empty
 	
 	Scenario: Device connected
 		When the device is connected
 		Then the driver should place a base:ValueChange message on the platform bus
 			And the capability devconn:state should be ONLINE
 			And the capability devconn:lastchange should be recent
		Then the driver should send Wake_Up set
			And with parameter seconds1 0
			And with parameter seconds2 0xA8
			And with parameter seconds3 0xC0
			And with parameter node 1
		Then the driver should set timeout
		Then the driver should poll Battery.get every 12 hours
		#		TODO: decode ZWave.poll
		Then the driver should send Battery get
		Then the driver should send Version get
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
			And with parameter notificationtype 7
			And with parameter event 8
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability mot:motion should be DETECTED
			And the capability mot:motionchanged should be recent
		When the device response with alarm report
			And with parameter alarmtype 0
			And with parameter alarmlevel 0
			And with parameter notificationstatus -1
			And with parameter notificationtype 7
			And with parameter event 0
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability mot:motion should be NONE
			And the capability mot:motionchanged should be recent
			
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
			| alarmtype	| alarmlevel	| notificationstatus	| notificationtype	| event	|
			|		1				|			0				|				-1						|				7						|		8		|
			|		0				|			2				|				-1						|				7						|		8		|
			|		0				|			0				|				 0						|				7						|		8		|
			|		0				|			0				|				-1						|				6						|		8		|
			|		0				|			0				|				-1						|				7						|		5		|

#Jasco Security Sensors can be configured to send either a notification, basic set, or basic report.  Driver should handle any.						
	Scenario: Device sends basic report
		When the device response with basic report
			And with parameter value -1
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability mot:motion should be DETECTED
			And the capability mot:motionchanged should be recent
		When the device response with basic report
			And with parameter value 0
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability mot:motion should be NONE
			And the capability mot:motionchanged should be recent
		Then both busses should be empty

		Scenario: Device sends basic set
		When the device response with basic set
			And with parameter value -1
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability mot:motion should be DETECTED
			And the capability mot:motionchanged should be recent
		When the device response with basic set
			And with parameter value 0
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability mot:motion should be NONE
			And the capability mot:motionchanged should be recent
		Then both busses should be empty
			