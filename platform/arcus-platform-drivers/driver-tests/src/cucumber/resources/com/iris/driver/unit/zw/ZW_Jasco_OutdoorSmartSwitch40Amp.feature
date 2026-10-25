@ZWave @Jasco
Feature: Unit Tests for the ZWJascoOutdoor40AmpSwitchDriver

   These scenarios test the meter report handling of the GE/Jasco Outdoor Smart Switch (40 Amp) driver.
   GenericZWaveMeter.feature covers the decoding shared with the other meter drivers.

   Background:
      Given the ZW_Jasco_OutdoorSmartSwitch40Amp.driver has been initialized

   @energy
   Scenario Outline: Device reports cumulative energy, and power while the switch is on
      Given the capability swit:state is ON
      When the device response with meter report
         And with parameter MeterType <MeterType>
         And with parameter Scale <Scale>
         And with parameter Value1 0
         And with parameter Value2 <Value2>
         And with parameter Value3 <Value3>
         And with parameter Value4 <Value4>
         And send to driver
      Then the platform attribute <attr> should change to <reading>
         And the driver should place a base:ValueChange message on the platform bus

      Examples:
         | reported    | MeterType | Scale | Value2 | Value3 | Value4 | attr              | reading |
         | 29.000 kWh  | 0x21      | 0x64  | 0x00   | 0x71   | 0x48   | pow:cumulative    | 29000.0 |
         | 30.0000 kWh | 0x21      | 0x84  | 0x04   | 0x93   | 0xE0   | pow:cumulative    | 30000.0 |
         | 2450.0 W    | 0x21      | 0x34  | 0x00   | 0x5F   | 0xB4   | pow:instantaneous | 2450.0  |

   @energy
   Scenario: Device ignores power while the switch is off
      Given the capability swit:state is OFF
         And the capability pow:instantaneous is 0.0
      When the device response with meter report
         And with parameter MeterType 0x21
         And with parameter Scale 0x34
         And with parameter Value1 0
         And with parameter Value2 0
         And with parameter Value3 0x5F
         And with parameter Value4 0xB4
         And send to driver
      Then the platform attribute pow:instantaneous should be 0.0
