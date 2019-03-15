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
package com.iris.agent.db;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.Nullable;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

public final class DbUtils {
   private DbUtils() {
   }

   public static boolean canBind(@Nullable Object arg) {
      Class<?> type = arg == null ? null : arg.getClass();

      if (arg == null) {
         return true;
      } else if (type == String.class) {
         return true;
      } else if (type == int.class || type == Integer.class) {
         return true;
      } else if (type == long.class || type == Long.class) {
         return true;
      } else if (type == double.class || type == Double.class) {
         return true;
      } else if (type == byte[].class) {
         return true;
      } else {
         return false;
      }
   }

   public static void bindOrNull(SQLiteStatement stmt, @Nullable Object arg, int idx) throws Exception {
      Class<?> type = arg == null ? null : arg.getClass();

      boolean bound = doBind(type, stmt, arg, idx);
      if (!bound) {
         stmt.bindNull(idx);
      }
   }

   public static void bindOrString(SQLiteStatement stmt, @Nullable Object arg, int idx) throws Exception {
      Class<?> type = arg == null ? null : arg.getClass();

      boolean bound = doBind(type, stmt, arg, idx);
      if (bound) {
         return;
      }

      if (arg == null || type == null) {
         stmt.bindNull(idx);
      } else if (type.isArray()) {
         stmt.bind(idx, Arrays.toString((Object[]) arg));
      } else {
         stmt.bind(idx, arg.toString());
      }
   }

   public static void bind(SQLiteStatement stmt, @Nullable Object arg, int idx) throws Exception {
      Class<?> type = arg == null ? null : arg.getClass();

      boolean bound = doBind(type, stmt, arg, idx);
      if (!bound) {
         throw new DbException("cannot bind type: " + type);
      }
   }

   private static boolean doBind(@Nullable Class<?> type, SQLiteStatement stmt, @Nullable Object arg, int idx) throws Exception {
      if (arg == null) {
         stmt.bindNull(idx);
         return true;
      } else if (type == String.class) {
         stmt.bind(idx, (String) arg);
         return true;
      } else if ( type == boolean.class || type == Boolean.class) {
         Integer i = ((boolean) arg) ? 1 : 0;
         stmt.bind(idx, i);
         return true;
      } else if (type == byte.class || type == Byte.class) {
         stmt.bind(idx, (byte) arg);
         return true;
      } else if (type == short.class || type == Short.class) {
         stmt.bind(idx, (short) arg);
         return true;
      } else if (type == int.class || type == Integer.class) {
         stmt.bind(idx, (int) arg);
         return true;
      } else if (type == long.class || type == Long.class) {
         stmt.bind(idx, (long) arg);
         return true;
      } else if (type == float.class || type == Float.class) {
         stmt.bind(idx, (float) arg);
         return true;
      } else if (type == double.class || type == Double.class) {
         stmt.bind(idx, (double) arg);
         return true;
      } else if (type == byte[].class) {
         stmt.bind(idx, (byte[]) arg);
         return true;
      } else {
         return false;
      }
   }

   @SuppressWarnings("unchecked")
   public static <T> T extract(SQLiteStatement stmt, Class<T> type, int idx) throws Exception {
      if (type == String.class) {
         return (T) stmt.columnString(idx);
      } else if (type == int.class || type == Integer.class) {
         return (T) (Integer) stmt.columnInt(idx);
      } else if (type == long.class || type == Long.class) {
         return (T) (Long) stmt.columnLong(idx);
      } else if (type == double.class || type == Double.class) {
         return (T) (Double) stmt.columnDouble(idx);
      } else if (type == byte[].class) {
         return (T) stmt.columnBlob(idx);
      } else if (type == Object.class) {
         return (T) stmt.columnValue(idx);
      } else {
         throw new DbException("cannot get column type: " + type);
      }
   }

   public static <T> T complete(Future<T> future) {
      return complete(future, 15, TimeUnit.MINUTES);
   }

   public static <T> T complete(Future<T> future, long timeout, TimeUnit unit) {
      try {
         return future.get(timeout, unit);
      } catch (ExecutionException | InterruptedException | TimeoutException ex) {
         throw new DbException("failed to execute sql", new Exception(ex));
      }
   }

   public static abstract class DbTaskImpl<I, O> extends AbstractDbTask<O> {
      private final String      sql;

      @Nullable
      private final DbBinder<I> binder;

      @Nullable
      private final I           value;

      DbTaskImpl(String sql, @Nullable DbBinder<I> binder, @Nullable I value) {
         this.sql = sql;
         this.binder = binder;
         this.value = value;
      }

      @Nullable
      @Override
      public O execute(SQLiteConnection conn) throws Exception {
         I val = value;
         DbBinder<I> bnd = binder;
         if (bnd == null || val == null) {
            conn.exec(sql);
            return results(conn);
         }

         SQLiteStatement stmt = conn.prepare(sql, true);
         try {
            bnd.bind(conn, stmt, val);
            while (stmt.step()) {
               row(conn, stmt);
            }

            return results(conn);
         } finally {
            stmt.dispose();
         }
      }

      @Override
      public String toString() {
         return sql;
      }

      @Nullable
      protected abstract O results(SQLiteConnection conn);

      protected abstract void row(SQLiteConnection conn, SQLiteStatement stmt);
   }

   public static abstract class DbBulkTaskImpl<I, O> extends AbstractDbTask<O> {
      private final String      sql;
      private final DbBinder<I> binder;
      private final List<I>     values;

      private @Nullable SQLiteStatement stmt;
      private boolean runInTx;

      DbBulkTaskImpl(String sql, DbBinder<I> binder, List<I> values) {
         this.sql = sql;
         this.binder = binder;
         this.values = values;
      }

      @Nullable
      @Override
      public O execute(SQLiteConnection conn) throws Exception {
         SQLiteStatement stmt = conn.prepare(sql, true);
         boolean runInTx = runInTransaction(conn, stmt);

         try {
            if (runInTx) {
               conn.exec("BEGIN");
            }

            for (I value : values) {
               stmt.reset();
               next(conn, stmt);

               binder.bind(conn, stmt, value);
               while (stmt.step()) {
                  row(conn, stmt);
               }
            }

            if (runInTx) {
               conn.exec("COMMIT");
            }

            return results(conn);
         } catch (Exception ex) {
            if (runInTx) {
               try {
                  conn.exec("ROLLBACK");
               } catch (SQLiteException rex) {
                  // ignore
               }
            }

            throw ex;
         } finally {
            try {
               stmt.dispose();
            } catch (Exception ex) {
               // ignore
            }
         }
      }

      protected boolean runInTransaction(SQLiteConnection conn, SQLiteStatement stmt) {
         return false;
      }

      @Override
      public String toString() {
         return sql;
      }

      @Nullable
      protected abstract O results(SQLiteConnection conn);

      protected abstract void next(SQLiteConnection conn, SQLiteStatement stmt);

      protected abstract void row(SQLiteConnection conn, SQLiteStatement stmt);
   }
}

