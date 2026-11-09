@ZWave @Aeon @leak
Feature: ZWave Aeon Water Sensor Driver Test

These scenarios test how the ZWave Aeon Water Sensor driver turns the sensor's reports into a leak state.
0x00 and 0xFF are taken by the driver's hub reflexes; any other non-zero value reaches its handlers.

    Background:
    Given the ZW_Aeon_WaterSensor.driver has been initialized

    @leak
    Scenario Outline: Device reports a state change via a 'basic' report
        Given the capability leakh2o:state is <prev_state>
        When the device response with basic report
            And with parameter value <value>
            And send to driver
        Then the platform attribute leakh2o:state should change to <new_state>
            And the capability leakh2o:statechanged should be recent

        Examples:
          | prev_state | value | new_state |
          | SAFE       |  255  | LEAK      |
          | LEAK       |    0  | SAFE      |
          | SAFE       |   99  | LEAK      |

    @leak
    Scenario Outline: Device reports a state change via a 'sensor_binary' report
        Given the capability leakh2o:state is <prev_state>
        When the device response with sensor_binary_v2 report
            And with parameter value <value>
            And send to driver
        Then the platform attribute leakh2o:state should change to <new_state>
            And the capability leakh2o:statechanged should be recent

        Examples:
          | prev_state | value | new_state |
          | SAFE       |  255  | LEAK      |
          | LEAK       |    0  | SAFE      |
          | SAFE       |   99  | LEAK      |
