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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestHex {

   @Test
   public void testOneByte() {
      String str = "FF";
      byte b = (byte) Hex.fromString(str);
      assertTrue(b == (byte) 0xFF);
   }

   @Test
   public void testFromHexString() {

      String str = "0102030405060708090A0B0C0D0E0F10";
      byte[] bytes = Hex.fromHexString(str);
      for (int i = 0; i < bytes.length; i++) {
         assertTrue(bytes[i] == (byte) (i + 1));
      }
      String str2 = Hex.toString(bytes);
      assertTrue(str.equals(str2));
   }

   @Test
   public void testPrint() {

      String str = "0102030405060708090A0B0C0D0E0F10";
      String print = "[ 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F 10 ]";
      byte[] bytes = Hex.fromHexString(str);
      for (int i = 0; i < bytes.length; i++) {
         assertTrue(bytes[i] == (byte) (i + 1));
      }
      String str2 = Hex.toPrint(bytes);
      assertTrue(print.equals(str2));
   }

}

