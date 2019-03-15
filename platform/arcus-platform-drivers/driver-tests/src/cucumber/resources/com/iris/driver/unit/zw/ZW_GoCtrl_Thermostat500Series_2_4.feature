@ZWave @therm24 @GoCtrl500
Feature: ZWave GoCtrl Thermostat Driver Test

These scenarios test the functionality of the ZWave Plus GoCtrl Thermostat driver

    Background:
    Given the ZW_GoCtrl_Thermostat500Series_2_4.driver has been initialized
    
    Scenario: Driver reports capabilities to platform. 
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'temp','therm','clock']
        And the message's dev:devtypehint attribute should be Thermostat
        And the message's devadv:drivername attribute should be ZWGoCtrlThermostat500Series 
        And the message's devadv:driverversion attribute should be 2.4
        And the message's devpow:linecapable attribute should be true
        And the message's devpow:backupbatterycapable attribute should be false
        And the message's therm:maxfanspeed attribute should be 1
        And the message's therm:autofanspeed attribute should be 1
        And the message's therm:supportsAuto attribute should be true
        And the message's therm:minsetpoint attribute should be -1.1
        And the message's therm:maxsetpoint attribute should be 44.4
        And the message's therm:setpointseparation attribute should be 1.67
    Then both busses should be empty

    Scenario: Device associated
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the driver variable size should be 1
            And the driver variable scale should be 1
            And the driver variable precision should be 0
            And nothing else should happen
            
    Scenario: Driver is connected
        When the device is connected
        Then the driver should send association get
                And with parameter groupingidentifier 1
        Then the driver should set timeout at 3 hours
        Then the driver should poll battery.get every 24 hours
            And the driver should poll clock.get every 24 hours
              #current temperature is in sensor_multilevel 
            And the driver should poll sensor_multilevel.get every 1 hour
            And the driver should poll thermostat_mode.get every 1 hour
            And the driver should poll thermostat_fan_mode.get every 1 hour
            And the driver should poll thermostat_setpoint.get every 1 hour
            And the driver should poll thermostat_setpoint.get every 1 hour
            #TODO CHECK THE TYPES ON POLL
        Then the driver should schedule event SetupReporting in 60 seconds
            And the driver should schedule event SetupDeviceStart in 120 seconds
            And the driver should schedule event SetLifeline every 5 seconds 10 times
        Then the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty            

############################################################
# Battery Test
############################################################

    Scenario Outline: Device reports battery level
        Given the driver attribute devpow:battery is 80
            And the driver attribute therm:dayssincefilterchange is 2
            And the driver attribute therm:runtimesincefilterchange is 12
            And the millisecond driver variable filterChangeDate is 72 hours ago
            And the millisecond driver variable runtimeStart is 24 hours ago
            And the driver variable filterRuntimeTotal is 43200000
        When the device response with battery report
            And with parameter level <level-arg>
            And send to driver 
        Then the platform attribute devpow:battery should change to <battery-attr>
            And the platform attribute therm:dayssincefilterchange should change to 3
            And the platform attribute therm:runtimesincefilterchange should be 36
            And the driver variable runtimeStart should be recent
            And the driver should place a base:ValueChange message on the platform bus
        Then nothing else should happen

        Examples:
          | level-arg | battery-attr | remarks                                                  |
          |  -1       |   0          | 0xFF indicates LOW BATTERY and level should be set to 0  |
          |   1       |   1          | if device can report level of 1 it should be accepted    |
          |  50       |  50          |                                                          |
          | 100       | 100          |                                                          |
          | 101       |  80          | ignore invalid values, leaves attribute unchanged        |

############################################################
# Generic ZWave Driver Tests
############################################################

    Scenario: Make sure driver handles ZWave Plus Info Reports
        When the device response with zwaveplus_info report
            And with parameter zwaveversion 5
            And with parameter roletype 6
            And with parameter nodetype 2
            And send to driver
        Then protocol bus should be empty

    Scenario: Make sure driver handles Device Reset Locally Notification
        When the device response with device_reset_locally notification
            And send to driver
        Then protocol bus should be empty

    Scenario: Handle Lifeline Set
            When the device responds with association report
                And with parameter group 1
                And with parameter node1 1
                And send to driver
            Then the driver should cancel event SetLifeline
                And the driver should schedule event SetupReporting in 5 seconds 
                And nothing else should happen
                
    Scenario Outline: Handle Lifeline Not Set
            When the device responds with association report
                And with parameter group <group>
                And with parameter node1 <node>
                And send to driver
            Then nothing should happen
            
        Examples:
          | group  | node     |
          |   2    |  1       |
          |   1    |  2       |
          |   1    |  null    |

        Scenario: Lifeline Set Event
            When the event SetLifeline triggers
            Then the driver should send association set
                And with parameter group 1
                And with parameter node1 1
            Then the driver should send association get
                And with parameter groupingidentifier 1
            Then nothing else should happen
            
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
        Given the driver attribute therm:active is NOT_RUNNING 
        When a therm:changeFilter command is placed on the platform bus
        Then the platform attribute therm:dayssincefilterchange should change to 0
            And the driver variable filterChangeDate is recent
        Then the platform attribute therm:runtimesincefilterchange should change to 0
            And the driver variable runtimeStart should be 0
            And the driver variable filterRuntimeTotal should be 0
        Then the driver should place a therm:changeFilterResponse message on the platform bus
            And the driver should place a base:ValueChange message on the platform bus        
        Then nothing else should happen

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

Scenario Outline: Device reports an operating state value
        When the device response with thermostat_operating_state report 
            And with parameter state <value>
            And send to driver
    Then nothing should happen
            
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
            And nothing else should happen
        
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
            And nothing else should happen
        
        Examples:
          | rpt_mode | speed |
          | 0x00     | 0     |
          | 0x01     | 1     |

    Scenario: Device report thermostat fan state: stop running
        Given the millisecond driver variable runtimeStart is 1 hour ago
            And the millisecond driver variable filterChangeDate is 12 hours ago
            And the driver variable filterRuntimeTotal is 86400000
            And the capability therm:runtimesincefilterchange is 24
            And the capability therm:dayssincefilterchange is 0
# 1 day in msec
        When the device response with thermostat_fan_state report
            And with parameter state 0
            And send to driver
        Then the platform attribute therm:active should change to NOTRUNNING
            And the driver should place a base:ValueChange message on the platform bus
    # range to account for test run time of ~200 msec
            And the numeric driver variable filterRuntimeTotal should be within 0.1% of 90000000 
            And the driver variable runtimeStart should be 0
            And the capability therm:runtimesincefilterchange should be 25
            And the capability therm:dayssincefilterchange is 0
            And nothing else should happen

    Scenario: Device report thermostat fan state: continue running
        Given the millisecond driver variable runtimeStart is 1 hr ago
            And the millisecond driver variable filterChangeDate is 12 hours ago
            And the driver variable filterRuntimeTotal is 86400000
            And the capability therm:runtimesincefilterchange is 24
            And the capability therm:dayssincefilterchange is 0
        When the device response with thermostat_fan_state report
            And with parameter state 1
            And send to driver
        Then the platform attribute therm:active should change to RUNNING
            And the driver should place a base:ValueChange message on the platform bus
            And the driver variable filterRuntimeTotal should be 86400000 
#            TODO: Update test harness to confirm value is the same as before
#            And the driver variable runtimeStart should be unchanged
            And the capability therm:runtimesincefilterchange should be 24
            And the capability therm:dayssincefilterchange is 1
            And nothing else should happen

    Scenario Outline: Device report thermostat fan state while OFF
        Given the driver variable runtimeStart is 0
            And the driver variable filterRuntimeTotal is 86400000 
            And the capability therm:runtimesincefilterchange is 24
            And the capability therm:dayssincefilterchange is 0
# 1 day in msec
        When the device response with thermostat_fan_state report
            And with parameter state <state>
            And send to driver
        Then the platform attribute therm:active should change to <active>
            And the driver should place a base:ValueChange message on the platform bus
            And the driver variable filterRuntimeTotal should be 86400000
            And the driver variable runtimeStart should be <afterStart>
            And the capability therm:runtimesincefilterchange should be 24
            And the capability therm:dayssincefilterchange is 1
            And nothing else should happen
                        
        Examples:
          | state | active     |  afterStart    |
          | 0            | NOTRUNNING |   0                    |
          | 1            | RUNNING      |   recent            |

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
          | therm:coolsetpoint | 25.0      | 1    |  9    | 72     |   N/A  | N/A    | N/A    | heatsetpoint | 22.22   | 
          | therm:coolsetpoint | 25.0      | 1    | 0x2A  |  3     | 0xB6   | N/A    | N/A    | heatsetpoint | 35.0    | 
          | therm:heatsetpoint | 25.0      | 2    |  9    | 72     |   N/A  | N/A    | N/A    | coolsetpoint | 22.22   | 
          | therm:heatsetpoint | 25.0      | 2    | 0x6c  | 0      |    1   | 0xE2   | 0x40   | coolsetpoint | 50.808  |

          | therm:coolsetpoint | 25.0      | 1    |  1    | 22     |   N/A  | N/A    | N/A    | heatsetpoint | 22    | 
          | therm:coolsetpoint | 25.0      | 1    | 0x22  | 1      |   0x60 | N/A    | N/A    | heatsetpoint | 35.2  |
          | therm:heatsetpoint | 25.0      | 2    |  1    | 22     |   N/A  | N/A    | N/A    | coolsetpoint | 22    | 
          | therm:heatsetpoint | 25.0      | 2    | 0x42  | 0xA    |   0xEF | N/A    | N/A    | coolsetpoint | 27.99 |

############################################################
# GoCtrl Thermostat Driver Specific Tests
#
# Notes:
#  maxHeatSetPoint: 32.2 C
#  minHeatSetPoint: -1.1 C
#  maxCoolSetPoint: 44.4 C
#  minCoolSetPoint: 15.5 C
#  minSetPointSeparation: 1.67 C
#
#  AUTO set point is average between HEAT and COOL set points, so:
#  - Max AUTO set point is actually 38.3 C to account for required minimum set point separation
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
          | HEAT      | 20        | 30        | 32.1     | true   | 20      | 32.1     | 32.2   | -1.1   | Set Heat set point below Max value                                  |
          | HEAT      | 20        | 30        | 32.2     | true   | 20      | 32.2     | 32.2   | -1.1   | Set Heat set point to Max value                                     |
          | HEAT      | 20        | 30        | 32.3     | true   | 20      | 32.2     | 32.2   | -1.1   | Try to set Heat set point above Max value, changes, but only to Max |
          | HEAT      | 20        | 30        | -1.0     | true   | 20      | -1.0     | 32.2   | -1.1   | Set Heat set point above Min value                                  |
          | HEAT      | 20        | 30        | -1.1     | true   | 20      | -1.1     | 32.2   | -1.1   | Set Heat set point to Min value                                     |
          | HEAT      | 20        | 30        | -1.2     | true   | 20      | -1.1     | 32.2   | -1.1   | Try to set Heat set point below Min value, changes, but only to Min |
          | HEAT      | 20.0      | 30.0      | 20.0     | true   | 20.0    | 20.0     | 32.2   | -1.1   | Set Heat set point to same value                                    |
          | HEAT      | 32.2      | 38.0      | 34.0     | false  | 32.2    | 32.2     | 32.2   | -1.1   | Reject setting Heat set point above Max value when already at Max   |
          | HEAT      | -1.1      | 15.0      | -2.0     | false  | -1.1    | -1.1     | 32.2   | -1.1   | Reject setting Heat set point below Min value when already at Min   |

          | COOL      | 20        | 30        | 44.3     | true   | 30      | 44.3     | 44.4   | 15.5   | Set Cool set point below Max value                                  |
          | COOL      | 20        | 30        | 44.4     | true   | 30      | 44.4     | 44.4   | 15.5   | Set Cool set point to Max value                                     |
          | COOL      | 20        | 30        | 44.5     | true   | 30      | 44.4     | 44.4   | 15.5   | Try to set Cool set point above Max value, changes, but only to Max |
          | COOL      | 20        | 30        | 15.6     | true   | 30      | 15.6     | 44.4   | 15.5   | Set Cool set point above Min value                                  |
          | COOL      | 20        | 30        | 15.5     | true   | 30      | 15.5     | 44.4   | 15.5   | Set Cool set point to Min value                                     |
          | COOL      | 20        | 30        | 15.4     | true   | 30      | 15.5     | 44.4   | 15.5   | Try to set Cool set point below Min value, changes, but only to Min |
          | COOL      | 20.0      | 26.0      | 26.0     | true   | 26.0    | 26.0     | 44.4   | 15.5   | Set Cool set point to same value                                    |
          | COOL      | 20.0      | 44.4      | 46.0     | false  | 44.4    | 44.4     | 44.4   | 15.5   | Reject setting Cool set point above Max value when already at Max   |
          | COOL      | 6.0       | 15.5      | 10.0     | false  | 15.5    | 15.5     | 44.4   | 15.5   | Reject setting Cool set point below Min value when already at Min   |

          | AUTO      | 20        | 30        | 38.2     | true   | 25      | 38.2     | 38.3   |  7.2   | Set Avg Cool & Heat set point below Avg Max value                                      |
          | AUTO      | 20        | 30        | 38.3     | true   | 25      | 38.3     | 38.3   |  7.2   | Set Avg Cool & Heat set point to Avg Max value                                         |
          | AUTO      | 20        | 30        | 38.4     | true   | 25      | 38.3     | 38.3   |  7.2   | Try to set Avg Cool & Heat set point above Avg Max value, changes, but only to Avg Max |
          | AUTO      | 20        | 30        |  7.3     | true   | 25      |  7.3     | 38.3   |  7.2   | Set Avg Cool & Heat set point above Avg Min value                                      |
          | AUTO      | 20        | 30        |  7.2     | true   | 25      |  7.2     | 38.3   |  7.2   | Set Avg Cool & Heat set point to Avg Min value                                         |
          | AUTO      | 20        | 30        |  7.1     | true   | 25      |  7.2     | 38.3   |  7.2   | Try to set Avg Cool & Heat set point below Avg Min value, changes, but only to Avg Min |
          | AUTO      | 20.0      | 26.0      | 23.0     | true   | 23.0    | 23.0     | 38.3   |  7.2   | Set Avg Cool & Heat set point to same value                                            |
          | AUTO      | 32.2      | 44.4      | 40.0     | false  | 38.3    | 38.3     | 38.3   |  7.2   | Reject setting Avg Cool & Heat set point above Avg Max value value when already at Max |
          | AUTO      | -1.1      | 15.5      |  5.0     | false  |  7.2    |  7.2     | 38.3   |  7.2   | Reject setting Avg Cool & Heat set point below Avg Min value when already at Min       |


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
          | HEAT      | 20        | 30        | 3      | true   | 20      | 23.0    | 32.2   | -1.1   | Increase by specified delta                   |
          | HEAT      | 30        | 30        | 3      | true   | 30      | 32.2    | 32.2   | -1.1   | Limit increase to max Heat value              |
          | HEAT      | 32.2      | 30        | 3      | false  | 32.2    | 32.2    | 32.2   | -1.1   | Reject increase if already at max Heat value  |
          
          | COOL      | 20        | 30        | 3      | true   | 30      | 33      | 44.4   | 15.5   | Increase by specified delta                   |
          | COOL      | 20        | 42        | 3      | true   | 42      | 44.4    | 44.4   | 15.5   | Limit increase to max Cool value              |
          | COOL      | 20        | 44.4      | 3      | false  | 44.4    | 44.4    | 44.4   | 15.5   | Reject increase if already at max Cool value  |

          | AUTO      | 20        | 30        | 3      | true   | 25      | 28.0    | 38.3   |  7.2   | Increase Avg by specified delta               |
          | AUTO      | 32        | 40        | 3      | true   | 36      | 38.3    | 38.3   |  7.2   | Limit increase to max Avg value               |
          | AUTO      | 32.2      | 44.4      | 3      | false  | 38.3    | 38.3    | 38.3   |  7.2   | Reject increase if already at max Avg value   |


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
          | HEAT      | 20        | 30        | 3      | true   | 20      | 17.0    | 32.2   | -1.1   | Decrease by specified delta                   |
          | HEAT      |  1        | 30        | 3      | true   |  1      | -1.1    | 32.2   | -1.1   | Limit decrease to min Heat value              |
          | HEAT      | -1.1      | 30        | 3      | false  | -1.1    | -1.1    | 32.2   | -1.1   | Reject decrease if already at min Heat value  |
          
          | COOL      | 20        | 30        | 3      | true   | 30      | 27.0    | 44.4   | 15.5   | Decrease by specified delta                   |
          | COOL      | 15        | 18        | 3      | true   | 18      | 15.5    | 44.4   | 15.5   | Limit decrease to min Cool value              |
          | COOL      | 10        | 15.5      | 3      | false  | 15.5    | 15.5    | 44.4   | 15.5   | Reject decrease if already at min Cool value  |

          | AUTO      | 20        | 30        | 3      | true   | 25      | 22.0    | 38.3   |  7.2   | Decrease Avg by specified delta               |
          | AUTO      |  2        | 16        | 3      | true   |  9      |  7.2    | 38.3   |  7.2   | Limit decrease to min Avg value               |
          | AUTO      | -1.1      | 15.5      | 3      | false  |  7.2    |  7.2    | 38.3   |  7.2   | Reject decrease if already at min Avg value   |

    Scenario Outline: Device reports configuration power source        
        When the device response with configuration report
            # parameter 0xB2 (-78) is used to send power source
            And with parameter param -78
            And with parameter level 1
            And with parameter val1 <val1>
            And send to driver
        Then the platform attribute devpow:source should change to <power-source-attr>
            And the driver should place a base:ValueChange message on the platform bus
        Then both busses should be empty

        Examples:
          | val1 | power-source-attr | remark          |
          | 1    | BATTERY           | Battery Powered |
          | 2    | LINE              | 24VAC Powered   |


    Scenario Outline: Device reports a Thermostat emergency-heat modes
        Given the capability therm:hvacmode is <curr-mode>
            And the capability therm:emergencyheat is <curr-eheat>
        When the device response with thermostat_mode report
            And with parameter level <level>
            And send to driver
        Then the platform attribute therm:hvacmode should change to <new-mode>
            And the platform attribute therm:emergencyheat should change to <eheat>
            And the driver should place a base:ValueChange message on the platform bus
        
        Examples:
          | curr-mode | curr-eheat | level | new-mode | eheat |
          | OFF       | OFF        | 4     | HEAT     | ON    |
          | HEAT      | OFF        | 4     | HEAT     | ON    |
          | COOL      | OFF        | 4     | HEAT     | ON    |
          | AUTO      | OFF        | 4     | HEAT     | ON    |
          | HEAT      | ON         | 1     | HEAT     | OFF   |
          | HEAT      | ON         | 0     | OFF      | OFF   |
          | HEAT      | ON         | 2     | COOL     | OFF   |
          | HEAT      | ON         | 3     | AUTO     | OFF   |


    Scenario: Device reports thermostat Heat set point value of 30F
        Given the capability therm:heatsetpoint is 1.0 
        When the device response with thermostat_setpoint report
            And with parameter type 1
            And with parameter scale 42
            And with parameter value1 1
            And with parameter value2 64
            And with parameter value3 0
            And with parameter value4 0
            And send to driver
        Then the platform attribute therm:heatsetpoint should change to 0.0
            And the driver should place a Thermostat:SetPointChanged message on the platform bus
            And the driver should place a base:ValueChange message on the platform bus


##################################################################
#
#        SET ATTRIBUTES TESTS
#
##################################################################

    @setattr
    Scenario Outline: Platform  sets thermostat capabilities
        When a base:SetAttributes command with the value of therm:<attribute> <state> is placed on the platform bus
        Then the driver should send <class> set 
            And with parameter <parameter> <value>        
        Then the driver should place a EmptyMessage message on the platform bus        
        Then the driver should send <class> get
            And nothing else should happen

      Examples:
        |    attribute           |    state    | class                | parameter | value | comments               |
        |    hvacmode            |    COOL     | thermostat_mode      |    mode   |   2   |                        |
        |    hvacmode            |    HEAT     | thermostat_mode      |    mode   |   1   |                        |
        |    hvacmode            |    AUTO     | thermostat_mode      |    mode   |   3   |                        |
        |    hvacmode            |    OFF      | thermostat_mode      |    mode   |   0   |                        |
        |    fanmode             |    0        | thermostat_fan_mode  |    mode   |   0   | AUTO                   |
        |    fanmode             |    1        | thermostat_fan_mode  |    mode   |   1   | LOW                    |
        |    fanmode             |    2        | thermostat_fan_mode  |    mode   |   1   | Faster Than Supported  |
        | emergencyheat          |    ON       | thermostat_mode      |    mode   |   4   |                        |

    @setattr
    Scenario Outline: Platform sets temperature capabilities
      Given the driver variable size is <size>
            And the driver variable scale is <scale>
            And the driver variable precision is <precision>
        Given the driver attribute therm:hvacmode is <mode>
        When a base:SetAttributes command with the value of therm:<attribute> <level> is placed on the platform bus
        Then the driver should send <class> set 
            And with parameter type <type>
            And with parameter scale <format>
            And with parameter value1 <value1>
            And with parameter value2 <value2>
        Then the driver should place a EmptyMessage message on the platform bus
        Then the driver should schedule event <event> in 2 seconds
            And nothing else should happen
            
       Examples:
       |size | scale| precision | mode  | attribute     | level | class                |  type | format| value1 | value2| event           |
       |  1  |   1  |    0      | AUTO  | heatsetpoint  |  20   | thermostat_setpoint  |  1    |   9   |   68   |   N/A | GetHeatSetPoint |
       |  1  |   1  |    0      | AUTO  | coolsetpoint  |  30   | thermostat_setpoint  |  2    |   9   |   86   |   N/A | GetCoolSetPoint |
       |  1  |   0  |    0      | HEAT  | heatsetpoint  |  20   | thermostat_setpoint  |  1    |   1   |   20   |   N/A | GetHeatSetPoint |
       |  1  |   0  |    0      | COOL  | coolsetpoint  |  30   | thermostat_setpoint  |  2    |   1   |   30   |   N/A | GetCoolSetPoint |
       |  2  |   1  |    1      | HEAT  | heatsetpoint  |  20   | thermostat_setpoint  |  1    | 0x2A  |   2    |  0xA8 | GetHeatSetPoint |
       |  2  |   1  |    1      | COOL  | coolsetpoint  |  30   | thermostat_setpoint  |  2    | 0x2A  |   3    |  0x5C | GetCoolSetPoint |
       |  2  |   0  |    1      | HEAT  | heatsetpoint  |  20   | thermostat_setpoint  |  1    | 0x22  |   0    |  0xC8 | GetHeatSetPoint |
       |  2  |   0  |    1      | COOL  | coolsetpoint  |  30   | thermostat_setpoint  |  2    | 0x22  |   1    |  0x2C | GetCoolSetPoint |

    @setattr
    Scenario Outline: Platform sets two capabilities              
        Given the driver variable size is 1
            And the driver variable scale is 0
            And the driver variable precision is 0
        Given the driver attribute therm:hvacmode is <mode>
        When the capability method base:SetAttributes
            And with capability <attr1> is <value1>
            And with capability <attr2> is <value2>
            And send to driver
        Then the driver should send <class1> set 
            And the driver should send <get> get    
        Then the driver should place a EmptyMessage message on the platform bus
            And the driver should schedule event <event1> in 2 seconds
            And the driver should schedule event <event2> in 4 seconds
        Then nothing else should happen

      Examples:
        | mode  | attr1              | value1 | attr2              | value2 |  class1              | event1          | event2          | get              | remarks                      |
        | HEAT  | therm:heatsetpoint |  20    | N/A                |  N/A   |  thermostat_setpoint | GetHeatSetPoint | N/A             | N/A              | Single  to validate test     |
        | AUTO  | therm:heatsetpoint |  20    | therm:coolsetpoint |  30    |  thermostat_setpoint | GetHeatSetPoint | SetCoolSetpoint | N/A              | ITWO-11403 Setpoints in Auto |
        | OFF   | therm:heatsetpoint |  20    | therm:coolsetpoint |  30    |  thermostat_setpoint | GetHeatSetPoint | SetCoolSetpoint | N/A              | ITWO-11403 Setpoints in Off  |
        | HEAT  | therm:heatsetpoint |  20    | therm:coolsetpoint |  30    |  thermostat_setpoint | GetHeatSetPoint | N/A             | N/A              | ITWO-10836                   |
        | COOL  | therm:heatsetpoint |  20    | therm:coolsetpoint |  30    |  thermostat_setpoint | GetCoolSetPoint | N/A             | N/A              | ITWO-10836                   |
        | COOL  | therm:heatsetpoint |  20    | therm:hvacmode     | HEAT   |  thermostat_mode     | N/A             | SetHeatSetpoint | thermostat_mode  | Opposite order               |

    @setattr
    Scenario: Platform sets both temperatures  and mode            
        Given the driver variable size is 1
            And the driver variable scale is 0
            And the driver variable precision is 0
        Given the driver attribute therm:hvacmode is COOL
        When the capability method base:SetAttributes
            And with capability therm:coolsetpoint is 29.4
            And with capability therm:heatsetpoint is 21
            And with capability therm:hvacmode is OFF
            And send to driver
        Then the driver should send thermostat_mode set
            And with parameter mode 0 
        Then the driver should send thermostat_mode get    
        Then the driver should place a EmptyMessage message on the platform bus
        Then the driver should schedule event SetHeatSetpoint in 4 second
            And the driver should schedule event SetCoolSetpoint in 8 second
            And nothing else should happen

        Scenario: Set Cool Event
        Given the driver variable size is 1
            And the driver variable scale is 0
            And the driver variable precision is 0
        When event SetCoolSetpoint triggers with {'setpoint': 30}
        Then the driver should send thermostat_setpoint set
            And with parameter type 2
            And with parameter value1 30
        Then the driver should schedule event GetCoolSetPoint in 2 seconds
            And nothing else should happen

    @setattr
    Scenario: Set Heat Event
        Given the driver variable size is 1
            And the driver variable scale is 0
            And the driver variable precision is 0
        When event SetHeatSetpoint triggers with {'setpoint': 12}
        Then the driver should send thermostat_setpoint set
            And with parameter type 1
            And with parameter value1 12
        Then the driver should schedule event GetHeatSetPoint in 2 seconds
            And nothing else should happen

#####################################################
# GoControl Setup Logic to reduce read rate reads
####################################################
    Scenario: Setup event runs the first time
    When event SetupDevice triggers
    Then the driver should send thermostat_setpoint get
        And with parameter type 1
            And nothing else should happen

   Scenario: Setup event runs the second time
    Given the driver attribute therm:heatsetpoint is 18.9
    When event SetupDevice triggers
    Then the driver should send thermostat_setpoint get
        And with parameter type 2
            And nothing else should happen
            
    Scenario: Setup event runs with setpoints set
    Given the driver attribute therm:heatsetpoint is 18.9
        And the driver attribute therm:coolsetpoint is 27.8
    When event SetupDevice triggers
    Then the driver should send thermostat_mode get
            And nothing else should happen

    Scenario: Setup event runs with mode and setpoints
    Given the driver attribute therm:heatsetpoint is 18.9
        And the driver attribute therm:coolsetpoint is 27.8
        And the driver attribute therm:hvacmode is AUTO
    When event SetupDevice triggers
    Then the driver should send sensor_multilevel get
            And nothing else should happen

        Scenario: Setup event runs with mode and all temps
    Given the driver attribute therm:heatsetpoint is 18.9
        And the driver attribute therm:coolsetpoint is 27.8
        And the driver attribute therm:hvacmode is AUTO
        And the driver attribute temp:temperature is 25
    When the event SetupDevice triggers
    Then the driver should send thermostat_fan_mode get
            And nothing else should happen

     Scenario: Setup event runs with both modes and all setpoints
    Given the driver attribute therm:heatsetpoint is 18.9
        And the driver attribute therm:coolsetpoint is 27.8
        And the driver attribute therm:hvacmode is AUTO
        And the driver attribute temp:temperature is 25
        And the driver attribute therm:fanmode is 1
    When the event SetupDevice triggers
    Then the driver should send battery get
            And nothing else should happen

     Scenario: Setup event runs with battery & everything before
    Given the driver attribute therm:heatsetpoint is 18.9
        And the driver attribute therm:coolsetpoint is 27.8
        And the driver attribute therm:hvacmode is AUTO
        And the driver attribute temp:temperature is 25
        And the driver attribute therm:fanmode is 1
        And the driver attribute devpow:battery is 100
    When the event SetupDevice triggers
    Then the driver should send configuration get
        And with parameter param -78
            And nothing else should happen

     Scenario: Setup event runs with temps, modes, devpower
    Given the driver attribute therm:heatsetpoint is 18.9
        And the driver attribute therm:coolsetpoint is 27.8
        And the driver attribute therm:hvacmode is AUTO
        And the driver attribute temp:temperature is 25
        And the driver attribute therm:fanmode is 1
        And the driver attribute devpow:battery is 100
        And the driver attribute devpow:source is LINE
    When event SetupDevice triggers
    Then the driver should send thermostat_fan_state get
            And nothing else should happen

     Scenario: Setup event runs needing version
    Given the driver attribute therm:heatsetpoint is 18.9
        And the driver attribute therm:coolsetpoint is 27.8
        And the driver attribute therm:fanmode is 1
        And the driver attribute therm:active is FALSE
        And the driver attribute temp:temperature is 25
        And the driver attribute therm:hvacmode is AUTO
        And the driver attribute devpow:source is LINE
        And the driver attribute devpow:battery is 95
        And the driver attribute therm:active is ACTIVE_NOTRUNNING
    When event SetupDevice triggers
        And the driver should send version get
            And nothing else should happen

    Scenario:Setup event runs later
    Given the driver attribute therm:heatsetpoint is 18.9
        And the driver attribute therm:coolsetpoint is 27.8
        And the driver attribute therm:fanmode is 1
        And the driver attribute therm:active is FALSE
        And the driver attribute temp:temperature is 25
        And the driver attribute therm:hvacmode is AUTO
        And the driver attribute devpow:source is LINE
        And the driver attribute devpow:battery is 95
        And the driver attribute devadv:firmwareVersion is 1.2.3.4
    When event SetupDevice triggers
         Then the driver should cancel event SetupDevice
             And nothing else should happen
             
  Scenario: Setup Reporting
  When the event SetupReporting triggers
  Then the driver should schedule event WriteReporting every 5 seconds 10 times
      And nothing else should happen
            
    Scenario: Configuration Set Event
        When event WriteReporting triggers
        Then the driver should send configuration set
            And with parameter param 0x17
            And with parameter size 2
            And with parameter val1 0x20
            And with parameter val2 0x7F
        Then the driver should send configuration get
        And nothing else should happen        
                  
    Scenario: Handle Configuration Report
            When the device responds with configuration report
                And with parameter param 0x17
                And with parameter size 2
                And with parameter val1 0x20
                And with parameter val2 0x7F
                And send to driver
            Then the driver should cancel event WriteReporting
                And the driver should defer event SetupDeviceStart
                And nothing else should happen
                          
        Scenario Outline: Configuration Not Set
            When the device responds with association report
                And with parameter param <param>
                And with parameter size 2
                And with parameter val1 <group>
                And with parameter val2 <node>
                And send to driver
        Then nothing should happen
            
      Examples:
        | param   | group  | node    |
        | 0x17    | 0x20   | 0x01    |
        | 0x17    | 0x21   | 0x7E    |
        | 0x16    | 0x20   | 0x7F    |
 
  Scenario: Setup Reporting
  When the event SetupReporting triggers
  Then the driver should schedule event WriteReporting every 5 seconds 10 times
      And nothing else should happen
        
          
    Scenario: Configuration Set Event
        When event WriteReporting triggers
        Then the driver should send configuration set
            And with parameter param 0x17
            And with parameter size 2
            And with parameter val1 0x20
            And with parameter val2 0x7F
        Then the driver should send configuration get
        And nothing else should happen        
        
    Scenario: Device Setup Start
        When the event SetupDeviceStart triggers
        Then the driver should schedule event SetupDevice every 5 seconds 100 times
          And nothing else should happen