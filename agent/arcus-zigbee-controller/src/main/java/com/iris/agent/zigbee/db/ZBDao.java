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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteStatement;
import com.iris.agent.config.ConversionService;
import com.iris.agent.db.Db;
import com.iris.agent.db.DbExtractor;
import com.iris.agent.db.DbService;
import com.iris.agent.zigbee.node.ZBAttribute;
import com.iris.agent.zigbee.node.ZBCluster;
import com.iris.agent.zigbee.node.ZBEndpoint;
import com.iris.agent.zigbee.node.ZBNode;
import com.iris.agent.zigbee.node.ZBProfile;

public class ZBDao {
   private static final Logger logger = LoggerFactory.getLogger(ZBDao.class);

   private static final Object LOCK = new Object();
   private static final Map<String, String> config = Collections.synchronizedMap(new HashMap<>());
   private static final Set<Long> knownNodes =
         java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

   private static final String NODES_QUERY = "SELECT ieeeAddr, nwkAddr, parentAddr, state, " +
         "maximumIncomingTransferSize, maximumOutgoingTransferSize, nodeFlags, serverMask, " +
         "manufacturerCode, descriptorCapability, maximumBufferSize, macCapabilityFlags, " +
         "powerDescriptor, deviceCapability, online, offlineTimeout FROM zigbee_node";

   private static final String READ_NODE = NODES_QUERY + " WHERE ieeeAddr=?";

   private static final String CREATE_NODE = "INSERT OR REPLACE INTO zigbee_node(ieeeAddr, nwkAddr, parentAddr, state, " +
         "maximumIncomingTransferSize, maximumOutgoingTransferSize, nodeFlags, serverMask, " +
         "manufacturerCode, descriptorCapability, maximumBufferSize, macCapabilityFlags, " +
         "powerDescriptor, deviceCapability, online, offlineTimeout) " +
         "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

   private static final String UPDATE_NODE = "UPDATE zigbee_node SET nwkAddr=?, parentAddr=?, state=?, " +
         "maximumIncomingTransferSize=?, maximumOutgoingTransferSize=?, nodeFlags=?, serverMask=?, " +
         "manufacturerCode=?, descriptorCapability=?, maximumBufferSize=?, macCapabilityFlags=?, " +
         "powerDescriptor=?, deviceCapability=?, online=?, offlineTimeout=? WHERE ieeeAddr=?";

   private static final String DELETE_NODE = "DELETE FROM zigbee_node WHERE ieeeAddr=?";
   private static final String DELETE_ALL_NODES = "DELETE FROM zigbee_node";

   // Profile queries
   private static final String PROFILES_BY_NODE = "SELECT id, nodeId, profileId FROM zigbee_profile WHERE nodeId=?";
   private static final String CREATE_PROFILE = "INSERT INTO zigbee_profile(nodeId, profileId) VALUES (?,?)";
   private static final String DELETE_PROFILES_BY_NODE = "DELETE FROM zigbee_profile WHERE nodeId=?";
   private static final String LAST_INSERT_ROWID = "SELECT last_insert_rowid()";

   // Endpoint queries
   private static final String ENDPOINTS_BY_PROFILE = "SELECT id, profileId, endpointId, deviceId, deviceVersion, " +
         "zclVersion, appVersion, stkVersion, hwVersion, manufacturerName, modelIdentifier, dateCode, powerSource " +
         "FROM zigbee_endpoint WHERE profileId=?";
   private static final String CREATE_ENDPOINT = "INSERT INTO zigbee_endpoint(profileId, endpointId, deviceId, deviceVersion, " +
         "zclVersion, appVersion, stkVersion, hwVersion, manufacturerName, modelIdentifier, dateCode, powerSource) " +
         "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
   private static final String UPDATE_ENDPOINT = "UPDATE zigbee_endpoint SET deviceId=?, deviceVersion=?, " +
         "zclVersion=?, appVersion=?, stkVersion=?, hwVersion=?, manufacturerName=?, modelIdentifier=?, " +
         "dateCode=?, powerSource=? WHERE id=?";

   // Cluster queries
   private static final String CLUSTERS_BY_ENDPOINT = "SELECT id, endpointId, clusterId, server FROM zigbee_cluster WHERE endpointId=?";
   private static final String CREATE_CLUSTER = "INSERT INTO zigbee_cluster(endpointId, clusterId, server) VALUES (?,?,?)";
   private static final String DELETE_CLUSTERS_BY_ENDPOINT = "DELETE FROM zigbee_cluster WHERE endpointId=?";

   // Attribute queries
   private static final String ATTRIBUTES_BY_CLUSTER = "SELECT id, clusterId, attributeId, attributeDt, attributeLastValue " +
         "FROM zigbee_attribute WHERE clusterId=?";
   private static final String CREATE_ATTRIBUTE = "INSERT INTO zigbee_attribute(clusterId, attributeId, attributeDt, attributeLastValue) " +
         "VALUES (?,?,?,?)";
   private static final String UPDATE_ATTRIBUTE = "UPDATE zigbee_attribute SET attributeDt=?, attributeLastValue=? WHERE id=?";
   private static final String DELETE_ATTRIBUTES_BY_CLUSTER = "DELETE FROM zigbee_attribute WHERE clusterId=?";

   private static final String CHECK_CONFIG_TABLE = "SELECT name FROM sqlite_master WHERE type='table' and name='zigbee_config'";
   private static final String CONFIG_QUERY = "SELECT key, value FROM zigbee_config";

   private static final String[] SCHEMA_SCRIPTS = {
         "/sql/zigbee.sql"
   };

   private static Db db;

   private ZBDao() {}

   static void setupSchema(Db db) {
      int curVersion = 0;

      if (configTableExists(db)) {
         String schema = db.query("SELECT value FROM zigbee_config WHERE key=?",
               ZBBinders.ConfigBinder.INSTANCE, "schema", ZBExtractors.ConfigExtractor.INSTANCE);
         Integer schemaver = ConversionService.to(Integer.class, schema);
         if (schemaver != null) {
            curVersion = schemaver;
         }
      }

      for (int i = curVersion; i < SCHEMA_SCRIPTS.length; i++) {
         logger.debug("executing sql script {}", SCHEMA_SCRIPTS[i]);
         if (db != null) {
            db.execute(ZBDao.class.getResource(SCHEMA_SCRIPTS[i]));
         } else {
            throw new RuntimeException("could not start zigbee dao: null db");
         }
      }
   }

   public static void start() {
      synchronized (LOCK) {
         if (db != null) {
            throw new RuntimeException("zigbee dao already started");
         }

         db = DbService.get();
         setupSchema(db);

         try {
            long start = System.nanoTime();
            List<KeyValuePair> all = getConfig();
            for (KeyValuePair kv : all) {
               config.put(kv.getKey(), kv.getValue());
            }
            double elapsed = (System.nanoTime() - start) / 1000000000.0;
            logger.info("loaded {} zigbee configuration records in {}s", all.size(), String.format("%.3f", elapsed));
         } catch (Exception ex) {
            logger.warn("failed to preload zigbee configuration:", ex);
         }
      }
   }

   public static void shutdown() {
      synchronized (LOCK) {
         db = null;
         config.clear();
      }
   }

   static boolean configTableExists(Db db) {
      String name = db.query(CHECK_CONFIG_TABLE, new DbExtractor<String>() {
         @Override
         public String extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
            return stmt.columnString(0);
         }
      });
      return name != null;
   }

   public static List<KeyValuePair> getConfig() {
      return DbService.get().queryAll(CONFIG_QUERY, ZBExtractors.ConfigAllExtractor.INSTANCE);
   }

   static Db get() {
      if (db == null) {
         throw new RuntimeException("zigbee dao not started");
      }
      return db;
   }

   public static <T> void put(String key, T value) {
      String svalue = ConversionService.from(value);
      config.put(key, svalue);
      KeyValuePair pair = new KeyValuePair(key, svalue);
      DbService.get().execute("INSERT OR REPLACE INTO zigbee_config (key,value) VALUES (?,?)",
            ZBBinders.ConfigInsertBinder.INSTANCE, pair);
   }

   public static <T> T get(String key, Class<T> type) {
      String result = config.get(key);
      return ConversionService.to(type, result);
   }

   public static <T> T get(String key, Class<T> type, T def) {
      T result = get(key, type);
      return (result == null) ? def : result;
   }

   // Node persistence

   public static List<ZBNode> getAllNodes() {
      List<ZBNode> nodes = DbService.get().queryAll(NODES_QUERY, ZBExtractors.NodeExtractor.INSTANCE);
      if (nodes != null) {
         knownNodes.clear();
         nodes.forEach(n -> knownNodes.add(n.getIeeeAddr()));
      }
      return nodes;
   }

   public static ZBNode getNode(long ieeeAddr) {
      return DbService.get().query(READ_NODE, ZBBinders.ConfigBinder.INSTANCE,
            String.valueOf(ieeeAddr), ZBExtractors.NodeExtractor.INSTANCE);
   }

   public static void saveNode(ZBNode node) {
      if (knownNodes.contains(node.getIeeeAddr())) {
         updateNode(node);
      } else {
         createNode(node);
      }
   }

   public static void createNode(ZBNode node) {
      get().execute(CREATE_NODE, ZBBinders.CreateNodeBinder.INSTANCE, node);
      knownNodes.add(node.getIeeeAddr());
   }

   public static void updateNode(ZBNode node) {
      get().execute(UPDATE_NODE, ZBBinders.UpdateNodeBinder.INSTANCE, node);
   }

   public static void deleteNode(ZBNode node) {
      knownNodes.remove(node.getIeeeAddr());
      get().execute(DELETE_NODE, ZBBinders.DeleteNodeBinder.INSTANCE, node);
   }

   public static void deleteAllNodes() {
      knownNodes.clear();
      get().execute(DELETE_ALL_NODES);
   }

   // Profile persistence

   public static List<ZBProfile> getProfilesByNode(long nodeId) {
      return DbService.get().queryAll(PROFILES_BY_NODE, ZBBinders.LongBinder.INSTANCE,
            nodeId, ZBExtractors.ProfileExtractor.INSTANCE);
   }

   public static long createProfile(ZBProfile profile) {
      get().execute(CREATE_PROFILE, ZBBinders.CreateProfileBinder.INSTANCE, profile);
      Long rowId = get().query(LAST_INSERT_ROWID, ZBExtractors.LongExtractor.INSTANCE);
      if (rowId != null) {
         profile.setId(rowId);
      }
      return rowId != null ? rowId : 0;
   }

   public static void deleteProfilesByNode(long nodeId) {
      get().execute(DELETE_PROFILES_BY_NODE, ZBBinders.DeleteProfilesByNodeBinder.INSTANCE, nodeId);
   }

   // Endpoint persistence

   public static List<ZBEndpoint> getEndpointsByProfile(long profileDbId) {
      return DbService.get().queryAll(ENDPOINTS_BY_PROFILE, ZBBinders.LongBinder.INSTANCE,
            profileDbId, ZBExtractors.EndpointExtractor.INSTANCE);
   }

   public static long createEndpoint(ZBEndpoint endpoint) {
      get().execute(CREATE_ENDPOINT, ZBBinders.CreateEndpointBinder.INSTANCE, endpoint);
      Long rowId = get().query(LAST_INSERT_ROWID, ZBExtractors.LongExtractor.INSTANCE);
      if (rowId != null) {
         endpoint.setId(rowId);
      }
      return rowId != null ? rowId : 0;
   }

   public static void updateEndpoint(ZBEndpoint endpoint) {
      get().execute(UPDATE_ENDPOINT, ZBBinders.UpdateEndpointBinder.INSTANCE, endpoint);
   }

   // Cluster persistence

   public static List<ZBCluster> getClustersByEndpoint(long endpointDbId) {
      return DbService.get().queryAll(CLUSTERS_BY_ENDPOINT, ZBBinders.LongBinder.INSTANCE,
            endpointDbId, ZBExtractors.ClusterExtractor.INSTANCE);
   }

   public static long createCluster(ZBCluster cluster) {
      get().execute(CREATE_CLUSTER, ZBBinders.CreateClusterBinder.INSTANCE, cluster);
      Long rowId = get().query(LAST_INSERT_ROWID, ZBExtractors.LongExtractor.INSTANCE);
      if (rowId != null) {
         cluster.setId(rowId);
      }
      return rowId != null ? rowId : 0;
   }

   public static void deleteClustersByEndpoint(long endpointDbId) {
      get().execute(DELETE_CLUSTERS_BY_ENDPOINT, ZBBinders.DeleteClustersByEndpointBinder.INSTANCE, endpointDbId);
   }

   // Attribute persistence

   public static List<ZBAttribute> getAttributesByCluster(long clusterDbId) {
      return DbService.get().queryAll(ATTRIBUTES_BY_CLUSTER, ZBBinders.LongBinder.INSTANCE,
            clusterDbId, ZBExtractors.AttributeExtractor.INSTANCE);
   }

   public static long createAttribute(ZBAttribute attribute) {
      get().execute(CREATE_ATTRIBUTE, ZBBinders.CreateAttributeBinder.INSTANCE, attribute);
      Long rowId = get().query(LAST_INSERT_ROWID, ZBExtractors.LongExtractor.INSTANCE);
      if (rowId != null) {
         attribute.setId(rowId);
      }
      return rowId != null ? rowId : 0;
   }

   public static void updateAttribute(ZBAttribute attribute) {
      get().execute(UPDATE_ATTRIBUTE, ZBBinders.UpdateAttributeBinder.INSTANCE, attribute);
   }

   public static void deleteAttributesByCluster(long clusterDbId) {
      get().execute(DELETE_ATTRIBUTES_BY_CLUSTER, ZBBinders.DeleteAttributesByClusterBinder.INSTANCE, clusterDbId);
   }
}
