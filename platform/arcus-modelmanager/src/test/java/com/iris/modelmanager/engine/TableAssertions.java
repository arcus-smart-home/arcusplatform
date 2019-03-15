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
package com.iris.modelmanager.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.datastax.driver.core.ResultSet;

public class TableAssertions {

   private TableAssertions() {
   }

   public static void assertTableExists(String table, ExecutionContext context) {
      ResultSet rs = context.getSession().execute("select * from " + table);
      assertEquals(0, rs.all().size());
   }

   public static void assertTableNotExists(String table, ExecutionContext context) {
      try {
         context.getSession().execute("select * from " + table);
         fail("An exception should have been thrown");
      } catch(Exception e) {
         // ok
      }
   }
}

