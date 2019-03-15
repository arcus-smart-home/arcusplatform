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

import java.io.Serializable;

public class SQLField {
   private final String name;
   private final Class<?> type;
   private final String typeName;

   private final boolean isPrimaryKey;
   private final boolean isForeignKey;

   private final String foreignTable;
   private final String foreignKey;
   private final boolean createIndex;
   private final boolean cascadeOnDelete;
   
   public SQLField(String name, Class<?> type, boolean isPrimaryKey, boolean isForeignKey,
      String foreignTable, String foreignKey, boolean createIndex, boolean cascadeOnDelete) {
      this.name = name;
      this.type = type;
      this.typeName = getSqlType(type);

      this.isPrimaryKey = isPrimaryKey;
      this.isForeignKey = isForeignKey;

      this.foreignTable = foreignTable;
      this.foreignKey = foreignKey;
      this.createIndex = createIndex;
      this.cascadeOnDelete = cascadeOnDelete;
   }

   public String getName() {
      return name;
   }

   public Class<?> getType() {
      return type;
   }

   public String getTypeText() {
      return typeName;
   }

   public boolean isPrimaryKey() {
      return isPrimaryKey;
   }

   public boolean isForeignKey() {
      return isForeignKey;
   }

   public String getForeignTable() {
      return foreignTable;
   }

   public String getForeignKey() {
      return foreignKey;
   }

   public boolean isCreateIndex() {
      return createIndex;
   }

   public boolean isCascadeOnDelete() {
      return cascadeOnDelete;
   }

   private static String getSqlType(Class<?> type) {
      if (type.equals(String.class)) {
         return "text";
      } else if (type.equals(Integer.class) || type.equals(int.class) || 
            type.equals(Short.class) || type.equals(short.class) ||
            type.equals(Long.class) || type.equals(long.class) || 
            type.equals(Byte.class) || type.equals(byte.class) || 
            type.equals(Boolean.class) || type.equals(boolean.class)) {
         return "integer";
      } else if (type.equals(Double.class) || type.equals(double.class) || 
            type.equals(Float.class) || type.equals(float.class)) {
         return "real";
      } else if (type.equals(Serializable.class) || type.equals(byte[].class)) {
         return "blob";
      }

      throw new RuntimeException("cannot translate class to sql type: " + type);
   }
}

