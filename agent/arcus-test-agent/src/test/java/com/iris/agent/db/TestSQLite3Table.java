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

import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.iris.agent.test.SystemTestCase;
import com.iris.agent.util.db.sqlite.SQLColumn;
import com.iris.agent.util.db.sqlite.SQLTableName;
import com.iris.agent.util.db.sqlite.SQLite3Table;

@Ignore
public class TestSQLite3Table extends SystemTestCase {
   @SQLTableName("POJO")
   public static class POJO {
      @SQLColumn
      public String  name;

      @SQLColumn()
      public byte    b = 0x01;
      //@SQLColumn()
      //public short   s = 0x02;
      @SQLColumn(isKey = true)
      public int     id;
      @SQLColumn()
      public long    l = 0x04;

      @SQLColumn()
      public float   f = (float) Math.PI;
      @SQLColumn()
      public double  d = Math.E;

      @SQLColumn()
      public byte[]  bytes = { 0x01, 0x02, 0x03, 0x04 };

      @SQLColumn()
      public boolean bool = false;

      public boolean other;

      public POJO() {

      }

      public POJO(String n, int id, boolean ot) {
         this.name = n;
         this.id = id;
         this.other = ot;
      }

   }
   
   @SQLTableName("POJO2")
   public static class POJO2 {
      @SQLColumn()
      public String  name;

      @SQLColumn(isKey = true)
      public int     id;

      public boolean other;

      public POJO2() {

      }

      public POJO2(String n, int id, boolean ot) {
         this.name = n;
         this.id = id;
         this.other = ot;
      }
   }
   
   @SQLTableName("F_POJO")
   public static class F_POJO {
      @SQLColumn(isKey = true)
      public String  name2;

      @SQLColumn(isForeignKey = true,foreignTableClass=POJO2.class,foreignKey="id",cascadeDelete=true)
      public int     id;

      public boolean other;

      public F_POJO() {

      }

      public F_POJO(String n, int id, boolean ot) {
         this.name2 = n;
         this.id = id;
         this.other = ot;
      }

   }

   @Test
   public void testAutoCreateTable() {
      Db database = DbService.get("testAutoCreateTable");
      SQLite3Table<POJO> table = new SQLite3Table<POJO>(database, POJO.class);

      POJO pj = new POJO("Test", 15, true);

      table.insert(pj);
      pj.id++;
      table.insert(pj);
      pj.id++;
      table.insert(pj);

      List<POJO> list = table.list();

      Assert.assertTrue(list.size() == 3);
   }

   @Test
   public void testDeleteForReal() {
      Db database = DbService.get("testDeleteForReal");
      SQLite3Table<POJO2> table = new SQLite3Table<POJO2>(database, POJO2.class);

      POJO2 pj = new POJO2("Test", 15, true);

      table.insert(pj);
      pj.id++;
      table.insert(pj);
      pj.id++;
      table.insert(pj);

      table.delete(pj);

      List<POJO2> list = table.list();

      Assert.assertTrue(list.size() == 2);
   }

   @Test
   public void testUpdateForReal() {
      Db database = DbService.get("testUpdateForReal");
      SQLite3Table<POJO> table = new SQLite3Table<POJO>(database, POJO.class);

      POJO pj = new POJO("Test", 15, true);

      table.insert(pj);
      pj.id++;
      table.insert(pj);
      pj.id++;
      table.insert(pj);

      pj.name = "newName";
      table.update(pj);

      List<POJO> list = table.list();

      Assert.assertTrue(list.size() == 3);
      Assert.assertTrue(list.get(2).name.equals("newName"));
   }
   
   @Test
   public void testForeignKey() {
      Db database = DbService.get("testForeignKey");

      SQLite3Table<POJO2> po_table = new SQLite3Table<POJO2>(database, POJO2.class);
      SQLite3Table<F_POJO> f_table = new SQLite3Table<F_POJO>(database, F_POJO.class);
     
      POJO2 po= new POJO2("Test", 1, true);
      POJO2 po4= new POJO2("Test4", 4, true);
      F_POJO fpo = new F_POJO("Fester", 1, false );
      F_POJO fpo2 = new F_POJO("Fester2", 1, false );
      F_POJO fpo3 = new F_POJO("Fester3", 1, false );
      F_POJO fpo4 = new F_POJO("Fester4", 4, false );
      
      po_table.insert(po);
      po_table.insert(po4);
      
      f_table.insert(fpo);
      f_table.insert(fpo2);
      f_table.insert(fpo3);
      f_table.insert(fpo4);
      
      Assert.assertTrue(f_table.list().size() == 4);
      po_table.delete(po);
      Assert.assertTrue(f_table.list().size() == 1);
   }
}

