@ZWave
Feature: ZWave Jasco In-Wall Toggle Driver Test

	These scenarios test the functionality of the ZWave Jasco In-Wall Toggle driver.
	
	Background:
		Given the ZW_Jasco_InWallToggle.driver has been initialized
	
	Scenario: Device connected
		When the device is connected
		#When the device connects to the platform
		Then the driver should place a base:ValueChange message on the platform bus 
