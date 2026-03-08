/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Arcus Project
 *
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

package com.iris.agent.zigbee.db;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteStatement;
import com.iris.agent.db.DbExtractor;
import com.iris.agent.zigbee.node.ZBAttribute;
import com.iris.agent.zigbee.node.ZBCluster;
import com.iris.agent.zigbee.node.ZBEndpoint;
import com.iris.agent.zigbee.node.ZBNode;
import com.iris.agent.zigbee.node.ZBProfile;

public class ZBExtractors {

   public enum ConfigExtractor implements DbExtractor<String> {
      INSTANCE;

      @Override
      public String extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         return stmt.columnString(0);
      }
   }

   public enum ConfigAllExtractor implements DbExtractor<KeyValuePair> {
      INSTANCE;

      @Override
      public KeyValuePair extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         return new KeyValuePair(stmt.columnString(0), stmt.columnString(1));
      }
   }

   public enum NodeExtractor implements DbExtractor<ZBNode> {
      INSTANCE;

      @Override
      public ZBNode extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         return ZBNode.builder(stmt.columnLong(0))
               .setNwkAddr(stmt.columnInt(1))
               .setParentAddr(stmt.columnInt(2))
               .setState(stmt.columnInt(3))
               .setMaximumIncomingTransferSize(stmt.columnInt(4))
               .setMaximumOutgoingTransferSize(stmt.columnInt(5))
               .setNodeFlags(stmt.columnInt(6))
               .setServerMask(stmt.columnInt(7))
               .setManufacturerCode(stmt.columnInt(8))
               .setDescriptorCapability(stmt.columnInt(9))
               .setMaximumBufferSize(stmt.columnInt(10))
               .setMacCapabilityFlags(stmt.columnInt(11))
               .setPowerDescriptor(stmt.columnInt(12))
               .setDeviceCapability(stmt.columnInt(13))
               .setOnline(stmt.columnInt(14) != 0)
               .setOfflineTimeout(stmt.columnInt(15))
               .build();
      }
   }

   public enum ProfileExtractor implements DbExtractor<ZBProfile> {
      INSTANCE;

      @Override
      public ZBProfile extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         return new ZBProfile(stmt.columnLong(0), stmt.columnLong(1), stmt.columnInt(2));
      }
   }

   public enum EndpointExtractor implements DbExtractor<ZBEndpoint> {
      INSTANCE;

      @Override
      public ZBEndpoint extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         return new ZBEndpoint(
               stmt.columnLong(0),   // id
               stmt.columnLong(1),   // profileId
               stmt.columnInt(2),    // endpointId
               stmt.columnInt(3),    // deviceId
               stmt.columnInt(4),    // deviceVersion
               stmt.columnInt(5),    // zclVersion
               stmt.columnInt(6),    // appVersion
               stmt.columnInt(7),    // stkVersion
               stmt.columnInt(8),    // hwVersion
               stmt.columnString(9), // manufacturerName
               stmt.columnString(10),// modelIdentifier
               stmt.columnString(11),// dateCode
               stmt.columnInt(12)    // powerSource
         );
      }
   }

   public enum ClusterExtractor implements DbExtractor<ZBCluster> {
      INSTANCE;

      @Override
      public ZBCluster extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         return new ZBCluster(
               stmt.columnLong(0),        // id
               stmt.columnLong(1),        // endpointId
               stmt.columnInt(2),         // clusterId
               stmt.columnInt(3) != 0     // server
         );
      }
   }

   public enum AttributeExtractor implements DbExtractor<ZBAttribute> {
      INSTANCE;

      @Override
      public ZBAttribute extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         return new ZBAttribute(
               stmt.columnLong(0),   // id
               stmt.columnLong(1),   // clusterId
               stmt.columnInt(2),    // attributeId
               stmt.columnInt(3),    // attributeDt
               stmt.columnBlob(4)    // attributeLastValue
         );
      }
   }

   public enum LongExtractor implements DbExtractor<Long> {
      INSTANCE;

      @Override
      public Long extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         return stmt.columnLong(0);
      }
   }
}
