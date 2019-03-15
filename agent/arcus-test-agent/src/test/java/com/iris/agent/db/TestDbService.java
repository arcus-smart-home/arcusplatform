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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.eclipse.jdt.annotation.Nullable;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteStatement;
import com.iris.agent.test.SystemTestCase;

@Ignore
@RunWith(JUnit4.class)
public class TestDbService extends SystemTestCase {
   private static final double EPSILON = 0.01;

   @Test
   public void testCustomDatabaseFile() throws Exception {
      Db db = DbService.get("testCustomDatabaseFile");
      db.execute(DbService.class.getResource("/sql/test.sql"));
   }

   @Test
   public void testMultipleGetsReturnSameInstance() throws Exception {
      Db db1 = DbService.get("testMultipleGetsReturnSameInstance");
      Db db2 = DbService.get("testMultipleGetsReturnSameInstance");
      Assert.assertSame(db1, db2);
   }

   @Test
   public void testDifferentGetsReturnDifferentInstance() throws Exception {
      Db db1 = DbService.get("testDifferentGetsReturnDifferentInstance1");
      Db db2 = DbService.get("testDifferentGetsReturnDifferentInstance2");
      Assert.assertNotSame(db1, db2);
   }

   @Test
   public void testShutdown() throws Exception {
      Db db1 = DbService.get("testShutdown");
      db1.execute(DbService.class.getResource("/sql/test.sql"));
      db1.close();

      Db db2 = DbService.get("testShutdown");
      String value1 = DbService.get().querySingleColumn(String.class, "SELECT value1 FROM test WHERE key=?", "key1");
      Assert.assertEquals("value" + 1, value1);

      Assert.assertNotSame(db1, db2);
   }

   @Test
   public void testLoadFromResource() throws Exception {
      DbService.get().execute(DbService.class.getResource("/sql/test.sql"));

      for (int i = 1; i <= 16; ++i) {
         List<?> results = DbService.get().query("SELECT value1,value2,value3 FROM test WHERE key=?", "key" + i);
         Assert.assertNotNull(results);
         Assert.assertEquals(3, results.size());
         Assert.assertEquals("value" + i, results.get(0));
         Assert.assertEquals((long)(i - 1), ((Number)results.get(1)).longValue());
         Assert.assertEquals((double)i, ((Number)results.get(2)).doubleValue(), EPSILON);
      }
   }

   @Test
   public void testCreateAndFillDb() throws Exception {
      createAndFill(false);
   }

   @Test
   public void testCreateAndFillInMemoryDb() throws Exception {
      createAndFill(true);
   }

   @Test
   public void testConcurrentDbAccess() throws Exception {
      final Thread[] threads = new Thread[4];

      final int LOOPS = 100;
      final int INSERTS = 10;
      final int[] success = new int[threads.length];

      DbService.get().execute("CREATE TABLE catest (key TEXT PRIMARY KEY, value TEXT);");
      for (int i=0; i < threads.length; ++i) {
         final int thr = i;
         threads[i] = new Thread(new Runnable() {
            @Override
            public void run() {
               for (int l=0; l<LOOPS; ++l) {
                  try {
                     List<Map.Entry<String,String>> inserts = new ArrayList<>(INSERTS);
                     for (int s=0; s<INSERTS; ++s) {
                        inserts.add(new AbstractMap.SimpleImmutableEntry<>("testkey_" + thr + "_" + l + "_" + s, "testval_" + s)); 
                     }

                     DbService.get().execute("INSERT OR REPLACE INTO catest (key,value) VALUES (?,?)", KvBinder.INSTANCE, inserts);
                     List<?> results = DbService.get().query("SELECT value FROM catest WHERE key=?", "testkey_" + thr + "_" + l + "_0");
                     if (results.size() == 1 && "testval_0".equals(results.get(0))) {
                        success[thr]++;
                     }
                  } catch (Exception ex) {
                     ex.printStackTrace();
                  }
               }
            }
         });
      
         threads[i].setName("dbtest" + i);
         threads[i].setDaemon(false);
      }

      for (int i=0; i < threads.length; ++i) {
         threads[i].start();
      }

      for (int i=0; i < threads.length; ++i) {
         threads[i].join();
      }

      for (int i=0; i < threads.length; ++i) {
         Assert.assertEquals(LOOPS, success[i]);
      }
   }

   private void createAndFill(boolean inmemory) {
      final String TABLE = "testCreateAndFillDb";
      final int NUM = 100000;

      String name = "test" + ThreadLocalRandom.current().nextInt();
      try (Db db = inmemory ? DbService.getInMemoryDb(name) : DbService.get(name)) {
         db.execute("CREATE TABLE " + TABLE + " (key PRIMARY KEY,value1,value2,value3)");
   
         List<TestPojo> values = new ArrayList<>(NUM);
         for (int i = 0; i < NUM; ++i) {
            values.add(TestPojo.create(i));
         }
   
         db.execute("INSERT INTO " + TABLE + " VALUES (?,?,?,?);", TestPojoBinder.INSTANCE, values);
         List<TestPojo> results = db.queryAll("SELECT key,value1,value2,value3 FROM " + TABLE, TestPojoExtractor.INSTANCE);
   
         Assert.assertNotNull(results);
         Assert.assertEquals(values.size(), results.size());
         for (int i = 0; i < values.size(); ++i) {
            TestPojo test1 = values.get(i);
            TestPojo test2 = results.get(i);
            Assert.assertEquals("at index " + i, test1, test2);
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Supporting classes for test cases
   /////////////////////////////////////////////////////////////////////////////

   private static final class TestPojo {
      @Nullable private String key;
      private long value1;
      private double value2;
      @Nullable private byte[] value3;

      @SuppressWarnings("null")
      private static TestPojo create(int i) {
         TestPojo pojo = new TestPojo();
         pojo.key = "key" + i;
         pojo.value1 = 1L << (i % 64);
         pojo.value2 = (double)pojo.value1 / (double)i;

         pojo.value3 = new byte[i % 64];
         for (int v = 0; v < pojo.value3.length; ++v) {
            pojo.value3[v] = (byte)v;
         }

         return pojo;
      }

      @Override
      @SuppressWarnings("null")
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + ((key == null) ? 0 : key.hashCode());
         result = prime * result + (int) (value1 ^ (value1 >>> 32));
         long temp;
         temp = Double.doubleToLongBits(value2);
         result = prime * result + (int) (temp ^ (temp >>> 32));
         result = prime * result + Arrays.hashCode(value3);
         return result;
      }

      @Override
      @SuppressWarnings("null")
      public boolean equals(@Nullable Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         TestPojo other = (TestPojo) obj;
         if (key == null) {
            if (other.key != null)
               return false;
         } else if (!key.equals(other.key))
            return false;
         if (value1 != other.value1)
            return false;
         if (Double.doubleToLongBits(value2) != Double.doubleToLongBits(other.value2))
            return false;
         if (!Arrays.equals(value3, other.value3))
            return false;
         return true;
      }

      @Override
      public String toString() {
         return "TestPojo [key=" + key + ", value1=" + value1 + ", value2=" + value2 + ", value3=" + Arrays.toString(value3) + "]";
      }
   }

   private static enum KvBinder implements DbBinder<Map.Entry<String,String>> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, Map.Entry<String,String> value) throws Exception {
         stmt.bind(1, value.getKey());
         stmt.bind(2, value.getValue());
      }
   }

   private static enum TestPojoBinder implements DbBinder<TestPojo> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, TestPojo value) throws Exception {
         stmt.bind(1, value.key);
         stmt.bind(2, value.value1);
         stmt.bind(3, value.value2);
         stmt.bind(4, value.value3);
      }
   }

   private static enum TestPojoExtractor implements DbExtractor<TestPojo> {
      INSTANCE;

      @Override
      public TestPojo extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         TestPojo result = new TestPojo();
         result.key = stmt.columnString(0);
         result.value1 = stmt.columnLong(1);
         result.value2 = stmt.columnDouble(2);

         byte[] value3 = stmt.columnBlob(3);
         if (value3 == null) {
            value3 = new byte[0];
         }

         result.value3 = value3;
         return result;
      }
   }
}

