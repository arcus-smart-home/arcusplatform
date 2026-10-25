@ZWave @Zooz @switch
Feature: ZWave Zooz Mini Plug Driver Test

These scenarios test the functionality of the ZWave Zooz Mini Plug driver

    Background:
    Given the ZW_Zooz_MiniPlug_2_4.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform. 
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'swit', 'pow']
        And the message's dev:devtypehint attribute should be Switch
        And the message's devadv:drivername attribute should be ZWZoozMiniPlugDriver 
        And the message's devadv:driverversion attribute should be 2.4
        And the message's devpow:source attribute should be LINE
        And the message's devpow:linecapable attribute should be true
        And the message's devpow:backupbatterycapable attribute should be false
        And the message's pow:wholehome attribute should be false
        And the message's pow:instantaneous attribute should be 0
        And the message's pow:cumulative attribute should be 0
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

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set 
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Device                   |
          | "My Device"              |
          | "Tom's Device"           |
          | "Bob & Sue's Device"     |



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
            And with parameter value <value>
        
        Examples:
          | prev_state | value | new_state |
          | OFF        |   -1  | ON        |
          | ON         |    0  | OFF       |
            
    @switch
    Scenario Outline: Device reports a Switch value changed via 'switch_binary' report
        Given the capability swit:state is <prev_state>
        When the device response with switch_binary report
            And with parameter value <value> 
            And send to driver
        Then the platform attribute swit:state should change to <new_state>
            And the capability swit:statechanged should be recent
            And the driver should place a base:ValueChange message on the platform bus
        
        Examples:
          | prev_state | value | new_state |
          | OFF        |   -1  | ON        |
          | ON         |    0  | OFF       |

    @switch
    Scenario Outline: Device reports an invalid Switch value changed via 'switch_binary' report
        Given the capability swit:state is <prev_state>
        When the device response with switch_binary report
            And with parameter value <value> 
            And send to driver
        Then the platform attribute swit:state should be <prev_state>
        
        Examples:
          | prev_state | value |
          | OFF        |    1  |
          | OFF        |   15  |
          | ON         |    1  |
          | ON         |  127  |

    @switch
    Scenario Outline: Device reports a Switch value changed via 'basic' report
        Given the capability swit:state is <prev_state>
        When the device response with basic report
            And with parameter value <value> 
            And send to driver
        Then the platform attribute swit:state should change to <new_state>
            And the capability swit:statechanged should be recent
            And the driver should place a base:ValueChange message on the platform bus
        
        Examples:
          | prev_state | value | new_state |
          | OFF        |   -1  | ON        |
          | ON         |    0  | OFF       |

    @switch
    Scenario Outline: Device reports an invalid Switch value changed via 'basic' report
        Given the capability swit:state is <prev_state>
        When the device response with basic report
            And with parameter value <value> 
            And send to driver
        Then the platform attribute swit:state should be <prev_state>
        
        Examples:
          | prev_state | value |
          | OFF        |    1  |
          | OFF        |   15  |
          | ON         |    1  |
          | ON         |  127  |


############################################################
# Zooz Smart Plug Driver Specific Tests
############################################################

    Scenario: Make sure the offline timeout is set each time the device Connects
        When the device is connected
        Then the driver should set timeout at 190 minutes
        Then the driver should send switch_binary get
        Then the driver should poll switch_binary.get every 60 minutes
            # config Energy reporting to Enabled
            And the driver should send configuration set
                And with parameter param 1
                And with parameter size 1
                And with parameter val1 1
            #config Energy reporting frequency to 60 minutes
            And the driver should send configuration set
                And with parameter param 2
                And with parameter size 2
                And with parameter val1 14
                And with parameter val2 16
            # bulk config params 3-6
            And the driver should send configuration bulk_set
            # config Auto Shut-Off Timer Minutes to 150
            And the driver should send configuration set
                And with parameter param 9
                And with parameter size 2
                And with parameter val1 0
                And with parameter val2 150
            # config Local Control to Enabled
            And the driver should send configuration set
                And with parameter param 10
                And with parameter size 1
                And with parameter val1 1

    @energy
    Scenario Outline: Device reports instantaneous energy reading
        Given the capability pow:instantaneous is <init_val>
        When the device response with meter report
            And with parameter MeterType 1
            And with parameter Scale 116
            And with parameter Value1 <Value1>
            And with parameter Value2 <Value2>
            And with parameter Value3 <Value3>
            And with parameter Value4 <Value4>
            And send to driver
        Then the platform attribute pow:instantaneous should change to <reading>
            And the driver should place a base:ValueChange message on the platform bus
            
        Examples:
          | init_val | Value1 | Value2 | Value3 | Value4 | reading |
          |  6.7     | 0      | 0      | 0      | 0      |  0.0    |
          |  0.0     | 0      | 0      | 0      | 1      |  0.001  |
          |  0.0     | 0      | 0      | 1      | 1      |  0.257  |
          |  0.0     | 0      | 1      | 1      | 1      | 65.793  |

    @energy
    Scenario Outline: Device reports cumulative energy reading
        Given the capability pow:cumulative is <init_val>
        When the device response with meter report
            And with parameter MeterType 1
            And with parameter Scale 100
            And with parameter Value1 <Value1>
            And with parameter Value2 <Value2>
            And with parameter Value3 <Value3>
            And with parameter Value4 <Value4>
            And send to driver
        Then the platform attribute pow:cumulative should change to <reading>
            And the driver should place a base:ValueChange message on the platform bus
            
        Examples:
          | init_val | Value1 | Value2 | Value3 | Value4 | reading |
          |  0.0     | 0      | 0      | 0      | 1      | 1       |
          |  0.0     | 0      | 0      | 1      | 1      | 257     |
          |  0.0     | 0      | 1      | 1      | 1      | 65793   |

