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
public class TestZipStringComparator extends IrisTestCase {

   private void compareAndAssertEqual(String zip1, String zip2) {
      ZipStringComparator zipStringComparator = new ZipStringComparator();
      int result = zipStringComparator.compare(zip1, zip2);

      assertEquals(0, result);
   }

   private void compareAndAssertNotEqual(String zip1, String zip2) {
      ZipStringComparator zipStringComparator = new ZipStringComparator();
      int result = zipStringComparator.compare(zip1, zip2);

      assertNotEquals(0, result);
   }

   @Test
   public void testSameZips() {
      compareAndAssertEqual("12345", "12345");
   }

   @Test
   public void testSameZipsDashNoDash() {
      compareAndAssertEqual("12345-", "12345");
   }

   @Test
   public void testSameZips9() {
      compareAndAssertEqual("12345-6789", "12345-6789");
   }

   @Test
   public void testSameZips9DashNoDash() {
      compareAndAssertEqual("12345-6789", "123456789");
   }

   @Test
   public void testSameZips5and9AreEqual() {
      compareAndAssertEqual("12345", "12345-6789");
   }

   @Test
   public void testSameZipsCanadaCaseInsensitive() {
      compareAndAssertEqual("a1a 1A1", "A1A 1a1");
   }

   @Test
   public void testDifferentZips() {
      String zip1 = "12346";
      String zip2 = "12345";

      compareAndAssertNotEqual(zip1, zip2);
   }

   @Test
   public void testDifferentZips9() {
      String zip1 = "12345-7890";
      String zip2 = "12345-6789";

      compareAndAssertNotEqual(zip1, zip2);
   }

   @Test
   public void testDifferentZips9and5() {
      String zip1 = "12346";
      String zip2 = "12345-6789";

      compareAndAssertNotEqual(zip1, zip2);
   }

}

