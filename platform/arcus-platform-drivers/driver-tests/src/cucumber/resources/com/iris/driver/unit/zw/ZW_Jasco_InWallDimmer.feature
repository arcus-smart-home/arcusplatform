@ZWave @Jasco
Feature: GE/Jasco In-Wall Dimmer
  
  Validates message handling for the GE/Jasco in-wall dimmer switch driver.

  Background: 
    Given the ZW_Jasco_InWallDimmer.driver has been initialized
	    And the capability dim:brightness is 10
	    And the capability swit:state is OFF
    Then both busses should be empty

  Scenario: Driver reports capabilities to platform.
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
	    And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'dim', 'swit', 'indicator']
	    And the message's dev:devtypehint attribute should be Dimmer
	    And the message's devadv:drivername attribute should be ZWJascoInWallDimmerDriver
	    And the message's devadv:driverversion attribute should be 1.1
	    And the message's devpow:source attribute should be LINE
	    And the message's devpow:linecapable attribute should be true
    # From setting default falues in Background
    Then the driver should place a base:ValueChange message on the platform bus
    Then both busses should be empty
@version
  Scenario: Multilevel switch reports state when first connected
		When the device is connected
		Then the driver should place a base:ValueChange message on the platform bus
    Then the driver should send switch_multilevel get
			And the driver should poll switch_multilevel.get every 71 seconds
			And the driver should send configuration get 
			And the driver should send configuration get 
			And the driver should set timeout
	    And the driver should send switch_multilevel get
			And the driver should send version get
		Then both busses should be empty

  Scenario: Platform turns on switch via attribute change. 
	When a base:SetAttributes command with the value of swit:state ON is placed on the platform bus
	Then the driver should send switch_multilevel set 
		# updated for generic switch capability
    	And with parameter level 10   

  Scenario Outline: Client sets brightness attribute
    When a base:SetAttributes command with the value of dim:brightness <brightness-attr> is placed on the platform bus
    Then the driver should send switch_multilevel set 
    	And with parameter level <brightness-arg> 

	Examples:
      | brightness-attr | brightness-arg | duration-arg |remarks                         |
      | 20              | 20             | 0            |                                |
      | 99              | 99             | 0            |                                |
      | 100             | 99             | 0            |                                |

  Scenario Outline: Multilevel switch reports various dim levels
		Given the capability dim:brightness is <before>
		And the capability swit:state is OFF
    When the device response with switch_multilevel report 
    	And with parameter value <report>
    	And send to driver
    Then the driver should place a base:ValueChange message on the platform bus
	    And the platform attribute dim:brightness should be <brightness>
	    And the platform attribute swit:state should change to <state>
    Then both busses should be empty

    Examples: 
      |before   | report | state | brightness | remarks                                        |
      | 10      | 0      | OFF   | 10         | Treated as binary off; no change to brightness |
      | 10      | 1      | ON    | 1          |                                                |
      | 10      | 99     | ON    | 100        |                                                |
      | 10      | 100    | ON    | 100        |                                                |
      | 50      | 253    | OFF   | 50         | Ignore out of bounds                           |
      | 50      | 121    | OFF   | 50         | Ignore out of bounds                           |
      
  Scenario Outline: Multilevel switch reports various dim levels
		Given the capability dim:brightness is <before>
		And the capability swit:state is ON
    When the device response with switch_multilevel report 
    	And with parameter value <report>
    	And send to driver
    Then the driver should place a base:ValueChange message on the platform bus
	    And the platform attribute dim:brightness should be <brightness>
	    And the platform attribute swit:state should change to <state>
    Then both busses should be empty

    Examples: 
      |before   | report | state | brightness | remarks                                        |
      | 10      | 0      | OFF   | 10         | Treated as binary off; no change to brightness |
      | 10      | 1      | ON    | 1          |                                                |
      | 10      | 99     | ON    | 100        |                                                |
      | 10      | 100    | ON    | 100        |                                                |
      | 50      | 253    | ON    | 50         | Ignore out of bounds                           |
      | 50      | 121    | ON    | 50         | Ignore out of bounds                           |