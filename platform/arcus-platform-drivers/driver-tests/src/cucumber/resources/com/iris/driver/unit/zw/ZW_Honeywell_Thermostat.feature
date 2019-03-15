@ZWave @therm @Honeywell
Feature: ZWave Honeywell Thermostat Driver Test

	These scenarios test the functionality of the ZWave Honeywell Thermostat driver.
	
	Background:
		Given the ZW_Honeywell_Thermostat.driver has been initialized
	
	Scenario: Driver reports capabilities to platform. 
	When a base:GetAttributes command is placed on the platform bus
	Then the driver should place a base:GetAttributesResponse message on the platform bus
		And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'temp','therm','humid']
		And the message's dev:devtypehint attribute should be Thermostat
		And the message's devadv:drivername attribute should be ZWHoneywellThermostat 
		And the message's devadv:driverversion attribute should be 1.0
		And the message's devpow:linecapable attribute should be true
		And the message's devpow:backupbatterycapable attribute should be false
		And the message's therm:maxfanspeed attribute should be 2
		And the message's therm:autofanspeed attribute should be 2
		And the message's therm:supportsAuto attribute should be true
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
		  | OFF       | 20        | 30        | 26.66667 | false  | 20      | 20        | Reject changes while in OFF mode                |

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
		  | OFF       | 20        | 30        | 1.11111 | false  | 20      | 20       | Delta has no effect when in OFF mode                    |

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
		  | OFF       | 20        | 30        | 1.11111 | false  | 20      | 20       | Delta has no effect when in OFF mode                    |

	Scenario: Device reports temperature in Celsius as multilevel sensor value 24.5C
		When the device response with sensor_multilevel report
			# type 1 = temperature
			And with parameter type 1
			# level 34 = prec:1, scale:0 (C=0,F=1), size:2 bytes
			And with parameter level 34
			# high order byte = 0
			And with parameter val1 0
			# low order byte = -11 (245)
			And with parameter val2 -11
			And with parameter val3 0
			And with parameter val4 0
			And send to driver
		Then the numeric capability temp:temperature should be within 1% of 24.4
			And the driver should place a base:ValueChange message on the platform bus

	Scenario: Device reports temperature in Fahrenheit as multilevel sensor value 75F (23.89C)
		When the device response with sensor_multilevel report
			# type 1 = temperature
			And with parameter type 1
			# level 42 = prec:1, scale:1 (C=0,F=1), size:2 bytes
			And with parameter level 42
			# want value of 750 = (2*256)+238
			# high order byte = 2 (2*256)
			And with parameter val1 2
			# low order byte = -18 (238)
			And with parameter val2 -18
			And with parameter val3 0
			And with parameter val4 0
			And send to driver
		Then the numeric capability temp:temperature should be within 1% of 23.9
			And the driver should place a base:ValueChange message on the platform bus

	Scenario: Device reports humidity as multilevel sensor value
		When the device response with sensor_multilevel report
			# type 5 = humidity
			And with parameter type 5
			And with parameter type 1
			# level 1 = prec:0, scale:0, size:1 byte
			And with parameter level 1
			And with parameter val1 50
			And with parameter val2 0
			And with parameter val3 0
			And with parameter val4 0
			And send to driver
		Then the numeric capability temp:temperature should be within 1% of 50
			And the driver should place a base:ValueChange message on the platform bus

	Scenario Outline: Device reports an operating state value
		When the device response with thermostat_operating_state report 
			And with parameter state <value>
			And send to driver
		Then the platform attribute therm:active should change to <state>
			And the driver should place a base:ValueChange message on the platform bus
		
		Examples:
		  | value | state      |
		  | 0     | NOTRUNNING |
		  | 1     | RUNNING    |

	Scenario Outline: Device reports an Operating mode
		Given the capability therm:hvacmode is <curr-mode>
		When the device response with thermostat_mode report
			And with parameter level <level>
			And send to driver
		Then the platform attribute therm:hvacmode should change to <new-mode>
			And the driver should place a base:ValueChange message on the platform bus
		
		Examples:
		  | curr-mode | level | new-mode |
		  | OFF       | 1     | HEAT     |
		  | OFF       | 2     | COOL     |
		  | OFF       | 3     | AUTO     |
		  | HEAT      | 0     | OFF      |
		  | HEAT      | 2     | COOL     |
		  | HEAT      | 3     | AUTO     |
		  | COOL      | 0     | OFF      |
		  | COOL      | 1     | HEAT     |
		  | COOL      | 3     | AUTO     |
		  | AUTO      | 0     | OFF      |
		  | AUTO      | 1     | HEAT     |
		  | AUTO      | 2     | COOL     |

	Scenario Outline: Device reports a new fan mode
		When the device response with thermostat_fan_mode report
			And with parameter mode <rpt_mode>
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the platform attribute therm:fanmode should change to <speed>
		
		Examples:
		  | rpt_mode | speed |
		  | 0x00     | 0     |
		  | 0x01     | 2     |

	Scenario Outline: Device report thermostat fan state
		When the device response with thermostat_fan_state report
			And with parameter state <state>
			And send to driver
		Then the platform attribute therm:active should change to <active>
			And the driver should place a base:ValueChange message on the platform bus
		    
		Examples:
		  | state | active     |
		  | 0     | NOTRUNNING |
		  | 1     | RUNNING    |    

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
		Then the numeric capability therm:<attr> should be within 1% of <exp_val>
    		And the driver should place a Thermostat:SetPointChanged message on the platform bus
    		And the driver should place a base:ValueChange message on the platform bus

		Examples:
		  | opp_type           | opp_value | type | scale | value1 | value2 | value3 | value4 | attr         | exp_val | 
		  | therm:coolsetpoint | 25.0      | 1    | 42    | 2      | 204    | 0      | 0      | heatsetpoint | 22.0    | 
		  | therm:coolsetpoint | 25.0      | 1    | 42    | 3      | 38     | 0      | 0      | heatsetpoint | 27.0    | 
		  | therm:coolsetpoint | 25.0      | 1    | 42    | 3      | 182    | 0      | 0      | heatsetpoint | 35.0    | 
		  | therm:heatsetpoint | 25.0      | 2    | 42    | 2      | 204    | 0      | 0      | coolsetpoint | 22.0    | 
		  | therm:heatsetpoint | 25.0      | 2    | 42    | 3      | 38     | 0      | 0      | coolsetpoint | 27.0    |
		  | therm:heatsetpoint | 25.0      | 2    | 42    | 1      | 82     | 0      | 0      | coolsetpoint |  1.0    | 

		  | therm:coolsetpoint | 25.0      | 1    | 34    | 0      | -36    | 0      | 0      | heatsetpoint | 22.0    | 
		  | therm:coolsetpoint | 25.0      | 1    | 34    | 1      | 14     | 0      | 0      | heatsetpoint | 27.0    | 
		  | therm:coolsetpoint | 25.0      | 1    | 34    | 1      | 94     | 0      | 0      | heatsetpoint | 35.0    | 
		  | therm:heatsetpoint | 25.0      | 2    | 34    | 0      | -36    | 0      | 0      | coolsetpoint | 22.0    | 
		  | therm:heatsetpoint | 25.0      | 2    | 34    | 1      | 14     | 0      | 0      | coolsetpoint | 27.0    |
		  | therm:heatsetpoint | 25.0      | 2    | 34    | 0      |  7     | 0      | 0      | coolsetpoint |  0.7    | 


############################################################
# Honeywell Thermostat Driver Specific Tests
#
# Notes:
#  maxHeatSetPoint: 32.2 C
#  minHeatSetPoint:  4.4 C
#  maxCoolSetPoint: 37.2 C
#  minCoolSetPoint: 10.0 C
#  minSetPointSeparation: 1.67 C
#
#  AUTO set point is average between HEAT and COOL set points, so:
#  - Max AUTO set point is actually 34.7 C to account for required minimum set point separation
#  - Min AUTO set point is actually 7.2 C to account for required minimum set point separation
#
#  We use an Epsilon of 0.05 for temperature float value comparisons to support tenth of a degree C changes.
#
############################################################

	@boundry
	Scenario Outline: Check boundy conditions for SetIdealTemperature
		Given the capability therm:hvacmode is <hvac-mode>
			And the capability therm:heatsetpoint is <curr-heat>
			And the capability therm:coolsetpoint is <curr-cool>
		When a therm:SetIdealTemperature command with argument temperature of value <setpoint> is placed on the platform bus
		Then the driver should place a therm:SetIdealTemperatureResponse message on the platform bus
			And the message's result attribute should be <result>
			And the message's hvacmode attribute should be <hvac-mode>
			And the message's idealTempSet attribute numeric value should be within delta 0.05 of <new-sp>
			And the message's prevIdealTemp attribute numeric value should be within delta 0.05 of <prev-sp>
			And the message's maxSetPoint attribute numeric value should be within delta 0.05 of <max-sp>
			And the message's minSetPoint attribute numeric value should be within delta 0.05 of <min-sp>

		Examples:
		  | hvac-mode | curr-heat | curr-cool | setpoint | result | prev-sp | new-sp   | max-sp | min-sp | remark                                                              |
		  | HEAT      | 20        | 30        | 32.1     | true   | 20      | 32.1     | 32.2   |  4.4   | Set Heat set point below Max value                                  |
		  | HEAT      | 20        | 30        | 32.2     | true   | 20      | 32.2     | 32.2   |  4.4   | Set Heat set point to Max value                                     |
		  | HEAT      | 20        | 30        | 32.3     | true   | 20      | 32.2     | 32.2   |  4.4   | Try to set Heat set point above Max value, changes, but only to Max |
		  | HEAT      | 20        | 30        |  4.5     | true   | 20      |  4.5     | 32.2   |  4.4   | Set Heat set point above Min value                                  |
		  | HEAT      | 20        | 30        |  4.4     | true   | 20      |  4.4     | 32.2   |  4.4   | Set Heat set point to Min value                                     |
		  | HEAT      | 20        | 30        |  4.3     | true   | 20      |  4.4     | 32.2   |  4.4   | Try to set Heat set point below Min value, changes, but only to Min |

		  | COOL      | 20        | 30        | 37.1     | true   | 30      | 37.1     | 37.2   | 10.0   | Set Cool set point below Max value                                  |
		  | COOL      | 20        | 30        | 37.2     | true   | 30      | 37.2     | 37.2   | 10.0   | Set Cool set point to Max value                                     |
		  | COOL      | 20        | 30        | 37.3     | true   | 30      | 37.2     | 37.2   | 10.0   | Try to set Cool set point above Max value, changes, but only to Max |
		  | COOL      | 20        | 30        | 10.1     | true   | 30      | 10.1     | 37.2   | 10.0   | Set Cool set point above Min value                                  |
		  | COOL      | 20        | 30        | 10.0     | true   | 30      | 10.0     | 37.2   | 10.0   | Set Cool set point to Min value                                     |
		  | COOL      | 20        | 30        |  9.9     | true   | 30      | 10.0     | 37.2   | 10.0   | Try to set Cool set point below Min value, changes, but only to Min |

		  | AUTO      | 20        | 30        | 34.6     | true   | 25      | 34.6     | 34.7   |  7.2   | Set Avg Cool & Heat set point below Avg Max value                   |
		  | AUTO      | 20        | 30        | 34.7     | true   | 25      | 34.7     | 34.7   |  7.2   | Set Avg Cool & Heat set point to Avg Max value                      |
		  | AUTO      | 20        | 30        | 34.8     | true   | 25      | 34.7     | 34.7   |  7.2   | Try to set Avg Cool & Heat set point above Avg Max value, changes, but only to Avg Max |
		  | AUTO      | 20        | 30        |  7.3     | true   | 25      |  7.3     | 34.7   |  7.2   | Set Avg Cool & Heat set point above Avg Min value                   |
		  | AUTO      | 20        | 30        |  7.2     | true   | 25      |  7.2     | 34.7   |  7.2   | Set Avg Cool & Heat set point to Avg Min value                      |
		  | AUTO      | 20        | 30        |  7.1     | true   | 25      |  7.2     | 34.7   |  7.2   | Try to set Avg Cool & Heat set point below Avg Min value, changes, but only to Avg Min |


	@boundry
	Scenario Outline: Check boundy conditions for IncrementIdealTemperature
		Given the capability therm:hvacmode is <hvac-mode>
			And the capability therm:heatsetpoint is <curr-heat>
			And the capability therm:coolsetpoint is <curr-cool>
		When a therm:IncrementIdealTemperature command with argument amount of value <delta> is placed on the platform bus
		Then the driver should place a therm:IncrementIdealTemperatureResponse message on the platform bus
			And the message's result attribute should be <result>
			And the message's hvacmode attribute should be <hvac-mode>
			And the message's idealTempSet attribute numeric value should be within delta 0.05 of <new-sp>
			And the message's prevIdealTemp attribute numeric value should be within delta 0.05 of <prev-sp>
			And the message's maxSetPoint attribute numeric value should be within delta 0.05 of <max-sp>
			And the message's minSetPoint attribute numeric value should be within delta 0.05 of <min-sp>

		Examples:
		  | hvac-mode | curr-heat | curr-cool | delta  | result | prev-sp | new-sp  | max-sp | min-sp | remark                                        |
		  | HEAT      | 20        | 30        | 3      | true   | 20      | 23.0    | 32.2   |  4.4   | Increase by specified delta                   |
		  | HEAT      | 30        | 30        | 3      | true   | 30      | 32.2    | 32.2   |  4.4   | Limit increase to max Heat value              |
		  | HEAT      | 32.2      | 30        | 3      | false  | 32.2    | 32.2    | 32.2   |  4.4   | Reject increase if already at max Heat value  |
		  
		  | COOL      | 20        | 30        | 3      | true   | 30      | 33      | 37.2   | 10.0   | Increase by specified delta                   |
		  | COOL      | 20        | 36        | 3      | true   | 36      | 37.2    | 37.2   | 10.0   | Limit increase to max Cool value              |
		  | COOL      | 20        | 37.2      | 3      | false  | 37.2    | 37.2    | 37.2   | 10.0   | Reject increase if already at max Cool value  |

		  | AUTO      | 20        | 30        | 3      | true   | 25      | 28.0    | 34.7   |  7.2   | Increase Avg by specified delta               |
		  | AUTO      | 32        | 36        | 3      | true   | 34      | 34.7    | 34.7   |  7.2   | Limit increase to max Avg value               |
		  | AUTO      | 32.2      | 37.2      | 3      | false  | 34.7    | 34.7    | 34.7   |  7.2   | Reject increase if already at max Avg value   |

	@boundry
	Scenario Outline: Check boundy conditions for DecrementIdealTemperature
		Given the capability therm:hvacmode is <hvac-mode>
			And the capability therm:heatsetpoint is <curr-heat>
			And the capability therm:coolsetpoint is <curr-cool>
		When a therm:DecrementIdealTemperature command with argument amount of value <delta> is placed on the platform bus
		Then the driver should place a therm:DecrementIdealTemperatureResponse message on the platform bus
			And the message's result attribute should be <result>
			And the message's hvacmode attribute should be <hvac-mode>
			And the message's idealTempSet attribute numeric value should be within delta 0.05 of <new-sp>
			And the message's prevIdealTemp attribute numeric value should be within delta 0.05 of <prev-sp>
			And the message's maxSetPoint attribute numeric value should be within delta 0.05 of <max-sp>
			And the message's minSetPoint attribute numeric value should be within delta 0.05 of <min-sp>

		Examples:
		  | hvac-mode | curr-heat | curr-cool | delta  | result | prev-sp | new-sp  | max-sp | min-sp | remark                                        |
		  | HEAT      | 20        | 30        | 3      | true   | 20      | 17.0    | 32.2   |  4.4   | Decrease by specified delta                   |
		  | HEAT      |  6        | 30        | 3      | true   |  6      |  4.4    | 32.2   |  4.4   | Limit decrease to min Heat value              |
		  | HEAT      |  4.4      | 30        | 3      | false  |  4.4    |  4.4    | 32.2   |  4.4   | Reject decrease if already at min Heat value  |
		  
		  | COOL      | 20        | 30        | 3      | true   | 30      | 27.0    | 37.2   | 10.0   | Decrease by specified delta                   |
		  | COOL      | 15        | 12        | 3      | true   | 12      | 10.0    | 37.2   | 10.0   | Limit decrease to min Cool value              |
		  | COOL      | 10        | 10.0      | 3      | false  | 10.0    | 10.0    | 37.2   | 10.0   | Reject decrease if already at min Cool value  |

		  | AUTO      | 20        | 30        | 3      | true   | 25      | 22.0    | 34.7   |  7.2   | Decrease Avg by specified delta               |
		  | AUTO      |  2        | 16        | 3      | true   |  9      |  7.2    | 34.7   |  7.2   | Limit decrease to min Avg value               |
		  | AUTO      |  4.4      | 10.0      | 3      | false  |  7.2    |  7.2    | 34.7   |  7.2   | Reject decrease if already at min Avg value   |
