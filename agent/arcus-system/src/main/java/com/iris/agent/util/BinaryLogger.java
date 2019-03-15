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
package com.iris.agent.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteStatement;
import com.iris.agent.db.Db;
import com.iris.agent.db.DbBinder;
import com.iris.agent.db.DbService;


public class BinaryLogger {
   private static final String TABLE = "rawPackets";
   private static final int RX = 0;
   private static final int TX = 1;

   private long dbKey;
   private Db logDb;

   public BinaryLogger(String logName) {

      logDb = DbService.get(logName);

      // Create a table with 3 values - timestamp, direction, data
      //  Key will be the index
      dbKey = 1;
      logDb.execute("CREATE TABLE IF NOT EXISTS " + TABLE +
		    " (key INTEGER PRIMARY KEY, value1 INTEGER," +
		    " value2 INTEGER, value3);");
   }

   public void logTx(long timestamp, byte[] data) {
       log(timestamp, TX, data);
   }

   public void logRx(long timestamp, byte[] data) {
       log(timestamp, RX, data);
   }

   private void log(long timestamp, int direction, byte[] data) {
      List<BinaryLogDbData> values = new ArrayList<>(1);
      values.add(BinaryLogDbData.create(dbKey++, timestamp, direction, data));
      logDb.asyncExecute("INSERT INTO " + TABLE + " VALUES (?,?,?,?);",
                              BinaryLogDbBinder.INSTANCE, values);
   }

   private static final class BinaryLogDbData {
      private long key;
      private long timestamp;
      private int direction;
      @Nullable private byte[] data;

      private static BinaryLogDbData create(long key, long timestamp,
					    int direction, byte[] data) {
         BinaryLogDbData dbData = new BinaryLogDbData();
         dbData.key = key;
         dbData.timestamp = timestamp;
         dbData.direction = direction;
         dbData.data = data;
         return dbData;
      }
   }

   private static enum BinaryLogDbBinder implements DbBinder<BinaryLogDbData> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, BinaryLogDbData value) throws Exception {
         stmt.bind(1, value.key);
         stmt.bind(2, value.timestamp);
         stmt.bind(3, value.direction);
         stmt.bind(4, value.data);
      }
   }

}

