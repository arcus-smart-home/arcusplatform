/*
 * Copyright 2019 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iris.protocol.zwave.constants;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

public class ZWaveCommandClassName {
	/*************** command class identifiers ****************/
	public final static byte ALARM                             =   (byte) 0x71;
	public final static byte ALARM_V2                          =   (byte) 0x71;
	public final static byte NOTIFICATION_V3                   =   (byte) 0x71;
	public final static byte NOTIFICATION_V4                   =   (byte) 0x71;
	public final static byte APPLICATION_STATUS                =   (byte) 0x22;
	public final static byte ASSOCIATION_COMMAND_CONFIGURATION =   (byte) 0x9B;
	public final static byte ASSOCIATION                       =   (byte) 0x85;
	public final static byte ASSOCIATION_V2                    =   (byte) 0x85;
	public final static byte AV_CONTENT_DIRECTORY_MD           =   (byte) 0x95;
	public final static byte AV_CONTENT_SEARCH_MD              =   (byte) 0x97;
	public final static byte AV_RENDERER_STATUS                =   (byte) 0x96;
	public final static byte AV_TAGGING_MD                     =   (byte) 0x99;
	public final static byte BASIC_TARIFF_INFO                 =   (byte) 0x36;
	public final static byte BASIC_WINDOW_COVERING             =   (byte) 0x50;
	public final static byte BASIC                             =   (byte) 0x20;
	public final static byte BATTERY                           =   (byte) 0x80;
	public final static byte CHIMNEY_FAN                       =   (byte) 0x2A;
	public final static byte CLIMATE_CONTROL_SCHEDULE          =   (byte) 0x46;
	public final static byte CLOCK                             =   (byte) 0x81;
	public final static byte CONFIGURATION                     =   (byte) 0x70;
	public final static byte CONFIGURATION_V2                  =   (byte) 0x70;
	public final static byte CONTROLLER_REPLICATION            =   (byte) 0x21;
	public final static byte CRC_16_ENCAP                      =   (byte) 0x56;
	public final static byte DCP_CONFIG                        =   (byte) 0x3A;
	public final static byte DCP_MONITOR                       =   (byte) 0x3B;
	public final static byte DOOR_LOCK_LOGGING                 =   (byte) 0x4C;
	public final static byte DOOR_LOCK                         =   (byte) 0x62;
	public final static byte DOOR_LOCK_V2                      =   (byte) 0x62;
	public final static byte ENERGY_PRODUCTION                 =   (byte) 0x90;
	public final static byte FIRMWARE_UPDATE_MD                =   (byte) 0x7A;
	public final static byte FIRMWARE_UPDATE_MD_V2             =   (byte) 0x7A;
	public final static byte FIRMWARE_UPDATE_MD_V3             =   (byte) 0x7A;
	public final static byte GEOGRAPHIC_LOCATION               =   (byte) 0x8C;
	public final static byte GROUPING_NAME                     =   (byte) 0x7B;
	public final static byte HAIL                              =   (byte) 0x82;
	public final static byte HRV_CONTROL                       =   (byte) 0x39;
	public final static byte HRV_STATUS                        =   (byte) 0x37;
	public final static byte INDICATOR                         =   (byte) 0x87;
	public final static byte IP_CONFIGURATION                  =   (byte) 0x9A;
	public final static byte LANGUAGE                          =   (byte) 0x89;
	public final static byte LOCK                              =   (byte) 0x76;
	public final static byte MANUFACTURER_PROPRIETARY          =   (byte) 0x91;
	public final static byte MANUFACTURER_SPECIFIC             =   (byte) 0x72;
	public final static byte MANUFACTURER_SPECIFIC_V2          =   (byte) 0x72;
	public final static byte MARK                              =   (byte) 0xEF;
	public final static byte METER_PULSE                       =   (byte) 0x35;
	public final static byte METER_TBL_CONFIG                  =   (byte) 0x3C;
	public final static byte METER_TBL_MONITOR                 =   (byte) 0x3D;
	public final static byte METER_TBL_MONITOR_V2              =   (byte) 0x3D;
	public final static byte METER_TBL_PUSH                    =   (byte) 0x3E;
	public final static byte METER                             =   (byte) 0x32;
	public final static byte METER_V2                          =   (byte) 0x32;
	public final static byte METER_V3                          =   (byte) 0x32;
	public final static byte METER_V4                          =   (byte) 0x32;
	public final static byte MTP_WINDOW_COVERING               =   (byte) 0x51;
	public final static byte MULTI_CHANNEL_ASSOCIATION_V2      =   (byte) 0x8E;
	public final static byte MULTI_CHANNEL_V2                  =   (byte) 0x60;
	public final static byte MULTI_CHANNEL_V3                  =   (byte) 0x60;
	public final static byte MULTI_CMD                         =   (byte) 0x8F;
	public final static byte MULTI_INSTANCE_ASSOCIATION        =   (byte) 0x8E; /*Discontinued*/
	public final static byte MULTI_INSTANCE                    =   (byte) 0x60; /*Discontinued*/
	public final static byte NETWORK_MANAGEMENT_PROXY          =   (byte) 0x52;
	public final static byte NETWORK_MANAGEMENT_BASIC          =   (byte) 0x4D;
	public final static byte NETWORK_MANAGEMENT_INCLUSION      =   (byte) 0x34;
	public final static byte NO_OPERATION                      =   (byte) 0x00;
	public final static byte NODE_NAMING                       =   (byte) 0x77;
	public final static byte NON_INTEROPERABLE                 =   (byte) 0xF0;
	public final static byte POWERLEVEL                        =   (byte) 0x73;
	public final static byte PREPAYMENT_ENCAPSULATION          =   (byte) 0x41;
	public final static byte PREPAYMENT                        =   (byte) 0x3F;
	public final static byte PROPRIETARY                       =   (byte) 0x88;
	public final static byte PROTECTION                        =   (byte) 0x75;
	public final static byte PROTECTION_V2                     =   (byte) 0x75;
	public final static byte RATE_TBL_CONFIG                   =   (byte) 0x48;
	public final static byte RATE_TBL_MONITOR                  =   (byte) 0x49;
	public final static byte REMOTE_ASSOCIATION_ACTIVATE       =   (byte) 0x7C;
	public final static byte REMOTE_ASSOCIATION                =   (byte) 0x7D;
	public final static byte SCENE_ACTIVATION                  =   (byte) 0x2B;
	public final static byte SCENE_ACTUATOR_CONF               =   (byte) 0x2C;
	public final static byte SCENE_CONTROLLER_CONF             =   (byte) 0x2D;
	public final static byte SCHEDULE_ENTRY_LOCK               =   (byte) 0x4E;
	public final static byte SCHEDULE_ENTRY_LOCK_V2            =   (byte) 0x4E;
	public final static byte SCHEDULE_ENTRY_LOCK_V3            =   (byte) 0x4E;
	public final static byte SCREEN_ATTRIBUTES                 =   (byte) 0x93;
	public final static byte SCREEN_ATTRIBUTES_V2              =   (byte) 0x93;
	public final static byte SCREEN_MD                         =   (byte) 0x92;
	public final static byte SCREEN_MD_V2                      =   (byte) 0x92;
	public final static byte SECURITY_PANEL_MODE               =   (byte) 0x24;
	public final static byte SECURITY_PANEL_ZONE_SENSOR        =   (byte) 0x2F;
	public final static byte SECURITY_PANEL_ZONE               =   (byte) 0x2E;
	public final static byte SECURITY                          =   (byte) 0x98;
	public final static byte SENSOR_ALARM                      =   (byte) 0x9C;/*SDS10963-4 The Sensor Alarm command class can be used to realize Sensor Alarms.*/
	public final static byte SENSOR_BINARY                     =   (byte) 0x30;
	public final static byte SENSOR_BINARY_V2                  =   (byte) 0x30;
	public final static byte SENSOR_CONFIGURATION              =   (byte) 0x9E; /*This command class adds the possibility for sensors to act on either a measured value or on a*/
	public final static byte SENSOR_MULTILEVEL                 =   (byte) 0x31;
	public final static byte SENSOR_MULTILEVEL_V2              =   (byte) 0x31;
	public final static byte SENSOR_MULTILEVEL_V3              =   (byte) 0x31;
	public final static byte SENSOR_MULTILEVEL_V4              =   (byte) 0x31;
	public final static byte SENSOR_MULTILEVEL_V5              =   (byte) 0x31;
	public final static byte SENSOR_MULTILEVEL_V6              =   (byte) 0x31;
	public final static byte SENSOR_MULTILEVEL_V7              =   (byte) 0x31;
	public final static byte SILENCE_ALARM                     =   (byte) 0x9D; /*SDS10963-4 The Alarm Silence command class can be used to nuisance silence to temporarily disable the sounding*/
	public final static byte SIMPLE_AV_CONTROL                 =   (byte) 0x94;
	public final static byte SWITCH_ALL                        =   (byte) 0x27;
	public final static byte SWITCH_BINARY                     =   (byte) 0x25;
	public final static byte SWITCH_MULTILEVEL                 =   (byte) 0x26;
	public final static byte SWITCH_MULTILEVEL_V2              =   (byte) 0x26;
	public final static byte SWITCH_MULTILEVEL_V3              =   (byte) 0x26;
	public final static byte SWITCH_TOGGLE_BINARY              =   (byte) 0x28;
	public final static byte SWITCH_TOGGLE_MULTILEVEL          =   (byte) 0x29;
	public final static byte TARIFF_CONFIG                     =   (byte) 0x4A;
	public final static byte TARIFF_TBL_MONITOR                =   (byte) 0x4B;
	public final static byte THERMOSTAT_FAN_MODE               =   (byte) 0x44;
	public final static byte THERMOSTAT_FAN_MODE_V2            =   (byte) 0x44;
	public final static byte THERMOSTAT_FAN_MODE_V3            =   (byte) 0x44;
	public final static byte THERMOSTAT_FAN_MODE_V4            =   (byte) 0x44;
	public final static byte THERMOSTAT_FAN_STATE              =   (byte) 0x45;
	public final static byte THERMOSTAT_FAN_STATE_V2           =   (byte) 0x45;
	public final static byte THERMOSTAT_HEATING                =   (byte) 0x38;
	public final static byte THERMOSTAT_MODE                   =   (byte) 0x40;
	public final static byte THERMOSTAT_MODE_V2                =   (byte) 0x40;
	public final static byte THERMOSTAT_MODE_V3                =   (byte) 0x40;
	public final static byte THERMOSTAT_OPERATING_STATE        =   (byte) 0x42;
	public final static byte THERMOSTAT_OPERATING_STATE_V2     =   (byte) 0x42;
	public final static byte THERMOSTAT_SETBACK                =   (byte) 0x47;
	public final static byte THERMOSTAT_SETPOINT               =   (byte) 0x43;
	public final static byte THERMOSTAT_SETPOINT_V2            =   (byte) 0x43;
	public final static byte THERMOSTAT_SETPOINT_V3            =   (byte) 0x43;
	public final static byte TIME_PARAMETERS                   =   (byte) 0x8B;
	public final static byte TIME                              =   (byte) 0x8A;
	public final static byte TIME_V2                           =   (byte) 0x8A;
	public final static byte TRANSPORT_SERVICE                 =   (byte) 0x55;
	public final static byte TRANSPORT_SERVICE_V2              =   (byte) 0x55;
	public final static byte USER_CODE                         =   (byte) 0x63;
	public final static byte VERSION                           =   (byte) 0x86;
	public final static byte VERSION_V2                        =   (byte) 0x86;
	public final static byte WAKE_UP                           =   (byte) 0x84;
	public final static byte WAKE_UP_V2                        =   (byte) 0x84;
	public final static byte ZIP_6LOWPAN                       =   (byte) 0x4F;
	public final static byte ZIP                               =   (byte) 0x23;
	public final static byte ZIP_V2                            =   (byte) 0x23;
	public final static byte APPLICATION_CAPABILITY            =   (byte) 0x57;
	public final static byte COLOR_CONTROL                     =   (byte) 0x33;
	public final static byte COLOR_CONTROL_V2                  =   (byte) 0x33;
	public final static byte SCHEDULE                          =   (byte) 0x53;
	public final static byte NETWORK_MANAGEMENT_PRIMARY        =   (byte) 0x54;
	public final static byte ZIP_ND                            =   (byte) 0x58;
	public final static byte ASSOCIATION_GRP_INFO              =   (byte) 0x59;
	public final static byte DEVICE_RESET_LOCALLY              =   (byte) 0x5A;
	public final static byte CENTRAL_SCENE                     =   (byte) 0x5B;
	public final static byte IP_ASSOCIATION                    =   (byte) 0x5C;
	public final static byte ANTITHEFT                         =   (byte) 0x5D;
	public final static byte ANTITHEFT_V2                      =   (byte) 0x5D;
	public final static byte ZWAVEPLUS_INFO                    =   (byte) 0x5E; /*SDS11907-3*/
	public final static byte ZWAVEPLUS_INFO_V2                 =   (byte) 0x5E; /*SDS11907-3*/
	public final static byte ZIP_GATEWAY                       =   (byte) 0x5F;
	public final static byte ZIP_PORTAL                        =   (byte) 0x61;
	public final static byte APPLIANCE                         =   (byte) 0x64;
	public final static byte DMX                               =   (byte) 0x65;
	public final static byte BARRIER_OPERATOR                  =   (byte) 0x66;

	public final static BiMap<String,Byte>	names = createNamesBiMap();

	private static BiMap<String, Byte> createNamesBiMap() {
		return ImmutableBiMap.<String,Byte>builder()
//				.put("alarm", 									(byte) 0x71)
//				.put("alarm_v2", 								(byte) 0x71)
//				.put("notification_v3",						(byte) 0x71)
//				.put("notification_v4", 					(byte) 0x71)
				.put("notification", 							(byte) 0x71)
				.put("application status", 					(byte) 0x22)
				.put("association command configuration", (byte) 0x9b)
				.put("association", 								(byte) 0x85)
//				.put("association v2", 						(byte) 0x85)
				.put("AV Content directory md", 				(byte) 0x95)
				.put("AV Content search md", 					(byte) 0x97)
				.put("av renderer status", 					(byte) 0x96)
				.put("av tagging md", 							(byte) 0x99)
				.put("basic tariff info", 						(byte) 0x36)
				.put("basic window covering", 				(byte) 0x50)
				.put("basic", 										(byte) 0x20)
				.put("battery", 									(byte) 0x80)
				.put("chimney fan", 								(byte) 0x2a)
				.put("climate control schedule", 			(byte) 0x46)
				.put("clock", 									(byte) 0x81)
				.put("configuration", 							(byte) 0x70)
//				.put("configuration v2", 					(byte) 0x70)
				.put("controller replication", 			(byte) 0x21)
				.put("crc 16 encap", 						(byte) 0x56)
				.put("dcp config",				 				(byte) 0x3a)
				.put("dcp monitor", 							(byte) 0x3b)
				.put("door lock logging", 					(byte) 0x4c)
				.put("dook lock", 								(byte) 0x62)
//				.put("door lock v2", 						(byte) 0x62)
				.put("energy production", 					(byte) 0x90)
				.put("firmware update md", 					(byte) 0x7a)
//				.put("firmware update md v2", 			(byte) 0x7a)
//				.put("firmware update md v3", 			(byte) 0x7a)
				.put("geographic location", 				(byte) 0x8c)
				.put("grouping name", 						(byte) 0x7b)
				.put("hail", 									(byte) 0x82)
				.put("hrv control", 							(byte) 0x39)
				.put("hrv status", 							(byte) 0x37)
				.put("indicator", 							(byte) 0x87)
				.put("ip configuration", 					(byte) 0x9a)
				.put("language", 								(byte) 0x89)
				.put("lock", 									(byte) 0x76)
				.put("manufacturer proprietary", 		(byte) 0x91)
				.put("manufacture speicfic", 				(byte) 0x72)
//				.put("manufacturer specific v2", 		(byte) 0x72)
				.put("mark", 									(byte) 0xef)
				.put("meter pulse", 							(byte) 0x35)
				.put("meter tbl config", 					(byte) 0x3c)
				.put("meter tbl monitor", 					(byte) 0x3d)
//				.put("meter tbl monitor v2", 				(byte) 0x3d)
				.put("meter tbl push", 						(byte) 0x3e)
				.put("meter", 									(byte) 0x32)
//				.put("meter v2",								(byte) 0x32)
//				.put("meter v3", 								(byte) 0x32)
//				.put("meter v4", 								(byte) 0x32)
				.put("mtp window covering", 				(byte) 0x51)
//				.put("multi channel association v2", 	(byte) 0x8e)
//				.put("multi channel v2", 					(byte) 0x60)
//				.put("multi channel v3", 					(byte) 0x60)
				.put("multi cmd", 							(byte) 0x8f)
				.put("multi instance association", 		(byte) 0x8e)
				.put("multi instance", 						(byte) 0x60)
				.put("network management proxy", 		(byte) 0x52)
				.put("network management basic", 		(byte) 0x4d)
				.put("network management inclusion", 	(byte) 0x34)
				.put("no operation", 						(byte) 0x00)
				.put("node naming", 							(byte) 0x77)
				.put("non interoperable", 					(byte) 0xf0)
				.put("powerlevel", 							(byte) 0x73)
				.put("prepayment encapsulation", 		(byte) 0x41)
				.put("prepayment", 							(byte) 0x3f)
				.put("proprietary", 							(byte) 0x88)
				.put("protection", 							(byte) 0x75)
//				.put("protection v2", 						(byte) 0x75)
				.put("rate tbl config", 					(byte) 0x48)
				.put("rate tbl monitor", 					(byte) 0x49)
				.put("remote association activate", 	(byte) 0x7c)
				.put("remote association", 				(byte) 0x7d)
				.put("scene activation", 					(byte) 0x2b)
				.put("scene actuator conf", 				(byte) 0x2c)
				.put("scene controller conf", 			(byte) 0x2d)
				.put("schedule entry lock", 				(byte) 0x4e)
//				.put("schedule entry lock v2",		 	(byte) 0x4e)
//				.put("schedule entry lock v3", 			(byte) 0x4e)
				.put("screen attributes", 					(byte) 0x93)
//				.put("screen attributes v2", 				(byte) 0x93)
				.put("screen md", 							(byte) 0x92)
//				.put("screen md v2", 						(byte) 0x92)
				.put("security panel mode", 				(byte) 0x24)
				.put("security panel zone sensor", 		(byte) 0x2f)
				.put("security panel zone", 				(byte) 0x2e)
				.put("security", 								(byte) 0x98)
				.put("sensor alarm", 						(byte) 0x9c)
				.put("sensor binary", 						(byte) 0x30)
//				.put("sensor binary v2", 					(byte) 0x30)
				.put("sensor configuration", 				(byte) 0x9e)
				.put("sensor multilevel", 					(byte) 0x31)
//				.put("sensor multilevel v2", 				(byte) 0x31)
//				.put("sensor multilevel v3", 				(byte) 0x31)
//				.put("sensor multilevel v4", 				(byte) 0x31)
//				.put("sensor multilevel v5", 				(byte) 0x31)
//				.put("sensor multilevel v6", 				(byte) 0x31)
//				.put("sensor multilevel v7", 				(byte) 0x31)
				.put("silence alarm", 						(byte) 0x9d)
				.put("simple av control", 					(byte) 0x94)
				.put("switch all", 							(byte) 0x27)
				.put("switch binary", 						(byte) 0x25)
				.put("switch multilevel", 					(byte) 0x26)
//				.put("switch multilevel v2", 				(byte) 0x26)
//				.put("switch multilevel v3", 				(byte) 0x26)
				.put("switch toggle binary", 				(byte) 0x28)
				.put("switch toggle multilevel", 		(byte) 0x29)
				.put("tariff config", 						(byte) 0x4a)
				.put("tariff tbl monitor", 				(byte) 0x4b)
				.put("thermostat fan mode", 				(byte) 0x44)
//				.put("thermostat fan mode v2", 			(byte) 0x44)
//				.put("thermostat fan mode v3",			(byte) 0x44)
//				.put("thermostat fan mode v4", 			(byte) 0x44)
				.put("thermostat fan state", 				(byte) 0x45)
//				.put("thermostat fan state v2", 			(byte) 0x45)
				.put("thermostat heating", 				(byte) 0x38)
				.put("thermostat mode", 					(byte) 0x40)
//				.put("thermostat mode v2", 				(byte) 0x40)
//				.put("thermostat mode v3", 				(byte) 0x40)
				.put("thermostat operating state", 		(byte) 0x42)
//				.put("thermostat operating state v2", 	(byte) 0x42)
				.put("thermostat setback", 				(byte) 0x47)
				.put("thermostat setpoint", 				(byte) 0x43)
//				.put("thermostat setpoint v2", 			(byte) 0x43)
//				.put("thermostat setpoint v3", 			(byte) 0x43)
				.put("time parameters", 					(byte) 0x8b)
				.put("time", 									(byte) 0x8a)
//				.put("time v2", 								(byte) 0x8a)
				.put("transport service", 					(byte) 0x55)
//				.put("transport service v2", 				(byte) 0x55)
				.put("user code", 							(byte) 0x63)
				.put("version", 								(byte) 0x86)
//				.put("version v2", 							(byte) 0x86)
				.put("wake up", 								(byte) 0x84)
//				.put("wake up v2", 							(byte) 0x84)
				.put("zip 6lowpan", 							(byte) 0x4f)
				.put("zip", 									(byte) 0x23)
//				.put("zip v2", 								(byte) 0x23)
				.put("application capability", 			(byte) 0x57)
				.put("color control", 						(byte) 0x33)
//				.put("color control v2", 					(byte) 0x33)
				.put("schedule", 								(byte) 0x53)
				.put("network management primary", 		(byte) 0x54)
				.put("zip nd", 								(byte) 0x58)
				.put("association grp info", 				(byte) 0x59)
				.put("device reset locally",			 	(byte) 0x5a)
				.put("central scene", 						(byte) 0x5b)
				.put("ip association", 						(byte) 0x5c)
				.put("antitheft", 							(byte) 0x5d)
//				.put("antitheft v2", 						(byte) 0x5d)
				.put("zwaveplus info", 						(byte) 0x5e)
//				.put("zwaveplus info v2", 					(byte) 0x5e)
				.put("zip gateway", 							(byte) 0x5f)
				.put("zip portal", 							(byte) 0x61)
				.put("appliance", 							(byte) 0x64)
				.put("dmx", 									(byte) 0x65)
				.put("barrier operator", 					(byte) 0x66)
				.build();
	}

	public static String get(byte b) {
	   return names.inverse().get(b);
	}

	public static byte get(String name) {
		return names.get(name);
	}

}

