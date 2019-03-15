@Zigbee  @Heater	
Feature: Zigbee Twinstar (Duraflame) Heater Test
	These scenarios test the functionality of the Twinstar (Duraflame) Heater

Background:
	Given the ZB_Twinstar_Heater.driver has been initialized
	And the device has endpoint 1

	Scenario: Driver reports capabilities to platform.
	When a base:GetAttributes command is placed on the platform bus
	Then the driver should place a base:GetAttributesResponse message on the platform bus
	And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devconn', 'devpow', 'ident', 'devota', 'temp', 'spaceheater', 'twinstar']
	And the message's dev:devtypehint attribute should be SpaceHeater
	And the message's devadv:drivername attribute should be ZBTwinstarHeater 
	And the message's devadv:driverversion attribute should be 1.0
	And the message's devpow:source attribute should be LINE
	And the message's devpow:linecapable attribute should be true	
	And the message's spaceheater:minsetpoint attribute should be 10
	And the message's spaceheater:maxsetpoint attribute should be 36.11
	And the capability devadv:errors should be [:]
		
	Then both busses should be empty

	Scenario: Device Added to Platform
	When the device is added
	Then protocol message count is 1
#  TODO Parse that it's a Bind Request
	And the platform bus should be empty	

	Scenario: Device Connects to Platform
	 	When the device is connected
		Then the driver should set timeout
		Then the driver should send thermostat zclReadAttributes
		Then the driver should send thermostatUi zclReadAttributes
#		Then the driver should send metering zclReadAttributes
		Then the driver should send appliancealerts 0x00	
 		Then the driver should place a base:ValueChange message on the platform bus
 			And the capability devconn:state should be ONLINE  
		Then both busses should be empty	
@test2
	Scenario Outline: Platform controls heater via attribute changes.
		When a base:SetAttributes command with the value of <attribute> <value> is placed on the platform bus
		Then the driver should send thermostat zclWriteAttributes
			And with attribute <zb_attribute> value <zb_value>
		Then the driver should place a EmptyMessage message on the platform bus
		Then both busses should be empty	


		Examples:
			|attribute              | value    | zb_attribute                                | zb_value 																					|   response											|
			|spaceheater:heatstate  | ON       | ATTR_SYSTEM_MODE                            |  SYSTEM_MODE_HEAT    															|	spaceheater:SetAttributesResponse	|
			|spaceheater:heatstate  | OFF      | ATTR_SYSTEM_MODE                            |  SYSTEM_MODE_OFF    															|	spaceheater:SetAttributesResponse	|
			|spaceheater:setpoint   | 10       | ATTR_OCCUPIED_HEATING_SETPOINT              |  1000																							|	spaceheater:SetAttributesResponse	|
			|spaceheater:setpoint   | 9.99     | ATTR_OCCUPIED_HEATING_SETPOINT              |  1000																							|	spaceheater:SetAttributesResponse	|
			|spaceheater:setpoint   | 36       | ATTR_OCCUPIED_HEATING_SETPOINT              |  3600																							|	spaceheater:SetAttributesResponse	|
			|spaceheater:setpoint   | 36.2	   | ATTR_OCCUPIED_HEATING_SETPOINT              |  3611																							|	spaceheater:SetAttributesResponse	|
			|twinstar:ecomode       | ENABLED  | ATTR_THERMOSTAT_PROGRAMMING_OPERATION_MODE  |  THERMOSTAT_PROGRAMMING_OPERATION_MODE_ECONOMY_ON	| twinstar:SetAttributesResponse  |
			|twinstar:ecomode       | DISABLED | ATTR_THERMOSTAT_PROGRAMMING_OPERATION_MODE  |  THERMOSTAT_PROGRAMMING_OPERATION_MODE_ECONOMY_OFF| twinstar:SetAttributesResponse	|
			
	Scenario Outline:  Platform is updated when attributes report or respond
	 	Given the capability <namespace>:<attribute> is <previous>
		When the device response with <cluster> <responseType>
			And with parameter <zb_attribute> <data>
			And send to driver

		Then the driver should place a base:ValueChange message on the platform bus
			And the capability <namespace>:<attribute> should be <after>
		Then both busses should be empty

		Examples:
			| namespace 	| attribute 	| previous	| cluster					|responseType								|		zb_attribute															| data 																								| after			|
			|	temp				|temperature	| 12.5			|thermostat 			| zclreadattributesresponse	|	ATTR_LOCAL_TEMPERATURE											| 2357   																							| 23.57 		|
			|	temp				|temperature	| 0					|thermostat 			| zclreportattributes   		| ATTR_LOCAL_TEMPERATURE											| 2813   																							| 28.13 		|	
#	Commented Out Test Cases document the enum meanings for the thermostat attributes.
#			| spaceheater | heatstate		| ON				|thermostat 			| zclreadattributesresponse	| ATTR_SYSTEM_MODE        										| SYSTEM_MODE_OFF																			|	OFF				|
			| spaceheater | heatstate		| ON				|thermostat 			| zclreadattributesresponse	| ATTR_SYSTEM_MODE        										| 0																										|	OFF				|
#			| spaceheater | heatstate   | OFF				|thermostat 			| zclreportattributes				| ATTR_SYSTEM_MODE        										| SYSTEM_MODE_HEAT																		|	ON				|
			| spaceheater | heatstate  	| OFF				|thermostat 			| zclreportattributes				| ATTR_SYSTEM_MODE        										| 4																										|	ON				|
			| spaceheater | setpoint 	  | 24				|thermostat 			| zclreportattributes				| ATTR_OCCUPIED_HEATING_SETPOINT       				| 2567																								|	25.67			|
#			| twinstar 		| ecomode     | ENABLED		|thermostat 			| zclreadattributesresponse	| ATTR_THERMOSTAT_PROGRAMMING_OPERATION_MODE 	| THERMOSTAT_PROGRAMMING_OPERATION_MODE_ECONOMY_OFF		|	DISABLED	|
			| twinstar 		| ecomode     | ENABLED		|thermostat 			| zclreadattributesresponse	| ATTR_THERMOSTAT_PROGRAMMING_OPERATION_MODE 	| 0																										|	DISABLED	|
#			| twinstar 		| ecomode     | DISABLED	|thermostat 			| zclreadattributesresponse	| ATTR_THERMOSTAT_PROGRAMMING_OPERATION_MODE 	| THERMOSTAT_PROGRAMMING_OPERATION_MODE_ECONOMY_ON		|	ENABLED		|
			| twinstar 		| ecomode     | DISABLED	|thermostat 			| zclreadattributesresponse	| ATTR_THERMOSTAT_PROGRAMMING_OPERATION_MODE 	| 1																										|	ENABLED		|

	Scenario Outline: Driver sets alerts alerts
		When the device response with appliancealerts <responseType>
			And with parameter numberAlerts <numberAlerts>
			And with parameter data <data>
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability devadv:errors should be <new>
		Then both busses should be empty

			Examples:
			| responseType				| numberAlerts|data 				| new    	| 
			| getalertsresponse   | 1      			|  -128,17,0 	|	[Overheat Shutdown:E3: The heater has overheated.  Check that the inlets/outlets are not blocked, unplug the heater and allow it to cool down before use again.] |
			| alertsnotification  | 1      			|  -127,17,0 	|	[Safer Plug Shutdown:ER: The Safer Plug has detected possible outlet overheating.  Check the plug and outlet temperature, if overheated discontinue use of that outlet.] |
			| alertsnotification  | 1      			|  -126,17,0 	|	[Safer Sensor Shutdown:The Safer Sensor has detected the heater outlet may be blocked.  Check the heater to ensure that no objects are in front of the heater outlet.] |
			| alertsnotification  | 1      			|  -125,17,0 	|	[Thermostat Disconnected:E1: The thermostat is disconnected or damaged; contact Duraflame customer service at 1-800-318-9373] |
			| alertsnotification  | 1      			|  -124,17,0 	|	[Thermostat Broken:E2: The thermostat is damaged; contact Duraflame customer service at 1-800-318-9373] |

Scenario: Set and Clear Multiple Alerts
		When the device response with appliancealerts alertsnotification
			And with parameter numberAlerts 1
			And with parameter data -128,17,0
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
		When the device response with appliancealerts alertsnotification
			And with parameter numberAlerts 1
			And with parameter data -127,17,0
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability devadv:errors should be [Safer Plug Shutdown:ER: The Safer Plug has detected possible outlet overheating.  Check the plug and outlet temperature, if overheated discontinue use of that outlet., Overheat Shutdown:E3: The heater has overheated.  Check that the inlets/outlets are not blocked, unplug the heater and allow it to cool down before use again.]	
		When the device response with appliancealerts alertsnotification
			And with parameter numberAlerts 1
			And with parameter data -127,1,0
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability devadv:errors should be [Overheat Shutdown:E3: The heater has overheated.  Check that the inlets/outlets are not blocked, unplug the heater and allow it to cool down before use again.]			
		When the device response with appliancealerts getalertsresponse
			And with parameter numberAlerts 0
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability devadv:errors should be [:]
		Then both busses should be empty

#Next 3 tests need the ability to send attribute data to be meaningful
#Also support for energy pulled until version that supports it well.

#@Ignore
#	Scenario:  Power/Energy not updated with initial production version
#	 		And the capability pow:cumulative is 0
#	 		And the capability devota:currentVersion is 6055700
#		When the device response with metering zclreportattributes
#		And with parameter ATTR_INSTANTANEOUS_DEMAND   10
#		And with parameter ATTR_CURRENT_SUMMATION_DELIVERED   5			
#			And send to driver
#	Then the capability pow:instantaneous should be 0
#		And the capability pow:cumulative should be 0
#@Ignore
#	Scenario:  Power/Energy not updated without version
#	 	Given the capability pow:instantaneous is 0
#	 		And the capability pow:cumulative is 0
#		When the device response with metering zclreportattributes
#			And with parameter ATTR_CURRENT_MAX_DEMAND_DELIVERED   10
#			And with parameter ATTR_CURRENT_SUMMATION_DELIVERED   5			
#			And send to driver
#	Then the capability pow:instantaneous should be 0
#		And the capability pow:cumulative should be 0
#@Ignore
#	Scenario:  Power/Energy updated after upgrade production version
#	 	Given the capability pow:instantaneous is 0
#	 		And the capability pow:cumulative is 0
#	 		And the capability devota:currentVersion is 6065700
#		When the device response with metering zclreportattributes
#		And with parameter ATTR_INSTANTANEOUS_DEMAND   10
#		And with parameter ATTR_CURRENT_SUMMATION_DELIVERED   5			
#			And send to driver
#	Then the capability pow:instantaneous should be 10
#		And the capability pow:cumulative should be 50
			