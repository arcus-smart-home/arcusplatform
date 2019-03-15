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
 * Created by wesleystueve on 4/20/17.
 */
public class TestStreetAddressZipBuilder extends IrisTestCase {

   @Test
   public void testInputEqualsOutputUsa5Digit() {
      StreetAddress.ZipBuilder builder = StreetAddress.zipBuilder();
      String result = builder.withZip("66061").build();

      assertEquals("66061", result);
   }

   @Test
   public void testInputEqualsOutputUsaZipPlus4() {
      StreetAddress.ZipBuilder builder = StreetAddress.zipBuilder();
      String result = builder.withZip("66061-1234").build();

      assertEquals("66061-1234", result);
   }

   @Test
   public void testUsaZipPlus4Build() {
      StreetAddress.ZipBuilder builder = StreetAddress.zipBuilder();
      String result = builder
            .withZip("66061")
            .withZipPlus4("1234")
            .build();

      assertEquals("66061-1234", result);
   }

   @Test
   public void testUsaZipPlus4Plus4ReplacementBuild() {
      StreetAddress.ZipBuilder builder = StreetAddress.zipBuilder();
      String result = builder
            .withZip("66061-1234")
            .withZipPlus4("5678")
            .build();

      assertEquals("66061-5678", result);
   }
}

