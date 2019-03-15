@ZWave @Jasco 
Feature: ZWave Jasco Plugin Dimmer Driver Test

	These scenarios test the functionality of the ZWave Jasco Plugin Dimmer driver.
	
  Background:
	Given the ZW_Jasco_PlugInDimmer.driver has been initialized
		And the capability dim:brightness is 10
		And the capability swit:state is ON
	Then both busses should be empty
	
  Scenario: Driver reports capabilities to platform.
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
	    And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'dim', 'swit']
	    And the message's dev:devtypehint attribute should be Dimmer
	    And the message's devadv:drivername attribute should be ZWJascoPlugInDimmerDriver
	    And the message's devpow:source attribute should be LINE
	    And the message's devpow:linecapable attribute should be true
    Then the driver should place a base:ValueChange message on the platform bus
    Then both busses should be empty

  Scenario: Device connected
	When the device is connected
    Then the driver should send switch_multilevel get

      Scenario: Multilevel switch reports state when first connected
	When the device is connected
    #When a switch_multilevel first connects to the platform
    Then the driver should send switch_multilevel get

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
	  | brightness-attr | brightness-arg | duration-arg |remarks						|
	  | 1               | 1              | 0            |								|
	  | 99              | 99             | 0            |								|
	  | 100             | 99             | 0            |								|

    