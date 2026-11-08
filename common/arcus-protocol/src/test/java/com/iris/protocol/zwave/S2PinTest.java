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
package com.iris.protocol.zwave;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class S2PinTest {

   @Test
   public void testPinIsExactlyFiveDigitsThatFitTwoBytes() {
      assertTrue(S2Pin.isValidPin("00000"));
      assertTrue(S2Pin.isValidPin("00042"));
      assertTrue(S2Pin.isValidPin("65535"));

      assertFalse(S2Pin.isValidPin("65536"));
      assertFalse(S2Pin.isValidPin("99999"));
   }

   @Test
   public void testPinRejectsAnythingElse() {
      assertFalse(S2Pin.isValidPin(null));
      assertFalse(S2Pin.isValidPin(""));
      assertFalse(S2Pin.isValidPin("1234"));
      assertFalse(S2Pin.isValidPin("123456"));
      assertFalse(S2Pin.isValidPin("1234a"));
      assertFalse(S2Pin.isValidPin(" 1234"));
      assertFalse(S2Pin.isValidPin("1234\n"));
      assertFalse(S2Pin.isValidPin("-1234"));
      // digits to Unicode, but not the digits the hub can use
      assertFalse(S2Pin.isValidPin("１２３４５"));
      // the type matters too: a number has lost any leading zeros
      assertFalse(S2Pin.isValidPin(12345));
   }

   @Test
   public void testBlankedDsk() {
      assertTrue(S2Pin.isValidBlankedDsk("00000-11111-22222-33333-44444-55555-11111-22222"));
      assertTrue(S2Pin.isValidBlankedDsk("00000-65535-00000-00000-00000-00000-00000-65535"));
   }

   @Test
   public void testDskWithItsFirstGroupFilledInIsRefused() {
      assertFalse(S2Pin.isValidBlankedDsk("12345-11111-22222-33333-44444-55555-11111-22222"));
   }

   @Test
   public void testMalformedDsk() {
      assertFalse(S2Pin.isValidBlankedDsk(null));
      assertFalse(S2Pin.isValidBlankedDsk(""));
      assertFalse(S2Pin.isValidBlankedDsk(12345));
      assertFalse(S2Pin.isValidBlankedDsk("00000-11111-22222-33333-44444-55555-11111"));
      assertFalse(S2Pin.isValidBlankedDsk("00000-11111-22222-33333-44444-55555-11111-22222-33333"));
      assertFalse(S2Pin.isValidBlankedDsk("00000-11111-22222-33333-44444-55555-11111-2222a"));
      assertFalse(S2Pin.isValidBlankedDsk("00000 11111 22222 33333 44444 55555 11111 22222"));
      assertFalse(S2Pin.isValidBlankedDsk("00000-65536-22222-33333-44444-55555-11111-22222"));
      assertFalse(S2Pin.isValidBlankedDsk("00000-11111-22222-33333-44444-55555-11111-99999"));
   }
}
