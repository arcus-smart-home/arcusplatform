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
		Then the driver should schedule event SetLifeline every 2 seconds 10 times
		Then the driver may place a base:ValueChange message on the platform bus
		Then nothing else should happen

	Scenario: Platform attempts to Open GDC via attribute change.
		Given the driver attribute motdoor:doorstate is CLOSED
		When a base:SetAttributes command with the value of motdoor:doorstate OPEN is placed on the platform bus
		Then the driver should send barrier_operator set
			And with parameter barrierstate -1
		Then the driver should schedule event CheckDoor in 120 seconds
		Then the driver should place a EmptyMessage message on the platform bus
		Then nothing else should happen

	Scenario: Platform attempts to Close GDC via attribute change.
		Given the driver attribute motdoor:doorstate is OPEN
		When a base:SetAttributes command with the value of motdoor:doorstate CLOSED is placed on the platform bus
		Then the driver should send barrier_operator set
			And with parameter barrierstate 0
		Then the driver should schedule event CheckDoor in 120 seconds
		Then the driver should place a EmptyMessage message on the platform bus
		Then nothing else should happen

	Scenario: Platform attempts to Open GDC via attribute change when obstructed.
		Given the driver attribute devadv:errors is {'ERR_OBSTRUCTION':'Obstructed'}
		When a base:SetAttributes command with the value of motdoor:doorstate OPEN is placed on the platform bus
		Then the driver should place a EmptyMessage message on the platform bus
		Then nothing else should happen

	Scenario: Platform attempts to Close GDC via attribute change when obstructed.
		Given the driver attribute devadv:errors is {'ERR_OBSTRUCTION':'Obstructed'}
		When a base:SetAttributes command with the value of motdoor:doorstate CLOSED is placed on the platform bus
		Then the driver should place a EmptyMessage message on the platform bus
		Then nothing else should happen

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
			And the capability motdoor:doorstate should be OBSTRUCTION
		Then the driver should place a base:ValueChange message on the platform bus
		Then nothing else should happen

	Scenario: Clear Alarms
		Given the driver attribute devpow:battery is 10
			And the driver variable obstructionCnt is 1
		When the device responds with alarm report
			And with parameter notificationType 6
			And with parameter event 0
			And send to driver
			And the capability devpow:battery should be 100
		Then the driver should place a base:ValueChange message on the platform bus
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

	# Every door state comes from the reflexes. The hub only reports a state that differs from its
	# own copy, so OPENING followed by a report of where the door started must still be a change.
	Scenario Outline: Device reports a door state
		Given the driver attribute motdoor:doorstate is <before>
		When the device response with barrier_operator report
			And with parameter barrierstate <barrier_state>
			And send to driver
			And the capability motdoor:doorstate should be <door_state>
			And the capability motdoor:doorstatechanged should be recent
		Then the driver should place a base:ValueChange message on the platform bus
		Then nothing else should happen

		Examples:
		  | before      | barrier_state | door_state  |
		  | CLOSED      | -2            | OPENING     |
		  | OPENING     | -1            | OPEN        |
		  | OPEN        | -4            | CLOSING     |
		  | CLOSING     | 0             | CLOSED      |
		  | OPENING     | 0             | CLOSED      |
		  | CLOSING     | -1            | OPEN        |
		  | OPEN        | -2            | OPENING     |
		  | OBSTRUCTION | -4            | CLOSING     |
		  | CLOSING     | -3            | OBSTRUCTION |

	Scenario Outline: Device repeats its door state
		Given the driver attribute motdoor:doorstate is <state>
		When the device response with barrier_operator report
			And with parameter barrierstate <barrier_state>
			And send to driver
		Then the driver should not place a base:ValueChange message on the platform bus
			And the capability motdoor:doorstate should be <state>
		Then nothing else should happen

		Examples:
		  | state   | barrier_state |
		  | CLOSED  | 0             |
		  | OPEN    | -1            |
		  | OPENING | -2            |
		  | CLOSING | -4            |

	Scenario Outline: Door is still moving when the move should be over
		Given the driver attribute motdoor:doorstate is <state>
		When event CheckDoor triggers
		Then the driver should send barrier_operator get
		Then nothing else should happen

		Examples:
		  | state   |
		  | OPENING |
		  | CLOSING |

	Scenario Outline: Door finished moving before the check
		Given the driver attribute motdoor:doorstate is <state>
		When event CheckDoor triggers
		Then nothing should happen

		Examples:
		  | state       |
		  | OPEN        |
		  | CLOSED      |
		  | OBSTRUCTION |

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
