@ZWave @Jasco
Feature: Unit Tests for the ZWJascoOutdoor40AmpSwitchDriver

	These scenarios test the meter report handling of the GE/Jasco Outdoor Smart Switch (40 Amp) driver.

	Background:
		Given the ZW_Jasco_OutdoorSmartSwitch40Amp.driver has been initialized
			And the capability swit:state is ON

	@energy
	Scenario Outline: Device reports cumulative energy and instantaneous power
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

	# Scale bit 2 is bit 7 of the meter type byte (0xA1 = electric meter, import, scale bit 2 set)
	@energy
	Scenario Outline: Device ignores voltage, current and power factor reports
		Given the capability pow:cumulative is 29000.0
			And the capability pow:instantaneous is 2450.0
		When the device response with meter report
			And with parameter MeterType <MeterType>
			And with parameter Scale <Scale>
			And with parameter Value1 0
			And with parameter Value2 0
			And with parameter Value3 <Value3>
			And with parameter Value4 <Value4>
			And send to driver
		Then the platform attribute pow:cumulative should be 29000.0
			And the platform attribute pow:instantaneous should be 2450.0

		Examples:
		  | reported | MeterType | Scale | Value3 | Value4 |
		  | 245.3 V  | 0xA1      | 0x24  | 0x09   | 0x95   |
		  | 3.2 A    | 0xA1      | 0x2C  | 0x00   | 0x20   |
		  | 0.95 PF  | 0xA1      | 0x54  | 0x00   | 0x5F   |
		  | 2-byte W | 0x21      | 0x32  | 0x00   | 0x3C   |
