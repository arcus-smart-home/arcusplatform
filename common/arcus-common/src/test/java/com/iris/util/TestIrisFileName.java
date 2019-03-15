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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestIrisFileName {
   @Test
   public void testContainsInvalidCharacter() {
      Boolean result = IrisFileName.containsInvalidCharacter("good.txt");
      Assert.assertFalse(result);

      result = IrisFileName.containsInvalidCharacter("foo-ä-€.html");
      Assert.assertFalse(result);

      result = IrisFileName.containsInvalidCharacter("fdgjnhg?,.html");
      Assert.assertTrue(result);

   }

   @Test
   public void testClean() {
      String fileName = IrisFileName.clean("good.txt", "_");
      Assert.assertEquals(fileName, "good.txt");

      fileName = IrisFileName.clean("foo-ä-€.html", "_");
      Assert.assertEquals(fileName, "foo-ä-€.html");

      fileName = IrisFileName.clean("dsfd?.html", "_");
      Assert.assertEquals(fileName, "dsfd_.html");

      fileName = IrisFileName.clean("foo-ä-€.?html", "_");
      Assert.assertEquals(fileName, "foo-ä-€._html");

      fileName = IrisFileName.clean(" foo-ä-€.?html\t", "_");
      Assert.assertEquals(fileName, "foo-ä-€._html");
   }

   @Test
   public void testIsValid() {
      Boolean result = IrisFileName.isValid("good.txt");
      Assert.assertTrue(result);

      result = IrisFileName.isValid("foo-ä-€.html");
      Assert.assertTrue(result);

      result = IrisFileName.isValid("dsfd*.html");
      Assert.assertFalse(result);

      result = IrisFileName.isValid("com1");
      Assert.assertFalse(result);

      String tooLong = new String(new char[256]).replace('\0', 'w');
      result = IrisFileName.isValid(tooLong);
      Assert.assertFalse(result);

      result = IrisFileName.isValid("  \t");
      Assert.assertFalse(result);
   }

}

