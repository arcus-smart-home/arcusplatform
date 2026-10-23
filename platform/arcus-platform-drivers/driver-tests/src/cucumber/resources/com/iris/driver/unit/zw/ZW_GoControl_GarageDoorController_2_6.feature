@ZWave @gdo @gdogc26
Feature: Motorized Door capability for the ZWGoControlGarageDoorControllerDriver

	These scenarios test how the GoControl Garage Door Controller Z-Wave Driver settles a door
	left OPENING or CLOSING. The hub drops a report of the state it already holds, and it never
	holds OPENING/CLOSING, so a door that fails to move and reports where it started would
	otherwise stay OPENING or CLOSING.

	Background:
		Given the ZW_GoControl_GarageDoorController_2_6.driver has been initialized

	Scenario Outline: Platform operates the door
		Given the driver attribute motdoor:doorstate is <before>
		When a base:SetAttributes command with the value of motdoor:doorstate <command> is placed on the platform bus
		Then the driver should send barrier_operator set
			And with parameter barrierstate <barrier_state>
		Then the driver should cancel event SettleDoor
			And the driver should schedule event CheckDoor in 120 seconds
		Then the driver should place a EmptyMessage message on the platform bus
		Then nothing else should happen

		Examples:
		  | before | command | barrier_state |
		  | CLOSED | OPEN    | -1            |
		  | OPEN   | CLOSED  | 0             |

	Scenario Outline: Device reports the door is moving
		Given the driver attribute motdoor:doorstate is <before>
			And the driver variable lastCommandTime is now
			And the driver variable lastCommandMode is <mode>
		When the device response with barrier_operator report
			And with parameter barrierstate <barrier_state>
			And send to driver
			And the capability motdoor:doorstate should be <door_state>
		Then the driver should cancel event SettleDoor
			And the driver should schedule event CheckDoor in 120 seconds
		Then the driver should place a base:ValueChange message on the platform bus
		Then nothing else should happen

		Examples:
		  | before | mode | barrier_state | door_state |
		  | CLOSED |  -1  | -2            | OPENING    |
		  | OPEN   |  0   | -4            | CLOSING    |

	Scenario Outline: Device reports an unexpected move
		Given the driver attribute motdoor:doorstate is <before>
			And the time driver variable lastCommandTime is 150 seconds ago
			And the driver variable lastCommandMode is <mode>
		When the device response with barrier_operator report
			And with parameter barrierstate <barrier_state>
			And send to driver
		Then the capability motdoor:doorstate should be <before>
		Then nothing else should happen

		Examples:
		  | before | mode | barrier_state |
		  | CLOSED |  -1  | -2            |
		  | OPEN   |  0   | -4            |

	Scenario Outline: Door is still moving when the move should be over
		Given the driver attribute motdoor:doorstate is <state>
		When event CheckDoor triggers
		Then the driver should send barrier_operator get
			And the driver should schedule event SettleDoor in 15 seconds
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

	Scenario Outline: Door never finished moving
		Given the driver attribute motdoor:doorstate is <before>
		When event SettleDoor triggers
		Then the capability motdoor:doorstate should be <after>
			And the capability motdoor:doorstatechanged should be recent
		Then the driver should place a base:ValueChange message on the platform bus
		Then nothing else should happen

		Examples:
		  | before  | after  |
		  | OPENING | CLOSED |
		  | CLOSING | OPEN   |

	Scenario Outline: Door finished moving after the check
		Given the driver attribute motdoor:doorstate is <state>
		When event SettleDoor triggers
		Then the capability motdoor:doorstate should be <state>
		Then nothing should happen

		Examples:
		  | state       |
		  | OPEN        |
		  | CLOSED      |
		  | OBSTRUCTION |
