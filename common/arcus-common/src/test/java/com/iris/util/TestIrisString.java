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
public class TestIrisString {
   @Test
   public void testCombine() {
      String result = IrisString.joinIfNotEmpty("_", "1", "2");
      Assert.assertEquals("1_2", result);

      result = IrisString.joinIfNotEmpty("_", "1", "2", "3");
      Assert.assertEquals("1_2_3", result);

      result = IrisString.joinIfNotEmpty("_", "", "2");
      Assert.assertEquals("2", result);

      result = IrisString.joinIfNotEmpty("_", "1", null, "3");
      Assert.assertEquals("1_3", result);
   }

}

