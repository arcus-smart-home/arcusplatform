@ZWave @Ring @contact
Feature: ZWave Ring Alarm Contact Sensor Driver Test

These scenarios test the functionality of the ZWave Ring Alarm Contact Sensor driver

    Background:
    Given the ZW_Ring_ContactSensor.driver has been initialized

    @basic
    Scenario Outline: Driver matches every Ring contact sensor model
    Then the driver should match Z-Wave device 0x0346 0x0201 <productId>

        Examples:
          | productId | remarks           |
          | 0x0201    | 1st gen           |
          | 0x0301    | 2nd gen           |
          | 0x0401    | 2nd gen           |
          | 0x0601    | 2nd gen           |
