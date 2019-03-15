@ZWave @Switch @Generic
Feature: Unit Tests for ZWaveSwitch

	These scenarios test the functionality of the Generic Z-Wave Switch driver.

	Background:
		Given the ZW_ZZWaveGenericSwitch_2_11.driver has been initialized

	Scenario: Driver reports capabilities to platform. 
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'swit' ]
			And the message's dev:devtypehint attribute should be Switch
			And the message's devadv:drivername attribute should be ZZWaveGenericSwitch
			And the message's devadv:driverversion attribute should be 2.11
			And the message's devpow:source attribute should be LINE
			And the message's devpow:linecapable attribute should be true		
			And the message's swit:state attribute should be OFF
            And the message's swit:state attribute should be OFF
		Then both busses should be empty



############################################################
# Generic Driver Tests
############################################################

    @basic @added
    Scenario: Make sure any "time of change" attributes are defaulted when the device is first Added
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the capability swit:statechanged should be recent

    @basic @connected @timeout
    Scenario: Make sure the offline timeout is set each time the device Connects
        When the device is connected
        Then the driver should set timeout at 190 minutes
		Then the driver should poll switch_binary.get every 60 minutes
		# then might do a version get, or a switch binary get, depends on order of included modules 

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set 
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Switch                   |
          | "My Switch"              |
          | "Tom's Switch"           |
          | "Light & Fan Switch"     |


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


############################################################
# Generic Switch Tests
############################################################

    @switch
    Scenario Outline: Platform turns switch ON/OFF via attribute change. 
        Given the capability swit:state is <prev_state>
        When a base:SetAttributes command with the value of swit:state <new_state> is placed on the platform bus
        Then the driver should send switch_binary set
            And with parameter value <send_value>

        Examples:
          | prev_state | new_state | send_value | 
          | OFF        | ON        | -1         | 
          | ON         | OFF       |  0         | 
            
    @switch
    Scenario Outline: Device reports a Switch value changed via 'switch_binary' report
        Given the capability swit:state is <prev_state>
        When the device response with switch_binary report
            And with parameter value <get_value> 
            And send to driver
        Then the platform attribute swit:state should change to <new_state>
            And the capability swit:statechanged should be recent
            And the driver should place a base:ValueChange message on the platform bus
        
        Examples:
          | prev_state | get_value | new_state |
          | OFF        | -1        | ON        |
          | ON         |  0        | OFF       |

    @switch
    Scenario Outline: Device reports a Basic value changed via 'basic' report
        Given the capability swit:state is <prev_state>
        When the device response with basic report
            And with parameter value <value> 
            And send to driver
        Then the platform attribute swit:state should change to <new_state>
            And the capability swit:statechanged should be recent
            And the driver should place a base:ValueChange message on the platform bus
        
        Examples:
          | prev_state | value | new_state |
          | OFF        | -1    | ON        |
          | ON         |  0    | OFF       |

    @switch
	Scenario: Node Info
		When the device sends nodeInfo
		Then the driver should send switch_binary get
			And both busses should be empty
			
    @switch
	Scenario: Hail
		When the device sends hail hail
			And send to driver
		Then the driver should send switch_binary get
			And both busses should be empty
			
    @switch
	Scenario: Busy
		When the device sends application_status busy
			And send to driver

