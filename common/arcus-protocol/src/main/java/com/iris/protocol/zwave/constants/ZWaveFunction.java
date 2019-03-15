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

public class ZWaveFunction {
	/* Function IDs */
	public static final byte GET_INIT_DATA                		= 0x02;
	public static final byte APPL_NODE_INFORMATION       	 		= 0x03;
	public static final byte APPLICATION_COMMAND_HANDLER     	= 0x04;
	public static final byte GET_CONTROLLER_CAPABILITIES     	= 0x05;

	/* SERIAL API ver 4 added - START */
	public static final byte SET_TIMEOUTS                			= 0x06;
	public static final byte GET_CAPABILITIES             		= 0x07;
	public static final byte SOFT_RESET                   		= 0x08;
	/* SERIAL API ver 4 added - END */

	public static final byte GET_PROTOCOL_VERSION               = 0x09;

	/* Function ID for startup message */
	public static final byte SERIALAPI_STARTED                  = 0x0A;

	public static final byte SET_RF_RECEIVE_MODE                = 0x10;
	public static final byte SET_SLEEP_MODE                     = 0x11;
	public static final byte SEND_NODE_INFORMATION              = 0x12;
	public static final byte SEND_DATA                          = 0x13;
	public static final byte SEND_DATA_MULTI                    = 0x14;
	public static final byte GET_VERSION                        = 0x15;

	/* SERIAL API ver 4 added - START */
	public static final byte SEND_DATA_ABORT                    = 0x16;
	public static final byte RF_POWER_LEVEL_SET                 = 0x17;
	public static final byte SEND_DATA_META                     = 0x18;
	/* SERIAL API ver 4 added - END */

	public static final byte RESERVED_SD                        = 0x19;
	public static final byte RESERVED_SDM                       = 0x1A;
	public static final byte RESERVED_SRI                       = 0x1B;

	public static final byte SET_ROUTING_INFO                   = 0x1B;

	public static final byte GET_RANDOM                      	= 0x1C;
	public static final byte RANDOM                           	= 0x1D;
	public static final byte RF_POWER_LEVEL_REDISCOVERY_SET   	= 0x1E;

	public static final byte MEMORY_GET_ID                    	= 0x20;
	public static final byte MEMORY_GET_BYTE                    = 0x21;
	public static final byte MEMORY_PUT_BYTE                    = 0x22;
	public static final byte MEMORY_GET_BUFFER                  = 0x23;
	public static final byte MEMORY_PUT_BUFFER                  = 0x24;
	/* Unimplemented - START */
	public static final byte GET_APPL_HOST_MEMORY_OFFSET  		= 0x25;
	public static final byte DEBUG_OUTPUT                      	= 0x26;
	/* Unimplemented - END */

	public static final byte AUTO_PROGRAMMING             	  	= 0x27;

	public static final byte NVR_GET_VALUE                  	  	= 0x28;

	public static final byte NVM_GET_ID                       	= 0x29;
	public static final byte NVM_EXT_READ_LONG_BUFFER        	= 0x2A;
	public static final byte NVM_EXT_WRITE_LONG_BUFFER         	= 0x2B;
	public static final byte NVM_EXT_READ_LONG_BYTE          	= 0x2C;
	public static final byte NVM_EXT_WRITE_LONG_BYTE          	= 0x2D;

	public static final byte CLOCK_SET                        	= 0x30;
	public static final byte CLOCK_GET              				= 0x31;
	public static final byte CLOCK_CMP            					= 0x32;
	public static final byte RTC_TIMER_CREATE                  	= 0x33;
	public static final byte RTC_TIMER_READ               	   = 0x34;
	public static final byte RTC_TIMER_DELETE                  	= 0x35;
	public static final byte RTC_TIMER_CALL             	      = 0x36;

	public static final byte CLEAR_TX_TIMERS                    = 0x37;
	public static final byte GET_TX_TIMERS                      = 0x38;

	public static final byte SET_LEARN_NODE_STATE               = 0x40;
	public static final byte GET_NODE_PROTOCOL_INFO             = 0x41;
	public static final byte SET_DEFAULT                        = 0x42;
	public static final byte NEW_CONTROLLER                     = 0x43;
	public static final byte REPLICATION_COMMAND_COMPLETE       = 0x44;
	public static final byte REPLICATION_SEND_DATA              = 0x45;
	public static final byte ASSIGN_RETURN_ROUTE                = 0x46;
	public static final byte DELETE_RETURN_ROUTE                = 0x47;
	public static final byte REQUEST_NODE_NEIGHBOR_UPDATE       = 0x48;
	public static final byte APPLICATION_UPDATE                 = 0x49;

	/*Obsolete use APPLICATION_UPDATE */
	public static final byte APPLICATION_CONTROLLER_UPDATE      = 0x49;

	public static final byte ADD_NODE_TO_NETWORK                = 0x4A;
	public static final byte REMOVE_NODE_FROM_NETWORK           = 0x4B;
	public static final byte CREATE_NEW_PRIMARY                 = 0x4C;
	public static final byte CONTROLLER_CHANGE                  = 0x4D;

	public static final byte RESERVED_FN                        = 0x4E;
	public static final byte RESERVED_AR                        = 0x4F;

	/* Slave only */
	public static final byte SET_LEARN_MODE                     = 0x50;
	/* Slave only end */

	public static final byte ASSIGN_SUC_RETURN_ROUTE            = 0x51;
	public static final byte ENABLE_SUC                         = 0x52;
	public static final byte REQUEST_NETWORK_UPDATE             = 0x53;
	public static final byte SET_SUC_NODE_ID                    = 0x54;
	public static final byte DELETE_SUC_RETURN_ROUTE            = 0x55;
	public static final byte GET_SUC_NODE_ID                    = 0x56;
	public static final byte SEND_SUC_ID                        = 0x57;

	public static final byte RESERVED_ASR                       = 0x58;
	public static final byte REDISCOVERY_NEEDED                 = 0x59;

	public static final byte REQUEST_NODE_NEIGHBOR_UPDATE_OPTION= 0x5A;

	/* Slave only */
	public static final byte SUPPORT9600_ONLY                   = 0x5B;
	/* Slave only end */

	/* Enhanced/Routing Slave only */
	public static final byte REQUEST_NEW_ROUTE_DESTINATIONS     = 0x5C;
	public static final byte IS_NODE_WITHIN_DIRECT_RANGE        = 0x5D;
	/* Enhanced/Routing Slave only end */

	public static final byte EXPLORE_REQUEST_INCLUSION          = 0x5E;

	public static final byte REQUEST_NODE_INFO                  = 0x60;
	public static final byte REMOVE_FAILED_NODE_ID              = 0x61;
	public static final byte IS_FAILED_NODE_ID                  = 0x62;
	public static final byte REPLACE_FAILED_NODE                = 0x63;

	public static final byte IS_PRIMARY_CTRL                    = 0x66;

	public static final byte AES_ECB                            = 0x67;		// No in serial api?

	public static final byte TIMER_START                        = 0x70;
	public static final byte TIMER_RESTART                      = 0x71;
	public static final byte TIMER_CANCEL                       = 0x72;
	public static final byte TIMER_CALL                         = 0x73;

	/* Firmware Update API */
	public static final byte FIRMWARE_UPDATE_NVM                = 0x78;

	/* Installer API */
	public static final byte GET_ROUTING_TABLE_LINE             = (byte) 0x80;
	public static final byte GET_TX_COUNTER                     = (byte) 0x81;
	public static final byte RESET_TX_COUNTER                   = (byte) 0x82;
	public static final byte STORE_NODEINFO                     = (byte) 0x83;
	public static final byte STORE_HOMEID                       = (byte) 0x84;
	/* Installer API only end */

	public static final byte LOCK_ROUTE_RESPONSE                = (byte) 0x90;

	public static final byte GET_LAST_WORKING_ROUTE             = (byte) 0x92;
	public static final byte SET_LAST_WORKING_ROUTE             = (byte) 0x93;

	//#ifdef CONTROLLER_SINGLE
	public static final byte TEST                         		= (byte) 0x95;
	//#endif

	public static final byte EXT                          		= (byte) 0x98;

	/* CONTROLLER_BRIDGE only START */
	public static final byte APPL_SLAVE_NODE_INFORMATION  		= (byte) 0xA0;
	/* OBSOLETE: In DevKit 4.5x/6.= 0x Controller Bridge applications, this is obsoleted */
	/* by the APPLICATION_COMMAND_HANDLER_BRIDGE */
	public static final byte APPLICATION_SLAVE_COMMAND_HANDLER  = (byte) 0xA1;
	public static final byte SEND_SLAVE_NODE_INFORMATION        = (byte) 0xA2;
	public static final byte SEND_SLAVE_DATA                    = (byte) 0xA3;
	public static final byte SET_SLAVE_LEARN_MODE               = (byte) 0xA4;
	public static final byte GET_VIRTUAL_NODES                  = (byte) 0xA5;
	public static final byte IS_VIRTUAL_NODE                    = (byte) 0xA6;
	public static final byte RESERVED_SSD                       = (byte) 0xA7;
	/* DevKit 4.5x/6.= 0x added - obsoletes APPLICATION_SLAVE_COMMAND_HANDLER and */
	/* APPLICATION_COMMAND_HANDLER for the Controller Bridge applications as */
	/* this handles both cases - only for 4.5x/6.= 0x based Controller Bridge applications */
	public static final byte APPLICATION_COMMAND_HANDLER_BRIDGE = (byte) 0xA8;
	/* DevKit 4.5x/6.= 0x added - Adds sourceNodeID to the parameter list */
	public static final byte SEND_DATA_BRIDGE                   = (byte) 0xA9;
	public static final byte SEND_DATA_META_BRIDGE              = (byte) 0xAA;
	public static final byte SEND_DATA_MULTI_BRIDGE             = (byte) 0xAB;
	/* CONTROLLER_BRIDGE only END */

	public static final byte PWR_SETSTOPMODE                    = (byte) 0xB0;    // ZW102 only
	public static final byte PWR_CLK_PD                         = (byte) 0xB1;    // ZW102 only
	public static final byte PWR_CLK_PUP                        = (byte) 0xB2;    // ZW102 only
	public static final byte PWR_SELECT_CLK                     = (byte) 0xB3;    // ZW102 only
	public static final byte SET_WUT_TIMEOUT                    = (byte) 0xB4;    // ZW201 only
	public static final byte IS_WUT_KICKED                      = (byte) 0xB5;    // ZW201 only

	public static final byte WATCHDOG_ENABLE                    = (byte) 0xB6;
	public static final byte WATCHDOG_DISABLE                   = (byte) 0xB7;
	public static final byte WATCHDOG_KICK                      = (byte) 0xB8;
	/* Obsolete use INT_EXT_LEVEL_SET */
	public static final byte SET_EXT_INT_LEVEL                  = (byte) 0xB9;    // ZW201 only
	public static final byte INT_EXT_LEVEL_SET                  = (byte) 0xB9;

	public static final byte RF_POWER_LEVEL_GET                 = (byte) 0xBA;
	public static final byte GET_NEIGHBOR_COUNT                 = (byte) 0xBB;
	public static final byte ARE_NODES_NEIGHBOURS               = (byte) 0xBC;

	public static final byte TYPE_LIBRARY                       = (byte) 0xBD;
	public static final byte SEND_TEST_FRAME                    = (byte) 0xBE;
	public static final byte GET_PROTOCOL_STATUS                = (byte) 0xBF;

	public static final byte SET_PROMISCUOUS_MODE               = (byte) 0xD0;
	/* SERIAL API ver 5 added - START */
	public static final byte PROMISCUOUS_APPLICATION_COMMAND_HANDLER = (byte) 0xD1;
	/* SERIAL API ver 5 added - END */

	public static final byte WATCHDOG_START                     = (byte) 0xD2;
	public static final byte WATCHDOG_STOP                      = (byte) 0xD3;

	public static final byte SET_ROUTING_MAX                    = (byte) 0xD4;
	/* In 6.= 0x the function id was wrong so we need to support this wrong function id as well in the future */
	public static final byte SET_ROUTING_MAX_6_00               = (byte) 0x65;
	/* Unimplemented - START */
	/* Obsoleted */
	public static final byte GET_ROUTING_MAX                    = (byte) 0xD5;
	/* Unimplemented - END */

	/* Allocated for Power Management */
	public static final byte POWER_MANAGEMENT            			= (byte) 0xEE;
	public static final byte READY                        		= (byte) 0xEF;

	/* Allocated for NUNIT test */
	public static final byte NUNIT_CMD                          = (byte) 0xE0;
	public static final byte NUNIT_INIT                         = (byte) 0xE1;
	public static final byte NUNIT_LIST                         = (byte) 0xE2;
	public static final byte NUNIT_RUN                          = (byte) 0xE3;
	public static final byte NUNIT_END                          = (byte) 0xE4;

	public static final byte IO_PORT_STATUS                     = (byte) 0xE5;
	public static final byte IO_PORT                            = (byte) 0xE6;

	/* Allocated for proprietary serial API commands */
	public static final byte PROPRIETARY_0                      = (byte) 0xF0;
	public static final byte PROPRIETARY_1                      = (byte) 0xF1;
	public static final byte PROPRIETARY_2                      = (byte) 0xF2;
	public static final byte PROPRIETARY_3                      = (byte) 0xF3;
	public static final byte PROPRIETARY_4                      = (byte) 0xF4;
	public static final byte PROPRIETARY_5                      = (byte) 0xF5;
	public static final byte PROPRIETARY_6                      = (byte) 0xF6;
	public static final byte PROPRIETARY_7                      = (byte) 0xF7;
	public static final byte PROPRIETARY_8                      = (byte) 0xF8;
	public static final byte PROPRIETARY_9                      = (byte) 0xF9;
	public static final byte PROPRIETARY_A                      = (byte) 0xFA;
	public static final byte PROPRIETARY_B                      = (byte) 0xFB;
	public static final byte PROPRIETARY_C                      = (byte) 0xFC;
	public static final byte PROPRIETARY_D                      = (byte) 0xFD;
	public static final byte PROPRIETARY_E                      = (byte) 0xFE;

	//////////////////////////////
	// Useful Static Functions
	// BiMap for mapping functions to names and vice versa.
	//final public static BiMap<String,Byte>	functionsBiMap = createFunctionsBiMap();
	final public static BiMap<String,Byte> functionsBiMap = createFunctionsBiMap();

	public static BiMap<String,Byte> createFunctionsBiMap() {
		return ImmutableBiMap.<String,Byte>builder()
				.put("Get Init Data", 						(byte) 0x02)
				.put("Application Node Information", 	(byte) 0x03)
				.put("Application Command Handler", 	(byte) 0x04)
				.put("Get Controller Capabilities",		(byte) 0x05)

				.put("Set Timeouts",							(byte) 0x06)
				.put("Get Capabilities",					(byte) 0x07)
				.put("Soft Reset",							(byte) 0x08)

				.put("Get Protocol Version",           (byte) 0x09)

				.put("Serial API Started",					(byte) 0x0A)

				// TODO Fix the ugly below.
				.put("Set RF Receive Mode",  				(byte) 0x10)
				.put("Set Sleep Mode", 					 	(byte) 0x11)
				.put("Send Node Information",  			(byte) 0x12)
				.put("Send Data",  							(byte) 0x13)
				.put("Send Data Multi", 					(byte) 0x14)
				.put("Get Version",  						(byte) 0x15)
				.put("Send Data Abort",  					(byte) 0x16)
				.put("RF Power Level Set",  				(byte) 0x17)
				.put("Send Data Meta",  					(byte) 0x18)
				.put("Reserved SD",  						(byte) 0x19)
				.put("Reserved SDM",  						(byte) 0x1A)
//				.put("RESERVED_SRI	",  (byte) 0x1B)
				.put("Set Routing Info",  					(byte) 0x1B)
				.put("Get Random", 							(byte) 0x1C)
				.put("Random",  								(byte) 0x1D)
				.put("RF Power Level Rediscovery Set",	(byte) 0x1E)
				.put("Memory Get ID", 	 					(byte) 0x20)
				.put("Memory Get Byte",  					(byte) 0x21)
				.put("Memory Put Byte",  					(byte) 0x22)
				.put("Memory Get Buffer",  				(byte) 0x23)
				.put("Memory Put Buffer", 			 		(byte) 0x24)
				.put("Get Appl Host Memory Offset", 	(byte) 0x25)
				.put("Debug Output",  						(byte) 0x26)
				.put("NVR Get Value",  						(byte) 0x28)
				.put("NVM Get Id",  							(byte) 0x29)
				.put("NVM Ext Read Long Buffer",  		(byte) 0x2A)
				.put("NVM Ext Write Long Buffer",  		(byte) 0x2B)
				.put("NVM Ext Read Long Byte",  			(byte) 0x2C)
				.put("NVM Ext Write Long Byte",  		(byte) 0x2D)
				.put("Clock Set",  							(byte) 0x30)
				.put("Clock Get",  							(byte) 0x31)
				.put("Clock Cmp", 		 					(byte) 0x32)
				.put("RTC Timer Create",  					(byte) 0x33)
				.put("RTC Timer Read",  					(byte) 0x34)
				.put("RTC Timer Delete",  					(byte) 0x35)
				.put("RTC Timer Call",  					(byte) 0x36)
				.put("Clear TX Timers", 					(byte) 0x37)
				.put("Get TX Timers",  						(byte) 0x38)
				.put("Set Learn Node State",  			(byte) 0x40)
				.put("Get Node Protocol Info",  			(byte) 0x41)
				.put("Set Default",  						(byte) 0x42)
				.put("New Controller",  					(byte) 0x43)
				.put("Replication Command Complete", 	(byte) 0x44)
				.put("Replication Send Data",  			(byte) 0x45)
				.put("Assign Return Route",  				(byte) 0x46)
				.put("Delete Return Route",  				(byte) 0x47)
				.put("Request Node Neighbot Update",	(byte) 0x48)
				.put("Application Update",  				(byte) 0x49)
//				.put("Application Controller Update",  (byte) 0x49)
				.put("Add Node To Network",  				(byte) 0x4A)
				.put("Remove Node From Network",  		(byte) 0x4B)
				.put("Create new Primary",  				(byte) 0x4C)
				.put("Controller Change",  				(byte) 0x4D)
				.put("Reserved FN",  						(byte) 0x4E)
				.put("Reserved AR",  						(byte) 0x4F)
				.put("Set Learn Mode",  					(byte) 0x50)
				.put("Assign SUC Return Route",  		(byte) 0x51)
				.put("Enable SUC",  							(byte) 0x52)
				.put("Requst Network Update",  			(byte) 0x53)
				.put("Set SUC Node Id",  					(byte) 0x54)
				.put("Delte SUC Return Route",  			(byte) 0x55)
				.put("Get SUC Node Id",  					(byte) 0x56)
				.put("Send Suc Id",  						(byte) 0x57)
				.put("Reserved ASR",  						(byte) 0x58)
				.put("Rediscovery Needed",  				(byte) 0x59)
				.put("Request Node Neighbor Update Option",  (byte) 0x5A)
				.put("Support 9600 Only",  				(byte) 0x5B)
				.put("Request New Route Destinations",	(byte) 0x5C)
				.put("Is Node Withing Direct Range",  	(byte) 0x5D)
				.put("Explore Request Inclusion",  		(byte) 0x5E)
				.put("Request Node Info",  				(byte) 0x60)
				.put("Remove Failed Node Id",  			(byte) 0x61)
				.put("Is Failed Node Id",  				(byte) 0x62)
				.put("Replace Fialed Node",  				(byte) 0x63)
				.put("Is Primary Ctrl",  					(byte) 0x66)
				.put("AES ECB",  								(byte) 0x67)
				.put("Timer Start",  						(byte) 0x70)
				.put("Timer Restart",  						(byte) 0x71)
				.put("Timer Cancel",  						(byte) 0x72)
				.put("Timer Call",  							(byte) 0x73)
				.put("Firmware Update NVM",  				(byte) 0x78)
				.put("Get Routing Table Line",  			(byte) 0x80)
				.put("Get TX Counter",  					(byte) 0x81)
				.put("Reset TX Counter",  					(byte) 0x82)
				.put("Store Node Info",  					(byte) 0x83)
				.put("Store Home Id",  						(byte) 0x84)
				.put("Lock Route Response",  				(byte) 0x90)
				.put("Get Last Working Route",  			(byte) 0x92)
				.put("Set Last Working Route",  			(byte) 0x93)
				.put("Test",  									(byte) 0x95)
				.put("Ext",  									(byte) 0x98)
				.put("Appl Slove Node Info",  			(byte) 0xA0)
				.put("Appl Slave Command Handler",  	(byte) 0xA1)
				.put("Send Slave Node Information",  	(byte) 0xA2)
				.put("Send Slave Data",  					(byte) 0xA3)
				.put("Set Slave Learn Mode",  			(byte) 0xA4)
				.put("Get Virtual Nodes",  				(byte) 0xA5)
				.put("Is Virtual Node",  					(byte) 0xA6)
				.put("Reserved SSD",  						(byte) 0xA7)
				.put("Application Command Handler Bridge",  (byte) 0xA8)
				.put("Send Data Bridge",  					(byte) 0xA9)
				.put("Send Data Meta Bridge",  			(byte) 0xAA)
				.put("Send Data Multi Bridge",  			(byte) 0xAB)
				.put("Power Set Stop Mode",  				(byte) 0xB0)	//	ZW102	only
				.put("Power Clock PD",  					(byte) 0xB1)	//	ZW102	only
				.put("Power Clock PUP",  					(byte) 0xB2)	//	ZW102	only
				.put("Power Select Clock",  				(byte) 0xB3)	//	ZW102	only
				.put("Set WUT Timeout",  					(byte) 0xB4)	//	ZW201	only
				.put("Is WUT Kicked",  						(byte) 0xB5)	//	ZW201	only
				.put("WatchDog Enable",  					(byte) 0xB6)
				.put("WatchDog Disable",  					(byte) 0xB7)
				.put("WatchDog Kick",  						(byte) 0xB8)
//				.put("Set Ext Int Level",  				(byte) 0xB9)	//	ZW201	only
				.put("Int Ext Level Set",  				(byte) 0xB9)
				.put("RF Power Level Get",  				(byte) 0xBA)
				.put("Get Neighbot Count",  				(byte) 0xBB)
				.put("Are Nodes Neightbours",  			(byte) 0xBC)
				.put("Type Library",  						(byte) 0xBD)
				.put("Send Test Frame",  					(byte) 0xBE)
				.put("Get Protocol Status",  				(byte) 0xBF)
				.put("Set Promicuous Mode",  				(byte) 0xD0)
				.put("Promiscuous Application command Handler",  (byte) 0xD1)
				.put("WatchDog Start",  					(byte) 0xD2)
				.put("WatchDog Stop",  						(byte) 0xD3)
				.put("Set Routing max",  					(byte) 0xD4)
				.put("Set Routing Max 6.00",  			(byte) 0x65)
				.put("Get Routing Max",  					(byte) 0xD5)
				.put("Power Management",  					(byte) 0xEE)
				.put("Ready",  								(byte) 0xEF)
				.put("NUnit CMD",  							(byte) 0xE0)
				.put("NUnit Init",  							(byte) 0xE1)
				.put("NUnit List",  							(byte) 0xE2)
				.put("NUnit Run",  							(byte) 0xE3)
				.put("NUnit End",  							(byte) 0xE4)
				.put("IO Port Status",  					(byte) 0xE5)
				.put("IO Port",  								(byte) 0xE6)
				.put("Proprietary 0",  						(byte) 0xF0)
				.put("Proprietary 1", 	 					(byte) 0xF1)
				.put("Proprietary 2",  						(byte) 0xF2)
				.put("Proprietary 3",  						(byte) 0xF3)
				.put("Proprietary 4",  						(byte) 0xF4)
				.put("Proprietary 5", 	 					(byte) 0xF5)
				.put("Proprietary 6",  						(byte) 0xF6)
				.put("Proprietary 7",  						(byte) 0xF7)
				.put("Proprietary 8",  						(byte) 0xF8)
				.put("Proprietary 9",  						(byte) 0xF9)
				.put("Proprietary A",  						(byte) 0xFA)
				.put("Proprietary B",  						(byte) 0xFB)
				.put("Proprietary C", 					 	(byte) 0xFC)
				.put("Proprietary D", 						(byte) 0xFD)
				.put("Proprietary E",  						(byte) 0xFE)
.build();
	}

	public static String get(byte functionID) {
		return functionsBiMap.inverse().get(functionID);
	}

	public static Byte get(String name) {
		return functionsBiMap.get(name);
	}


}

