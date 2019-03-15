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
package com.iris.agent.reflex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteStatement;
import com.google.common.collect.ImmutableMap;
import com.iris.agent.config.ConversionService;
import com.iris.agent.db.Db;
import com.iris.agent.db.DbBinder;
import com.iris.agent.db.DbExtractor;
import com.iris.agent.db.DbService;
import com.iris.agent.db.DbUtils;
import com.iris.io.json.JSON;
import com.iris.messages.address.Address;
import com.iris.model.Version;
import com.iris.util.IrisUUID;
import com.iris.util.TypeMarker;

public final class ReflexDao {
   private static final Logger log = LoggerFactory.getLogger(ReflexDao.class);
   
   public static final String REFLEX_STATE_DRIVER = "driver";
   public static final String REFLEX_STATE_VERSION = "version";
   public static final String REFLEX_STATE_STATE = "state";

   private static final Object LOCK = new Object();
   private static final String CHECK_CONFIG_TABLE = "SELECT name FROM sqlite_master WHERE type='table' AND name='reflexconfig'";
   private static final String[] SCHEMA_SCRIPTS = {
      "/sql/reflex.sql",
   };

   private static final String CONFIG_REFLEXDB = "reflexdb";
   private static final String CONFIG_REFLEXDB_PINS = "reflexdbpins";

   private static final Map<String,String> config = Collections.synchronizedMap(new HashMap<>());
   private static boolean reflexStarted = false;

   private ReflexDao() {
   }

   /////////////////////////////////////////////////////////////////////////////
   // Startup persistence support
   /////////////////////////////////////////////////////////////////////////////

   /////////////////////////////////////////////////////////////////////////////
   // High level support
   /////////////////////////////////////////////////////////////////////////////
   
   public static @Nullable String getReflexDB() {
      try {
         String json = get(CONFIG_REFLEXDB, String.class, "");
         if (json.isEmpty()) {
            return null;
         }

         return json;
      } catch (Exception ex) {
         log.info("failed to load persisted reflex db, will use builtin reflexdb");
         return null;
      }
   }

   public static Map<UUID,String> getReflexDBPins() {
      try {
         String json = get(CONFIG_REFLEXDB_PINS);
         if (json == null || json.isEmpty()) {
            return ImmutableMap.of();
         }

         Map<String,String> map = JSON.fromJson(json, TypeMarker.mapOf(String.class));
         ImmutableMap.Builder<UUID,String> pins = ImmutableMap.builder();
         for (Map.Entry<String,String> pin : map.entrySet()) {
            pins.put(IrisUUID.fromString(pin.getKey()), pin.getValue());
         }

         return pins.build();
      } catch (Exception ex) {
         log.warn("failed to load pins from reflex db: ", ex);
         return ImmutableMap.of();
      }
   }

   public static void putReflexDBPins(Map<UUID,String> pins) {
      Map<String,String> put = new HashMap<>();
      for (Map.Entry<UUID,String> pin : pins.entrySet()) {
         put.put(IrisUUID.toString(pin.getKey()), pin.getValue());
      }

      String json = JSON.toJson(put);
      put(CONFIG_REFLEXDB_PINS, json);
   }

   public static void putReflexDB(@Nullable String reflexesBase64) {
      try {
         put(CONFIG_REFLEXDB, reflexesBase64);
      } catch (Exception ex) {
         log.warn("failed to persist reflex db:", ex);
      }
   }

   public static Map<Address,Map<String,String>> getAllReflexStates() {
      long start = System.nanoTime();
      List<AddressKeyValue> all = DbService.get().queryAll("SELECT addr,key,value FROM reflexes", AddressKeyValueAllExtractor.INSTANCE);
      Map<Address,Map<String,String>> result = toAddressMap(all);

      double elapsed = (System.nanoTime() - start) / 1000000000.0;
      log.info("reflex controller loaded all reflex records in {}s", String.format("%.3f", elapsed));
      return result;
   }

   public static void putReflexStates(Map<Address,Map<String,String>> reflexStates) {
      List<AddressKeyValue> updates = new ArrayList<>(reflexStates.size());
      for (Map.Entry<Address,Map<String,String>> aentry : reflexStates.entrySet()) {
         String saddr = aentry.getKey().getRepresentation();
         for (Map.Entry<String,String> entry : aentry.getValue().entrySet()) {
            updates.add(new AddressKeyValue(saddr,entry.getKey(),entry.getValue()));
         }
      }

      DbService.get().execute(
         "INSERT OR REPLACE INTO reflexes (addr,key,value) VALUES (?,?,?)",
         AddressKeyValueInsertBinder.INSTANCE,
         updates
      );
   }

   public static void putReflexCurrentState(Address addr, ReflexProcessor.State state) {
      DbService.get().execute(
         "INSERT OR REPLACE INTO reflexes (addr,key,value) VALUES (?,?,?)",
         AddressKeyValueInsertBinder.INSTANCE,
         new AddressKeyValue(addr.getRepresentation(), REFLEX_STATE_STATE, state.name())
      );
   }

   public static Map<Address,Map<String,String>> getAllDriverStates() {
      long start = System.nanoTime();

      List<AddressKeyValue> all = DbService.get().queryAll("SELECT addr,key,value FROM drivers", AddressKeyValueAllExtractor.INSTANCE);
      Map<Address,Map<String,String>> result = toAddressMap(all);

      double elapsed = (System.nanoTime() - start) / 1000000000.0;
      log.info("reflex controller loaded all driver records in {}s", String.format("%.3f", elapsed));
      return result;
   }

   public static Map<String,String> getDriverState(Address addr) {
      List<KeyValue> all = DbService.get().queryAll("SELECT key,value FROM drivers WHERE addr=?", AddressBinder.INSTANCE, addr, AddressKeyValueExtractor.INSTANCE);
      Map<String,String> result = new HashMap<>();
      for (KeyValue kv : all) {
         result.put(kv.key, kv.value);
      }

      return result;
   }

   public static void putDriverState(String addr, String key, Object val) {
      DbService.get().asyncExecute(
         "INSERT OR REPLACE INTO drivers (addr,key,value) VALUES (?,?,?)",
         AddressKeyValueInsertBinder.INSTANCE,
         new AddressKeyValue(addr, key, ConversionService.from(val))
      );
   }

   public static void putDriverState(String addr, Map<String,Object> state) {
      List<AddressKeyValue> updates = new ArrayList<>(state.size());
      for (Map.Entry<String,Object> entry : state.entrySet()) {
         updates.add(new AddressKeyValue(addr,entry.getKey(), ConversionService.from(entry.getValue())));
      }

      DbService.get().asyncExecute(
         "INSERT OR REPLACE INTO drivers (addr,key,value) VALUES (?,?,?)",
         AddressKeyValueInsertBinder.INSTANCE,
         updates
      );
   }

   public static void removeAllDriverAndReflexState(String addr) {
      try {
         DbService.get().execute("DELETE FROM drivers WHERE addr=?", addr);
      } catch (Exception ex) {
         log.warn("could not delete driver state:", ex);
      }

      try {
         DbService.get().execute("DELETE FROM reflexes WHERE addr=?", addr);
      } catch (Exception ex) {
         log.warn("could not delete reflex state:", ex);
      }
   }

   private static Map<Address,Map<String,String>> toAddressMap(List<AddressKeyValue> all) {
      Map<String,Address> addrs = new HashMap<>();
      Map<Address,Map<String,String>> result = new HashMap<>();
      for (AddressKeyValue akv : all) {
         Address addr = addrs.get(akv.addr);
         if (addr == null) {
            addr = Address.fromString(akv.addr);
            addrs.put(akv.addr, addr);
         }

         Map<String,String> config = result.get(addr);
         if (config == null) {
            config = new HashMap<>(5);
            result.put(addr,config);
         }

         config.put(akv.key, akv.value);
      }

      return result;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Startup and shutdown
   /////////////////////////////////////////////////////////////////////////////

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
         db.execute(ReflexDao.class.getResource(SCHEMA_SCRIPTS[i]));
      }
   }

   static void start() {
      synchronized (LOCK) {
         if (reflexStarted) {
            throw new RuntimeException("reflex dao already started");
         }

         reflexStarted = true;
         setupSchema(DbService.get());

         try {
            long start = System.nanoTime();
            config.putAll(all());
            double elapsed = (System.nanoTime() - start) / 1000000000.0;
            log.info("loaded {} reflex configuration records in {}s", config.size(), String.format("%.3f", elapsed));
         } catch (Exception ex) {
            log.warn("failed to load reflex configuration:", ex);
         }
      }
   }

   static void shutdown() {
      synchronized (LOCK) {
         reflexStarted = false;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Config support
   /////////////////////////////////////////////////////////////////////////////

   public static <T> void put(String key, @Nullable T value) {
      String svalue = ConversionService.from(value);

      // write through cache
      config.put(key, svalue);

      KeyValue pair = new KeyValue(key,svalue);
      DbService.get().execute("INSERT OR REPLACE INTO reflexconfig (key,value) VALUES (?,?)", InsertBinder.INSTANCE, pair);
   }

   @SuppressWarnings("null")
   public static <T> T get(String key, Class<T> type) {
      return ConversionService.to(type, get(key));
   }

   @SuppressWarnings({"unused","null"})
   public static <T> T get(String key, Class<T> type, @Nullable T def) {
      String value = get(key);
      if (value == null) return def;
      return ConversionService.to(type, value);
   }

   public static String get(String key) {
      return config.get(key);
   }

   public static <T> void put(Db db, String key, @Nullable T value) {
      String svalue = ConversionService.from(value);
      KeyValue pair = new KeyValue(key,svalue);
      db.execute("INSERT OR REPLACE INTO reflexconfig (key,value) VALUES (?,?)", InsertBinder.INSTANCE, pair);
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

   public static String get(Db db, String key) {
      return db.query("SELECT value FROM reflexconfig WHERE key=?", ConfigBinder.INSTANCE, key, ConfigExtractor.INSTANCE);
   }

   private static Map<String,String> all() {
      Map<String,String> result = new HashMap<>();
      List<KeyValue> all = DbService.get().queryAll("SELECT key,value FROM reflexconfig", ConfigAllExtractor.INSTANCE);
      for (KeyValue kv : all) {
         result.put(kv.key, kv.value);
      }

      return result;
   }

   /////////////////////////////////////////////////////////////////////////////
   // POJOs supporting config
   /////////////////////////////////////////////////////////////////////////////

   private static final class KeyValue {
      private final String key;
      private final @Nullable String value;

      public KeyValue(String key, @Nullable String value) {
         this.key = key;
         this.value = value;
      }
   }

   private static final class AddressKeyValue {
      private final String addr;
      private final String key;
      private final @Nullable String value;

      public AddressKeyValue(String addr, String key, @Nullable String value) {
         this.addr = addr;
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

   private static enum InsertBinder implements DbBinder<KeyValue> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, KeyValue pair) throws Exception {
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

   private static enum ConfigAllExtractor implements DbExtractor<KeyValue> {
      INSTANCE;

      @Override
      public KeyValue extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         String key = stmt.columnString(0);
         String val = stmt.columnString(1);
         return new KeyValue(key,val);
      }
   }

   private static enum AddressBinder implements DbBinder<Address> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, Address addr) throws Exception {
         stmt.bind(1, addr.getRepresentation());
      }
   }

   private static enum AddressKeyValueInsertBinder implements DbBinder<AddressKeyValue> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, AddressKeyValue akv) throws Exception {
         stmt.bind(1, akv.addr);
         stmt.bind(2, akv.key);
         stmt.bind(3, akv.value);
      }
   }

   private static enum AddressKeyValueExtractor implements DbExtractor<KeyValue> {
      INSTANCE;

      @Override
      public KeyValue extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         String key = stmt.columnString(0);
         String val = stmt.columnString(1);
         return new KeyValue(key,val);
      }
   }

   private static enum AddressKeyValueAllExtractor implements DbExtractor<AddressKeyValue> {
      INSTANCE;

      @Override
      public AddressKeyValue extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         String addr = stmt.columnString(0);
         String key = stmt.columnString(1);
         String val = stmt.columnString(2);
         return new AddressKeyValue(addr,key,val);
      }
   }
}

