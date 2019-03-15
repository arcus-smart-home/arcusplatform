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
package com.iris.agent.util.db.sqlite;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteStatement;
import com.iris.agent.db.Db;
import com.iris.agent.db.DbBinder;
import com.iris.agent.db.DbExtractor;
import com.iris.agent.db.DbUtils;

public class SQLite3Table<T> {
   private static final Logger log = LoggerFactory.getLogger(SQLite3Table.class);
   private static final String LIST_TABLES = "SELECT name FROM sqlite_master WHERE type='table';";

   private final Db database;
   private final Class<T> type;
   private final String name;
   private final Map<Field,SQLField> fields;

   private final String insertString;
   private final String deleteString;
   private final String updateString;
   private final String queryAllString;

   private final InsertBinder insertBinder;
   private final DeleteBinder deleteBinder;
   private final UpdateBinder updateBinder;
   private final AllQueryExtractor allQueryExtractor;

   public SQLite3Table(Db database, Class<T> type) {
      this.database = database;
      this.type = type;
      this.name = (type.isAnnotationPresent(SQLTableName.class))
         ? ((SQLTableName)type.getAnnotation(SQLTableName.class)).value()
         : type.getName();

      this.fields = new LinkedHashMap<>();
      for (Field f : type.getFields()) {
         if (!f.isAnnotationPresent(SQLColumn.class)) {
            continue;
         }

         // NOTE: This statement has a large impact on the performance of 
         //       Java reflection. It disables the access checks that are
         //       normally done on every reflective call.
         f.setAccessible(true);

         SQLColumn col = f.getAnnotation(SQLColumn.class);
         String tableName = "";
         String keyName = "";
         if (col.isForeignKey()) {
            tableName = col.foreignTableName();
            keyName = col.foreignKey();

            Class<?> clzz = col.foreignTableClass();
            if (!clzz.equals(SQLNull.class)) {
               tableName = clzz.getAnnotation(SQLTableName.class).value();
            }
         }

         fields.put(f,new SQLField(f.getName(), f.getType(), col.isKey(),
            col.isForeignKey(), tableName, keyName, col.createIndex(),
            col.cascadeDelete()));
      }

      String allFields = genAllFieldNames(this.fields);
      String allHolders = genAllPlaceHolders(this.fields);
      String keyHolders = genKeyPlaceHolders(this.fields);
      String valueHolders = genValuePlaceHolders(this.fields);
      this.insertString = createInsertQuery(this.name, allFields, allHolders);
      this.updateString = createUpdateQuery(this.name, valueHolders, keyHolders);
      this.deleteString = createDeleteQuery(this.name, keyHolders);
      this.queryAllString = createSelectAllQuery(this.name, allFields);
      
      this.insertBinder = new InsertBinder();
      this.deleteBinder = new DeleteBinder();
      this.updateBinder = new UpdateBinder();
      this.allQueryExtractor = new AllQueryExtractor();

      createDbIfNeeded();
   }

   /////////////////
   // All Query

   public List<T> list() {
      return list(database);
   }

   public List<T> list(Db database) {
      return database.queryAll(queryAllString, allQueryExtractor);
   }

   public List<T> list(String match) {
      return list(database, match);
   }

   public List<T> list(Db database, String match) {
      String query = queryAllString + " WHERE " + match + ";";
      return database.queryAll(query, allQueryExtractor);
   }

   ///////////////
   // Insert

   public boolean insert(T item) {
      return insert(database, item);
   }

   boolean insert(Db database, T item) {
      try {
         database.execute(insertString, insertBinder, item);
         return true;
      } catch (Exception e) {
         log.warn("unable to insert:", e);
         return false;
      }
   }

   ///////////////
   // Delete

   public boolean delete(T item) {
      return delete(this.database, item);
   }

   public boolean delete(Db database, T item) {
      try {
         database.execute(deleteString, deleteBinder, item);
         return true;
      } catch (Exception e) {
         log.warn("unable to delete:", e);
         return false;
      }
   }

   ///////////////
   // Update

   public boolean update(T item) {
      return update(this.database, item);
   }

   public boolean update(Db database, T item) {
      try {
         database.execute(updateString, updateBinder, item);
         return true;
      } catch (Exception e) {
         log.warn("unable to update:", e);
         return false;
      }
   }

   ///////////////
   // Statements

   private static String genAllFieldNames(Map<Field,SQLField> fields) {
      StringBuilder bld = new StringBuilder();
      for (SQLField field : fields.values()) {
         if (bld.length() != 0) bld.append(",");
         bld.append(field.getName());
      }

      return bld.toString();
   }

   private static String genAllPlaceHolders(Map<Field,SQLField> fields) {
      StringBuilder bld = new StringBuilder();
      for (SQLField field : fields.values()) {
         if (bld.length() != 0) bld.append(",?");
         else bld.append("?");
      }

      return bld.toString();
   }

   private static String genKeyPlaceHolders(Map<Field,SQLField> fields) {
      StringBuilder bld = new StringBuilder();
      for (SQLField field : fields.values()) {
         if (!field.isPrimaryKey()) continue;

         if (bld.length() != 0) bld.append(" AND ");
         bld.append(field.getName()).append("=?");
      }

      return bld.toString();
   }

   private static String genValuePlaceHolders(Map<Field,SQLField> fields) {
      StringBuilder bld = new StringBuilder();
      for (SQLField field : fields.values()) {
         if (field.isPrimaryKey()) continue;

         if (bld.length() != 0) bld.append(",");
         bld.append(field.getName()).append("=?");
      }

      return bld.toString();
   }

   private static String createSelectAllQuery(String name, String fields) {
      return "SELECT " + fields + " FROM " + name;
   }

   private static String createInsertQuery(String name, String fields, String holders) {
      return "INSERT INTO " + name + " (" + fields + ") VALUES (" + holders + ");";
   }

   private static String createUpdateQuery(String name, String valueHolders, String keyHolders) {
      return "UPDATE " + name + " SET " + valueHolders + " WHERE " + keyHolders;
   }

   private static String createDeleteQuery(String name, String keyHolders) {
      return "DELETE FROM " + name + " WHERE " + keyHolders;
   }

   ///////////////
   // DB Creation

   private void createDbIfNeeded() {
      if (!exists(database)) {
         create(database);
      } else {
         checkAndCreateFields(database);
      }
   }

   private boolean exists(Db database) {
      return database.queryAll(LIST_TABLES, StringExtractor.INSTANCE)
         .contains(name);
   }

   private String genFieldNamesAndType() {
      StringBuilder bld = new StringBuilder();
      for (SQLField field : fields.values()) {
         if (bld.length() != 0) bld.append(",");
         genFieldAsTableConstruct(bld,field);
      }

      return bld.toString();
   }

   private void genFieldAsTableConstruct(StringBuilder bld, SQLField field) {
      bld.append(field.getName()).append(" ").append(field.getTypeText());
      if (field.isPrimaryKey() ) {
         bld.append(" PRIMARY KEY");
      }
   }

   public String genForeignKeyInformation() {
      boolean delete = false;
      StringBuilder keys = new StringBuilder();
      StringBuilder refs = new StringBuilder();
      for (SQLField field : fields.values()) {
         if (!field.isForeignKey()) continue;

         if (keys.length() != 0) keys.append(",");
         if (refs.length() != 0) refs.append(",");

         keys.append(field.getName());
         refs.append(field.getForeignTable()).append("(").append(field.getForeignKey()).append(")");

         if (field.isCascadeOnDelete()) {
            delete = true;
         }
      }

      if (keys.length() == 0) {
         return "";
      }

      if (delete) {
         return ", FOREIGN KEY (" + keys + ") REFERENCES " + refs  + " ON DELETE CASCADE ";
      } else {
         return ", FOREIGN KEY (" + keys + ") REFERENCES " + refs;
      }
   }

   private String genAlterTable(SQLField field) {
      StringBuilder bld = new StringBuilder();
      bld.append("ALTER TABLE ").append(name).append(" ADD ");
      genFieldAsTableConstruct(bld, field);
      return bld.toString();
   }

   private void create(Db database) {
      String create = "CREATE TABLE " + name + " (" +
         genFieldNamesAndType() +
         genForeignKeyInformation() +
      ")";

      database.execute(create);
   }

   private void checkAndCreateFields(Db database) {
      String query = "PRAGMA table_info(" + name + ");";
      List<SQLite3TableInfo> list = database.queryAll(query, SQLite3TableInfoExtractor.INSTANCE);

      for (SQLField field : fields.values()) {
         String fieldName = field.getName();
         boolean containsField = false;

         for (SQLite3TableInfo info : list) {
            if (fieldName.equals(info.name)) {
               containsField = true;
               break;
            }
         }

         if (!containsField) {
            database.execute(genAlterTable(field));
         }
      }
   }

   /////////////////////////////////////////////////////////////
   // Internal Classes
   /////////////////////////////////////////////////////////////

   private final class AllQueryExtractor implements DbExtractor<T> {
      @Override
      @Nullable
      public T extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         int index = 0;

         T tbr = type.newInstance();
         for (Field f : fields.keySet()) {
            f.set(tbr, SQLite3StatementBindInvoker.bind(stmt, f.getType(), index++));
         }

         return tbr;
      }
   }

   private final class InsertBinder implements DbBinder<T> {
      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, T value) throws Exception {
         int index = 1;
         for (Field f : fields.keySet()) {
            DbUtils.bind(stmt, f.get(value), index++);
         }
      }
   }

   private final class DeleteBinder implements DbBinder<T> {
      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, T value) throws Exception {
         int index = 1;
         for (Map.Entry<Field,SQLField> entry : fields.entrySet()) {
            if (!entry.getValue().isPrimaryKey()) continue;

            Field f = entry.getKey();
            DbUtils.bind(stmt, f.get(value), index++);
         }
      }
   }

   private final class UpdateBinder implements DbBinder<T> {
      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, T value) throws Exception {
         int index = 1;

         for (Map.Entry<Field,SQLField> entry : fields.entrySet()) {
            if (entry.getValue().isPrimaryKey()) continue;

            Field f = entry.getKey();
            DbUtils.bind(stmt, f.get(value), index++);
         }

         for (Map.Entry<Field,SQLField> entry : fields.entrySet()) {
            if (!entry.getValue().isPrimaryKey()) continue;

            Field f = entry.getKey();
            DbUtils.bind(stmt, f.get(value), index++);
         }
      }
   }

   private static final class SQLite3TableInfo {
      private final int cid;
      private final String name;
      private final String type;
      private final String notnull;
      private final String defaultValue;
      private final int pk;

      public SQLite3TableInfo(int cid, String name, String type, String notnull, String defaultValue, int pk) {
         this.cid = cid;
         this.name = name;
         this.type = type;
         this.notnull = notnull;
         this.defaultValue = defaultValue;
         this.pk = pk;
      }
   }

   private static enum SQLite3TableInfoExtractor implements DbExtractor<SQLite3TableInfo> {
      INSTANCE;

      @Override
      public SQLite3TableInfo extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         int cid = stmt.columnInt(0);
         String name = stmt.columnString(1);
         String type = stmt.columnString(2);
         String notnull = stmt.columnString(3);
         String defaultValue = stmt.columnString(4);
         int pk = stmt.columnInt(5);
         return new SQLite3TableInfo(cid,name,type,notnull,defaultValue,pk);
      }
   }

   private static enum StringExtractor implements DbExtractor<String> {
      INSTANCE;

      @Override
      public String extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         return stmt.columnString(0);
      }
   }
}

