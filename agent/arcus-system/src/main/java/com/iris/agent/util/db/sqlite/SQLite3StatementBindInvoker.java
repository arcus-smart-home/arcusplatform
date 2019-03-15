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

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

final class SQLite3StatementBindInvoker {
   private static final Logger log = LoggerFactory.getLogger(SQLite3StatementBindInvoker.class);

   private SQLite3StatementBindInvoker() {
   }

   @Nullable
   public static Object bind(SQLiteStatement stmt, Class<?> type, int index) {
      try {
         if (type.equals(String.class)) {
            return stmt.columnString(index);
         } else if (type.equals(Integer.class) || type.equals(int.class) ) {
            return stmt.columnInt(index);
         } else if (type.equals(Long.class) || type.equals(long.class)) {
            return stmt.columnLong(index);
         } else if (type.equals(Byte.class) || type.equals(byte.class) ) {
            return (byte)stmt.columnInt(index);
         } else if ( type.equals(Boolean.class) || type.equals(boolean.class) ) {
            return stmt.columnInt(index) != 0;
         } else if (type.equals(Double.class) || type.equals(double.class)) {
            return stmt.columnDouble(index);
         } else if (type.equals(float.class) || type.equals(Float.class)) {
            return (float)stmt.columnDouble(index);
         } else if (type.equals(Serializable.class)) {
            return stmt.columnBlob(index);
         }
      } catch (SQLiteException e) {
         log.error("sql bind invoker could not invoke:" , e);
      }

      return null;
   }
}

