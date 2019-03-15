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
package com.iris.core.protocol.ipcd.cassandra;

public class IpcdDeviceTable {
   public static final String NAME = "ipcd_device";

   public static final String[] INSERT_COLUMNS = new String[] {
      Columns.PROTOCOL_ADDRESS,
      Columns.ACCOUNT_ID,
      Columns.PLACE_ID,
      Columns.DRIVER_ADDRESS,
      Columns.CREATED,
      Columns.MODIFIED,
      Columns.LAST_CONNECTED,
      Columns.VENDOR,
      Columns.MODEL,
      Columns.SN,
      Columns.IPCD_VER,
      Columns.FIRMWARE,
      Columns.CONNECTION,
      Columns.ACTIONS,
      Columns.COMMANDS,
      Columns.V1DEVICEID,
      Columns.PARTITION_ID,
      Columns.CONN_STATE,
      Columns.REGISTRATION_STATE,
   };

   public static final String[] UPDATE_COLUMNS = new String[] {
      Columns.ACCOUNT_ID,
      Columns.PLACE_ID,
      Columns.DRIVER_ADDRESS,
      Columns.CREATED,
      Columns.MODIFIED,
      Columns.LAST_CONNECTED,
      Columns.VENDOR,
      Columns.MODEL,
      Columns.SN,
      Columns.IPCD_VER,
      Columns.FIRMWARE,
      Columns.CONNECTION,
      Columns.ACTIONS,
      Columns.COMMANDS,
      Columns.V1DEVICEID,
      Columns.PARTITION_ID,
      Columns.CONN_STATE,
      Columns.REGISTRATION_STATE
   };

   public static final class Columns {
      public static final String PROTOCOL_ADDRESS = "protocolAddress";
      public static final String ACCOUNT_ID = "accountId";
      public static final String PLACE_ID = "placeId";
      public static final String DRIVER_ADDRESS = "driverAddress";
      public static final String CREATED = "created";
      public static final String MODIFIED = "modified";
      public static final String LAST_CONNECTED = "lastConnected";
      public static final String VENDOR = "vendor";
      public static final String MODEL = "model";
      public static final String SN = "sn";
      public static final String IPCD_VER = "ipcdver";
      public static final String FIRMWARE = "firmware";
      public static final String CONNECTION = "connection";
      public static final String ACTIONS = "actions";
      public static final String COMMANDS = "commands";
      public static final String V1DEVICEID = "v1deviceid";
      public static final String PARTITION_ID = "partitionId";
      public static final String CONN_STATE = "connState";
      public static final String REGISTRATION_STATE = "registrationState";
   }
}

