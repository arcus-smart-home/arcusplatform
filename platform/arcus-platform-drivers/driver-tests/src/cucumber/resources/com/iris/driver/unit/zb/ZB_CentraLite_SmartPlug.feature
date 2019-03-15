@Zigbee @zebrashark
Feature: Test of the CentraLite Smart Plug ZigBee driver
	
	These scenarios test the functionality of the CentraLite Smart Plug ZigBee driver.

	Background:
	Given the ZB_CentraLite_SmartPlug.driver has been initialized
	And the device has endpoint 1

	Scenario: Driver reports capabilities to platform.
	When a base:GetAttributes command is placed on the platform bus
	Then the driver should place a base:GetAttributesResponse message on the platform bus
	And the message's base:caps attribute list should be ['base', 'centralitesmartplug', 'dev', 'devadv', 'devconn', 'devpow', 'swit', 'pow', 'devota', 'ident']
	And the message's dev:devtypehint attribute should be Switch
	And the message's devadv:drivername attribute should be ZBCentraLiteSmartPlug 
	And the message's devadv:driverversion attribute should be 1.0
	And the message's devpow:source attribute should be LINE
	And the message's devpow:linecapable attribute should be true		
	And the message's devpow:backupbatterycapable attribute should be false		
	And the message's pow:wholehome attribute should be false
	Then nothing else should happen
	
	Scenario: Device added
	When the device is added
	Then the capability devpow:sourcechanged should be recent
		And the capability swit:statechanged should be recent
	Then protocol message count is 1
#  TODO Parse that it's a Bind Request
	And the platform bus should be empty	

	Scenario: Device connected fresh
	When the device is connected
	Then the driver should send onOff zclReadAttributes
	Then the driver should send 0x0B05 zclReadAttributes
	Then the driver should set timeout at 10 minutes
	Then the driver should defer event CnfgOnOffRpt
		And the driver should defer event CnfgElecMeasRpt
		And the driver should defer event PairZwave
		#from OTA Include
		And the driver should schedule event DeviceOtaDeferredRead in 120 seconds
  Then the driver should place a base:ValueChange message on the platform bus
		And nothing else should happen
	
	Scenario: Device reconnects
	Given the capability centralitesmartplug:homeid is DB0123EF
		And  the capability centralitesmartplug:nodeid is 12
	When the device is connected
	Then the driver should send onOff zclReadAttributes
	Then the driver should send 0x0B05 zclReadAttributes
	Then the driver should set timeout at 10 minutes
	Then the driver should defer event CnfgOnOffRpt
		And the driver should defer event CnfgElecMeasRpt
		#from OTA Include
		And the driver should schedule event DeviceOtaDeferredRead in 120 seconds
  Then the driver should place a base:ValueChange message on the platform bus
		And nothing else should happen

	Scenario: Device reconnects after reset
	Given the capability centralitesmartplug:homeid is 00000000
		And  the capability centralitesmartplug:nodeid is 000
	When the device is connected
	Then the driver should send onOff zclReadAttributes
	Then the driver should send 0x0B05 zclReadAttributes
	Then the driver should set timeout at 10 minutes
	Then the driver should defer event CnfgOnOffRpt
		And the driver should defer event CnfgElecMeasRpt
		And the driver should defer event PairZwave
		#from OTA Include
		And the driver should schedule event DeviceOtaDeferredRead in 120 seconds
  Then the driver should place a base:ValueChange message on the platform bus
		And nothing else should happen

	Scenario: Event DeferredOtaRead
	Given the driver variable OTA_COUNTER is 0 
	When event DeviceOtaDeferredRead trigger
	Then the driver should send 0x0019 0x00
	
	Scenario: Event PairZwaveRead
	When event PairZwaveRead triggers
	Then the driver should send 0xFC03 zclReadAttributes
		And nothing else should happen
		
	Scenario: Send PairZwaveStop
	When event PairZwaveStop triggers
	Then the driver should send 0xFC03 0x00
		And with payload 0x00
		And nothing else should happen
		
	#CNFG_ONOFF_RPT
	Scenario: schedule event CnfgOnOffRpt
	Given the driver variable CNFG_ONOFF_RPT is 0
	When event CnfgOnOffRpt trigger
	Then the driver should send 0x0006 0x06
	
	#CNFG_ELEC_RPT
	Scenario: Event CNFG_ELEC_RPT
	Given the driver variable CNFG_ELEC_RPT is 0
	When event CnfgElecMeasRpt trigger
	Then the driver should send 0x0B04 0x06
	
	#CNFG_DIAG_RPT
	Scenario: Event CnfgDiagRpt
	Given the driver variable CNFG_DIAG_RPT is 0
	When event CnfgDiagRpt trigger
	Then the driver should send 0x0B05 0x06
	
	#setAttributes('swit')
	Scenario Outline: Set on/off via base:setAttributes
	When the capability method base:SetAttributes
	And with capability swit:state is <value>
	And send to driver
	Then the driver should send onoff <action>
	And the driver should send onoff zclReadAttributes
	
	Examples:
	| value | action |
	| ON    | on     |
	| OFF   | off    |
	
	#onoff.zclreadattributesresponse / zclreportattributes	
	Scenario Outline: on/off read attribute
	Given the capability swit:state is <before>
	When the device response with onoff <message>
	And with parameter ATTR_ONOFF <onoff>
	And send to driver
	Then the capability swit:state should be <state>
	And the capability swit:statechanged should be recent
	 
	Examples:
	| message                   | before| onoff | state |
	| zclreadattributesresponse | ON    | 0     | OFF   |
	| zclreportattributes       | OFF   | 1     | ON    |
	
	#diagnostics.zclreadattributesresponse / zclreportattributes
	Scenario Outline: diagnostics read / report response
	When the device response with diagnostics <messageType>
	And with parameter ATTR_LAST_MESSAGE_RSSI <rssi>
	And with parameter ATTR_LAST_MESSAGE_LQI <lqi>
	And send to driver
	Then the capability devconn:signal should be <signal> 
	
	Examples:
	| messageType               | rssi | lqi      | signal |
	| zclreadattributesresponse | 10   | 10       | 4      |
	| zclreportattributes       | 10   | INVALID  | 100    |
		
	#ota.zclreadattributesresponse
	Scenario: OTA read response
	Given the capability devota:targetVersion is 1
	When the device response with ota zclreadattributesresponse
	And with parameter ATTR_CURRENT_FILE_VERSION 1
	And send to driver
	Then the driver should place a base:ValueChange message on the platform bus
	And the capability devota:currentVersion should be 1
	And the capability devota:status should be COMPLETED	
	
	#ota.querynextimagerequest
	Scenario: OTA query next image
	Given the capability devota:targetVersion is 1
	When the device response with ota querynextimagerequest
	And with parameter manufacturerCode 1
	And with parameter imageType 1
	And with parameter fileVersion 1
	And with header flags 1
	And send to driver
	Then the driver should place a base:ValueChange message on the platform bus
	And the capability devota:currentVersion should be 1
	And the capability devota:status should be COMPLETED
	
	#ota.imageblockreques / ota.imagePageRequest
	Scenario Outline: OTA image block / page
	Given the capability devota:status is IDLE
	When the device response with ota <messageType>
	And with parameter fileVersion 1
	And with parameter fileOffset 0
	And with header flags 1
	And send to driver 
	Then the driver should place a base:ValueChange message on the platform bus
	And the capability devota:targetVersion should be 1
	And the capability devota:status should be INPROGRESS
	
	Examples:
	| messageType       |
	| imageblockrequest |
	| imagePageRequest  |
		
	#ota.upgradeendrequest
	Scenario Outline: OTA upgrade end request
	When the device response with ota upgradeendrequest
	And with parameter status <status>
	And with parameter manufacturerCode 0
	And with parameter imageType 0
	And with parameter fileVersion 0
	And with header flags 1
	And send to driver 
	Then the driver should place a base:ValueChange message on the platform bus
	And the capability devota:status should be <result>

	Examples:
	| status | result    |
	| 0      | COMPLETED |
	| -1     | FAILED    |
		
	#chkFragmentRqstTimeout
	Scenario: Event chkFragmentRqstTimeout
	Given the capability devota:status is INPROGRESS
	And the driver variable lastOtaFragmentRqstTime is NOW
	When event chkFragmentRqstTimeout trigger
		
	#onIdentify.Identify
	Scenario: identify
	When the capability method ident:Identify
	And send to driver
	Then the driver should send identify identifyCmd	
		
	#onZigbeeMessage(Zigbee.TYPE_ZCL)
	Scenario Outline: default ZigbeeMessage processing
	When the device response with <cluster> 0x07
	And with payload <payload>
	And send to driver
	
	Examples:
	| cluster | payload |
	| 0x0006  | 0x01    |
	| 0x0B04  | 0x01    |
	| 0x0B05  | 0x01    |
	| 0x0006  | 0x00    |
	| 0x0B04  | 0x00    |
	| 0x0B05  | 0x00    |
	
	Scenario Outline: default ZigbeeMessage processing for diagnostics
	When the device response with 0x0B05 0x00
	And with payload 0x1C, 0x01, 0x20, <lqi>
	And send to driver
	Then the capability devconn:signal should be <expected>
	
	Examples:
	| lqi | expected |
	| -1  | 100      |
	| 127 | 50       |
	| 0   | 0        |

	
	Scenario Outline: default ZigbeeMessage processing for electricity measurement
	Given the driver variable ZWaveNodeId is 0
	When the device response with 0x0B04 0x00
	And with payload 0x0B, 0x05, 0x00, <payload>
	And send to driver

	Examples:
	| payload |
	| 0xFF, 0xFF |
	| 0x01, 0xFF |
	| 0xFF, 0x01 |	
	| 0x01, 0x01 |	

	Scenario: default ZigbeeMessage processing for zwave attributes
	When the device response with 0xFC03 0x01
	And with payload 0, 0, 0, 35, -36, 1, 10, 31, 1, 0, 0, 32, 28
	And send to driver
	Then the driver should place a base:ValueChange message on the platform bus
		And the capability centralitesmartplug:homeid should be DC010A1F
		And the capability centralitesmartplug:nodeid should be 28
	Then nothing else should happen

	Scenario: ZigbeeMessage processing for unpaired device
	Given the capability centralitesmartplug:homeid is DC010A1F
		And the capability centralitesmartplug:nodeid is 28
	When the device response with 0xFC03 0x01
	And with payload 0, 0, 0, 35, -34, 21, 12, 04, 1, 0, 0, 32, 00
	And send to driver
	Then the driver should place a base:ValueChange message on the platform bus
	Then nothing else should happen
