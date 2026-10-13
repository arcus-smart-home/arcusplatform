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
package com.iris.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.datastax.oss.driver.api.core.ProtocolVersion;
import com.datastax.oss.driver.api.core.type.codec.TypeCodecs;

public class TimeUuidSessionIdGeneratorTest {

   private static final int SAMPLES = 10000;

   /**
    * Session ids are stored in a {@code timeuuid} column, whose codec rejects anything
    * that isn't a version 1 UUID. Randomizing the low half must not break that.
    */
   @Test
   public void testAcceptedByTimeUuidCodec() {
      for(int i = 0; i < 100; i++) {
         UUID id = TimeUuidSessionIdGenerator.randomizedTimeBased();
         assertEquals("must remain a version 1 UUID", 1, id.version());
         // Throws IllegalArgumentException if the driver would refuse to bind it.
         TypeCodecs.TIMEUUID.encode(id, ProtocolVersion.DEFAULT);
      }
   }

   @Test
   public void testIetfVariant() {
      for(int i = 0; i < 100; i++) {
         assertEquals(2, TimeUuidSessionIdGenerator.randomizedTimeBased().variant());
      }
   }

   /**
    * The regression this class exists to prevent: {@code Uuids.timeBased()} reuses one
    * clock-sequence-and-node value for the life of the JVM, making the low 64 bits of
    * every session id identical and the id as a whole guessable.
    */
   @Test
   public void testLowBitsDifferBetweenIds() {
      Set<Long> lowBits = new HashSet<>();
      for(int i = 0; i < SAMPLES; i++) {
         lowBits.add(TimeUuidSessionIdGenerator.randomizedTimeBased().getLeastSignificantBits());
      }
      // 62 bits of entropy over 10k draws: collisions are not plausible, so anything
      // short of all-distinct means the low half is being reused.
      assertEquals("low bits must not repeat across session ids", SAMPLES, lowBits.size());
   }

   @Test
   public void testIdsAreUnique() {
      Set<UUID> ids = new HashSet<>();
      for(int i = 0; i < SAMPLES; i++) {
         ids.add(TimeUuidSessionIdGenerator.randomizedTimeBased());
      }
      assertEquals(SAMPLES, ids.size());
   }

   /**
    * Guards against a lazy fix that randomizes only a few bits: over many samples every
    * bit position in the low half other than the two variant bits should vary.
    */
   @Test
   public void testAllNonVariantLowBitsVary() {
      long ored = 0L;
      long anded = ~0L;
      for(int i = 0; i < SAMPLES; i++) {
         long lsb = TimeUuidSessionIdGenerator.randomizedTimeBased().getLeastSignificantBits();
         ored |= lsb;
         anded &= lsb;
      }
      // A bit that never varies shows up as set in `anded` or clear in `ored`.
      long varying = ored & ~anded;
      long expected = 0x3FFFFFFFFFFFFFFFL; // everything except the two variant bits
      assertEquals("every non-variant low bit should take both values", expected, varying);
   }

   @Test
   public void testGeneratorReturnsUuid() {
      Object id = new TimeUuidSessionIdGenerator().generateId(null);
      assertTrue(id instanceof UUID);
      assertEquals(1, ((UUID) id).version());
   }
}
