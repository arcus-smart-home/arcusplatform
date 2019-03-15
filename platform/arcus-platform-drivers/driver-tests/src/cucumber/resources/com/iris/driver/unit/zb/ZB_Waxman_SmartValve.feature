@Zigbee
Feature: Test of the Waxman Smart Valve driver
	
	These scenarios test the functionality of the AlertMe binary sensor.

	Background:
		Given the ZB_Waxman_SmartValve.driver has been initialized
			And the device has endpoint 1 
				
	Scenario: Driver reports capabilities to platform.
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devconn', 'devpow', 'ident', 'test', 'valv']
			And the message's dev:devtypehint attribute should be Water Valve
			And the message's devadv:drivername attribute should be ZBWaxmanSmartValve 
			And the message's devadv:driverversion attribute should be 1.0
			And the message's devpow:source attribute should be BATTERY
			And the message's devpow:linecapable attribute should be true		
		Then both busses should be empty
 		
 	Scenario: Device associated
		When the device is added
		#When the device has been associated
 	
 	Scenario: Device connected
 		When the device is connected
		#When the device connects to the platform
 		Then the driver should place a base:ValueChange message on the platform bus 
 		Then protocol message count is 12 
		Then the driver should send pollcontrol 2
		Then the driver should send pollcontrol 3
		Then the driver should send onoff zclReadAttributes
		Then the driver should send basic zclReadAttributes
		Then the driver should send power zclReadAttributes
		Then the driver should send pollcontrol zclReadAttributes
		Then the driver should send 0x0B02 0
		Then the driver should send basic ZclConfigureReporting
		Then the driver should send power ZclConfigureReporting
		Then the driver should send onoff ZclConfigureReporting
		Then the driver should send pollcontrol ZclWriteAttributes
		Then the driver should set timeout
		Then both busses should be empty		
 	
 	Scenario Outline: setAttributes for valv
 		When the capability method base:SetAttributes 
 		And with capability valv:valvestate is <valvestate>
 		And send to driver
		Then the driver should place a EmptyMessage message on the platform bus
 		Then the driver should send onoff <messageID>
 		And the driver should place a base:ValueChange message on the platform bus
 			
 		Examples:		
 			| valvestate | messageID |
 			| OPEN       | 1         |
 			| CLOSED     | 0         |
 		
 	#onEvent('DelayedRead')
 	Scenario: DelayedRead
		When event DelayedRead trigger 
		Then the driver should send onoff zclReadAttributes
			And with payload ATTR_ONOFF
		
 	#onIdentify.Identify
 	
 	#onZigbeeMessage.Zcl.onoff.zclreadattributesresponse() 
 	#onZigbeeMessage.Zcl.onoff.zclreportattributes() 
 	Scenario Outline: Device sending onoff response
 		Given the driver variable obstructed is false
 			And the capability valv:valvestate is <previous>
		When the device response with onoff <responseType>
			And with parameter ATTR_ONOFF <on_off>
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability valv:valvestate should be <new>

		Examples:
			| responseType				| previous | on_off | new    | 
			| zclreadattributesresponse	| OPEN     | 0      | CLOSED | 
			| zclreportattributes   	| OPEN     | 0      | CLOSED | 
			| zclreadattributesresponse	| CLOSED   | 1      | OPEN   | 
			| zclreportattributes   	| CLOSED   | 1      | OPEN   | 

 	
 	#onZigbeeMessage.Zcl.basic.zclreadattributesresponse()
 	#onZigbeeMessage.Zcl.basic.zclreportattributes()
 	Scenario Outline: Device sending basic response
 	 	Given the capability devpow:source is <previous>
		When the device response with basic <responseType>
			And with parameter ATTR_POWER_SOURCE <source>
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability devpow:source should be <value>
			And the capability devpow:sourcechanged should be recent
			
		Examples:
			| responseType				| previous| source | value   |
			| zclreadattributesresponse	| LINE    | 3      | BATTERY |
			| zclreadattributesresponse	| LINE    | 3      | BATTERY |
			| zclreportattributes   	| BATTERY | 1      | LINE    |		
			| zclreportattributes   	| BATTERY | 1      | LINE    |	
			
	# Don't seems to have any purpose
 	#onZigbeeMessage.Zcl.pollcontrol.zclreadattributesresponse()
 	#onZigbeeMessage.Zcl.pollcontrol.zclreportattributes()
 	#onZigbeeMessage.Zcl.pollcontrol.checkin()

 	
 	#onZigbeeMessage.Zcl.power.zclreadattributesresponse() 	
 	#onZigbeeMessage.Zcl.power.zclreportattributes() 
 	 Scenario Outline: Device sending power response
		When the device response with power <responseType>
			And with parameter ATTR_BATTERY_VOLTAGE <voltage>
			And with parameter ATTR_BATTERY_VOLTAGE_MIN_THRESHOLD <threshold>
			And with parameter ATTR_BATTERY_ALARM_MASK 0x00
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability devpow:battery should be 72

			
		Examples:
			| responseType				| voltage | threshold | 
			| zclreadattributesresponse	| 45      | 5         |	
			| zclreportattributes   	| 45      | 5         |	
 	
