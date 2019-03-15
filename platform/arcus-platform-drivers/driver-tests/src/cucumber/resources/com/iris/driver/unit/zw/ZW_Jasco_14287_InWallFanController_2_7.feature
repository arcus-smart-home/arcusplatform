@ZWave @Jasco500 @Fan
Feature: GE/Jasco 500 Series 14287 In-Wall Fan Controller
  
    Validates message handling for the GE/Jasco in-wall fan controller driver.

    Background: 
        Given the ZW_Jasco_14287_InWallFanController_2_7.driver has been initialized

    Scenario: Driver reports capabilities to platform.
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'fan', 'swit']
            And the message's dev:devtypehint attribute should be Fan Control
            And the message's devadv:drivername attribute should be ZWJasco14287InWallFanControllerDriver
            And the message's devadv:driverversion attribute should be 2.7
            And the message's devpow:source attribute should be LINE
            And the message's devpow:linecapable attribute should be true
            And the message's swit:state attribute should be OFF
            And the message's fan:maxSpeed attribute should be 3
            And the message's fan:speed attribute should be 1
        Then both busses should be empty


############################################################
# General Driver Tests
############################################################

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set 
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Fan                      |
          | "Kitchen Fan"            |
          | "Tom's Fan"              |
          | "Bob & Sue's Fan"        |


############################################################
# Driver Lifecycle Tests
############################################################

    @basic @added
     Scenario: Device associated
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the capability swit:statechanged should be recent
        Then both busses should be empty

    @basic @connected @timeout
      Scenario: Multilevel switch reports state when first connected
        When the device is connected
        Then the driver should send switch_multilevel get
        Then the driver should poll switch_multilevel.get every 1 hour
        Then the driver should set timeout at 3 hours
        Then the driver should place a base:ValueChange message on the platform bus
        # onConnected from GenericZWaveFan
        Then the driver should send switch_multilevel get
        # onConnected from GenericZWaveVersion
        Then the driver should send version get
        Then both busses should be empty


############################################################
# Fan Tests
############################################################

    Scenario: Platform turns on fan via attribute change.
      When a base:SetAttributes command with the value of swit:state ON is placed on the platform bus
        # Default behavior if no target level is LOW
        Then the driver should send switch_multilevel set
            And with parameter level 32
        Then the driver should send switch_multilevel get
        # send response to setAttributes request
        Then the driver should place a EmptyMessage message on the platform bus        
        Then both busses should be empty

    Scenario: Platform turns off fan via attribute change.
        When a base:SetAttributes command with the value of swit:state OFF is placed on the platform bus
        Then the driver should send switch_multilevel set 
            And with parameter level 0    
        Then the driver should send switch_multilevel get
        # send response to setAttributes request
        Then the driver should place a EmptyMessage message on the platform bus        
        Then both busses should be empty

    Scenario Outline: Client sets speed attribute
        When a base:SetAttributes command with the value of fan:speed <speed> is placed on the platform bus
        Then the driver should send switch_multilevel set 
            # formula is (speed/100) - 1
            And with parameter level <value>
        Then the driver should send switch_multilevel get
        # send response to setAttributes request
        Then the driver should place a EmptyMessage message on the platform bus        
        Then both busses should be empty

        Examples:
            | speed | value |
            | 1     | 32    |
            | 2     | 65    |
            | 3     | 98    |

    Scenario Outline: Multilevel switch reports value
        When the device response with switch_multilevel report 
            And with parameter value <val-arg>
            And send to driver
            And the platform attribute swit:state should change to <swit-state>
            #Math is level/(100/3) + 1
            And the platform attribute fan:speed should change to <fan-speed>

        Examples:
          | val-arg | swit-state | fan-speed |
          | 0       | OFF        | 1      |
          #FF is a special value which must be treated as 100% ON per spec.
          | 255     | ON         | 3             |
          | 1       | ON         | 1         |
          | 32      | ON         | 1         |
          | 33      | ON         | 2         |
          | 65      | ON         | 2         |
          | 66      | ON         | 3         |
          | 99      | ON         | 3         |

    Scenario: Multilevel switch reports a reserved/illegal value
        When the device response with switch_multilevel report 
            And with parameter value 100
            And send to driver
        Then both busses should be empty

