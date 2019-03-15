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
package com.iris.platform.address;

import com.iris.test.IrisTestCase;
import org.junit.Test;

/**
 * Created by wesleystueve on 4/13/17.
 */
public class TestStreetAddressLenientComparator extends IrisTestCase
{
   private void compareAndAssertEqual(StreetAddress streetAddress1, StreetAddress streetAddress2) {
      StreetAddressLenientComparator streetAddressLenientComparator = new StreetAddressLenientComparator();
      int result = streetAddressLenientComparator.compare(streetAddress1, streetAddress2);

      assertEquals(0, result);
   }

   private void compareAndAssertNotEqual(StreetAddress streetAddress1, StreetAddress streetAddress2) {
      StreetAddressLenientComparator streetAddressLenientComparator = new StreetAddressLenientComparator();
      int result = streetAddressLenientComparator.compare(streetAddress1, streetAddress2);

      assertNotEquals(0, result);
   }

   @Test
   public void testNullAndNullStreetAddressEqual()
   {
      compareAndAssertEqual(null, null);
   }

   @Test
   public void testNullAndNotNullStreetAddressNotEqual()
   {
      compareAndAssertNotEqual(null, new StreetAddress());
   }

   @Test
   public void testNotNullAndNullStreetAddressNotEqual()
   {
      compareAndAssertNotEqual(new StreetAddress(), null);
   }

   @Test
   public void testDegenerativeStreetAddressesEqual()
   {
      compareAndAssertEqual(new StreetAddress(), new StreetAddress());
   }

   @Test
   public void testSameStreetAddressesAreEqualCaseInsensitive()
   {
      StreetAddress streetAddress1 = StreetAddress.builder()
            .withAddress2("Address Line 2")
            .withCity("Olathe")
            .withState("KS")
            .withStreetName("Carbondale")
            .withStreetNumber("11458")
            .withStreetPreDirection("S")
            .withStreetSuffix("St")
            .withZip("66061")
            .withZipPlus4("1234")
            .build();

      StreetAddress streetAddress2 = StreetAddress.builder()
            .withAddress2("address Line 2")
            .withCity("olathe")
            .withState("kS")
            .withStreetName("carbondale")
            .withStreetNumber("11458")
            .withStreetPreDirection("s")
            .withStreetSuffix("st")
            .withZip("66061")
            .withZipPlus4("1234")
            .build();

      compareAndAssertEqual(streetAddress1, streetAddress2);
   }

   @Test
   public void testSameStreetAddressesAreEqualCaseInsensitiveAndNullEqualsEmptyString()
   {
      StreetAddress streetAddress1 = StreetAddress.builder()
            .withAddress2("")
            .withCity("Olathe")
            .withState("KS")
            .withStreetName("Carbondale")
            .withStreetNumber("11458")
            .withStreetPreDirection("S")
            .withStreetSuffix("St")
            .withZip("66061")
            .withZipPlus4("1234")
            .build();

      StreetAddress streetAddress2 = StreetAddress.builder()
            .withCity("olathe")
            .withState("kS")
            .withStreetName("carbondale")
            .withStreetNumber("11458")
            .withStreetPreDirection("s")
            .withStreetSuffix("st")
            .withZip("66061")
            .withZipPlus4("1234")
            .build();

      compareAndAssertEqual(streetAddress1, streetAddress2);
   }

   @Test
   public void testStreetAddressesWithDifferent9and5ZipAreEqualCaseInsensitive()
   {
      StreetAddress streetAddress1 = StreetAddress.builder()
            .withAddress2("Address Line 2")
            .withCity("Olathe")
            .withState("KS")
            .withStreetName("Carbondale")
            .withStreetNumber("11458")
            .withStreetPreDirection("S")
            .withStreetSuffix("St")
            .withZip("66061")
            .build();

      StreetAddress streetAddress2 = StreetAddress.builder()
            .withAddress2("address Line 2")
            .withCity("olathe")
            .withState("kS")
            .withStreetName("carbondale")
            .withStreetNumber("11458")
            .withStreetPreDirection("s")
            .withStreetSuffix("st")
            .withZip("66061")
            .withZipPlus4("1234")
            .build();

      compareAndAssertEqual(streetAddress1, streetAddress2);
   }

   @Test
   public void testStreetAddressesWithDifferent9ZipAreNotEqual()
   {
      StreetAddress streetAddress1 = StreetAddress.builder()
            .withAddress2("Address Line 2")
            .withCity("Olathe")
            .withState("KS")
            .withStreetName("Carbondale")
            .withStreetNumber("11458")
            .withStreetPreDirection("S")
            .withStreetSuffix("St")
            .withZip("66061")
            .withZipPlus4("5678")
            .build();

      StreetAddress streetAddress2 = StreetAddress.builder()
            .withAddress2("address Line 2")
            .withCity("olathe")
            .withState("kS")
            .withStreetName("carbondale")
            .withStreetNumber("11458")
            .withStreetPreDirection("s")
            .withStreetSuffix("st")
            .withZip("66061")
            .withZipPlus4("1234")
            .build();

      compareAndAssertNotEqual(streetAddress1, streetAddress2);
   }

   @Test
   public void testDifferentStreetAddressesAreNotEqual()
   {
      StreetAddress streetAddress1 = StreetAddress.builder()
            .withCity("Olathe")
            .withState("KS")
            .withStreetName("Carbondale")
            .withStreetNumber("11458")
            .withStreetPreDirection("S")
            .withStreetSuffix("St")
            .withZip("66061")
            .build();

      StreetAddress streetAddress2 = StreetAddress.builder()
            .withCity("Eudora")
            .withState("kS")
            .withStreetName("Peach")
            .withStreetNumber("1022")
            .withStreetSuffix("st")
            .withZip("66512")
            .build();

      compareAndAssertNotEqual(streetAddress1, streetAddress2);
   }
}

