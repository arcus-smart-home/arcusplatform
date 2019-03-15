@ZWave
Feature: ZWave Schlage Keypad Deadbolt Driver Test

	These scenarios test the functionality of the ZWave Schlage Keypad Deadbolt driver.
	
	Background:
		Given the ZW_Schlage_Keypad_Deadbolt.driver has been initialized
	
	Scenario: Device connected
		When the device connects to the platform
		Then the driver should place a base:ValueChange message on the platform bus 
