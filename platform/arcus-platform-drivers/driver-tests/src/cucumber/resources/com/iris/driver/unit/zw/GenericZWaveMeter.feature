@ZWave @meter
Feature: Z-Wave Meter Report decoding in GenericZWaveMeter

   These scenarios run Meter Reports through the drivers that decode them with zwave/GenericZWaveMeter.capability.
   A Meter Report's scale is three bits: bit 2 is bit 7 of the MeterType byte, and bits 0-1 are bits 3-4 of the
   Scale byte, which also holds the precision (bits 5-7) and the value size (bits 0-2). MeterType 0x21 is an
   electric meter reporting consumed (imported) energy, and 0xA1 is the same with scale bit 2 set.

   Scenario Outline: <driver> stores consumed kWh as Wh
      Given the <driver> has been initialized
      When the device response with meter report
         And with parameter MeterType 0x21
         And with parameter Scale 0x64
         And with parameter Value1 0x00
         And with parameter Value2 0x00
         And with parameter Value3 0x71
         And with parameter Value4 0x48
         And send to driver
      Then the platform attribute pow:cumulative should change to 29000.0

      Examples:
         | driver                                  |
         | ZW_Jasco_OutdoorSmartSwitch40Amp.driver |
         | ZW_Aeon_SmartEnergySwitch.driver        |
         | ZW_Aeon_SmartSwitch6.driver             |
         | ZW_Aeon_HomeEnergyMonitorV2.driver      |
         | ZW_Aeon_HomeEnergyMonitor_Gen5.driver   |
         | ZW_Zooz_SmartPlug_2_4.driver            |
         | ZW_Zooz_MiniPlug_2_4.driver             |
         | ZW_Dome_MiniPlug_2_4.driver             |

   # The Jasco 40A driver only stores power while the switch is on, see ZW_Jasco_OutdoorSmartSwitch40Amp.feature
   Scenario Outline: <driver> stores W
      Given the <driver> has been initialized
      When the device response with meter report
         And with parameter MeterType 0x21
         And with parameter Scale 0x34
         And with parameter Value1 0x00
         And with parameter Value2 0x00
         And with parameter Value3 0x5F
         And with parameter Value4 0xB4
         And send to driver
      Then the platform attribute pow:instantaneous should change to 2450.0

      Examples:
         | driver                                |
         | ZW_Aeon_SmartEnergySwitch.driver      |
         | ZW_Aeon_SmartSwitch6.driver           |
         | ZW_Aeon_HomeEnergyMonitorV2.driver    |
         | ZW_Aeon_HomeEnergyMonitor_Gen5.driver |
         | ZW_Zooz_SmartPlug_2_4.driver          |
         | ZW_Zooz_MiniPlug_2_4.driver           |
         | ZW_Dome_MiniPlug_2_4.driver           |

   Scenario Outline: <driver> ignores a voltage report
      Given the <driver> has been initialized
         And the capability pow:cumulative is 29000.0
      When the device response with meter report
         And with parameter MeterType 0xA1
         And with parameter Scale 0x24
         And with parameter Value1 0x00
         And with parameter Value2 0x00
         And with parameter Value3 0x09
         And with parameter Value4 0x95
         And send to driver
      Then the platform attribute pow:cumulative should be 29000.0

      Examples:
         | driver                                  |
         | ZW_Jasco_OutdoorSmartSwitch40Amp.driver |
         | ZW_Aeon_SmartEnergySwitch.driver        |
         | ZW_Aeon_SmartSwitch6.driver             |
         | ZW_Aeon_HomeEnergyMonitorV2.driver      |
         | ZW_Aeon_HomeEnergyMonitor_Gen5.driver   |
         | ZW_Zooz_SmartPlug_2_4.driver            |
         | ZW_Zooz_MiniPlug_2_4.driver             |
         | ZW_Dome_MiniPlug_2_4.driver             |

   Scenario Outline: <driver> ignores values that are not 4 bytes long
      Given the <driver> has been initialized
         And the capability pow:instantaneous is 5.0
      When the device response with meter report
         And with parameter MeterType 0x21
         And with parameter Scale 0x32
         And with parameter Value1 0x5F
         And with parameter Value2 0xB4
         And with parameter Value3 0x00
         And with parameter Value4 0x3C
         And send to driver
      Then the platform attribute pow:instantaneous should be 5.0

      Examples:
         | driver                       |
         | ZW_Zooz_SmartPlug_2_4.driver |
         | ZW_Zooz_MiniPlug_2_4.driver  |
         | ZW_Dome_MiniPlug_2_4.driver  |

   # Value3 and Value4 after a 1 or 2 byte value stand in for the delta time that follows it
   Scenario Outline: Decodes <reported>
      Given the ZW_Aeon_SmartEnergySwitch.driver has been initialized
      When the device response with meter report
         And with parameter MeterType <MeterType>
         And with parameter Scale <Scale>
         And with parameter Value1 <Value1>
         And with parameter Value2 <Value2>
         And with parameter Value3 <Value3>
         And with parameter Value4 <Value4>
         And send to driver
      Then the platform attribute <attribute> should change to <reading>

      Examples:
         | reported                 | MeterType | Scale | Value1 | Value2 | Value3 | Value4 | attribute         | reading |
         | 29.000 kWh               | 0x21      | 0x64  | 0x00   | 0x00   | 0x71   | 0x48   | pow:cumulative    | 29000.0 |
         | 30.0000 kWh              | 0x21      | 0x84  | 0x00   | 0x04   | 0x93   | 0xE0   | pow:cumulative    | 30000.0 |
         | 29.000 kWh from Meter v1 | 0x01      | 0x64  | 0x00   | 0x00   | 0x71   | 0x48   | pow:cumulative    | 29000.0 |
         | 2450.0 W                 | 0x21      | 0x34  | 0x00   | 0x00   | 0x5F   | 0xB4   | pow:instantaneous | 2450.0  |
         | 2450.0 W in 2 bytes      | 0x21      | 0x32  | 0x5F   | 0xB4   | 0x00   | 0x3C   | pow:instantaneous | 2450.0  |
         | 100 W in 1 byte          | 0x21      | 0x11  | 0x64   | 0x00   | 0x3C   | 0x00   | pow:instantaneous | 100.0   |
         | -200 W in 2 bytes        | 0x21      | 0x12  | 0xFF   | 0x38   | 0x00   | 0x3C   | pow:instantaneous | -200.0  |
         | 37.7 W marked produced   | 0x41      | 0x34  | 0x00   | 0x00   | 0x01   | 0x79   | pow:instantaneous | 37.7    |

   Scenario Outline: Ignores <reported>
      Given the ZW_Aeon_SmartEnergySwitch.driver has been initialized
         And the capability pow:cumulative is 1000.0
         And the capability pow:instantaneous is 5.0
      When the device response with meter report
         And with parameter MeterType <MeterType>
         And with parameter Scale <Scale>
         And with parameter Value1 0x00
         And with parameter Value2 0x00
         And with parameter Value3 <Value3>
         And with parameter Value4 <Value4>
         And send to driver
      Then the platform attribute pow:cumulative should be 1000.0
         And the platform attribute pow:instantaneous should be 5.0

      Examples:
         | reported            | MeterType | Scale | Value3 | Value4 |
         | 245.3 V             | 0xA1      | 0x24  | 0x09   | 0x95   |
         | 3.2 A               | 0xA1      | 0x2C  | 0x00   | 0x20   |
         | 0.95 power factor   | 0xA1      | 0x54  | 0x00   | 0x5F   |
         | 29.0 kVAh           | 0x21      | 0x2C  | 0x01   | 0x22   |
         | 29.000 kWh exported | 0x41      | 0x64  | 0x71   | 0x48   |
         | 29.000 m3 of gas    | 0x22      | 0x64  | 0x71   | 0x48   |
         | a 3 byte value      | 0x21      | 0x63  | 0x71   | 0x48   |
