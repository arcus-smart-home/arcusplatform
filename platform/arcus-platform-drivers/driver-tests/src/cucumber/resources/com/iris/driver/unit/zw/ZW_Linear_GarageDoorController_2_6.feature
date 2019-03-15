@ZWave @gdo @gdo26
Feature: Motorized Door capability for the ZWLinearGarageDoorControllerDriver

	These scenarios test the functionality of the Linear Garage Door Controller Z-Wave Driver.

	Background:
		Given the ZW_Linear_GarageDoorController_2_6.driver has been initialized

	Scenario: Driver reports capabilities to platform.
		When a base:GetAttributes command is placed on the platform bus
		Then the driver should place a base:GetAttributesResponse message on the platform bus
			And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'motdoor']
			And the message's dev:devtypehint attribute should be Garage Door
			And the message's devadv:drivername attribute should be ZWLinearGarageDoorControllerDriver
			And the message's devadv:driverversion attribute should be 2.6
			And the message's devpow:source attribute should be LINE
			And the message's devpow:linecapable attribute should be true
			And the message's devpow:backupbatterycapable attribute should be false
			And the message's motdoor:doorstate attribute should be CLOSED
		Then both busses should be empty

	Scenario: Device is added
		When the device is added
		Then the capability devpow:sourcechanged should be recent
			And the capability motdoor:doorstatechanged should be recent
		Then both busses should be empty
@connected
	Scenario: Device is connected
		When the device is connected
		Then the driver should send version get
		Then the driver should send barrier_operator get
			And the driver should send association get
			And the driver should poll barrier_operator.get every 20 minutes
			And the driver should set timeout at 70 minutes
		Then the driver should place a base:ValueChange message on the platform bus
		Then the driver should schedule event SetLifeline every 2 seconds 10 times
		Then nothing else should happen

	Scenario: Platform attempts to Open GDC via attribute change.
		Given the driver attribute motdoor:doorstate is CLOSED
		When a base:SetAttributes command with the value of motdoor:doorstate OPEN is placed on the platform bus
		Then the driver should send barrier_operator set
			And with parameter barrierstate -1
		Then the driver variable lastCommandTime should be recent
			And the driver variable lastCommandMode should be -1
		Then the driver should place a EmptyMessage message on the platform bus
		Then both busses should be empty

	Scenario: Platform attempts to Close GDC via attribute change.
		Given the driver attribute motdoor:doorstate is OPEN
		When a base:SetAttributes command with the value of motdoor:doorstate CLOSED is placed on the platform bus
		Then the driver should send barrier_operator set
			And with parameter barrierstate 0
		Then the driver variable lastCommandTime should be recent
			And the driver variable lastCommandMode should be 0
		Then the driver should place a EmptyMessage message on the platform bus
		Then both busses should be empty

	Scenario: Platform attempts to Open GDC via attribute change when obstructed.
		Given the driver attribute devadv:errors is {'ERR_OBSTRUCTION':'Obstructed'}
		When a base:SetAttributes command with the value of motdoor:doorstate OPEN is placed on the platform bus
		Then the driver variable lastCommandTime should be null
		And the driver variable lastCommandMode should be null
		Then the driver should place a EmptyMessage message on the platform bus
		Then both busses should be empty

	Scenario: Platform attempts to Close GDC via attribute change when obstructed.
		Given the driver attribute devadv:errors is {'ERR_OBSTRUCTION':'Obstructed'}
		When a base:SetAttributes command with the value of motdoor:doorstate CLOSED is placed on the platform bus
		Then the driver variable lastCommandTime should be null
		And the driver variable lastCommandMode should be null
		Then the driver should place a EmptyMessage message on the platform bus
		Then both busses should be empty

	Scenario Outline: Platform sends another state.
		When a base:SetAttributes command with the value of motdoor:doorstate <state> is placed on the platform bus
		Then the driver should place a EmptyMessage message on the platform bus
		Then both busses should be empty

	Examples:
		| state       |
		| OPENING     |
		| CLOSING     |
		| OBSTRUCTION |
@Alert
	Scenario Outline: Device sends a notification
		Given the driver attribute devpow:battery is <before>
		Given the driver attribute motdoor:doorstate is <door>
		When the device responds with alarm report
			And with parameter notificationType 6
			And with parameter event <event>
			And send to driver
			And the capability devpow:battery should be <battery>
			And the capability motdoor:doorstate should be <after>
		Then the driver <sends> place a base:ValueChange message on the platform bus
		Then both busses should be empty

		Examples:
		  | before  | door        | event | battery | after       | sends      |
		  |  10     | OBSTRUCTION |  0    |  100    | OBSTRUCTION | should     |
		  | 100     |  OPEN       | 68    |  100    | OPEN        | should not |
		  | 100     | CLOSING     | 69    |  100    | OBSTRUCTION | should     |
		  | 100     | CLOSED      | 73    |  100    | CLOSED      | should not |
		  | 100     | OPEN        | 74    |   10    | OPEN        | should     |
@Alert
	Scenario: Get Status on obstruction
		When the device responds with alarm report
			And with parameter notificationType 6
			And with parameter event 70
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability motdoor:doorstate should be OBSTRUCTION
		Then nothing else should happen

	Scenario: Clear Alarms
		Given the driver attribute devpow:battery is 10
			And the driver variable obstructionCnt is 1
		When the device responds with alarm report
			And with parameter notificationType 6
			And with parameter event 0
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability devpow:battery should be 100
		Then both busses should be empty

	Scenario Outline: Other Alarm Notifications
		When the device responds with alarm report
			And with parameter notificationType <type>
			And with parameter event <event>
			And send to driver
		Then the driver should not place a base:ValueChange message on the platform bus
			And both busses should be empty

		Examples:
		  | type | event |
		  |  6   |  1    |
		  |  6   |  0x4  |
		  |  7   |  0    |
		  |  7   |  -1   |
		  |  9   |  2    |
		  |  10  |  12   |

	Scenario Outline: Device reports a door state
		Given the driver attribute motdoor:doorstate is <before>
			And the driver variable lastCommandTime is now
			And the driver variable lastCommandMode is <mode>
		When the device response with barrier_operator report
			And with parameter barrierstate <barrier_state>
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability motdoor:doorstate should be <door_state>
			And the capability motdoor:doorstatechanged should be recent
		Then both busses should be empty

		Examples:
		  |before   | mode | barrier_state | door_state  |
		  | CLOSED  |  -1  | -2            | OPENING     |
		  | OPEN    |  0   | -4            | CLOSING     |

	Scenario Outline: Device reports an incorrect door state
		Given the driver attribute motdoor:doorstate is <before>
			And the time driver variable lastCommandTime is <time>
			And the driver variable lastCommandMode is <mode>
		When the device response with barrier_operator report
			And with parameter barrierstate <barrier_state>
			And send to driver
		Then the driver should not place a base:ValueChange message on the platform bus
			And the capability motdoor:doorstate should be <door_state>
		Then both busses should be empty

		Examples:
		  |before      |  time           |  mode | barrier_state | door_state  |
		  | CLOSED     |  5 seconds ago  |  0    |  -2           | CLOSED      |
		  | CLOSED     | 150 seconds ago |  -1   |  -2           | CLOSED      |
		  | CLOSED     | 150 seconds ago |  0    |  -2           | CLOSED      |
		  | OPEN       |  5 seconds ago  |  0    |  -2           | OPEN        |
		  | OBSTRUCTED |  5 seconds ago  |  0    |  -2           | OBSTRUCTED  |
		  | OPEN       |  5 seconds ago  | -1    |  -4           | OPEN        |
		  | OPEN       | 150 seconds ago |  0    |  -4           | OPEN        |
		  | OPEN       | 150 seconds ago | -1    |  -4           | OPEN        |
		  | CLOSED     |  5 seconds ago  | -1    |  -4           | CLOSED      |
		  | OBSTRUCTED |  5 seconds ago  | -1    |  -4           | OBSTRUCTED  |

	Scenario Outline: Device reports an unknown door state
		When the device response with barrier_operator report
			And with parameter barrierstate <barrier_state>
			And send to driver
		Then the driver should not place a base:ValueChange message on the platform bus
			And both busses should be empty

		Examples:
		  | barrier_state |
			|     1         |
			|   0x63        |
			|   0x64        |
			|   0xFB        |

	Scenario Outline: Device reports a door state event
		Given the driver attribute motdoor:doorstate is <before>
		When the device response with barrier_operator <event>
			And send to driver
		Then the driver should place a base:ValueChange message on the platform bus
			And the capability motdoor:doorstate should be <door_state>
			And the capability motdoor:doorstatechanged should be recent
		Then both busses should be empty

		Examples:
		  |before       |            event                         |  door_state |
		  | OBSTRUCTION |  report_unknown_position_moving_to_close |   CLOSING   |
		  | CLOSING     |  report_unknown_position_stopped         | OBSTRUCTION |
		  | OBSTRUCTION |  report_unknown_position_moving_to_open  |   OPENING   |
