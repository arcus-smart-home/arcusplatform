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
import com.iris.agent.db.DbBinder;
import com.iris.agent.zigbee.node.ZBAttribute;
import com.iris.agent.zigbee.node.ZBCluster;
import com.iris.agent.zigbee.node.ZBEndpoint;
import com.iris.agent.zigbee.node.ZBNode;
import com.iris.agent.zigbee.node.ZBProfile;

public class ZBBinders {

   public enum ConfigBinder implements DbBinder<String> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, String value) throws Exception {
         stmt.bind(1, value);
      }
   }

   public enum ConfigInsertBinder implements DbBinder<KeyValuePair> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, KeyValuePair value) throws Exception {
         stmt.bind(1, value.getKey());
         stmt.bind(2, value.getValue());
      }
   }

   public enum DeleteNodeBinder implements DbBinder<ZBNode> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, ZBNode value) throws Exception {
         stmt.bind(1, value.getIeeeAddr());
      }
   }

   public enum CreateNodeBinder implements DbBinder<ZBNode> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, ZBNode value) throws Exception {
         stmt.bind(1, value.getIeeeAddr());
         stmt.bind(2, value.getNwkAddr());
         stmt.bind(3, value.getParentAddr());
         stmt.bind(4, value.getState());
         stmt.bind(5, value.getMaximumIncomingTransferSize());
         stmt.bind(6, value.getMaximumOutgoingTransferSize());
         stmt.bind(7, value.getNodeFlags());
         stmt.bind(8, value.getServerMask());
         stmt.bind(9, value.getManufacturerCode());
         stmt.bind(10, value.getDescriptorCapability());
         stmt.bind(11, value.getMaximumBufferSize());
         stmt.bind(12, value.getMacCapabilityFlags());
         stmt.bind(13, value.getPowerDescriptor());
         stmt.bind(14, value.getDeviceCapability());
         stmt.bind(15, value.isOnline() ? 1 : 0);
         stmt.bind(16, value.getOfflineTimeout());
      }
   }

   public enum UpdateNodeBinder implements DbBinder<ZBNode> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, ZBNode value) throws Exception {
         stmt.bind(1, value.getNwkAddr());
         stmt.bind(2, value.getParentAddr());
         stmt.bind(3, value.getState());
         stmt.bind(4, value.getMaximumIncomingTransferSize());
         stmt.bind(5, value.getMaximumOutgoingTransferSize());
         stmt.bind(6, value.getNodeFlags());
         stmt.bind(7, value.getServerMask());
         stmt.bind(8, value.getManufacturerCode());
         stmt.bind(9, value.getDescriptorCapability());
         stmt.bind(10, value.getMaximumBufferSize());
         stmt.bind(11, value.getMacCapabilityFlags());
         stmt.bind(12, value.getPowerDescriptor());
         stmt.bind(13, value.getDeviceCapability());
         stmt.bind(14, value.isOnline() ? 1 : 0);
         stmt.bind(15, value.getOfflineTimeout());
         stmt.bind(16, value.getIeeeAddr()); // WHERE clause
      }
   }

   // Profile binders

   public enum CreateProfileBinder implements DbBinder<ZBProfile> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, ZBProfile value) throws Exception {
         stmt.bind(1, value.getNodeId());
         stmt.bind(2, value.getProfileId());
      }
   }

   public enum DeleteProfilesByNodeBinder implements DbBinder<Long> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, Long value) throws Exception {
         stmt.bind(1, value);
      }
   }

   // Endpoint binders

   public enum CreateEndpointBinder implements DbBinder<ZBEndpoint> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, ZBEndpoint value) throws Exception {
         stmt.bind(1, value.getProfileDbId());
         stmt.bind(2, value.getEndpointId());
         stmt.bind(3, value.getDeviceId());
         stmt.bind(4, value.getDeviceVersion());
         stmt.bind(5, value.getZclVersion());
         stmt.bind(6, value.getAppVersion());
         stmt.bind(7, value.getStkVersion());
         stmt.bind(8, value.getHwVersion());
         stmt.bind(9, value.getManufacturerName());
         stmt.bind(10, value.getModelIdentifier());
         stmt.bind(11, value.getDateCode());
         stmt.bind(12, value.getPowerSource());
      }
   }

   public enum UpdateEndpointBinder implements DbBinder<ZBEndpoint> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, ZBEndpoint value) throws Exception {
         stmt.bind(1, value.getDeviceId());
         stmt.bind(2, value.getDeviceVersion());
         stmt.bind(3, value.getZclVersion());
         stmt.bind(4, value.getAppVersion());
         stmt.bind(5, value.getStkVersion());
         stmt.bind(6, value.getHwVersion());
         stmt.bind(7, value.getManufacturerName());
         stmt.bind(8, value.getModelIdentifier());
         stmt.bind(9, value.getDateCode());
         stmt.bind(10, value.getPowerSource());
         stmt.bind(11, value.getId()); // WHERE clause
      }
   }

   // Cluster binders

   public enum CreateClusterBinder implements DbBinder<ZBCluster> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, ZBCluster value) throws Exception {
         stmt.bind(1, value.getEndpointDbId());
         stmt.bind(2, value.getClusterId());
         stmt.bind(3, value.isServer() ? 1 : 0);
      }
   }

   public enum DeleteClustersByEndpointBinder implements DbBinder<Long> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, Long value) throws Exception {
         stmt.bind(1, value);
      }
   }

   // Attribute binders

   public enum CreateAttributeBinder implements DbBinder<ZBAttribute> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, ZBAttribute value) throws Exception {
         stmt.bind(1, value.getClusterDbId());
         stmt.bind(2, value.getAttributeId());
         stmt.bind(3, value.getAttributeDt());
         if (value.getLastValue() != null) {
            stmt.bind(4, value.getLastValue());
         } else {
            stmt.bindNull(4);
         }
      }
   }

   public enum UpdateAttributeBinder implements DbBinder<ZBAttribute> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, ZBAttribute value) throws Exception {
         stmt.bind(1, value.getAttributeDt());
         if (value.getLastValue() != null) {
            stmt.bind(2, value.getLastValue());
         } else {
            stmt.bindNull(2);
         }
         stmt.bind(3, value.getId()); // WHERE clause
      }
   }

   public enum DeleteAttributesByClusterBinder implements DbBinder<Long> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, Long value) throws Exception {
         stmt.bind(1, value);
      }
   }

   public enum LongBinder implements DbBinder<Long> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, Long value) throws Exception {
         stmt.bind(1, value);
      }
   }
}
