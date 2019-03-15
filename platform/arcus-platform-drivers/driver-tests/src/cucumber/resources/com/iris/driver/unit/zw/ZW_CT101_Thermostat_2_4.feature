@ZWave @therm24 @CT101
Feature: ZWave CT101 Thermostat Driver Test

	These scenarios test the functionality of the ZWave CT101 Thermostat driver.

	Background:
		Given the ZW_CT101_Thermostat_2_4.driver has been initialized
		# These should be initialized in the driver to avoid NPEs in production
        And the capability therm:heatsetpoint is 19
    	And the capability therm:coolsetpoint is 25
    	And the driver variable expectedHeatSetpoint is 19
    	And the driver variable expectedCoolSetpoint is 25
    	
    @basic
	Scenario: Driver reports capabilities to platform.
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'therm', 'temp', 'humid', 'indicator', 'clock']
			And the message's dev:devtypehint attribute should be Thermostat
			And the message's devadv:drivername attribute should be ZWRadioThermostat 
			And the message's devadv:driverversion attribute should be 2.4
			And the message's devpow:linecapable attribute should be true
			And the message's indicator:enabled attribute should be true
			And the message's indicator:enableSupported attribute should be false
			And the message's therm:maxfanspeed attribute should be 1
			And the message's therm:autofanspeed attribute should be 1
			And the message's therm:supportsAuto attribute should be true
		#from initializing the setpoints
		And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty

############################################################
# Generic Driver Tests
############################################################

	Scenario Outline: Make sure driver allows device name to be set 
		When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
		Then the platform attribute dev:name should change to <value>

		Examples:
		  | value                    |
		  | Thermostat               |
		  | "My Device"              |
		  | "Tom's Device"           |
		  | "Bob & Sue's Device"     |


############################################################
# Generic Thermostat Driver Tests
############################################################

	Scenario Outline: Make sure driver processes user writable Thermostat attributes 
		When a base:SetAttributes command with the value of <type> <value> is placed on the platform bus
		Then the platform attribute <type> should change to <value>

		Examples:
		  | type                        | value                  |
		  | therm:filtertype            | Size:16x25x1           |
		  | therm:filterlifespanruntime | 720                    |
		  | therm:filterlifespandays    | 180                    |

	Scenario: Make sure driver has implemented onThermostat.changeFilter 
		When a therm:changeFilter command is placed on the platform bus
		Then the platform attribute therm:dayssincefilterchange should change to 0
		Then the platform attribute therm:runtimesincefilterchange should change to 0
			And the driver should place a therm:changeFilterResponse message on the platform bus
		Then protocol bus should be empty

	Scenario Outline: Make sure driver has implemented onThermostat.SetIdealTemperature
		Given the capability therm:hvacmode is <hvac-mode>
			And the capability therm:heatsetpoint is <curr-heat>
			And the capability therm:coolsetpoint is <curr-cool>
		When a therm:SetIdealTemperature command with argument temperature of value <setpoint> is placed on the platform bus
		Then the driver should place a therm:SetIdealTemperatureResponse message on the platform bus
			And the message's result attribute should be <result>
			And the message's hvacmode attribute should be <hvac-mode>
			And the message's idealTempSet attribute numeric value should be within delta 0.05 of <new-sp>
			And the message's prevIdealTemp attribute numeric value should be within delta 0.05 of <prev-sp>

		Examples:
		  | hvac-mode | curr-heat | curr-cool | setpoint | result | prev-sp | new-sp    | remark                                          |
		  | HEAT      | 20        | 30        | 24       | true   | 20      | 24        | Set Heat set point while in HEAT mode           |
		  | COOL      | 20        | 30        | 26.66667 | true   | 30      | 26.66667  | Set Cool set point while in COOL mode           |
		  | AUTO      | 20        | 30        | 26.66667 | true   | 25      | 26.66667  | Set Heat and Cool set points while in AUTO mode |

	Scenario Outline: Make sure driver has implemented onThermostat.IncrementIdealTemperature
		Given the capability therm:hvacmode is <hvac-mode>
			And the capability therm:heatsetpoint is <curr-heat>
			And the capability therm:coolsetpoint is <curr-cool>
		When a therm:IncrementIdealTemperature command with argument amount of value <delta> is placed on the platform bus
		Then the driver should place a therm:IncrementIdealTemperatureResponse message on the platform bus
			And the message's result attribute should be <result>
			And the message's hvacmode attribute should be <hvac-mode>
			And the message's idealTempSet attribute numeric value should be within delta 0.05 of <new-sp>
			And the message's prevIdealTemp attribute numeric value should be within delta 0.05 of <prev-sp>

		Examples:
		  | hvac-mode | curr-heat | curr-cool | delta   | result | prev-sp | new-sp   | remark                                                  |
		  | HEAT      | 20        | 30        | 1.11111 | true   | 20      | 21.11111 | Delta affects Heat set point when in HEAT mode          |
		  | COOL      | 20        | 30        | 1.11111 | true   | 30      | 31.11111 | Delta affects Cool set point when in COOL mode          |
		  | AUTO      | 20        | 30        | 1.11111 | true   | 25      | 26.11111 | Delta affects Heat and Cool set point when in AUTO mode |

	Scenario Outline: Make sure driver has implemented onThermostat.DecrementIdealTemperature
		Given the capability therm:hvacmode is <hvac-mode>
			And the capability therm:heatsetpoint is <curr-heat>
			And the capability therm:coolsetpoint is <curr-cool>
		When a therm:DecrementIdealTemperature command with argument amount of value <delta> is placed on the platform bus
		Then the driver should place a therm:DecrementIdealTemperatureResponse message on the platform bus
			And the message's result attribute should be <result>
			And the message's hvacmode attribute should be <hvac-mode>
			And the message's idealTempSet attribute numeric value should be within delta 0.05 of <new-sp>
			And the message's prevIdealTemp attribute numeric value should be within delta 0.05 of <prev-sp>

		Examples:
		  | hvac-mode | curr-heat | curr-cool | delta   | result | prev-sp | new-sp   | remark                                                  |
		  | HEAT      | 20        | 30        | 1.11111 | true   | 20      | 18.88889 | Delta affects Heat set point when in HEAT mode          |
		  | COOL      | 20        | 30        | 1.11111 | true   | 30      | 28.88889 | Delta affects Cool set point when in COOL mode          |
		  | AUTO      | 20        | 30        | 1.11111 | true   | 25      | 23.88889 | Delta affects Heat and Cool set point when in AUTO mode |



    Scenario Outline: Platform controls HVAC system via attribute change.
		When a base:SetAttributes command with the value of therm:hvacmode <mode> is placed on the platform bus
		Then the driver should place a EmptyMessage message on the platform bus
		Then the driver should schedule event SyncState in 0 ms with {'attempt': 1} 
			And the driver variable expectedHvacMode should be <mode>
		#from initializing the setpoints
		And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty

    	Examples:
		  | mode   |
		  | OFF    |
		  | HEAT   |
		  | COOL   |
		  | AUTO   |
		  
	Scenario Outline: Platform set fan mode via attribute change. 
		When a base:SetAttributes command with the value of therm:fanmode <mode> is placed on the platform bus
		Then the driver should place a EmptyMessage message on the platform bus
		Then the driver should schedule event SyncState in 0 ms with {'attempt': 1} 
			And the driver variable expectedFanMode should be <mode>
		#from initializing the setpoints
		And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty

	   	Examples:
			  | mode  | value |
			  | 0     | 0     |
			  | 1     | 1     |

	Scenario Outline: Platform sets a change in the target setpoint 
		When a base:SetAttributes command with the value of therm:<attribute> <value> is placed on the platform bus
		Then the driver should place a EmptyMessage message on the platform bus
		Then the driver should schedule event SyncState in 0 ms with {'attempt': 1} 
			And the numeric driver variable expectedCoolSetpoint should be within 1.5% of <cool>
			And the numeric driver variable expectedHeatSetpoint should be within 1.5% of <heat>
		#from initializing the setpoints
		And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty

			Examples:
				|  attribute   	| value 	|  cool   |   heat  |
				|	coolsetpoint	|	26			|		26		|		19		|
				|	coolsetpoint	|	21			|		21		|		19		|
				|	heatsetpoint	|	20			|		25		|		20		|
				|	heatsetpoint	|	26			|		27.5	|		26		|

	Scenario Outline: Platform sets a change in the target setpoint 
		When the capability method base:SetAttributes
		    And with capability therm:coolsetpoint is <coolsetpoint>
		    And with capability therm:heatsetpoint is <heatsetpoint>
		    And with capability therm:hvacmode is <hvacmode>
		    And send to driver
		Then the driver should place a EmptyMessage message on the platform bus
		Then the driver should schedule event SyncState in 0 ms with {'attempt': 1} 
			And the numeric driver variable expectedCoolSetpoint should be within 1.5% of <cool>
			And the numeric driver variable expectedHeatSetpoint should be within 1.5% of <heat>
		#from initializing the setpoints
		And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty

			Examples:
                | coolsetpoint | heatsetpoint | hvacmode | cool  | heat  | remarks                                                                                           |
                | 25           | 22           | HEAT     | 25    | 22    | ignore cool set point (same as prev val) when HVAC is HEAT                                        |
                | 26           | 22           | HEAT     | 25    | 22    | ignore cool set point (new val) when HVAC is HEAT                                                 |
                | 25           | 24           | HEAT     | 25.67 | 24    | ignore cool set point when HVAC is HEAT, but enforce set point separation                         |
                | 27           | 24           | HEAT     | 25.67 | 24    | ignore cool set point when HVAC is HEAT, but enforce set point separation                         |
                | 27           | 19           | COOL     | 27    | 19    | ignore heat set point (same as prev val) when HVAC is COOL                                        |
                | 27           | 22           | COOL     | 27    | 19    | ignore heat set point (new val) when HVAC is COOL                                                 |
                | 20           | 19           | COOL     | 20    | 18.33 | ignore heat set point when HVAC is COOL, but enforce set point separation                         |
                | 20           | 16           | COOL     | 20    | 18.33 | ignore heat set point when HVAC is COOL, but enforce set point separation                         |
                | 27           | 22           | AUTO     | 27    | 22    | process both set points if HVAC mode is AUTO                                                      |
                | 22           | 21           | AUTO     | 22    | 20.33 | process both set points if HVAC mode is AUTO, but enforce set point separation, Cool has priority |
                | 27           | 22           | OFF      | 27    | 22    | process both set points if HVAC mode is OFF                                                       |
                | 22           | 21           | OFF      | 22    | 20.33 | process both set points if HVAC mode is OFF, but enforce set point separation, Cool has priority  |

 
  Scenario Outline: Platform signals a change in the temperature level.
    Given the driver variable <type> is <temp_celcius>
		When event SyncState triggers with {'attempt': 1} 
		Then the driver should send Thermostat_Setpoint set				
		And with parameter type <setpoint>
				# precision 2; units deg F; 2 Bytes
				And with parameter scale 74
				And with parameter value1 <value1>
				And with parameter value2 <value2>
				And with parameter value3 <value3>
				And with parameter value4 <value4>
		#from initializing the setpoints
		And the driver should place a base:ValueChange message on the platform bus
		Then the driver should send Thermostat_Setpoint get				
		Then both busses should be empty

    	Examples:
    	  | type    	           	| temp_celcius | setpoint | value1 | value2 | value3 | value4 |
    	  | expectedHeatSetpoint | 20           | 1        | 26     | -112   | 0      | 0      |
    	  | expectedCoolSetpoint | 1.66667      | 2        | 13     | 172    | 0      | 0      |
    	  	
	Scenario Outline: Platform set filter via attribute change. 
		When a base:SetAttributes command with the value of <type> <value> is placed on the platform bus
		Then the platform attribute <type> should change to <value>

  	Examples:
    	  | type                        | value |
    	  | therm:filtertype            | TEST  |
    	  | therm:filterlifespanruntime | 99    |
    	  | therm:filterlifespandays    | 100   |
 
    Scenario: Platform signals that a filter has been changed. 
		When a therm:changeFilter command is placed on the platform bus
		Then the platform attribute therm:dayssincefilterchange should change to 0
		Then the platform attribute therm:runtimesincefilterchange should change to 0
			And the driver should place a therm:changeFilterResponse message on the platform bus
    	Then protocol bus should be empty
    	
    Scenario Outline: Device reports an indicator value
		When the device response with indicator report 
			And with parameter value <value>
			And send to driver 
		Then the platform attribute indicator:indicator should change to <state>
    		And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty
		
		Examples:
		  | value | state |
		  | 0     | OFF   |
		  | 1     | ON    |
	
	Scenario Outline: Device reports an operating state value
		When the device response with thermostat_operating_state report 
			And with parameter state <value>
			And send to driver
		Then the platform attribute therm:active should change to <state>
    		And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty
		
		Examples:
		  | value | state |
		  | 0     | NOTRUNNING |
		  | 1     | RUNNING    |
		  
	Scenario Outline: Device reports a new fan mode
		When the device response with thermostat_fan_mode report
			And with parameter mode <rpt_mode>
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
    		And the platform attribute therm:fanmode should change to <speed>
  		Then both busses should be empty
		
		Examples:
		  | rpt_mode | speed |
		  | 0x00     | 0     |
		  | 0x01     | 1     |

	Scenario: Device reports multilevel sensor values
		When the device response with sensor_multilevel report
			And with parameter type 1
			And with parameter level 42
			And with parameter val1 2
			And with parameter val2 -18
			And with parameter val3 0
			And with parameter val4 0
			And send to driver
		Then the numeric capability temp:temperature should be within 1% of 23.9
    		And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty
		
	Scenario Outline: Device reports thermostat setpoint values
		Given the capability <opp_type> is <opp_value> 
		When the device response with thermostat_setpoint report
			And with parameter type <type>
			And with parameter scale <scale>
			And with parameter value1 <value1>
			And with parameter value2 <value2>
			And with parameter value3 <value3>
			And with parameter value4 <value4>
			And send to driver
		Then the numeric capability therm:<attr> should be within 0.01% of <exp_val>
    		And the driver should place a Thermostat:SetPointChanged message on the platform bus
    		And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty

		Examples:
		  | opp_type           | opp_value | type | scale | value1 | value2 | value3 | value4 | attr         | exp_val | 
		  | therm:coolsetpoint | 25.0      | 1    | 42    | 2      | 204    | 0      | 0      | heatsetpoint | 22.0    | 
		  | therm:coolsetpoint | 25.0      | 1    | 42    | 3      | 38     | 0      | 0      | heatsetpoint | 27.0    | 
		  | therm:coolsetpoint | 25.0      | 1    | 42    | 3      | 182    | 0      | 0      | heatsetpoint | 35.0    | 
		  | therm:heatsetpoint | 25.0      | 2    | 42    | 2      | 204    | 0      | 0      | coolsetpoint | 22.0    | 
		  | therm:heatsetpoint | 25.0      | 2    | 42    | 3      | 38     | 0      | 0      | coolsetpoint | 27.0    |
		  | therm:heatsetpoint | 25.0      | 2    | 42    | 1      | 76     | 0      | 0      | coolsetpoint | 0.66667 | 

	Scenario Outline: Device reports battery level
		Given the capability devpow:battery is 1
		When the device response with battery report
			And with parameter level <level-arg>
			And send to driver 
		Then the platform attribute devpow:battery should change to <battery-attr>
			And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty
		
		Examples:
		  | level-arg | battery-attr |
		  | -1        | 0            |
		  | 50        | 50           |
		  | 100       | 100          |
		  
	Scenario Outline: Device reports configuration power source		
		When the device response with configuration report
			And with parameter param <param1>
			And with parameter level 1
			And with parameter val1 <val1>
			And send to driver
		Then the platform attribute devpow:source should change to <power-source-attr>
			And the driver should place a base:ValueChange message on the platform bus
		Then both busses should be empty

		Examples:
		  | param1 | val1 | power-source-attr |
		  | 4      | 1    | LINE              |
		  | 4      | 2    |	BATTERY           |

	Scenario: Device report humidity
		When the device response with multi_channel instance_cmd_encap
			And with parameter command_class 49
			And with parameter command 5
			And with parameter parameter1 5
			And with parameter parameter2 1
			And with parameter parameter3 1
			And send to driver
		Then the platform attribute humid:humidity should change to 1.0
			And the driver should place a base:ValueChange message on the platform bus
		    And both busses should be empty
		    
	Scenario Outline: Device report thermostat fan mode
		When the device response with thermostat_fan_mode report
			And with parameter mode <mode>
			And send to driver
		Then the platform attribute therm:fanmode should change to <mode>
			And the driver should place a base:ValueChange message on the platform bus
		    And both busses should be empty		    
		    
		Examples:
		  | mode |
		  | 0    |
		  | 1    |  
		  
	Scenario Outline: Device report thermostat fan state
		When the device response with thermostat_fan_state report
			And with parameter state <state>
			And send to driver
		Then the platform attribute therm:active should change to <active>
			And the driver should place a base:ValueChange message on the platform bus
		    And both busses should be empty	
		    
		Examples:
		  | state | active     |
		  | 0     | NOTRUNNING |
		  | 1     | RUNNING    |    
		  
	Scenario: Device report clock
		When the device response with clock report
			And with parameter weekday_hour 1
			And with parameter minute 30
			And send to driver
		Then the platform attribute clock:hour should change to 1
			And the platform attribute clock:minute should change to 30
			And the driver should place a base:ValueChange message on the platform bus
		    And both busses should be empty			  