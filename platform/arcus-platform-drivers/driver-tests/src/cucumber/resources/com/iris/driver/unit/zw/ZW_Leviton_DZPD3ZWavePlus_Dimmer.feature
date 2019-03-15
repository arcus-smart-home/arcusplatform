@ZWave @Leviton @Dimmer
Feature: Unit Tests Binary for the DZPD3 ZWave Plus Plug-In Dimmer

    These scenarios test the functionality of the ZWave Dimmer 

    Background:
        Given the ZW_Leviton_DZPD3ZWavePlus_PlugInDimmer_2_9.driver has been initialized

    @basic
    Scenario: Driver reports capabilities to platform. 
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'swit', 'dim' ]
            And the message's dev:devtypehint attribute should be Dimmer
            And the message's devadv:drivername attribute should be ZWLevitonDZPD3Dimmer
            And the message's devadv:driverversion attribute should be 2.9
            And the message's devpow:source attribute should be LINE
            And the message's devpow:linecapable attribute should be true
        Then both busses should be empty


############################################################
# General Driver Tests
############################################################

  @basic @added
  Scenario: Device associated
    When the device is added
    Then the capability devpow:sourcechanged should be recent
    And the capability swit:statechanged should be recent
    Then both busses should be empty

  @basic @connected @timeout
  Scenario: Device reports state when first connected
    When the device is connected
    Then the driver should set timeout at 200 minutes
    Then the driver should poll switch_multilevel.get every 60 minutes
    Then the driver should send switch_multilevel get
    Then the driver should send version get
    Then the driver should place a base:ValueChange message on the platform bus
    Then both busses should be empty


  @basic @name
    Scenario Outline: Make sure driver allows device name to be set 
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Dimmer                   |
          | "Kitchen Dimmer"         |
          | "Tom's Room"             |
          | "Bob & Sue's Room"       |


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
# Dimmer Tests
############################################################

# Setting just the Dimmer.brightness to 1-100 will adjust the brightness to that value and turn the device ON, whatever state it was previously in
@dimProc
Scenario Outline: Client sets brightness attribute of 1-100, and no switch state
	Given the capability dim:brightness is <init-brightness>
		And the capability swit:state is <init-state>
		And the driver variable targetlevel is <init-brightness>
		And the driver variable targetstate is <init-state>
    When a base:SetAttributes command with the value of dim:brightness <to-brightness> is placed on the platform bus
    Then the driver should send switch_multilevel set 
    	And with parameter level <level-param>
    Then protocol bus should be empty
    Then the driver variable targetlevel should be <target-brightness>
    Then the driver variable targetstate should be ON
    
	Examples:
      | init-brightness | init-state | to-brightness | level-param | target-brightness | remarks                                                                 |
      | 10              | ON         | 1             | 1           | 1                 | adjust ON device to level 1                                             |
      | 10              | ON         | 10            | 10          | 10                | adjust ON device to level 10                                            |
      | 10              | ON         | 50            | 50          | 50                | adjust ON device to level 50                                            |
      | 10              | ON         | 99            | 99          | 99                | adjust ON device to level 99                                            |
      | 10              | ON         | 100           | 99          | 100               | adjust ON device to level 100 (Zwave drivers map 100 to max of 99)      |
      | 10              | OFF        | 1             | 1           | 1                 | turn OFF device ON to level 1                                           |
      | 10              | OFF        | 10            | 10          | 10                | turn OFF device ON to level 10                                          |
      | 50              | OFF        | 99            | 99          | 99                | turn OFF device ON to level 99                                          |
      | 10              | OFF        | 100           | 99          | 100               | turn OFF device ON to level 100  (Zwave drivers map 100 to max of 99)   |
      | 50              | OFF        | 101           | 99          | 100               | brightness out-of-range high uses value of 100                          |
      | 50              | ON         | 101           | 99          | 100               | brightness out-of-range high uses value of 100                          |
      | 50              | OFF        | 128           | 99          | 100               | brightness out-of-range high uses value of 100                          |
      | 50              | ON         | 128           | 99          | 100               | brightness out-of-range high uses value of 100                          |
      | 50              | OFF        | 255           | 99          | 100               | brightness out-of-range high uses value of 100                          |
      | 50              | ON         | 255           | 99          | 100               | brightness out-of-range high uses value of 100                          |
      | 50              | OFF        | 256           | 99          | 100               | brightness out-of-range high uses value of 100                          |
      | 50              | ON         | 256           | 99          | 100               | brightness out-of-range high uses value of 100                          |

# Setting just the Dimmer.brightness to 0 will be ignored (Apps should not be doing this)
@dimProc
Scenario Outline: Client sets brightness attribute of 0, and no switch state, so command is ignored
	Given the capability dim:brightness is <init-brightness>
		And the capability swit:state is <init-state>
		And the driver variable targetlevel is <init-brightness>
		And the driver variable targetstate is <init-state>
    When a base:SetAttributes command with the value of dim:brightness <to-brightness> is placed on the platform bus
	Then the driver should place a EmptyMessage message on the platform bus
    Then protocol bus should be empty
    Then the driver variable targetlevel should be <target-brightness>
    Then the driver variable targetstate should be <target-state>
    
	Examples:
      | init-brightness | init-state | to-brightness | target-brightness | target-state | remarks                                                                 |
      | 10              | ON         | 0             | 10                | ON           | leave ON switch ON at same brightness                                   |
      | 50              | OFF        | 0             | 50                | OFF          | leave OFF switch OFF at same brightness                                 |
      | 50              | OFF        | -1            | 50                | OFF          | brightness out-of-range low uses value of 0, ignores brightness setting |
      | 50              | ON         | -1            | 50                | ON           | brightness out-of-range low uses value of 0, ignores brightness setting |

# Setting just the Switch.state to OFF, will turn the device off and leave the brightness setting at whatever brightness it was previously
@dimProc
Scenario Outline: Client sets swit:state to OFF with no dim:brightness setting
    Given the capability dim:brightness is <init-brightness>
		And the capability swit:state is <init-state>
		And the driver variable targetlevel is <init-brightness>
		And the driver variable targetstate is <init-state>
	When a base:SetAttributes command with the value of swit:state OFF is placed on the platform bus
	Then the driver should send switch_multilevel set 
    	And with parameter level 0
    Then protocol bus should be empty
    Then the driver variable targetlevel should be <init-brightness>
    Then the driver variable targetstate should be OFF

	Examples:
      | init-brightness | init-state | remarks                                         |
      | 1               | OFF        | turn OFF device to OFF (to make sure it is OFF) |
      | 10              | OFF        | turn OFF device to OFF (to make sure it is OFF) |
      | 99              | OFF        | turn OFF device to OFF (to make sure it is OFF) |
      | 100             | OFF        | turn OFF device to OFF (to make sure it is OFF) |
      | 1               | ON         | turn ON device to OFF                           |
      | 10              | ON         | turn ON device to OFF                           |
      | 99              | ON         | turn ON device to OFF                           |
      | 100             | ON         | turn ON device to OFF                           |

# Setting just the Switch.state to ON, will turn the device on at the current Dimmer.brightness (which should be non-zero, but if it is somehow zero then the brightness will default to 100)
@dimProc
Scenario Outline: Client sets swit:state to ON with no dim:brightness setting
    Given the capability dim:brightness is <init-brightness>
		And the capability swit:state is <init-state>
		And the driver variable targetlevel is <init-brightness>
		And the driver variable targetstate is <init-state>
	When a base:SetAttributes command with the value of swit:state ON is placed on the platform bus
	Then the driver should send switch_multilevel set 
    	And with parameter level <level-param>
    Then protocol bus should be empty
    Then the driver variable targetlevel should be <target-brightness>
    Then the driver variable targetstate should be ON

	Examples:
      | init-brightness | init-state | level-param | target-brightness | remarks                                                                             |
      | 1               | OFF        | 1           | 1                 | turn OFF device to ON at prev level                                                 |
      | 10              | OFF        | 10          | 10                | turn OFF device to ON at prev level                                                 |
      | 99              | OFF        | 99          | 99                | turn OFF device to ON at prev level                                                 |
      | 100             | OFF        | 99          | 100               | turn OFF device to ON at prev level (Zwave drivers map 100 to max of 99)            |
      | 1               | ON         | 1           | 1                 | turn ON device to ON (to make sure it is ON)                                        |
      | 10              | ON         | 10          | 10                | turn ON device to ON (to make sure it is ON)                                        |
      | 99              | ON         | 99          | 99                | turn ON device to ON (to make sure it is ON)                                        |
      | 100             | ON         | 99          | 100               | turn ON device to ON (to make sure it is ON, Zwave drivers map 100 to max of 99)    |
      | 0               | OFF        | 99          | 100               | turn OFF device to ON at default 100 (should never really happen that brightness=0) |
      | 0               | ON         | 99          | 100               | turn OFF device to ON at default 100 (should never really happen that brightness=0) |

# Setting the Switch.state to OFF and Dimmer.brightness to 1-100 will turn the device OFF and set the driver brightness to the specified brightness so that value is used as the default when turned back ON
@dimProc
Scenario Outline: Client sets swit:state to OFF with a dim:brightness
    Given the capability dim:brightness is <init-brightness>
		And the capability swit:state is <init-state>
		And the driver variable targetlevel is <init-brightness>
		And the driver variable targetstate is <init-state>
	When the capability method base:SetAttributes
		 And with capability swit:state is OFF
		 And with capability dim:brightness is <to-brightness>
		 And send to driver
	Then the driver should send switch_multilevel set 
    	And with parameter level 0
    Then protocol bus should be empty
    Then the driver variable targetlevel should be <target-brightness>
    Then the driver variable targetstate should be OFF

	Examples:
      | init-brightness | init-state | to-brightness | target-brightness | remarks                                                                                |
      | 1               | OFF        | 1             | 1                 | turn OFF device to OFF with same default ON level                                      |
      | 10              | OFF        | 1             | 1                 | turn OFF device to OFF with new default ON level                                       |
      | 99              | OFF        | 10            | 10                | turn OFF device to OFF with new default ON level                                       |
      | 100             | OFF        | 50            | 50                | turn OFF device to OFF with new default ON level                                       |
      | 100             | OFF        | 99            | 99                | turn OFF device to OFF with new default ON level                                       |
      | 100             | OFF        | 100           | 100               | turn OFF device to OFF with same default ON level (Zwave drivers map 100 to max of 99) |
      | 1               | ON         | 1             | 1                 | turn ON device to OFF with same default ON level                                       |
      | 10              | ON         | 1             | 1                 | turn ON device to OFF with new default ON level                                        |
      | 99              | ON         | 10            | 10                | turn ON device to OFF with new default ON level                                        |
      | 100             | ON         | 50            | 50                | turn ON device to OFF with new default ON level                                        |
      | 100             | ON         | 99            | 99                | turn ON device to OFF with new default ON level                                        |
      | 100             | ON         | 100           | 100               | turn ON device to OFF with same default ON level (Zwave drivers map 100 to max of 99)  |
      | 50              | OFF        | 101           | 100               | brightness out-of-range high uses value of 100                                         |
      | 50              | ON         | 101           | 100               | brightness out-of-range high uses value of 100                                         |
      | 50              | OFF        | 128           | 100               | brightness out-of-range high uses value of 100                                         |
      | 50              | ON         | 128           | 100               | brightness out-of-range high uses value of 100                                         |
      | 50              | OFF        | 255           | 100               | brightness out-of-range high uses value of 100                                         |
      | 50              | ON         | 255           | 100               | brightness out-of-range high uses value of 100                                         |
      | 50              | OFF        | 256           | 100               | brightness out-of-range high uses value of 100                                         |
      | 50              | ON         | 256           | 100               | brightness out-of-range high uses value of 100                                         |
      | 50              | OFF        | -1            | 50                | brightness out-of-range low uses value of 0, ignores brightness setting                |
      | 50              | ON         | -1            | 50                | brightness out-of-range low uses value of 0, ignores brightness setting                |

# Setting the Switch.state to OFF and Dimmer.brightness to 0 will turn the device off and leave the brightness setting at whatever value it was previously
@dimProc
Scenario Outline: Client sets swit:state to OFF with a dim:brightness of zero
    Given the capability dim:brightness is <init-brightness>
		And the capability swit:state is <init-state>
		And the driver variable targetlevel is <init-brightness>
		And the driver variable targetstate is <init-state>
	When the capability method base:SetAttributes
		 And with capability swit:state is OFF
		 And with capability dim:brightness is 0
		 And send to driver
	Then the driver should send switch_multilevel set 
    	And with parameter level 0
    Then protocol bus should be empty
    Then the driver variable targetlevel should be <init-brightness>
    Then the driver variable targetstate should be OFF

	Examples:
      | init-brightness | init-state | remarks                                         |
      | 1               | OFF        | turn OFF device to OFF (to make sure it is OFF) |
      | 10              | OFF        | turn OFF device to OFF (to make sure it is OFF) |
      | 99              | OFF        | turn OFF device to OFF (to make sure it is OFF) |
      | 100             | OFF        | turn OFF device to OFF (to make sure it is OFF) |
      | 1               | ON         | turn ON device to OFF                           |
      | 10              | ON         | turn ON device to OFF                           |
      | 99              | ON         | turn ON device to OFF                           |
      | 100             | ON         | turn ON device to OFF                           |

# Setting the Switch.state to ON and Dimmer.brightness to 1-100 will set the device to the specified brightness first and then turn the device ON (after a short delay)
@dimProc
Scenario Outline: Client sets swit:state to ON with a dim:brightness
    Given the capability dim:brightness is <init-brightness>
		And the capability swit:state is <init-state>
		And the driver variable targetlevel is <init-brightness>
		And the driver variable targetstate is <init-state>
	When the capability method base:SetAttributes
		 And with capability swit:state is ON
		 And with capability dim:brightness is <to-brightness>
		 And send to driver
	Then the driver should send switch_multilevel set 
    	And with parameter level <level-param>
    Then protocol bus should be empty
    Then the driver variable targetlevel should be <target-brightness>
    Then the driver variable targetstate should be ON

	Examples:
      | init-brightness | init-state | to-brightness | level-param | target-brightness | remarks                                                                         |
      | 1               | OFF        | 1             | 1           | 1                 | turn ON device to current default ON level                                      |
      | 10              | OFF        | 1             | 1           | 1                 | turn ON device to a new level                                                   |
      | 99              | OFF        | 100           | 99          | 100               | turn ON device to a new level (Zwave drivers map 100 to max of 99)              |
      | 100             | OFF        | 99            | 99          | 99                | turn ON device to a new level                                                   |
      | 100             | OFF        | 100           | 99          | 100               | turn ON device to current default ON level (Zwave drivers map 100 to max of 99) |
      | 1               | ON         | 1             | 1           | 1                 | turn ON device to current default ON level                                      |
      | 10              | ON         | 1             | 1           | 1                 | turn ON device to a new level                                                   |
      | 99              | ON         | 100           | 99          | 100               | turn ON device to a new level (Zwave drivers map 100 to max of 99)              |
      | 100             | ON         | 99            | 99          | 99                | turn ON device to a new level                                                   |
      | 100             | ON         | 100           | 99          | 100               | turn ON device to current default ON level (Zwave drivers map 100 to max of 99) |
      | 50              | OFF        | 101           | 99          | 100               | brightness out-of-range high uses value of 100                                  |
      | 50              | ON         | 101           | 99          | 100               | brightness out-of-range high uses value of 100                                  |
      | 50              | OFF        | 128           | 99          | 100               | brightness out-of-range high uses value of 100                                  |
      | 50              | ON         | 128           | 99          | 100               | brightness out-of-range high uses value of 100                                  |
      | 50              | OFF        | 255           | 99          | 100               | brightness out-of-range high uses value of 100                                  |
      | 50              | ON         | 255           | 99          | 100               | brightness out-of-range high uses value of 100                                  |
      | 50              | OFF        | 256           | 99          | 100               | brightness out-of-range high uses value of 100                                  |
      | 50              | ON         | 256           | 99          | 100               | brightness out-of-range high uses value of 100                                  |
      | 50              | OFF        | -1            | 50          | 50                | brightness out-of-range low uses value of 0, ignores brightness setting         |
      | 50              | ON         | -1            | 50          | 50                | brightness out-of-range low uses value of 0, ignores brightness setting         |

# Setting the Switch.state to ON and Dimmer.brightness to 0 will turn the device on at the previous Dimmer.brightness (which should be non-zero, but if it is somehow zero then the brightness will default to 100)
@dimProc
Scenario Outline: Client sets swit:state to ON with a dim:brightness of zero
    Given the capability dim:brightness is <init-brightness>
		And the capability swit:state is <init-state>
		And the driver variable targetlevel is <init-brightness>
		And the driver variable targetstate is <init-state>
	When the capability method base:SetAttributes
		 And with capability swit:state is ON
		 And with capability dim:brightness is 0
		 And send to driver
	Then the driver should send switch_multilevel set 
    	And with parameter level <level-param>
    Then protocol bus should be empty
    Then the driver variable targetlevel should be <target-brightness>
    Then the driver variable targetstate should be ON

	Examples:
      | init-brightness | init-state | level-param | target-brightness | remarks                                                                         |
      | 1               | OFF        | 1           | 1                 | turn ON device to current default ON level                                      |
      | 10              | OFF        | 10          | 10                | turn ON device to current default ON level                                      |
      | 99              | OFF        | 99          | 99                | turn ON device to current default ON level                                      |
      | 100             | OFF        | 99          | 100               | turn ON device to current default ON level (Zwave drivers map 100 to max of 99) |
      | 1               | ON         | 1           | 1                 | turn ON device to current default ON level                                      |
      | 10              | ON         | 10          | 10                | turn ON device to current default ON level                                      |
      | 99              | ON         | 99          | 99                | turn ON device to current default ON level                                      |
      | 100             | ON         | 99          | 100               | turn ON device to current default ON level (Zwave drivers map 100 to max of 99) |
      | 0               | OFF        | 99          | 100               | turn ON device to default 100 (should never really happen that brightness=0)    |
      | 0               | ON         | 99          | 100               | turn ON device to default 100 (should never really happen that brightness=0)    |



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
      |before    | report | state | brightness | remarks                                        |
      | 10       | 0      | OFF   | 10         | Treated as binary off; no change to brightness |
      | 10       | 1      | ON    | 1          | New common dimmer capability                   |
      | 10       | 99     | ON    | 100        |                                                |
      | 10       | 100    | ON    | 100        |                                                |
      | 50       | 253    | OFF   | 50         | Ignore out of bounds                           |
      | 50       | 121    | OFF   | 50         | Ignore out of bounds                           |

