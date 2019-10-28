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
import com.iris.agent.db.DbExtractor;
import com.iris.agent.util.ByteUtils;
import com.iris.agent.zwave.ZWServices;
import com.iris.agent.zwave.node.ZWNode;
import com.iris.agent.zwave.node.ZWNodeBuilder;

class Extractors {
   private final static int CONFIG_NAME_COL = 0;
   
   private final static int CONFIG_ALL_KEY_COL = 0;
   private final static int CONFIG_ALL_VALUE_COL = 1;
   
   static enum ConfigExtractor implements DbExtractor<String> {
      INSTANCE;

      @Override
      public String extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         return stmt.columnString(CONFIG_NAME_COL);
      }
   }

   static enum ConfigAllExtractor implements DbExtractor<KeyValuePair> {
      INSTANCE;

      @Override
      public KeyValuePair extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         String key = stmt.columnString(CONFIG_ALL_KEY_COL);
         String val = stmt.columnString(CONFIG_ALL_VALUE_COL);
         return new KeyValuePair(key,val);
      }
   }
   
   private final static int NODE_ID_COL = 0;
   private final static int NODE_BASIC_TYPE_COL = 1;
   private final static int NODE_GENERIC_TYPE_COL = 2;
   private final static int NODE_SPECIFIC_TYPE_COL = 3;
   private final static int NODE_MANUFACTURER_ID_COL = 4;
   private final static int NODE_PRODUCT_TYPE_COL = 5;
   private final static int NODE_PRODUCT_ID = 6;
   private final static int NODE_CMD_CLASSES = 7;
   private final static int NODE_ONLINE = 8;
   private final static int NODE_OFFLINE_TIMEOUT = 9;
   
   static enum NodeExtractor implements DbExtractor<ZWNode> {
      INSTANCE;
      
      @Override
      public ZWNode extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         int node_id = stmt.columnInt(NODE_ID_COL);
         ZWNodeBuilder builder = ZWNode.builder(node_id);
         ZWNode node = builder.setBasicType(stmt.columnInt(NODE_BASIC_TYPE_COL))
               .setGenericType(stmt.columnInt(NODE_GENERIC_TYPE_COL))
               .setSpecificType(stmt.columnInt(NODE_SPECIFIC_TYPE_COL))
               .setManufacturerId(stmt.columnInt(NODE_MANUFACTURER_ID_COL))
               .setProductTypeId(stmt.columnInt(NODE_PRODUCT_TYPE_COL))
               .setProductId(stmt.columnInt(NODE_PRODUCT_ID))
               .setHomeId(ZWServices.INSTANCE.getNetwork().getHomeId())
               .addCmdClasses(ByteUtils.string2bytes(stmt.columnString(NODE_CMD_CLASSES)))
               .setOnline(stmt.columnInt(NODE_ONLINE))
               .setOfflineTimeout(stmt.columnInt(NODE_OFFLINE_TIMEOUT))
               .build();
         return node;
      }
   }
}


