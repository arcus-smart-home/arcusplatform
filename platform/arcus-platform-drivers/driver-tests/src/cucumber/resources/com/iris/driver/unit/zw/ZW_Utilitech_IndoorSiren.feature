@ZWave
Feature: Utilitech Indoor Siren Driver Unit Tests

	These scenarios test the functionality of the Utilitech Indoor Siren driver.

	Background:
		Given the ZW_Utilitech_Indoor_Siren.driver has been initialized
		
	Scenario: Driver reports capabilities to platform. 
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'alert', 'ident']
			And the message's dev:devtypehint attribute should be Siren
			And the message's devadv:drivername attribute should be ZWaveUtilitechIndoorSirenDriver 
			And the message's devadv:driverversion attribute should be 1.0
			And the message's devpow:source attribute should be BATTERY
			And the message's devpow:linecapable attribute should be false		
		Then both busses should be empty
	
	Scenario: Platform turns on siren via attribute change. 
		When a base:SetAttributes command with the value of alert:state ALERTING is placed on the platform bus
		Then the driver should send switch_binary set 
			And with parameter value -1

	Scenario: Platform turns off siren via attribute change. 
		When a base:SetAttributes command with the value of alert:state QUIET is placed on the platform bus
		Then the driver should send switch_binary set 
			And with parameter value 0		
			
	Scenario Outline: Alert state changed by device report
		When the device responds with switch_binary report 
			And with parameter value <value>
			And send to driver
		Then the platform attribute alert:state should change to <state>
			And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty
			
		Examples:
		| value | state    |
		| -1    | ALERTING |
		| 0     | QUIET    |
		
	Scenario: Device reports alarm Low Battery
		When the device responds with alarm report 
			And with parameter alarmtype 1
			And with parameter alarmlevel 1
			And send to driver
		Then the driver should send battery get
		Then both busses should be empty

	Scenario Outline: Device reports battery level
		When the device responds with battery report 
			And with parameter level <level-arg>
			And send to driver 
		Then the platform attribute devpow:battery should change to <battery-attr>
			And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty

		Examples:
		  | level-arg | battery-attr |
		  | -1        | 0            |
		  | 1         | 1            |
		  | 100       | 100          |

