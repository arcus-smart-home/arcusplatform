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
package com.iris.oculus.modules.pairing.wizard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

public class S2PinsTest {

   @Test
   public void theUserChecksEverythingButThePin() {
      assertEquals("11111-22222-33333-44444-55555-11111-22222",
         S2Pins.groupsToCheck("00000-11111-22222-33333-44444-55555-11111-22222"));
   }

   @Test
   public void anEmptyRequestIdMeansNoRequest() {
      assertFalse(S2Pins.isWaiting(null));
      assertFalse(S2Pins.isWaiting(""));
      assertTrue(S2Pins.isWaiting("b0666ff0-bbf4-4737-ae88-000000000000"));
   }

   @Test
   public void theCountdownEndsAtZero() {
      assertEquals(30, S2Pins.secondsLeft(new Date(30000), 0));
      assertEquals(1, S2Pins.secondsLeft(new Date(30000), 29500));
      assertEquals(0, S2Pins.secondsLeft(new Date(30000), 40000));
      assertEquals(0, S2Pins.secondsLeft(null, 0));
   }
}
