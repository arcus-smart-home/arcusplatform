@Zigbee
Feature: Test of the Orbit Hose Tap 8 Timer driver
	
	These scenarios test the functionality of the Orbit Hose Tap 8 Timer driver.

	Background:
	Given the ZB_Orbit_HoseTap8.driver has been initialized
	And the device has endpoint 1
@Ignore				
	Scenario: Driver reports capabilities to platform.
	When a base:GetAttributes command is placed on the platform bus
	Then the driver should place a base:GetAttributesResponse message on the platform bus
	And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devconn', 'devpow', 'ident', 'irrcont', 'devota']
	And the message's dev:devtypehint attribute should be Irrigation
	And the message's devadv:drivername attribute should be ZBOrbitHT8 
	And the message's devadv:driverversion attribute should be 1.0
	And the message's devpow:source attribute should be BATTERY
	And the message's devpow:linecapable attribute should be false		
	Then both busses should be empty
@Ignore	
	Scenario: Device Added to Platform
	When the device is added
	Then protocol message count is 1
@Ignore	
	Scenario: DeferredSetup event
	When event DeferredSetup trigger
	Then protocol message count is 6
@Ignore	
	Scenario: DeferredOtaRead
	Given the driver variable OTA_COUNTER is 0
	When event DeferredOtaRead trigger 
	Then the driver should send Ota 0x00
	And with payload 2, 0
@Ignore	
	Scenario: Configure Thermostat Report
	Given the driver variable CNFG_THERM_RPT is 0
	When event CnfgThermRpt trigger
	Then the driver should send 0x0201 0x06
	

	Scenario: Configure power report 
 	Given the driver variable CNFG_PWR_RPT is 0
	When event CnfgPwrRpt trigger 
	Then the driver should send power 0x06
@Ignore	
	Scenario: Configure poll control
	Given the driver variable CNFG_POLL_CTRL is 0
	When event CnfgPollCrtl trigger 
	Then the driver should send PollControl 0x02	
	
	Scenario Outline: setAttributes for irrcont
 	When the capability method base:SetAttributes 
 	And with capability irrcont:rainDelay is 5
 	And with capability irrcont:budget is 10
 	And send to driver
 	Then protocol message count is 2
 	And the driver should place a base:ValueChange message on the platform bus
@Ignore 	
 	Scenario: onIrrigationController.WaterNow
 	Given the capability irrcont:controllerState is NOT_WATERING
 	When the capability method irrcont:WaterNow
 	And with capability duration is 5
 	And with capability zonenum is 5 
 	And send to driver
 	Then the driver should send onoff 0x0003
 	And the driver should place a irrigationcontroller.WaterNowResponse message on the platform bus
@Ignore	
 	Scenario: onIrrigationController.Cancel
 	When the capability method irrcont:Cancel
 	And send to driver
 	Then the driver should send onoff 0x00
 	And the driver should place a irrigationcontroller:CancelResponse message on the platform bus

@Ignore
 	Scenario: DeferredWaterNowReadDuration
 	When event DeferredWaterNowReadDuration trigger
 	Then the driver should send onoff 0x00
 	And with payload 0x02, 0x00
 	
 	
	# Need to figure out multi-instance

@Ignore
 	Scenario Outline: DeferredReadDuration
 	Given the capability irr:wateringRemainingTime.z1 is <previous>
 	And the capability irrcont:controllerState is WATERING
 	When event DeferredReadDuration trigger 
 	Then the capability irr:zoneState.z1 should be <expected>	
 	And the capability irrcont:controllerState should be <expected>
 	And the capability irr:wateringRemainingTime.z1 should be <update>
 	And the driver should place a base:ValueChange message on the platform bus
 	
 	Examples:
 	| previous | expected     | update |
 	| 1		   | NOT_WATERING | 0      |
 	| 2        | WATERING     |	1      |
 	
 	# setAttributes('irr') 
 	Scenario: set irrigation zone attribute
 	When the capability method base:SetAttributes
 	And with capability irr:zonename.z1 is TEST
 	And with capability irr:zonecolor.z1 is YELLOW
 	And send to driver
	Then the driver should place a EmptyMessage message on the platform bus
 	Then the capability irr:zonename.z1 should be TEST
 	And the capability irr:zonecolor.z1 should be YELLOW
 	And the driver should place a base:ValueChange message on the platform bus
 	
 	# thermostat.zclreportattributes / zclreadattributesresponse
 	Scenario: Thermostat report with transisitions and budget
 	Given the capability irrcont:controllerState is NOT_WATERING
	When the device response with thermostat zclreportattributes
	And with parameter 0x0101 0x01
	And with parameter ATTR_NUMBER_OF_DAILY_TRANSITIONS 2
	And with parameter ATTR_NUMBER_OF_WEEKLY_TRANSITIONS 3
	And send to driver
	Then the capability irrcont:maxdailytransitions should be 2
	And the capability irrcont:maxtransitions should be 3
	And the capability irrcont:budget should be 1
	
	Scenario Outline: Thermostat report Rain Delay
	Given the capability irrcont:controllerState is <previousState>
	And the capability irrcont:rainDelay is 5
	And the capability irr:zoneState.z1 is <previousState> 
	When the device response with thermostat zclreadattributesresponse
	And with parameter 0x0024 <rainDelay>
	And send to driver
	Then the capability irrcont:controllerState should be <newState>
	And the capability irrcont:rainDelay should be <newRainDelay>
	
	Examples:
	| previousState | rainDelay | newState     | newRainDelay |
	| NOT_WATERING  | 4         | RAIN_DELAY   | 4            |
	| NOT_WATERING  | 0         | NOT_WATERING | 5            |
 	
 	Scenario Outline: Thermostat report Remaining Hold Time
	Given the capability irrcont:controllerState is <previousState>
	And the capability irrcont:rainDelay is 5
	And the capability irr:zoneState.z1 is <previousState> 
	When the device response with thermostat zclreportattributes
	And with parameter 0x0102 <remainingHoldTime>
	And send to driver
	Then the capability irrcont:controllerState should be <newState>
	And the capability irrcont:rainDelay should be <newRainDelay>
	
	Examples:
	| previousState | remainingHoldTime | newState     | newRainDelay |
	| NOT_WATERING  | 4                 | RAIN_DELAY   | 4            |
	| NOT_WATERING  | 0                 | NOT_WATERING | 0            |
	
 	# onoff.zclreadattributesresponse / zclreportattributes
 	
 	Scenario Outline: power read / report attribute
 	When the device response with power <messageType>
 	And with parameter ATTR_BATTERY_VOLTAGE 30
 	And send to driver
 	
 	Examples:
 	| messageType |
 	| zclreadattributesresponse |
 	| zclreportattributes |
 	
	# pollcontrol.zclreadattributesresponse / zclreportattributes
	Scenario Outline: process poll control response
	When the device response with pollcontrol <messageType>
	And send to driver
	
	Examples: 
	| messageType |
	| zclreadattributesresponse |
	| zclreportattributes | 
	
	# pollcontrol.checkin
@Ignore
	Scenario: poll control checkin
	When the device response with pollcontrol checkin
	And send to driver
	Then the driver should send pollcontrol checkInResponse
	
	# ota.zclreadattributesresponse
	Scenario: ota read
	When the device response with ota zclreadattributesresponse
	And send to driver
	
	# ota.querynextimagerequest
	Scenario: OTA completed
	Given the capability devota:targetVersion is 1
	When the device response with ota zclreadattributesresponse
	And with parameter ATTR_CURRENT_FILE_VERSION 1
	And send to driver 
	Then the capability devota:status should be COMPLETED 
	And the driver should place a base:ValueChange message on the platform bus

	# ota.imageblockrequest / ota.imagePageRequest
	Scenario Outline: OTA page request IN PROGRESS
	When the device response with ota <request>
	And with parameter fileVersion 1
	And with parameter fileOffset 1
	And with header flags 1
	And send to driver
	Then the capability devota:status should be INPROGRESS 	
	And the driver should place a base:ValueChange message on the platform bus	 	
	
	# ota.upgradeendrequest
	Scenario Outline: OTA upgrade end request
	When the device response with ota upgradeendrequest
	And with parameter status <status>
	And with parameter manufacturerCode 1
	And with parameter imageType 1
	And with parameter fileVersion 1
	And with header flags 1
	And send to driver
	Then the capability devota:status should be <otaStatus> 	
	And the driver should place a base:ValueChange message on the platform bus
		
	Examples:
	| status | otaStatus |
	| 1		 | FAILED    |
	| 0      | COMPLETED |
	
	# event chkFragmentRqstTimeout
	Scenario Outline: chkFragmentRqstTimeout 
	Given the driver variable lastOtaFragmentRqstTime is <time>
	And the capability devota:status is INPROGRESS
	When event chkFragmentRqstTimeout trigger 
	Then the capability devota:status should be <otaStatus> 	
	And the driver should place a base:ValueChange message on the platform bus
@Ignore		
	Examples:
	| time | otaStatus  |
	| 0	   | FAILED     |
	| NOW  | INPROGRESS |	
	
	# onZigbeeMessage general 
	
	# Identify
	Scenario: identify
	When the capability method ident:Identify
	And send to driver
	Then the driver should send identify identifyCmd	