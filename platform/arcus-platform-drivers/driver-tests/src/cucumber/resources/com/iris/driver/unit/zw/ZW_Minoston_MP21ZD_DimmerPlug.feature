@ZWave @Minoston @Dimmer @MP21ZD
Feature: Unit Tests for the ZWMinostonMP21ZDDimmerPlugDriver

    These scenarios test the functionality of the Minoston MP21ZD Smart Plug Dimmer driver

    Background:
        Given the ZW_Minoston_MP21ZD_DimmerPlug.driver has been initialized


    @basic
    Scenario: Driver reports capabilities to platform.
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'devsettings', 'swit', 'dim']
            And the message's dev:devtypehint attribute should be Dimmer
            And the message's devadv:drivername attribute should be ZWMinostonMP21ZDDimmerPlugDriver
            And the message's devadv:driverversion attribute should be 2.4
            And the message's devpow:source attribute should be LINE
            And the message's devpow:linecapable attribute should be true
            And the message's devpow:backupbatterycapable attribute should be false
            And the message's swit:state attribute should be OFF
        Then both busses should be empty


############################################################
# General Driver Tests
############################################################

    @basic @added
    Scenario: Make sure any "time of change" attributes are defaulted when the device is first Added
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the capability swit:statechanged should be recent
        Then both busses should be empty

    @basic @connected @timeout
    Scenario: Device reports state when first connected
        When the device is connected
        Then the driver should set timeout at 3 hours
        Then the driver should poll switch_multilevel.get every 1 hour
        Then the driver should send switch_multilevel get
        Then the driver should send association set
            And with parameter group 1
            And with parameter node1 1
        Then the driver should send configuration get
            And with parameter param 2
        Then the driver should send configuration get
            And with parameter param 4
        Then the driver should send configuration get
            And with parameter param 6
        Then the driver should send configuration get
            And with parameter param 7
        Then the driver should send configuration get
            And with parameter param 8
        Then the driver should send configuration get
            And with parameter param 9
        Then the driver should send configuration get
            And with parameter param 10
        Then the driver should send configuration get
            And with parameter param 11
        Then the driver should send configuration get
            And with parameter param 12
        Then the driver should not send configuration set

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Dimmer                   |
          | "Lamp Dimmer"            |
          | "Tom's Plug"             |
          | "Bob & Sue's Porch"      |


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
      | 10              | ON         | 1             | 1           | 1                 | turn ON device to a new level                                                   |
      | 99              | ON         | 100           | 99          | 100               | turn ON device to a new level (Zwave drivers map 100 to max of 99)              |
      | 100             | ON         | 99            | 99          | 99                | turn ON device to a new level                                                   |
      | 50              | OFF        | 101           | 99          | 100               | brightness out-of-range high uses value of 100                                  |
      | 50              | ON         | 101           | 99          | 100               | brightness out-of-range high uses value of 100                                  |
      | 50              | OFF        | -1            | 50          | 50                | brightness out-of-range low uses value of 0, ignores brightness setting         |

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
      | 0               | OFF        | 99          | 100               | turn ON device to default 100 (should never really happen that brightness=0)    |
      | 0               | ON         | 99          | 100               | turn ON device to default 100 (should never really happen that brightness=0)    |

# The MP21ZD reports every change on its own, so an ON it has already reported isn't sent again. The level is read instead.
@dimProc
Scenario Outline: Client turns ON a device that has reported it is ON at that brightness
    Given the capability dim:brightness is 50
		And the capability swit:state is ON
		And the driver variable targetlevel is 50
		And the driver variable targetstate is ON
	When a base:SetAttributes command with the value of <attribute> <value> is placed on the platform bus
	Then the driver should place a EmptyMessage message on the platform bus
	Then the driver should send switch_multilevel get
	Then protocol bus should be empty
		And there should be no more scheduled events

	Examples:
      | attribute      | value |
      | swit:state     | ON    |
      | dim:brightness | 50    |


############################################################
# Read-back Tests (MAX_READBACKS 1, DFLT_READBACK_DELAY 5000)
############################################################

    # The MP21ZD reports its new level on its own, so after a command the driver asks for the
    # level once, 5 seconds later, and only if no matching report has arrived.
    @dimmer @readback
    Scenario: A command asks for the level once, 5 seconds later
        When a base:SetAttributes command with the value of dim:brightness 50 is placed on the platform bus
        Then the driver should send switch_multilevel set
            And the driver should cancel event DeferredSwitchReadLevel
            And the driver should schedule event DeferredSwitchReadLevel in 5 seconds
        When event DeferredSwitchReadLevel triggers
        Then the driver should send switch_multilevel get
            And the driver should schedule event DeferredSwitchReadLevel in 1 second
        When event DeferredSwitchReadLevel triggers
        Then protocol bus should be empty
            And there should be no more scheduled events

    # onStarted sets the read-back vars; connecting no longer resets them
    @connected @readback
    Scenario: Connecting keeps the read-back settings
        When the device is connected
        Then the driver variable MAX_READBACKS should be 1
            And the driver variable DFLT_READBACK_DELAY should be 5000


############################################################
# Multilevel Switch Report Tests
############################################################

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
