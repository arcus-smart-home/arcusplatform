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
package com.iris.agent.zwave.engine;

/**
 * Z-Wave Serial API constants for SOF/ACK/NAK/CAN framing and function IDs.
 */
public final class ZWaveSerialConstants {

   private ZWaveSerialConstants() {}

   // Frame delimiters
   public static final byte SOF = 0x01;
   public static final byte ACK = 0x06;
   public static final byte NAK = 0x15;
   public static final byte CAN = 0x18;

   // Frame types
   public static final byte REQUEST = 0x00;
   public static final byte RESPONSE = 0x01;

   // Serial API function IDs
   public static final byte FUNC_ID_SERIAL_API_GET_INIT_DATA          = 0x02;
   public static final byte FUNC_ID_SERIAL_API_APPL_NODE_INFORMATION  = 0x03;
   public static final byte FUNC_ID_APPLICATION_COMMAND_HANDLER       = 0x04;
   public static final byte FUNC_ID_ZW_GET_CONTROLLER_CAPABILITIES    = 0x05;
   public static final byte FUNC_ID_SERIAL_API_SET_TIMEOUTS           = 0x06;
   public static final byte FUNC_ID_SERIAL_API_GET_CAPABILITIES       = 0x07;
   public static final byte FUNC_ID_SERIAL_API_SOFT_RESET             = 0x08;

   public static final byte FUNC_ID_ZW_SEND_NODE_INFORMATION          = 0x12;
   public static final byte FUNC_ID_ZW_SEND_DATA                      = 0x13;
   public static final byte FUNC_ID_ZW_GET_VERSION                    = 0x15;

   public static final byte FUNC_ID_ZW_R_F_POWER_LEVEL_SET            = 0x17;

   public static final byte FUNC_ID_ZW_MEMORY_GET_ID                  = 0x20;

   public static final byte FUNC_ID_ZW_GET_NODE_PROTOCOL_INFO         = 0x41;
   public static final byte FUNC_ID_ZW_SET_DEFAULT                    = 0x42;

   public static final byte FUNC_ID_ZW_ADD_NODE_TO_NETWORK            = 0x4A;
   public static final byte FUNC_ID_ZW_REMOVE_NODE_FROM_NETWORK       = 0x4B;

   public static final byte FUNC_ID_ZW_REQUEST_NODE_INFO              = 0x60;
   public static final byte FUNC_ID_ZW_REMOVE_FAILED_NODE_ID          = 0x61;
   public static final byte FUNC_ID_ZW_IS_FAILED_NODE_ID              = 0x62;
   public static final byte FUNC_ID_ZW_REPLACE_FAILED_NODE            = 0x63;

   public static final byte FUNC_ID_ZW_GET_ROUTING_INFO               = (byte) 0x80;

   public static final byte FUNC_ID_ZW_SET_LEARN_MODE                 = 0x50;
   public static final byte FUNC_ID_ZW_ASSIGN_SUC_RETURN_ROUTE        = 0x51;
   public static final byte FUNC_ID_ZW_ENABLE_SUC                     = 0x52;
   public static final byte FUNC_ID_ZW_REQUEST_NETWORK_UPDATE         = 0x53;
   public static final byte FUNC_ID_ZW_SET_SUC_NODE_ID                = 0x54;
   public static final byte FUNC_ID_ZW_GET_SUC_NODE_ID                = 0x56;

   public static final byte FUNC_ID_ZW_REQUEST_NODE_NEIGHBOR_UPDATE   = 0x48;

   // Transmit options
   public static final byte TRANSMIT_OPTION_ACK         = 0x01;
   public static final byte TRANSMIT_OPTION_LOW_POWER   = 0x02;
   public static final byte TRANSMIT_OPTION_AUTO_ROUTE  = 0x04;
   public static final byte TRANSMIT_OPTION_EXPLORE     = 0x20;

   // Default transmit options
   public static final byte DEFAULT_TRANSMIT_OPTIONS = TRANSMIT_OPTION_ACK | TRANSMIT_OPTION_AUTO_ROUTE | TRANSMIT_OPTION_EXPLORE;

   // Add node modes
   public static final byte ADD_NODE_ANY        = 0x01;
   public static final byte ADD_NODE_CONTROLLER = 0x02;
   public static final byte ADD_NODE_SLAVE      = 0x03;
   public static final byte ADD_NODE_EXISTING   = 0x04;
   public static final byte ADD_NODE_STOP       = 0x05;

   // Remove node modes
   public static final byte REMOVE_NODE_ANY  = 0x01;
   public static final byte REMOVE_NODE_STOP = 0x05;

   // Add/Remove node status
   public static final byte ADD_NODE_STATUS_LEARN_READY        = 0x01;
   public static final byte ADD_NODE_STATUS_NODE_FOUND         = 0x02;
   public static final byte ADD_NODE_STATUS_ADDING_SLAVE       = 0x03;
   public static final byte ADD_NODE_STATUS_ADDING_CONTROLLER  = 0x04;
   public static final byte ADD_NODE_STATUS_PROTOCOL_DONE      = 0x05;
   public static final byte ADD_NODE_STATUS_DONE               = 0x06;
   public static final byte ADD_NODE_STATUS_FAILED             = 0x07;

   public static final byte REMOVE_NODE_STATUS_LEARN_READY    = 0x01;
   public static final byte REMOVE_NODE_STATUS_NODE_FOUND     = 0x02;
   public static final byte REMOVE_NODE_STATUS_REMOVING_SLAVE = 0x03;
   public static final byte REMOVE_NODE_STATUS_REMOVING_CONTROLLER = 0x04;
   public static final byte REMOVE_NODE_STATUS_DONE           = 0x06;
   public static final byte REMOVE_NODE_STATUS_FAILED         = 0x07;

   // Baud rate for Z-Wave serial communication
   public static final int ZWAVE_BAUD_RATE = 115200;

   // Timeouts
   public static final long ACK_TIMEOUT_MS = 1500;
   public static final long RESPONSE_TIMEOUT_MS = 5000;
   public static final long CALLBACK_TIMEOUT_MS = 30000;

   // Node info mask bits from serial API init data
   public static final int NODE_BITMASK_LENGTH = 29;
}
