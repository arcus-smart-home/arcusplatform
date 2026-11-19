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
			And the driver should cancel event RetryDoorCommand
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

#############################################################
# Commands refused with Application Status Busy
#############################################################

	@busy
	Scenario Outline: A close refused as busy is sent again after the wait time
		Given the driver attribute motdoor:doorstate is OPEN
		When a base:SetAttributes command with the value of motdoor:doorstate CLOSED is placed on the platform bus
		Then the driver should send barrier_operator set
			And with parameter barrierstate 0
			And the driver should schedule event CheckDoor in 120 seconds
			And the driver should cancel event RetryDoorCommand
		When the device response with application_status busy
			And with parameter status <status>
			And with parameter waittime <wait>
			And send to driver
		Then the driver should schedule event RetryDoorCommand in <delay> seconds
		When event RetryDoorCommand triggers
		Then the driver should send barrier_operator set
			And with parameter barrierstate 0
			And the driver should schedule event CheckDoor in 120 seconds
			And the driver variable commandTries should be 2

		Examples:
		  | status | wait | delay | remarks                        |
		  | 0      | 0    | 5     | try again later, default wait  |
		  | 1      | 10   | 10    | the controller's wait time     |
		  | 1      | 60   | 30    | a long wait time is capped     |
		  | 1      | 0    | 5     | no wait time given             |
		  | 7      | 3    | 5     | unknown status, default wait   |

	@busy
	Scenario: An open refused as busy is sent again
		Given the driver attribute motdoor:doorstate is CLOSED
			And the driver variable commandTarget is OPEN
			And the driver variable commandTime is now
			And the driver variable commandTries is 1
		When the device response with application_status busy
			And with parameter status 0
			And with parameter waittime 0
			And send to driver
		Then the driver should schedule event RetryDoorCommand in 5 seconds
		When event RetryDoorCommand triggers
		Then the driver should send barrier_operator set
			And with parameter barrierstate -1
			And the driver should schedule event CheckDoor in 120 seconds

	@busy
	Scenario: A command the controller queued is not sent again
		Given the driver attribute motdoor:doorstate is OPEN
			And the driver variable commandTarget is CLOSED
			And the driver variable commandTime is now
			And the driver variable commandTries is 1
		When the device response with application_status busy
			And with parameter status 2
			And with parameter waittime 0
			And send to driver
		Then there should be no more scheduled events

	@busy
	Scenario Outline: A command the door has started or carried out is not sent again
		Given the driver attribute motdoor:doorstate is <state>
			And the driver variable commandTarget is <target>
			And the driver variable commandTime is now
			And the driver variable commandTries is 1
		When the device response with application_status busy
			And with parameter status 0
			And with parameter waittime 0
			And send to driver
		Then there should be no more scheduled events

		Examples:
		  | target | state   |
		  | CLOSED | CLOSING |
		  | CLOSED | CLOSED  |
		  | OPEN   | OPENING |
		  | OPEN   | OPEN    |

	@busy
	Scenario: A Busy long after the last command is ignored
		Given the driver attribute motdoor:doorstate is OPEN
			And the driver variable commandTarget is CLOSED
			And the time driver variable commandTime is 3 minutes ago
			And the driver variable commandTries is 1
		When the device response with application_status busy
			And with parameter status 0
			And with parameter waittime 0
			And send to driver
		Then there should be no more scheduled events

	@busy
	Scenario: A Busy with no command sent is ignored
		Given the driver attribute motdoor:doorstate is OPEN
		When the device response with application_status busy
			And with parameter status 0
			And with parameter waittime 0
			And send to driver
		Then there should be no more scheduled events

	@busy
	Scenario: A command still refused after three tries raises an error
		Given the driver attribute motdoor:doorstate is OPEN
			And the driver variable commandTarget is CLOSED
			And the driver variable commandTime is now
			And the driver variable commandTries is 3
		When the device response with application_status busy
			And with parameter status 0
			And with parameter waittime 0
			And send to driver
		Then there should be no more scheduled events
			And the capability devadv:errors should be [ERR_COMMAND_REFUSED:Garage door controller was busy and did not move the door]

	@busy
	Scenario: A retry is not sent once the door got there
		Given the driver attribute motdoor:doorstate is CLOSED
			And the driver variable commandTarget is CLOSED
			And the driver variable commandTime is now
			And the driver variable commandTries is 2
		When event RetryDoorCommand triggers
		Then the driver should not send barrier_operator set

	@busy
	Scenario: A new command clears the refused error
		Given the driver attribute motdoor:doorstate is OPEN
			And the driver attribute devadv:errors is {'ERR_COMMAND_REFUSED':'Garage door controller was busy and did not move the door'}
			And the driver variable commandTries is 3
		When a base:SetAttributes command with the value of motdoor:doorstate CLOSED is placed on the platform bus
		Then the driver should send barrier_operator set
			And with parameter barrierstate 0
			And the capability devadv:errors should be [:]
			And the driver variable commandTries should be 1
			And the driver variable commandTarget should be "CLOSED"
