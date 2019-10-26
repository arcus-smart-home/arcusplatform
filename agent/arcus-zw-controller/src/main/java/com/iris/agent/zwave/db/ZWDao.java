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
import com.iris.agent.zwave.ZWException;
import com.iris.agent.zwave.ZWNetwork;
import com.iris.agent.zwave.node.ZWNode;

public class ZWDao {
   private static final Logger logger = LoggerFactory.getLogger(ZWDao.class);
   
   private static final Object LOCK = new Object();
   private static final Map<String, String> config = Collections.synchronizedMap(new HashMap<>());
   private static final Set<Integer> knownNodes = new HashSet<>();
   
   private static final String NODES_QUERY = "SELECT node_id, basic_type, generic_type, specific_type, man_id, type_id, product_id, cmdclasses, online, offline_timeout FROM zip_node";
   
   private static final String READ_NODE = "SELECT node_id, basic_type, generic_type, specific_type, man_id, type_id, product_id, cmdclasses, online, offline_timeout FROM zip_node WHERE node_id=?";
   private static final String CREATE_NODE = "INSERT INTO zip_node(node_id, basic_type ,generic_type ,specific_type, man_id, type_id ,product_id, cmdclasses, online, offline_timeout) VALUES (?,?,?,?,?,?,?,?,?,?)";
   private static final String UPDATE_NODE = "UPDATE zip_node SET node_id=?, basic_type=?, generic_type=?, specific_type=?, man_id=?, type_id=?, product_id=?, cmdclasses=?, online=?, offline_timeout=? WHERE node_id=?";
   private static final String DELETE_NODE = "DELETE FROM zip_node WHERE node_id=?";
   private static final String DELETE_ALL = "DELETE FROM zip_node";
   
   private static final String CHECK_CONFIG_TABLE = "SELECT name FROM sqlite_master WHERE type='table' and name='zip_config'";
   private static final String CONFIG_QUERY = "SELECT key, value FROM zip_config";
   
   private static final String[] SCHEMA_SCRIPTS = {
         "/sql/zip.sql"
   };
        
   private static Db db;
   
   private static NetworkRecord network;
   
   private ZWDao() {      
   }
   
   //////////////////////////////////////////////////////////////////////////////
   // Startup and shutdown
   /////////////////////////////////////////////////////////////////////////////
   
   static void setupSchema(Db db) {
      int curVersion = 0;

      if(configTableExists(db)) {
         String schema = db.query("SELECT value FROM zip_config WHERE key=?", Binders.ConfigBinder.INSTANCE, "schema", Extractors.ConfigExtractor.INSTANCE);
         Integer schemaver = ConversionService.to(Integer.class, schema);
         if (schemaver != null) {
            curVersion = schemaver;
         }
      }

      for(int i = curVersion; i < SCHEMA_SCRIPTS.length; i++) {
         logger.debug("executing sql script {}", SCHEMA_SCRIPTS[i]);
         if (db != null) {
            db.execute(ZWDao.class.getResource(SCHEMA_SCRIPTS[i]));
         } else {
            throw new RuntimeException("could not start zip dao: null db");
         }
      }
   }

   public static void start() {
      synchronized (LOCK) {
         if (db != null) {
            throw new ZWException("zip dao already started");
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
            logger.info("loaded {} zwave configuration records in {}s", all.size(), String.format("%.3f", elapsed));
         } catch (Exception ex) {
            logger.warn("failed to preload zwave configuration:", ex);
         }
         
         long homeId =        get(NetworkRecord.CONFIG_HOMEID, Long.class, ZWNetwork.NO_HOME_ID).longValue();
         long networkKeyMsb = get(NetworkRecord.CONFIG_NWKKEYMSB, Long.class, ZWNetwork.NO_NETWORK_KEY_MSB).longValue();
         long networkKeyLsb = get(NetworkRecord.CONFIG_NWKKEYLSB, Long.class, ZWNetwork.NO_NETWORK_KEY_LSB).longValue();

         network = new NetworkRecord(homeId, networkKeyMsb, networkKeyLsb);
      }
   }

   public static void shutdown() {
      synchronized (LOCK) {
         db = null;
         config.clear();
      }
   }

   static void drop() {
      get().execute("DROP TABLE IF EXISTS zip_config");
      get().execute("DROP TABLE IF EXISTS zip_node");
   }
   
   
   /////////////////////////////////////////////////////////////////////////////
   // Configuration support
   /////////////////////////////////////////////////////////////////////////////
   
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
      return DbService.get().queryAll(CONFIG_QUERY, Extractors.ConfigAllExtractor.INSTANCE);
   }
   
   static <T> void put(String key, T value) {
      String svalue = ConversionService.from(value);

      // write through cache
      config.put(key,svalue);

      KeyValuePair pair = new KeyValuePair(key,svalue);
      DbService.get().execute("INSERT OR REPLACE INTO zip_config (key,value) VALUES (?,?)", Binders.ConfigInsertBinder.INSTANCE, pair);
   }
   
   static <T> void put(Db db, String key, T value) {
      String svalue = ConversionService.from(value);
      KeyValuePair pair = new KeyValuePair(key,svalue);
      db.execute("INSERT OR REPLACE INTO zip_config (key,value) VALUES (?,?)", Binders.ConfigInsertBinder.INSTANCE, pair);
   }
   
   static Db get() {
      if (db == null) {
         throw new ZWException("zip dao not started");
      }
      return db;
   }
   
   static <T> T get(String key, Class<T> type) {
      String result = config.get(key);
      return ConversionService.to(type, result);
   }
   
   static <T> T get(String key, Class<T> type, T def) {
      T result = get(key, type);
      return (result == null) ? def : result;
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // Network persistence support
   /////////////////////////////////////////////////////////////////////////////
   
   public static NetworkRecord getNetwork() {
      if (network == null) {
         throw new ZWException("zip dao not started");
      }
      return network;
   }
   
   public static void updateHomeId(long homeId) {
      network.setHomeId(homeId);
      put(db, NetworkRecord.CONFIG_HOMEID, network.getHomeId());
   }
   
   /*
   public static void updateNetwork() {
      synchronized (LOCK) {
         if (network.needsUpdate()) {
            Db db = get();
            
            put(db, NetworkRecord.CONFIG_NWKKEYLSB, network.getNetworkKeyLsb());
            put(db, NetworkRecord.CONFIG_NWKKEYMSB, network.getNetworkKeyMsb());
            network.updated();
         }import com.iris.bootstrap.guice.Binders;

      }
   }
   */
   
   public static void deleteNetwork() {
      put(NetworkRecord.CONFIG_HOMEID, null);
      network.invalidate();
      knownNodes.clear();
      get().execute(DELETE_ALL);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // Node persistence support
   /////////////////////////////////////////////////////////////////////////////
   
   public static List<ZWNode> getAllNodes() {
      List<ZWNode> nodes =  DbService.get().queryAll(NODES_QUERY, Extractors.NodeExtractor.INSTANCE);
      if (nodes != null) {
         knownNodes.clear();
         nodes.forEach(n -> knownNodes.add(n.getNodeId()));
      }
      return nodes;
   }
   
   public static ZWNode getNode() {
      return DbService.get().query(READ_NODE, Extractors.NodeExtractor.INSTANCE);
   }
   
   public static void saveNode(ZWNode node) {
      if (knownNodes.contains(node.getNodeId())) {
         updateNode(node);
      }
      else {
         createNode(node);
      }
   }
   
   public static void createNode(ZWNode node) {
      Db db = get();
      db.execute(CREATE_NODE, Binders.CreateNodeBinder.INSTANCE, node);
      knownNodes.add(node.getNodeId());
   }
   
   public static void updateNode(ZWNode node) {
      Db db = get();
      db.execute(UPDATE_NODE, Binders.UpdateNodeBinder.INSTANCE, node);
   }
   
   public static void deleteNode(ZWNode node) {
      knownNodes.remove(node.getNodeId());
      Db db = get();
      db.execute(DELETE_NODE, Binders.DeleteNodeBinder.INSTANCE, node);
   }
}

