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
package com.iris.agent.zwave.db;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteStatement;
import com.iris.agent.db.DbBinder;
import com.iris.agent.db.DbUtils;
import com.iris.agent.util.ByteUtils;
import com.iris.agent.zwave.node.ZWNode;

class Binders {
   
   static enum ConfigBinder implements DbBinder<String> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, String value) throws Exception {
         stmt.bind(1, value);
      }
   }

   static enum ConfigInsertBinder implements DbBinder<KeyValuePair> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, KeyValuePair pair) throws Exception {
         stmt.bind(1, pair.getKey());
         DbUtils.bind(stmt, pair.getValue(), 2);
      }
   }

   static enum IdBinder implements DbBinder<Long> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, Long id) throws Exception {
         stmt.bind(1, id);
      }
   }
   
   static enum DeleteNodeBinder implements DbBinder<ZWNode> {
      INSTANCE;
      
      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, ZWNode node) throws Exception {
         int idx = 1;
         stmt.bind(idx, node.getNodeId());
      }
   }
   
   static enum CreateNodeBinder implements DbBinder<ZWNode> {
      INSTANCE;
      
      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, ZWNode node) throws Exception {
         int idx = 1;
         stmt.bind(idx++, node.getNodeId());
         stmt.bind(idx++, node.getBasicDeviceType());
         stmt.bind(idx++, node.getGenericDeviceType());
         stmt.bind(idx++, node.getSpecificDeviceType());
         stmt.bind(idx++, node.getManufacturerId());
         stmt.bind(idx++, node.getProductTypeId());
         stmt.bind(idx++, node.getProductId());
         stmt.bind(idx++, ByteUtils.byteArray2SpacedString(node.getCmdClassBytes()));
         stmt.bind(idx++, node.isOnline() ? 1 : 0);
         stmt.bind(idx++, node.getOfflineTimeout());
      }
   }
   
   static enum UpdateNodeBinder implements DbBinder<ZWNode> {
      INSTANCE;
      
      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, ZWNode node) throws Exception {
         int idx = 1;
         stmt.bind(idx++, node.getNodeId());
         stmt.bind(idx++, node.getBasicDeviceType());
         stmt.bind(idx++, node.getGenericDeviceType());
         stmt.bind(idx++, node.getSpecificDeviceType());
         stmt.bind(idx++, node.getManufacturerId());
         stmt.bind(idx++, node.getProductTypeId());
         stmt.bind(idx++, node.getProductId());
         stmt.bind(idx++, ByteUtils.byteArray2SpacedString(node.getCmdClassBytes()));
         stmt.bind(idx++, node.isOnline() ? 1 : 0);
         stmt.bind(idx++, node.getOfflineTimeout());
         
         // The where clause
         stmt.bind(idx++, node.getNodeId());
      }
   }
}

