@Zigbee
Feature: Zigbee AlertMe Care Pendant Driver Test

	These scenarios test the functionality of the Zigbee AlertMe Care Pendant driver.
	
	Background:
	Given the ZB_AlertMe_RangeExtender.driver has been initialized
	And the device has endpoint 2
		


	Scenario Outline: ITWO-4455, ITWO-4447 Investigation
	When the device response with 0x00C0 0x0A
	And with payload <DATA> 
	And send to driver
	
	Examples:
	| DATA                          |
	| 0x24,0x00,0x41,0x01,0x13,0x01 |
	
#	| 0x24,0x00,0x41,0x01,0x13,0x01 | OK
#	| 0x24,0x00,0x03,0x01,0x13,0x01 | unknown zigbee data type: 3
#	| 0x24,0x00,0x11,0x01,0x13,0x01 | unknown zigbee data type: 17
#	| 0x24,0x00,0x48,0x01,0x00,0x01 | OK
#   | 0x24,0x00,0x48,0x01,0x00 |	IllegalArgumentException