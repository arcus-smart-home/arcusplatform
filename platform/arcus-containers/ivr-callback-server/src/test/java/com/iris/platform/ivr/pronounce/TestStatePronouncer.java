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
package com.iris.platform.ivr.pronounce;

import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;

import com.google.inject.Inject;
import com.iris.platform.location.UspsDataService;
import com.iris.platform.location.UspsDataServiceModule;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules({UspsDataServiceModule.class})
public class TestStatePronouncer extends IrisTestCase
{
   @Inject
   private UspsDataService uspsDataService;

   private StatePronouncer componentUnderTest;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      componentUnderTest = new StatePronouncer(uspsDataService);
   }

   @Test
   public void testNormal()
   {
      test("IL", "Illinois");
   }

   @Test
   public void testCompound()
   {
      test("FM", "Federated States of Micronesia");
   }

   @Test
   public void testNonExistent()
   {
      test("AA", "AA");
   }

   @Test
   public void testTrim()
   {
      test(" IL ", "Illinois");
   }

   @Test
   public void testInvalid()
   {
      test("BLAH", "BLAH");
   }

   @Test
   public void testEmpty()
   {
      test("", "");
   }

   @Test
   public void testNull()
   {
      test(null, null);
   }

   private void test(String inputState, String expectedPronouncedState)
   {
      String actualPronouncedState = componentUnderTest.pronounce(inputState);

      assertThat(actualPronouncedState, equalTo(expectedPronouncedState));
   }
}

