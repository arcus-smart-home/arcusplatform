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
 * Created by wesleystueve on 4/24/17.
 */
public class TestStreetAddressLinesComparator extends IrisTestCase {

   private void compareAndAssertEqual(StreetAddress streetAddress1, StreetAddress streetAddress2) {
      StreetAddressLinesComparator streetAddressLinesComparator = new StreetAddressLinesComparator();
      int result = streetAddressLinesComparator.compare(streetAddress1, streetAddress2);

      assertEquals(0, result);
   }

   private void compareAndAssertNotEqual(StreetAddress streetAddress1, StreetAddress streetAddress2) {
      StreetAddressLinesComparator streetAddressLinesComparator = new StreetAddressLinesComparator();
      int result = streetAddressLinesComparator.compare(streetAddress1, streetAddress2);

      assertNotEquals(0, result);
   }

   @Test
   public void testSameStreetAddressesDifferentLine1WithLine2ConcatAreEqual()
   {
      StreetAddress streetAddress1 = StreetAddress.builder()
            .withCity("Olathe")
            .withState("KS")
            .withStreetName("Carbondale")
            .withStreetNumber("11458")
            .withStreetPreDirection("S")
            .withStreetSuffix("St")
            .withZip("66061")
            .withZipPlus4("1234")
            .build();
      streetAddress1.setLine1(streetAddress1.getLine1() + " Apt 201");

      StreetAddress streetAddress2 = StreetAddress.builder()
            .withAddress2("APT 201")
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
   public void testSameStreetAddressesLine1AndDifferentLine2AreEqual()
   {
      StreetAddress streetAddress1 = StreetAddress.builder()
            .withAddress2("I DON'T MATTER")
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
            .withAddress2("I'm unique and different.")
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

   public void testDifferentStreetAddressesLine1AreNotEqual()
   {
      StreetAddress streetAddress1 = StreetAddress.builder()
            .withAddress2("I DON'T MATTER")
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
            .withAddress2("I'm unique and different.")
            .withCity("olathe")
            .withState("kS")
            .withStreetName("carbondale")
            .withStreetNumber("11458")
            .withStreetPreDirection("N")
            .withStreetSuffix("st")
            .withZip("66061")
            .withZipPlus4("1234")
            .build();

      compareAndAssertNotEqual(streetAddress1, streetAddress2);
   }
}

