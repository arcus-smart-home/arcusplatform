@ZWave @Generic @gdc
Feature: Generic Z-Wave Garage Door Controller driver tests

    These scenarios test the functionality of the Generic Z-Wave Garage Door Controller driver.

    Background:
        Given the ZW_ZZWave_GarageDoorController_2_12.driver has been initialized

    Scenario: Driver reports capabilities to platform.
        When a base:GetAttributes command is placed on the platform bus
        Then the driver should place a base:GetAttributesResponse message on the platform bus
            And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'motdoor']
            And the message's dev:devtypehint attribute should be Garage Door
            And the message's devadv:drivername attribute should be ZZWaveGarageDoorController
            And the message's devadv:driverversion attribute should be 2.12
            And the message's devpow:source attribute should be LINE
            And the message's devpow:linecapable attribute should be true
            And the message's devpow:backupbatterycapable attribute should be false
            And the message's motdoor:doorstate attribute should be CLOSED
        Then both busses should be empty

    Scenario: Device is added
        When the device is added
        Then the capability devpow:sourcechanged should be recent
            And the capability motdoor:doorstatechanged should be recent
        Then the driver should send barrier_operator signal_supported_get
        
    Scenario: Device is connected
        When the device is connected
        Then the driver should send barrier_operator get
            And the driver should poll barrier_operator.get every 20 minutes
            And the driver should set timeout at 70 minutes
        Then the driver should place a base:ValueChange message on the platform bus
        Then nothing else should happen

    Scenario: Platform attempts to Open GDC via attribute change.
        Given the driver attribute motdoor:doorstate is CLOSED
        When a base:SetAttributes command with the value of motdoor:doorstate OPEN is placed on the platform bus
        Then the driver should send barrier_operator set
            And with parameter barrierstate -1
        Then the driver should place a EmptyMessage message on the platform bus
        Then both busses should be empty

    Scenario: Platform attempts to Close GDC via attribute change.
        Given the driver attribute motdoor:doorstate is OPEN
        When a base:SetAttributes command with the value of motdoor:doorstate CLOSED is placed on the platform bus
        Then the driver should send barrier_operator set
            And with parameter barrierstate 0
        Then the driver should place a EmptyMessage message on the platform bus
        Then both busses should be empty

    Scenario: Platform attempts to Open GDC via attribute change when obstructed.
        Given the driver attribute devadv:errors is {'ERR_OBSTRUCTION':'Garage door may be obstructed'}
        When a base:SetAttributes command with the value of motdoor:doorstate OPEN is placed on the platform bus
        Then the driver should place a EmptyMessage message on the platform bus
        Then both busses should be empty

    Scenario: Platform attempts to Close GDC via attribute change when obstructed.
        Given the driver attribute devadv:errors is {'ERR_OBSTRUCTION':'Garage door may be obstructed'}
        When a base:SetAttributes command with the value of motdoor:doorstate CLOSED is placed on the platform bus
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

        Examples:
          | before  | door        | event | battery | after       | sends      |
          |  10     | OBSTRUCTION |  0    |  100    | OBSTRUCTION | should     |
          | 100     | OPEN        | 68    |  100    | OBSTRUCTION | should     |
          | 100     | CLOSING     | 69    |  100    | OBSTRUCTION | should     |
          | 100     | CLOSED      | 73    |  100    | CLOSED      | should not |
          | 100     | OPEN        | 74    |   10    | OPEN        | should     |

    Scenario: Get Status on obstruction
        Given the driver variable obstructionCount is 1
        When the device responds with alarm report
            And with parameter notificationType 6
            And with parameter event 70
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability motdoor:doorstate should be OBSTRUCTION
        Then nothing else should happen

    Scenario: Clear Alarms
        Given the driver attribute devpow:battery is 10
            And the driver variable obstructionCount is 1
        When the device responds with alarm report
            And with parameter notificationType 6
            And with parameter event 0
            And send to driver
        Then the driver should place a base:ValueChange message on the platform bus
            And the capability devpow:battery should be 100

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


    Scenario Outline: Device reports the Signaling Capabilities Supported
        When the device responds with barrier_operator signal_supported_report
            And with parameter bitmask1 <type>
            And with parameter bitmask2 <state>
            And send to driver
        Then the driver variable SignalingSubsystemType should be <type_val>
            And the driver variable SignalingSubsystemState should be <state_val>

        Examples:
          | type | state | type_val | state_val |
          | 0x01 | 0xFF  | Audible  | ON        |
          | 0x01 | 0x00  | Audible  | OFF       |
          | 0x02 | 0xFF  | Visual   | ON        |
          | 0x02 | 0x00  | Visual   | OFF       |
            

#############################################################
# Commands refused with Application Status Busy
#############################################################

    @busy
    Scenario Outline: A close refused as busy is sent again after the wait time
        Given the driver attribute motdoor:doorstate is OPEN
        When a base:SetAttributes command with the value of motdoor:doorstate CLOSED is placed on the platform bus
        Then the driver should send barrier_operator set
            And with parameter barrierstate 0
            And the driver should cancel event RetryDoorCommand
            And the driver should schedule event ReadDoorState in 45 seconds
        When the device response with application_status busy
            And with parameter status <status>
            And with parameter waittime <wait>
            And send to driver
        Then the driver should schedule event RetryDoorCommand in <delay> seconds
        When event RetryDoorCommand triggers
        Then the driver should send barrier_operator set
            And with parameter barrierstate 0
            And the driver variable commandTries should be 2
            And the driver should schedule event ReadDoorState in 45 seconds

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

