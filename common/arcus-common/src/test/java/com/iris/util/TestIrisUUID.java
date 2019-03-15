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
package com.iris.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class TestIrisUUID {
   @Test
   public void testIrisUUIDToString() {
      final int NUM = 100 * 1000;
      for (int i = 0; i < NUM; ++i) {
         UUID uuid = UUID.randomUUID();
         assertEquals("UUID representation different", uuid.toString(), IrisUUID.toString(uuid));
      }
   }

   @Test
   public void testIrisUUIDFromString() {
      final int NUM = 100 * 1000;
      for (int i = 0; i < NUM; ++i) {
         String uuid = UUID.randomUUID().toString();
         assertEquals("UUID parsed differently", UUID.fromString(uuid), IrisUUID.fromString(uuid));
      }
   }

   @Test
   public void testRandomUuidCollisions() throws Exception {
      testUuidCollisions(true);
   }

   @Test
   public void testTimeUuidCollisions() throws Exception {
      testUuidCollisions(false);
   }

   @Test
   public void testMinTimeUUID() throws Exception {
      assertTrue(IrisUUID.isTime(IrisUUID.minTimeUUID()));
      assertEquals(-1, IrisUUID.ascTimeUUIDComparator().compare(IrisUUID.minTimeUUID(), IrisUUID.timeUUID(0)));
   }
   
   @Test
   public void testMaxTimeUUID() throws Exception {
      assertTrue(IrisUUID.isTime(IrisUUID.maxTimeUUID()));
      assertEquals(1, IrisUUID.ascTimeUUIDComparator().compare(IrisUUID.maxTimeUUID(), IrisUUID.timeUUID(0)));
   }
   
   private void testUuidCollisions(final boolean random) throws Exception {
      final int NUM = 500 * 1000;

      final Set<UUID> set = new HashSet<UUID>(NUM*4/3);
      for (int i = 0; i < NUM; ++i) {
         UUID uuid = random ? IrisUUID.randomUUID() : IrisUUID.timeUUID();
         assertTrue("uuid collision: " + uuid, set.add(uuid));
      }
   }
}

