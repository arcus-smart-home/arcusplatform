@ZWave @Generic @Siren
Feature: Unit Tests for Generic ZWave Siren driver

    These scenarios test the functionality of the Generic Z-Wave Siren driver.

    Background:
        Given the ZW_ZZWaveGenericSiren_2_12.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform. 
	    When a base:GetAttributes command is placed on the platform bus
	    Then the driver should place a base:GetAttributesResponse message on the platform bus
	        And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'alert']
	        And the message's dev:devtypehint attribute should be Siren
	        And the message's devadv:drivername attribute should be ZZWaveGenericSiren 
	        And the message's devadv:driverversion attribute should be 2.12
	        And the message's devpow:source attribute should be LINE
	        And the message's devpow:linecapable attribute should be true
	        And the message's alert:state attribute should be QUIET
	    Then both busses should be empty



############################################################
# Generic Driver Tests
############################################################

    @basic @added
    Scenario: Make sure any "time of change" attributes are defaulted when the device is first Added
        When the device is added
        Then the capability devpow:sourcechanged should be recent

    @basic @connected @timeout
    Scenario: Make sure the offline timeout is set each time the device Connects
        When the device is connected
        Then the driver should set timeout at 190 minutes

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set 
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Siren                    |
          | "My Siren"               |
          | "Tom's Siren"            |
          | "Bob & Sue's Siren"      |



############################################################
# Generic ZWave Driver Tests
############################################################

    Scenario: Make sure driver handles ZWave Plus Info Reports
        When the device response with zwaveplus_info report
            And with parameter zwaveversion 4
            And with parameter roletype 3
            And with parameter nodetype 2
            And send to driver
        Then protocol bus should be empty

    Scenario: Make sure driver handles Device Reset Locally Notification
        When the device response with device_reset_locally notification
            And send to driver
        Then protocol bus should be empty


############################################################
# Generic Alert Tests
############################################################

    @siren
    Scenario Outline: Platform turns siren ON/OFF via attribute change. 
        Given the capability alert:state is <prev_state>
        When a base:SetAttributes command with the value of alert:state <new_state> is placed on the platform bus
        Then the driver should send switch_binary set
            And with parameter value <value>
        
        Examples:
          | prev_state | value | new_state |
          | QUIET      | -1    | ALERTING  |
          | ALERTING   |  0    | QUIET     |
            
    @siren
    Scenario: Device reports Siren ON via 'switch_binary' report
        Given the capability alert:state is QUIET
        When the device response with switch_binary report
            And with parameter value -1
            And send to driver
        Then the platform attribute alert:state should change to ALERTING
            And the capability alert:lastAlertTime should be recent
            And the driver should place a base:ValueChange message on the platform bus

    @siren
    Scenario: Device reports Siren OFF via 'switch_binary' report
        Given the capability alert:state is ALERTING
        When the device response with switch_binary report
            And with parameter value 0
            And send to driver
        Then the platform attribute alert:state should change to QUIET
            And the driver should place a base:ValueChange message on the platform bus


    @siren
    Scenario Outline: Device reports invalid value via 'switch_binary' report
        Given the capability alert:state is <prev_state>
        When the device response with switch_binary report
            And with parameter value <value>
            And send to driver
        Then the platform attribute alert:state should be <prev_state>

        Examples:
          | prev_state | value |
          | QUIET      |    1  |
          | QUIET      |   15  |
          | ALERTING   |    1  |
          | ALERTING   |  127  |


