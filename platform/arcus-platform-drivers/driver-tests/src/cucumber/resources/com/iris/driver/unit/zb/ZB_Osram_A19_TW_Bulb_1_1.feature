@Zigbee @Osram @A19TW @dimmer @bulb
Feature: Test of the OSRAM A19 Tuneable White Dimmable Bulb driver
	
	These scenarios test the functionality of the OSRAM A19 Tuneable White Dimmable Bulb driver.

Background:
	Given the ZB_Osram_A19_TW_Bulb_1_1.driver has been initialized
	And the device has endpoint 3


Scenario: Driver reports capabilities to platform.
	When a base:GetAttributes command is placed on the platform bus
	Then the driver should place a base:GetAttributesResponse message on the platform bus
		And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devconn', 'devpow', 'dim', 'swit', 'devota', 'light', 'ident', 'colortemp']
		And the message's dev:devtypehint attribute should be Light
		And the message's devadv:drivername attribute should be ZBOsramA19TwBulb 
		And the message's devadv:driverversion attribute should be 1.1
		And the message's devpow:source attribute should be LINE
		And the message's devpow:linecapable attribute should be true		
		And the message's devpow:backupbatterycapable attribute should be false		
	Then both busses should be empty


Scenario: Device connected for first time
	When the device is connected
	Then the driver should set timeout
	# driver restores bulb to default level
	Then the driver should send level moveToLevel
	# driver restores bulb to default color temperature
	Then the driver should send color moveToColorTemperature

@reconnect
Scenario: Device disconnected while OFF should be restored to OFF state when reconnected
	Given the capability swit:state is OFF
	When the device is connected
	Then the driver should set timeout
	# driver restores bulb to default level
	Then the driver should send onoff OFF

@reconnect
Scenario Outline: Device disconnected while using Color Temperature should restore Color Temperature settings when reconnected
	Given the capability dim:brightness is <init-brightness>
		And the capability swit:state is ON
		And the capability light:colormode is COLORTEMP
		And the capability colortemp:colortemp is <init-clr-tmp>
	When the device is connected
	Then the driver should set timeout
	# driver restores bulb to default level
	Then the driver should send level moveToLevel
	# driver restores bulb to default color temperature
	Then the driver should send color moveToColorTemperature
    Then the driver variable targetLevel should be <init-brightness>
    Then the driver variable targetState should be ON
	Then the driver variable KEY_ACTUAL_COLOR_TEMPERATURE should be <act-clr-temp>

	# act-clr-temp should be 1000000/init-clr-tmp using integer math
	Examples:
      | init-brightness | init-clr-tmp | act-clr-temp | remarks        |
      | 20              | 2700         | 370          |                |
      | 50              | 6500         | 153          |                |


# Setting just the Dimmer.brightness to 1-100 will adjust the brightness to that value and turn the device ON, whatever state it was previously in
@dimProc
Scenario Outline: Client sets brightness attribute of 1-100, and no switch state
	Given the capability dim:brightness is <init-brightness>
		And the capability swit:state is <init-state>
		And the driver variable targetLevel is <init-brightness>
		And the driver variable targetState is <init-state>
    When a base:SetAttributes command with the value of dim:brightness <to-brightness> is placed on the platform bus
	Then the driver should send level moveToLevel
    Then protocol bus should be empty
    Then the driver variable targetLevel should be <target-brightness>
    Then the driver variable targetState should be ON
    
	Examples:
      | init-brightness | init-state | to-brightness | target-brightness | remarks                           |
      | 10              | ON         | 50            | 50                | adjust ON device to level 50      |
      | 10              | OFF        | 50            | 50                | turn OFF device to ON at level 50 |

# Setting just the Switch.state to OFF, will turn the device off and leave the brightness setting at whatever brightness it was previously
@dimProc
Scenario Outline: Client sets swit:state to OFF with no dim:brightness setting
    Given the capability dim:brightness is <init-brightness>
		And the capability swit:state is <init-state>
		And the driver variable targetLevel is <init-brightness>
		And the driver variable targetState is <init-state>
	When a base:SetAttributes command with the value of swit:state OFF is placed on the platform bus
	Then the driver should send onoff off
    Then protocol bus should be empty
    Then the driver variable targetLevel should be <init-brightness>
    Then the driver variable targetState should be OFF

	Examples:
      | init-brightness | init-state | remarks                                         |
      | 10              | OFF        | turn OFF device to OFF (to make sure it is OFF) |
      | 10              | ON         | turn ON device to OFF                           |

# Setting just the Switch.state to ON, will turn the device on at the current Dimmer.brightness (which should be non-zero, but if it is somehow zero then the brightness will default to 100)
@dimProc
Scenario Outline: Client sets swit:state to ON with no dim:brightness setting
    Given the capability dim:brightness is <init-brightness>
		And the capability swit:state is <init-state>
		And the driver variable targetLevel is <init-brightness>
		And the driver variable targetState is <init-state>
	When a base:SetAttributes command with the value of swit:state ON is placed on the platform bus
	Then the driver should send level moveToLevel
    Then protocol bus should be empty
    Then the driver variable targetLevel should be <target-brightness>
    Then the driver variable targetState should be ON

	Examples:
      | init-brightness | init-state | target-brightness | remarks                                      |
      | 10              | OFF        | 10                | turn OFF device to ON at prev level          |
      | 10              | ON         | 10                | turn ON device to ON (to make sure it is ON) |

# Setting the Switch.state to OFF and Dimmer.brightness to 1-100 will turn the device OFF and set the driver brightness to the specified brightness so that value is used as the default when turned back ON
@dimProc
Scenario Outline: Client sets swit:state to OFF with a dim:brightness
    Given the capability dim:brightness is <init-brightness>
		And the capability swit:state is <init-state>
		And the driver variable targetLevel is <init-brightness>
		And the driver variable targetState is <init-state>
	When the capability method base:SetAttributes
		 And with capability swit:state is OFF
		 And with capability dim:brightness is <to-brightness>
		 And send to driver
	Then the driver should send onoff off
    Then protocol bus should be empty
    Then the driver variable targetLevel should be <target-brightness>
    Then the driver variable targetState should be OFF

	Examples:
      | init-brightness | init-state | to-brightness | target-brightness | remarks                                          |
      | 100             | OFF        | 50            | 50                | turn OFF device to OFF with new default ON level |
      | 100             | ON         | 50            | 50                | turn ON device to OFF with new default ON level  |

# Setting the Switch.state to ON and Dimmer.brightness to 1-100 will set the device to the specified brightness first and then turn the device ON (after a short delay)
@dimProc
Scenario Outline: Client sets swit:state to ON with a dim:brightness
    Given the capability dim:brightness is <init-brightness>
		And the capability swit:state is <init-state>
		And the driver variable targetLevel is <init-brightness>
		And the driver variable targetState is <init-state>
	When the capability method base:SetAttributes
		 And with capability swit:state is ON
		 And with capability dim:brightness is <to-brightness>
		 And send to driver
	Then the driver should send level moveToLevel
    Then protocol bus should be empty
    Then the driver variable targetLevel should be <target-brightness>
    Then the driver variable targetState should be ON

	Examples:
      | init-brightness | init-state | to-brightness | target-brightness | remarks                              |
      | 10              | OFF        | 50            | 50                | turn OFF device to ON at a new level |
      | 10              | ON         | 50            | 50                | turn ON device to ON at a new level  |


