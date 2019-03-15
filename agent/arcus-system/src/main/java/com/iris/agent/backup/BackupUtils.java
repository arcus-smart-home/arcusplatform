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
package com.iris.agent.backup;

import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteStatement;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iris.agent.config.ConversionService;
import com.iris.agent.db.Db;
import com.iris.agent.db.DbBinder;
import com.iris.agent.db.DbExtractor;
import com.iris.agent.db.DbService;
import com.iris.agent.db.DbUtils;

import io.netty.buffer.Unpooled;

public final class BackupUtils {
   private static final Logger log = LoggerFactory.getLogger(BackupUtils.class);

   private BackupUtils() {
   }

   /////////////////////////////////////////////////////////////////////////////
   // Json Utilities
   /////////////////////////////////////////////////////////////////////////////

   @Nullable
   public static String getString(JsonObject data, String name) {
      JsonElement elem = data.get(name);
      if (elem == null || elem.isJsonNull()) {
         return null;
      }

      return elem.getAsString();
   }

   @Nullable
   public static List<String> getStringArray(JsonObject data, String name) {
      JsonElement elem = data.get(name);
      if (elem == null) {
         return null;
      }

      if (!elem.isJsonArray()) {
         return null;
      }

      List<String> result = new ArrayList<>();
      for (JsonElement e : elem.getAsJsonArray()) {
         result.add(e.getAsString());
      }

      return result;
   }

   @Nullable
   public static Long getV1Ui64(JsonObject data, String name) {
      return getV1Ui64(getString(data, name));
   }

   @Nullable
   public static Long getV1Ui64(@Nullable String ui64) {
      String eui64Str = (ui64 == null) ? null : ui64.replaceAll("[-:]", "").trim();
      return (eui64Str == null) ? null : new BigInteger(eui64Str,16).longValue() & 0xFFFFFFFFFFFFFFFFL;
   }

   @Nullable
   public static byte[] getBase64(JsonObject data, String name) {
      String base64 = getString(data, name);
      if (base64 == null) {
         return null;
      }

      return Base64.decodeBase64(base64);
   }

   @Nullable
   public static Long getBase64Long(JsonObject data, String name, ByteOrder order) {
      byte[] base64 = getBase64(data, name);
      return Unpooled.wrappedBuffer(base64).order(order).getLong(0);
   }

   @Nullable
   public static Boolean getBoolean(JsonObject data, String name) {
      JsonElement elem = data.get(name);
      if (elem == null || elem.isJsonNull()) {
         return null;
      }

      return elem.getAsBoolean();
   }

   public static Integer getInt(JsonObject data, String name, Integer def) {
      Integer result = getInt(data, name);
      return (result == null) ? def : result;
   }

   @Nullable
   public static Integer getInt(JsonObject data, String name) {
      JsonElement elem = data.get(name);
      if (elem == null || elem.isJsonNull()) {
         return null;
      }

      return elem.getAsInt();
   }

   @Nullable
   public static Long getLong(JsonObject data, String name) {
      JsonElement elem = data.get(name);
      if (elem == null || elem.isJsonNull()) {
         return null;
      }

      return elem.getAsLong();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Iris V1 Migration Data
   /////////////////////////////////////////////////////////////////////////////

   public static Map<String,Map<String,String>> getDevicesConf(JsonObject data) {
      String devicesConf = BackupUtils.getString(data, "devices-conf");
      if (devicesConf == null) {
         return Collections.emptyMap();
      }

      Map<String,Map<String,String>> result = new LinkedHashMap<>();
      Map<String,String> current = null;

      String[] lines = devicesConf.split("\\r?\\n");
      for (int i = 0; i < lines.length; i++) {
         String line = StringUtils.trim(lines[i]);

         if (StringUtils.startsWith(line,"[") && StringUtils.endsWith(line,"]")) {
            String title = line.substring(1, line.length() - 1);
            int idx = title.indexOf('=');
            if (idx >= 0) {
               String type = StringUtils.trim(title.substring(0,idx));
               String name = StringUtils.trim(title.substring(idx+1));

               current = new HashMap<>();
               current.put("type", type.trim().toLowerCase());
               result.put(name,current);
            }
         } else if (current != null) {
            int idx = line.indexOf('=');
            if (idx >= 0) {
               String key = StringUtils.trim(line.substring(0,idx));
               String val = StringUtils.trim(line.substring(idx+1));
               current.put(key.trim().toLowerCase(),val.trim());
            }
         }
      }

      return result;
   }

   public static Map<Long,Map<String,String>> getDevices(JsonArray data) {
      Map<Long,Map<String,String>> result = new LinkedHashMap<>();
      for (JsonElement elem : data) {
         JsonObject dev = elem.getAsJsonObject();

         Long id = getV1Ui64(dev, "id");
         if (id == null) {
            log.warn("device missing identifier: {}", dev);
            continue;
         }

         Boolean isGeneric = getBoolean(dev, "isGeneric");
         if (isGeneric == null) {
            isGeneric = false;
         }

         String model = getString(dev, "model");
         if (model == null) {
            model = "unknown";
         }

         String devType = getString(dev, "type");
         if (devType == null) {
            log.warn("unknown device type: {}", dev);
            if (isGeneric) {
               devType = "generic";
            } else {
               devType = "unknown";
            }
         }

         List<String> protocols = getStringArray(dev, "protocol");
         if (protocols == null) {
            protocols = Collections.emptyList();
         }

         Boolean isZWave = getBoolean(dev, "isZwave");
         if (isZWave == null) {
            isZWave = false;
         }

         String type;
         if (isZWave) {
            type = "zwave";
         } else if ((!isZWave && protocols.contains("zigbee")) || (!isZWave && protocols.contains("zwave"))) {
            type = "zigbee";
         } else if (!isZWave && protocols.contains("wifi")) {
            type = "wifi";
         } else if ("Repeater".equalsIgnoreCase(devType)) {
            type = "zigbee";
         } else {
            type = "unknown";
         }

         if (isGeneric && !model.startsWith("00900001")) {
            type = "unknown";
         }

         String name = getString(dev, "name");
         Boolean online = getBoolean(dev, "presence");

         Map<String,String> props = new HashMap<>();
         result.put(id, props);

         props.put("type", type);
         props.put("devType", devType);
         props.put("online", (online == null) ? "true" : String.valueOf(online));
         props.put("name", (name == null) ? "unknown" : name);
         props.put("model", model);
      }

      return result;
   }

   public static Map<String,JsonObject> getMigrationInfo(JsonArray data, String protocol) {
      Map<String,JsonObject> result = new HashMap<>();
      for (JsonElement elem : data) {
         JsonObject info = elem.getAsJsonObject();

         String type = getString(info, "type");
         if (type == null || type.trim().isEmpty()) {
            continue;
         }

         String infoProtocol = getString(info, "protocol");
         if (infoProtocol == null || !protocol.equals(infoProtocol)) {
            continue;
         }

         result.put(type, info);
      }

      return result;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Migration table support
   /////////////////////////////////////////////////////////////////////////////

   public static void putName(Db db, String key, @Nullable String name) {
      if (name != null)  {
         put(db, "migration", key, name);
      }
   }

   @Nullable
   public static String getName(String key) {
      return getName(DbService.get(), key);
   }

   @Nullable
   public static String getName(Db db, String key) {
      return get(db, "migration", key, String.class);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Config table support
   /////////////////////////////////////////////////////////////////////////////

   public static <T> void put(Db db, String tableName, String key, @Nullable T value) {
      String svalue = ConversionService.from(value);
      KeyValuePair pair = new KeyValuePair(key,svalue);
      db.execute("INSERT OR REPLACE INTO " + tableName + " (key,value) VALUES (?,?)", ConfigInsertBinder.INSTANCE, pair);
   }

   @Nullable
   public static <T> T get(Db db, String tableName, String key, Class<T> type) {
      String result = db.query("SELECT value FROM " + tableName + " WHERE key=?", ConfigBinder.INSTANCE, key, ConfigExtractor.INSTANCE);
      return ConversionService.to(type, result);
   }

   public static <T> T get(Db db, String tableName, String key, Class<T> type, T def) {
      T result = get(db, tableName, key, type);
      return (result == null) ? def : result;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Schema management
   /////////////////////////////////////////////////////////////////////////////

   @SuppressWarnings("null")
   public static boolean configTableExists(Db db, String tableName) {
      String name = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'",
         new DbExtractor<String>() {
            @Override
            @Nullable
            public String extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
               return stmt.columnString(0);
            }
         }
      );

      return name != null;
   }

   public static void setupSchema(Db db, String tableName, String[] schemaScripts, Class<?> loader) {
      int curVersion = 0;
      if(configTableExists(db, tableName)) {
         curVersion = get(db, tableName, "schema", Integer.class, 0);
      }

      for(int i = curVersion; i < schemaScripts.length; i++) {
         log.debug("executing sql script {}", schemaScripts[i]);
         db.execute(loader.getResource(schemaScripts[i]));
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Implementation details
   /////////////////////////////////////////////////////////////////////////////

   private static final class KeyValuePair {
      private final String key;

      @Nullable
      private final String value;

      private KeyValuePair(String key, @Nullable String value) {
         this.key = key;
         this.value = value;
      }
   }

   private static enum ConfigBinder implements DbBinder<String> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, String value) throws Exception {
         stmt.bind(1, value);
      }
   }

   private static enum ConfigInsertBinder implements DbBinder<KeyValuePair> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, KeyValuePair pair) throws Exception {
         stmt.bind(1, pair.key);
         DbUtils.bind(stmt, pair.value, 2);
      }
   }

   private static enum ConfigExtractor implements DbExtractor<String> {
      INSTANCE;

      @Override
      public String extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         return stmt.columnString(0);
      }
   }
}

