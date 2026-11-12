@ZWave @Ring @extender
Feature: ZWave Ring Alarm Range Extender Driver Test

These scenarios test the functionality of the ZWave Ring Alarm Range Extender driver

    Background:
    Given the ZW_Ring_RangeExtender.driver has been initialized

    @basic
    Scenario Outline: Driver matches every Ring range extender model
    Then the driver should match Z-Wave device 0x0346 0x0401 <productId>

        Examples:
          | productId | remarks |
          | 0x0101    | 1st gen |
          | 0x0201    | not in Z-Wave JS |
          | 0x0301    | 2nd gen |
          | 0x0401    | 2nd gen |
