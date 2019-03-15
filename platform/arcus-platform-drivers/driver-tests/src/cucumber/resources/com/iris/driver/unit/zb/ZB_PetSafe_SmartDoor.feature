@Zigbee
Feature: Test of the Waxman Smart Valve driver
	
	These scenarios test the functionality of the AlertMe binary sensor.

	Background:
	Given the ZB_PetSafe_SmartDoor.driver has been initialized
	And the device has endpoint 12 

 	Scenario: Device Added
 	When the device is added
 	Then protocol message count is 2
 	
	Scenario: Device Connected
	When the device is connected
	Then protocol message count is 4 	
	
	Scenario: Setup Power Configuration Report
	Given the driver variable CNFG_PWR_RPT is 0 
	When event CNFG_PWR_RPT trigger
	Then the driver should send 0x0001 0x06
	
	Scenario: Setup Basic Configuration Report
	When event CNFG_BASIC_RPT trigger
	Then the driver should send 0x0000 0x06	
	
	Scenario Outline: Process LOCKED, UNLOCKED  and AUTO lock state
	Given the capability petdoor:lockstate is <previous>
	When the capability method base:SetAttributes
	And with capability petdoor:lockstate is <value>
	And send to driver
	Then the capability petdoor:lockstate should be <value>
	And the driver should send 0x0101 0x02
	And the driver should send 0x0101 <lockCmd>
	
	Examples:
	| previous | value    | lockCmd |
	| AUTO     | LOCKED   | 0x00    | 
	| AUTO     | UNLOCKED | 0x01    |
	| LOCKED   | AUTO     | 0x00    |
	
	Scenario: Update pet name
	Given the capability pettoken:petName.pt2 is Fluffy
	When the capability method base:SetAttributes
	And with capability pettoken:petName.pt2 is Fang
	And send to driver
	Then the capability pettoken:petName.pt2 should be Fang
	
	Scenario: Identify
	When the capability method ident:Identify
	And send to driver
	Then the driver should send Identify IdentifyCmd
	
	Scenario: Power read attribute response
	When the device response with power zclreadattributesresponse
	And with parameter ATTR_BATTERY_VOLTAGE 45
	And with parameter ATTR_BATTERY_VOLTAGE_MIN_THRESHOLD 38
	And with parameter ATTR_BATTERY_ALARM_MASK 0
	And with parameter ATTR_BATTERY_RATED_VOLTAGE 60
	And send to driver
	Then the capability devpow:battery should be 73

	Scenario: Power report attribute response
	Given the driver variable battMinThreshold is 4
	And the driver variable battRatedVoltage is 6
	When the device response with power zclreportattributes
	And with parameter ATTR_BATTERY_VOLTAGE 54
	And send to driver
	Then the capability devpow:battery should be 89
	
	
	Scenario: Door lock read attribute
	When the device response with doorlock zclreadattributesresponse
	And with parameter ATTR_LOCK_STATE 1
	And with parameter ATTR_LOCK_TYPE 1
	And with parameter ATTR_ACTUATOR_ENABLED 1
	And with parameter ATTR_DOOR_STATE 1
	And with parameter ATTR_DOOR_OPEN_EVENTS 1
	And with parameter ATTR_DOOR_CLOSED_EVENTS 1
	And with parameter ATTR_OPEN_PERIOD 1
	And send to driver
	Then the capability petdoor:lockstate should be LOCKED
	Then the capability petdoor:lastlockstatechangedtime should be recent
	
	Scenario: Process new RFID token paired
	When the device response with doorlock programmingeventnotification
	And with parameter source 0x03
	And with parameter code 0x05
	And with parameter userId 99 
	And send to driver
	Then the capability pettoken:tokenId.pt1 should be 99
	
	Scenario: Process remove RFID token paired
	Given the capability pettoken:tokenId.pt2 is 99
	When the device response with doorlock programmingeventnotification
	And with parameter source 0x03
	And with parameter code 0x06
	And with parameter userId 99 
	And send to driver
	Then the capability pettoken:tokenId.pt2 should be -1
	
	Scenario Outline: Pet access
	Given the capability pettoken:tokenId.pt3 is 99
	When the device response with doorlock operationeventnotification
	And with parameter code 0x02
	And with parameter userId 99 
	And with parameter data <direction> 
	And send to driver
	Then the capability pettoken:lastAccessDirection.pt3 should be <expected>
	And the capability pettoken:lastAccessTime.pt3 should be recent
	And the capability petdoor:direction should be <expected>
	And the capability petdoor:lastaccesstime should be recent
	
	Examples:
	| direction | expected |
	| 1         | OUT      |
	| 2         | IN       |
	
	Scenario Outline: Pet Door Operartion Status
	When the device response with doorlock operationeventnotification
	And with parameter code <code>
	And send to driver
	Then the capability petdoor:lockstate should be <expected>
	And the capability petdoor:lastlockstatechangedtime should be recent
	
	Examples:
	| code | expected |
	| 0x0A | AUTO     |
	| 0x0D | LOCKED   |
	| 0x0E | UNLOCKED |
	
	Scenario: Platform Remove Token
	Given the capability pettoken:tokenId.pt5 is 99
	And the capability pettoken:paired.pt5 is true
	When the capability method petdoor:RemoveToken
	And with capability tokenId is 99
	And send to driver
	Then the capability pettoken:tokenId.pt5 should be -1
	And the capability pettoken:paired.pt5 should be false
	And the driver should place a PetDoor:TokenRemoved message on the platform bus
		
		
	