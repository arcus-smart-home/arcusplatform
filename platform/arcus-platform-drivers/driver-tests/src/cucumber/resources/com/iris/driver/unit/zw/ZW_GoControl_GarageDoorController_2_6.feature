@ZWave @gdo @gdogc26
Feature: Motorized Door capability for the ZWGoControlGarageDoorControllerDriver

	These scenarios test how the GoControl Garage Door Controller Z-Wave Driver tracks the door state.
	Every door state comes from the reflexes. The hub only reports a state that differs from its own
	copy, so OPENING followed by a report of where the door started must still be a change.

	Background:
		Given the ZW_GoControl_GarageDoorController_2_6.driver has been initialized

	Scenario Outline: Platform operates the door
		Given the driver attribute motdoor:doorstate is <before>
		When a base:SetAttributes command with the value of motdoor:doorstate <command> is placed on the platform bus
		Then the driver should send barrier_operator set
			And with parameter barrierstate <barrier_state>
		Then the driver should schedule event CheckDoor in 120 seconds
		Then the driver should place a EmptyMessage message on the platform bus
		Then nothing else should happen

		Examples:
		  | before | command | barrier_state |
		  | CLOSED | OPEN    | -1            |
		  | OPEN   | CLOSED  | 0             |

	Scenario Outline: Platform operates an obstructed door
		Given the driver attribute devadv:errors is {'ERR_OBSTRUCTION':'Obstructed'}
		When a base:SetAttributes command with the value of motdoor:doorstate <command> is placed on the platform bus
		Then the driver should place a EmptyMessage message on the platform bus
		Then nothing else should happen

		Examples:
		  | command |
		  | OPEN    |
		  | CLOSED  |

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
