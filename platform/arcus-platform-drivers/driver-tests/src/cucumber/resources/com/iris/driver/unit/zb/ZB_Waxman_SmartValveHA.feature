@Zigbee
Feature: Test of the CentraLite Contact Sensor ZigBee driver
	
	These scenarios test the functionality of the CentraLite Contact Sensor ZigBee driver.

	Background:
	Given the ZB_Waxman_SmartValveHA.driver has been initialized
	And the device has endpoint 1
				
	# onZigbeeMessage(Zigbee.TYPE_ZCL)
	Scenario: Zone request
	When the device response with 0x0B02 0x00
	And send to driver
