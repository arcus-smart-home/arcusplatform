@ZWave @Springs @Shades            
Feature: ZWave Springs Cellular Shade Test

    These scenarios test the functionality of the Springs Native ZWave Cellular Shade driver.

    Background:
        Given the ZW_Springs_Cellular_Shade.driver has been initialized

    Scenario: Driver reports capabilities to platform.
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'shade']
            And the message's dev:devtypehint attribute should be Shade
            And the message's devadv:drivername attribute should be ZWSpringsCellularShadeDriver
            And the message's devadv:driverversion attribute should be 1.0
            And the message's devpow:source attribute should be BATTERY                                                                                            
            And the message's devpow:linecapable attribute should be false
            And the message's shade:shadestate attribute should be OK

    Scenario: Device is added
        When the device is added
            And a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the driver should place a base:ValueChange message on the platform bus
    
    Scenario: Device connected
        When the device connects to the platform
        Then the driver should place a base:ValueChange message on the platform bus 
        Then the driver should send Battery get
        Then the driver should send Switch_Multilevel get
        Then the driver should set timeout at 37 hours
        Then the driver variable setTo100 should be false
            And the driver variable moveInProgress should be false
        
    Scenario: Device reports battery level
        When the device response with battery report 
            And with parameter level 30
            And send to driver 
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability devpow:battery should be 30
        Then both busses should be empty

    Scenario Outline: Device reports open level
        When the device response with switch_multilevel report
            And with parameter value <rptlevel>
            And send to driver 
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability shade:level should be <level>
            And the capability shade:levelchanged should be recent
        Then both busses should be empty

      Examples:
        | rptlevel | level |
        |  99      |  100  |
        |  98      |   98  |
        |   0      |    0  |
        |   1      |    1  |
       
    Scenario Outline: Device reports level when 100 expected
        Given the driver variable setTo100 is true
        When the device response with switch_multilevel report 
            And with parameter value <level>
            And send to driver 
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability shade:level should be <new>
            And the capability shade:levelchanged should be recent
            And the driver variable setTo100 is <after>
        Then both busses should be empty

      Examples:
        | level | new | after |
        |  99   | 100 | true  |
        |  75   |  75 | false |
        
    Scenario Outline: Device reports invalid level
        Given the driver attribute shade:level is 10
        When the device response with switch_multilevel report 
            And with parameter value <level>
            And send to driver 
        Then the capability shade:level should be 10
        Then both busses should be empty

      Examples:
        | level |
        |  100  |
        |  -1   |

    Scenario Outline: Move using shade capability
        Given the driver variable setTo100 is false
        When a base:SetAttributes command with the value of shade:level <level> is placed on the platform bus
        Then the driver should send switch_multilevel set
            And with parameter level <level_out>
        Then the driver variable moveInProgress should be true
            And the driver variable setTo100 should be <100?>
#       Then both busses should be empty    
# TODO Work around Empty Message  

      Examples:
        | level | level_out | 100?  |
        |   99  |  99       | false |
        |    1  |   1       | false |
        |    0  |   0       | false |
        |  100  |  99       | true  |
   
    Scenario Outline: Don't move using shade capability if invalid
        When a base:SetAttributes command with the value of shade:level <level> is placed on the platform bus
        Then the driver variable moveInProgress should be null
        # nothing should happen
#       Then both busses should be empty    
# TODO Work around Empty Message 
 
      Examples:
        | level |
        |   101 |
        |    -1 |

    Scenario Outline: Platform Moves Blinds via shade methods change.
        When a <command> command is placed on the platform bus
        Then the driver should send switch_multilevel set 
            And with parameter level <level>
        Then the driver should place a <response> message on the platform bus 
        Then the driver variable moveInProgress should be true
        Then both busses should be empty   

      Examples:
        | command          | level | response                    |  
        | shade:GoToOpen   |  99   |  shade:GoToOpenResponse     |
        | shade:GoToClosed |  00   |  shade:GoToClosedResponse   |
   
    #The Blind does not report it's new position if the hub told it to move
    #but it does report battery after every move
    Scenario: Battery Report Triggers Position Read if MOVE_IN_PROGRESS
        Given the driver variable moveInProgress is true
            Then the driver variable moveInProgress should be true
        When the device responds with battery report
            And with parameter level 25
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
        Then the driver should send Switch_Multilevel get
        Then both busses should be empty
