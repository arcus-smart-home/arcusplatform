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
package com.iris.core.dao.cassandra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.util.List;

import org.junit.Test;

public class BaseCassandraModuleTest {

   @Test
   public void testParseContactPointsSingle() {
      List<InetAddress> result = BaseCassandraModule.parseContactPoints("127.0.0.1");
      assertEquals(1, result.size());
      assertEquals("127.0.0.1", result.get(0).getHostAddress());
   }

   @Test
   public void testParseContactPointsMultiple() {
      List<InetAddress> result = BaseCassandraModule.parseContactPoints("127.0.0.1,127.0.0.2");
      assertEquals(2, result.size());
      assertEquals("127.0.0.1", result.get(0).getHostAddress());
      assertEquals("127.0.0.2", result.get(1).getHostAddress());
   }

   @Test
   public void testParseContactPointsWithSpaces() {
      List<InetAddress> result = BaseCassandraModule.parseContactPoints("127.0.0.1, 127.0.0.2");
      assertEquals(2, result.size());
      assertEquals("127.0.0.1", result.get(0).getHostAddress());
      assertEquals("127.0.0.2", result.get(1).getHostAddress());
   }

   @Test
   public void testParseContactPointsWithExtraSpaces() {
      List<InetAddress> result = BaseCassandraModule.parseContactPoints("127.0.0.1 , 127.0.0.2 , 127.0.0.3");
      assertEquals(3, result.size());
      assertEquals("127.0.0.1", result.get(0).getHostAddress());
      assertEquals("127.0.0.2", result.get(1).getHostAddress());
      assertEquals("127.0.0.3", result.get(2).getHostAddress());
   }

   @Test
   public void testParseContactPointsSkipsUnresolvable() {
      List<InetAddress> result = BaseCassandraModule.parseContactPoints("127.0.0.1,this.host.does.not.exist.invalid");
      assertEquals(1, result.size());
      assertEquals("127.0.0.1", result.get(0).getHostAddress());
   }

   @Test(expected = RuntimeException.class)
   public void testParseContactPointsAllUnresolvableThrows() {
      BaseCassandraModule.parseContactPoints("this.host.does.not.exist.invalid");
   }

   @Test
   public void testParseContactPointsLocalhost() {
      List<InetAddress> result = BaseCassandraModule.parseContactPoints("localhost");
      assertTrue(result.size() >= 1);
   }
}
