/*
 * Copyright (C) 2013 Les Hazlewood
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iris.security;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.SessionIdGenerator;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.UUID;

/**
 * Generates session ids, which are handed to clients verbatim as the value of the
 * auth cookie. They must therefore be unguessable.
 * <p>
 * {@link Uuids#timeBased()} is <em>not</em> suitable on its own: it packs a timestamp
 * into the most significant bits and a clock-sequence-and-node value into the least
 * significant bits, and that second half is computed once per JVM and reused for every
 * subsequent call. Every session minted by a given process therefore shares an identical
 * lower 64 bits, so anyone holding one valid session id (their own, for instance) learns
 * that half for free and only has to guess the creation timestamp to forge another.
 * <p>
 * This generator keeps the time-based most significant bits -- the version 1 nibble that
 * Cassandra's {@code timeuuid} type requires, plus the driver's monotonically increasing
 * timestamp -- and replaces the least significant bits with 62 bits drawn from a
 * {@link SecureRandom} on every call. The result is still a well-formed version 1 UUID
 * and still sorts by creation time, but is no longer predictable from another session id.
 *
 * @since 2013-06-09
 */
public class TimeUuidSessionIdGenerator  implements SessionIdGenerator {

   /** Two most significant bits of the low half are the IETF variant marker, not entropy. */
   private static final long VARIANT_MASK  = 0x3FFFFFFFFFFFFFFFL;
   private static final long VARIANT_IETF  = 0x8000000000000000L;

   private static final SecureRandom RANDOM = new SecureRandom();

   public Serializable generateId(Session session) {
      return randomizedTimeBased();
   }

   /**
    * A version 1 UUID whose timestamp is real but whose clock-sequence-and-node half is
    * unpredictable. Visible for testing.
    */
   static UUID randomizedTimeBased() {
      // Keep the driver's timestamp half: it carries the version 1 nibble that the
      // timeuuid codec enforces, and its monotonic counter keeps ids distinct even when
      // two are requested inside the same 100ns tick.
      long msb = Uuids.timeBased().getMostSignificantBits();
      long lsb = (RANDOM.nextLong() & VARIANT_MASK) | VARIANT_IETF;
      return new UUID(msb, lsb);
   }
}
