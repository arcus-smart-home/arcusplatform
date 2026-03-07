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
import com.iris.agent.zigbee.node.ZBNode;

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
}
