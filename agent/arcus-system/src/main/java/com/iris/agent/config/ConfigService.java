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
package com.iris.agent.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteStatement;
import com.google.common.base.Supplier;
import com.iris.agent.db.Db;
import com.iris.agent.db.DbBinder;
import com.iris.agent.db.DbExtractor;
import com.iris.agent.db.DbService;
import com.iris.agent.db.DbUtils;

public final class ConfigService {
   private static final Logger log = LoggerFactory.getLogger(ConfigService.class);
   private static final Object START_LOCK = new Object();

   private static final String CHECK_CONFIG_TABLE = "SELECT name FROM sqlite_master WHERE type='table' AND name='config'";

   private static final String[] SCHEMA_SCRIPTS = {
      "/sql/config.sql",
      "/sql/update-config-1.sql",
   };

   private static boolean configStarted = false;

   @SuppressWarnings("null")
   private static Map<String,String> defaults;
   private static final Map<String,ValueWithTime<String>> config = Collections.synchronizedMap(new HashMap<>());

   private ConfigService() {
   }

   @SuppressWarnings("null")
   private static boolean configTableExists(Db db) {
      String name = db.query(CHECK_CONFIG_TABLE, new DbExtractor<String>() {
         @Override
         @Nullable
         public String extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
            return stmt.columnString(0);
         }
      });
      return name != null;
   }

   public static void setupSchema(Db db) {
      int curVersion = 0;

      if(configTableExists(db)) {
         curVersion = get(db, "schema", Integer.class, 0);
      }

      for(int i = curVersion; i < SCHEMA_SCRIPTS.length; i++) {
         log.debug("executing sql script {}", SCHEMA_SCRIPTS[i]);
         db.execute(ConfigService.class.getResource(SCHEMA_SCRIPTS[i]));
      }
   }

   public static void start(Set<File> configs) {
      synchronized (START_LOCK) {
         if (configStarted) {
            throw new IllegalStateException("configuration service already started");
         }

         // Read the default configuration
         defaults = mergeProperties(configs);

         // Run database initialization
         setupSchema(DbService.get());

         // Preload all config
         try {
            long start = System.nanoTime();
            List<KeyValuePair> kvs = allWithTime();
            for (KeyValuePair kv : kvs) {
               config.putIfAbsent(kv.key, kv.value);
            }
            double elapsed = (System.nanoTime() - start) / 1000000000.0;
            log.info("loaded {} configuration records in {}s", kvs.size(), String.format("%.3f", elapsed));
         } catch (Exception ex) {
            log.warn("failed to preload configuration:", ex);
         }
      }
   }

   public static void shutdown() {
      synchronized (START_LOCK) {
         config.clear();
      }
   }

   private static Map<String,String> mergeProperties(Set<File> configs) {
      Map<String,String> result = new HashMap<>();

      for(File f : configs) {
         result.putAll(loadProperties(f));
      }

      for (String key : System.getProperties().stringPropertyNames()) {
         result.put(key, System.getProperty(key));
      }

      for (Map.Entry<String,String> entry : System.getenv().entrySet()) {
         String key = entry.getKey();
         String value = entry.getValue();
         if (key == null) continue;

         String trimKey = key.trim();
         if (trimKey.isEmpty()) continue;

         result.put(trimKey, value);

         String dottedKey = trimKey.replaceAll("_+", ".").toLowerCase();
         result.put(dottedKey, value);
      }

      if (log.isDebugEnabled()) {
         for (Map.Entry<String,String> entry : new TreeMap<>(result).entrySet()) {
            log.debug("default configuration: {} -> {}", entry.getKey(), entry.getValue());
         }
      }

      return result;
   }

   private static Map<String,String> loadProperties(File file) {
      Properties properties = new Properties();

      try(FileInputStream fis = new FileInputStream(file)) {
         properties.load(fis);
      } catch (IOException ex) {
         log.warn("failed to load properties from configuratino file {}: {}", file, ex.getMessage(), ex);
      }

      Map<String,String> result = new HashMap<>();
      for (String key : properties.stringPropertyNames()) {
         result.put(key, properties.getProperty(key));
      }

      return result;
   }

   /////////////////////////////////////////////////////////////////////////////
   // High-level support
   /////////////////////////////////////////////////////////////////////////////

   public static <T> void put(String key, @Nullable T value) {
      put(key, value, System.currentTimeMillis(), Long.MIN_VALUE);
   }

   public static <T> void put(String key, @Nullable T value, long lastUpdateTime, long lastReportTime) {
      String svalue = ConversionService.from(value);
      ValueWithTime<String> vwt = new ValueWithTime<String>(svalue,lastUpdateTime,lastReportTime);

      // write through cache
      config.put(key, vwt);

      put(DbService.get(), key, vwt);
   }

   @SuppressWarnings("null")
   public static <T> T get(String key, Class<T> type) {
      return ConversionService.to(type, get(key));
   }

   public static <T> T get(String key, Class<T> type, @Nullable T def) {
      String value = get(key);
      if (value == null) return def;

      return ConversionService.to(type, value);
   }

   public static String get(String key) {
      ValueWithTime<String> vwt = getWithTime(key);

      String result = (vwt != null) ? vwt.value : null;
      if (result == null) {
         result = defaults.get(key);
      }

      return result;
   }

   public static <T> ValueWithTime<T> getWithTime(String key, Class<T> type) {
      ValueWithTime<String> result = getWithTime(key);
      if (result == null) return null;

      return new ValueWithTime(ConversionService.to(type, result.value), result.lastUpdateTime, result.lastReportTime);
   }

   public static <T> ValueWithTime<T> getWithTime(String key, Class<T> type, @Nullable T def) {
      ValueWithTime<T> result = getWithTime(key, type);
      return (result == null) ? new ValueWithTime(def,Long.MIN_VALUE,Long.MIN_VALUE) : result;
   }

   public static ValueWithTime<String> getWithTime(String key) {
      return config.get(key);
   }

   public static Supplier<String> supplier(String key) {
      return supplier(key, null);
   }

   public static Supplier<String> supplier(String key, @Nullable String def) {
      return supplier(key, String.class, def);
   }

   public static <T> Supplier<T> supplier(String key, Class<T> type, @Nullable T def) {
      return new ConfigSupplier<T>(key, type, def);
   }

   public static Map<String,String> all() {
      return all(true);
   }

   public static Map<String,String> all(boolean withDefaults) {
      Map<String,String> result = new HashMap<>();
      if (withDefaults) {
         result.putAll(defaults);
      }

      List<KeyValue> all = DbService.get().queryAll("SELECT key,value FROM config", ConfigAllExtractor.INSTANCE);
      for (KeyValue kv : all) {
         result.put(kv.key, kv.value);
      }

      return result;
   }

   private static List<KeyValuePair> allWithTime() {
      return DbService.get().queryAll("SELECT key,value,lastUpdateTime,lastReportTime FROM config", ConfigAllWithTimeExtractor.INSTANCE);
   }

   /////////////////////////////////////////////////////////////////////////////
   // High-level support with custom db
   /////////////////////////////////////////////////////////////////////////////

   public static <T> void put(Db db, String key, @Nullable T value) {
      put(db, key, value, System.currentTimeMillis(), Long.MIN_VALUE);
   }

   public static <T> void put(Db db, String key, @Nullable T value, long lastUpdateTime, long lastReportTime) {
      String svalue = ConversionService.from(value);
      put(db, key,new ValueWithTime<String>(svalue,lastUpdateTime,lastReportTime));
   }

   private static void put(Db db, String key, @Nullable ValueWithTime<String> value) {
      KeyValuePair pair = new KeyValuePair(key,value);
      db.execute("INSERT OR REPLACE INTO config (key,value,lastUpdateTime,lastReportTime) VALUES (?,?,?,?)", InsertBinder.INSTANCE, pair);
   }

   @SuppressWarnings("null")
   public static <T> T get(Db db, String key, Class<T> type) {
      return ConversionService.to(type, get(db,key));
   }

   @SuppressWarnings("null")
   public static <T> T get(Db db, String key, Class<T> type, @Nullable T def) {
      T result = get(db, key, type);
      return (result == null) ? def : result;
   }

   @SuppressWarnings({"unused", "null" })
   public static String get(Db db, String key) {
      String result = db.query("SELECT value FROM config WHERE key=?", ConfigBinder.INSTANCE, key, ConfigExtractor.INSTANCE);
      if (result == null) {
         result = defaults.get(key);
      }

      return result;
   }

   @SuppressWarnings("null")
   public static <T> ValueWithTime<T> getWithTime(Db db, String key, Class<T> type) {
      ValueWithTime<String> result = getWithTime(db,key);
      if (result == null) return null;

      return new ValueWithTime(ConversionService.to(type, result.value), result.lastUpdateTime, result.lastReportTime);
   }

   @SuppressWarnings("null")
   public static <T> ValueWithTime<T> getWithTime(Db db, String key, Class<T> type, @Nullable T def) {
      ValueWithTime<T> result = getWithTime(db, key, type);
      return (result == null) ? new ValueWithTime(def,Long.MIN_VALUE,Long.MIN_VALUE) : result;
   }

   @SuppressWarnings({"unused", "null" })
   public static ValueWithTime<String> getWithTime(Db db, String key) {
      ValueWithTime<String> result = db.query("SELECT value,lastUpdateTime,lastReportTime FROM config WHERE key=?", ConfigBinder.INSTANCE, key, ConfigExtractorWithTime.INSTANCE);
      if (result == null) {
         String def = defaults.get(key);
         if (def != null) {
            return new ValueWithTime(def, Long.MIN_VALUE, Long.MIN_VALUE);
         }
      }

      return result;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Implementation details
   /////////////////////////////////////////////////////////////////////////////

   public static final class ValueWithTime<T> {
      public final T value;
      public final long lastUpdateTime;
      public final long lastReportTime;

      public ValueWithTime(T value, long lastUpdateTime, long lastReportTime) {
         this.value = value;
         this.lastUpdateTime = lastUpdateTime;
         this.lastReportTime = lastReportTime;
      }
   }

   private static final class ConfigSupplier<T> implements Supplier<T> {
      private final String key;
      private final Class<T> type;
      private final @Nullable T defaultValue;

      public ConfigSupplier(String key, Class<T> type, @Nullable T defaultValue) {
         this.key = key;
         this.type = type;
         this.defaultValue = defaultValue;
      }

      @Override
      public T get() {
         return ConfigService.get(key, type, defaultValue);
      }
   }

   private static final class KeyValuePair {
      private final String key;

      @Nullable
      private final ValueWithTime<String> value;

      private KeyValuePair(String key, @Nullable ValueWithTime<String> value) {
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

   private static enum InsertBinder implements DbBinder<KeyValuePair> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, KeyValuePair pair) throws Exception {
         ValueWithTime<String> val = pair.value;

         stmt.bind(1, pair.key);
         DbUtils.bind(stmt, val == null ? null : val.value, 2);
         stmt.bind(3, val == null ? Long.MIN_VALUE : val.lastUpdateTime);
         stmt.bind(4, val == null ? Long.MIN_VALUE : val.lastReportTime);
      }
   }

   private static enum ConfigExtractor implements DbExtractor<String> {
      INSTANCE;

      @Override
      public String extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         return stmt.columnString(0);
      }
   }

   private static enum ConfigExtractorWithTime implements DbExtractor<ValueWithTime<String>> {
      INSTANCE;

      @Override
      public ValueWithTime<String> extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         return new ValueWithTime<String>(stmt.columnString(0), stmt.columnLong(1), stmt.columnLong(2));
      }
   }

   private static final class KeyValue {
      private final String key;
      private final String value;

      public KeyValue(String key, String value) {
         this.key = key;
         this.value = value;
      }
   }

   private static enum ConfigAllExtractor implements DbExtractor<KeyValue> {
      INSTANCE;

      @Override
      public KeyValue extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         String key = stmt.columnString(0);
         String val = stmt.columnString(1);
         return new KeyValue(key,val);
      }
   }

   private static enum ConfigAllWithTimeExtractor implements DbExtractor<KeyValuePair> {
      INSTANCE;

      @Override
      public KeyValuePair extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         String key = stmt.columnString(0);
         String val = stmt.columnString(1);
         Long lastUpdateTime = stmt.columnLong(2);
         Long lastReportTime = stmt.columnLong(3);

         ValueWithTime<String> vwt = new ValueWithTime(val, lastUpdateTime, lastReportTime);
         return new KeyValuePair(key,vwt);
      }
   }

   /**
    * Puts any unknown object into the config.
    * Works by figuring out if it matches a class that can be converted.
    *
    * Use long for date information.
    *
    * @param key
    * @param obj
    */
   public static void putObject(String key, @Nullable Object obj) {
      if (obj == null ) {
         put(key,null);
         return;
      }

      Class<?> cls = obj.getClass();
      if (cls.equals(Boolean.class) || cls.equals(boolean.class)) {
         put(key,(Boolean) obj);
      } else if (cls.equals(Byte.class) || cls.equals(byte.class)) {
         put(key,(Byte) obj);
      } else if (cls.equals(Short.class) || cls.equals(short.class)) {
         put(key,(Short) obj);
      } else if (cls.equals(Integer.class) || cls.equals(int.class)) {
         put(key,(Integer) obj);
      } else if (cls.equals(Long.class) || cls.equals(long.class)) {
         put(key,(Long) obj);
      } else if (cls.equals(Float.class) || cls.equals(float.class)) {
         put(key,(Float) obj);
      } else if (cls.equals(Double.class) || cls.equals(double.class)) {
         put(key,(Double) obj);
      } else if (cls.equals(String.class)) {
         put(key,(String) obj);
      } else if (cls.equals(UUID.class) ) {
         put(key,(UUID) obj);
      } else if (cls.equals(byte[].class) ) {
         put(key,(byte[]) obj);
      }
   }
}

