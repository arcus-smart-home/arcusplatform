@ZWave @Jasco 
Feature: ZWave Jasco In-Wall Dimmer Toggle Driver Test

	These scenarios test the functionality of the Jasco In-Wall Dimmer Toggle driver.
	
	Background:
		Given the ZW_Jasco_InWallDimmerToggle.driver has been initialized
	
	Scenario: Device connected
		When the device is connected
		#When the device connects to the platform
		Then the driver should place a base:ValueChange message on the platform bus 
