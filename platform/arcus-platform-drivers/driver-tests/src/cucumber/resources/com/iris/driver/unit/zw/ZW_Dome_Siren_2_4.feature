@ZWave @Dome @siren
Feature: ZWave Dome Siren Driver Test

These scenarios test the functionality of the ZWave Dome Siren driver

    Background:
    Given the ZW_Dome_Siren_2_4.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform. 
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'alert', 'ident']
        And the message's dev:devtypehint attribute should be Siren
        And the message's devadv:drivername attribute should be ZWDomeSirenDriver 
        And the message's devadv:driverversion attribute should be 2.4
        And the message's devpow:source attribute should be BATTERY
        And the message's devpow:linecapable attribute should be false
        And the message's devpow:backupbatterycapable attribute should be false
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


############################################################
# Dome Siren Driver Specific Tests
############################################################

    Scenario: Make sure the offline timeout is set each time the device Connects
        When the device is connected
        Then the driver should set timeout at 190 minutes
            # read initial state
            And the driver should send switch_binary get
            And the driver should poll switch_binary.get every 60 minutes
            And the driver should poll battery.get every 1440 minutes
            # config chime sound to number 9
            And the driver should send configuration set
                And with parameter param 6
                And with parameter size 1
                And with parameter val1 9
            #config volumn to HIGH
            And the driver should send configuration set
                And with parameter param 1
                And with parameter size 1
                And with parameter val1 3
            # config alert length to 5 minutes
            And the driver should send configuration set
                And with parameter param 2
                And with parameter size 1
                And with parameter val1 3
            # config chime length to 1 minutes
            And the driver should send configuration set
                And with parameter param 3
                And with parameter size 1
                And with parameter val1 1
            # config chime volume to MEDIUM
            And the driver should send configuration set
                And with parameter param 4
                And with parameter size 1
                And with parameter val1 2
            # config to use Alert sound
            And the driver should send configuration set
                And with parameter param 7
                And with parameter size 1
                And with parameter val1 1
            # config alert sound to number 7
            And the driver should send configuration set
                And with parameter param 5
                And with parameter size 1
                And with parameter val1 7

