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
public class TestAddressLinePronouncer extends IrisTestCase
{
   @Inject
   private UspsDataService uspsDataService;

   private AddressLinePronouncer componentUnderTest;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      componentUnderTest = new AddressLinePronouncer(uspsDataService);
   }

   @Test
   public void testStreetSuffix_byCode()
   {
      test("1 Main St", "1 MAIN STREET");
   }

   @Test
   public void testStreetSuffix_byName()
   {
      test("1 Main Street", "1 MAIN STREET");
   }

   @Test
   public void testStreetSuffix_byVariant()
   {
      test("1 Main Str", "1 MAIN STREET");
   }

   @Test
   public void testStreetSuffix_noConversion1()
   {
      test("1 Ave Maria Blvd", "1 AVE MARIA BOULEVARD");
   }

   @Test
   public void testStreetSuffix_noConversion1_nonAbbreviated()
   {
      test("1 Ave Maria Boulevard", "1 AVE MARIA BOULEVARD");
   }

   @Test
   public void testStreetSuffix_noConversion2()
   {
      test("1 St Vincent St", "1 ST VINCENT STREET");
   }

   @Test
   public void testStreetSuffix_noConversion2_nonAbbreviated()
   {
      test("1 St Vincent Street", "1 ST VINCENT STREET");
   }

   @Test
   public void testStreetSuffix_noConversion3()
   {
      test("1 Dr Martin Luther King Dr", "1 DR MARTIN LUTHER KING DRIVE");
   }

   @Test
   public void testStreetSuffix_noConversion3_nonAbbreviated()
   {
      test("1 Dr Martin Luther King Drive", "1 DR MARTIN LUTHER KING DRIVE");
   }

   @Test
   public void testStreetSuffix_invalid()
   {
      test("1 Main Abc", "1 MAIN ABC");
   }

   @Test
   public void testStreetSuffix_missing()
   {
      test("1 Main", "1 MAIN");
   }

   @Test
   public void testDirectionals()
   {
      test("1 N Main St", "1 North MAIN STREET");
   }

   @Test
   public void testDirectionals_twoLetter()
   {
      test("1 SW Main St", "1 Southwest MAIN STREET");
   }

   @Test
   public void testDirectionals_postfix()
   {
      test("1 Main St N", "1 MAIN STREET North");
   }

   @Test
   public void testDirectionals_noConversion()
   {
      test("1 N N St Apt N", "1 North N STREET Apartment N");
   }

   @Test
   public void testDirectionals_noConversion_postfix()
   {
      test("1 N St N Apt N", "1 N STREET North Apartment N");
   }

   @Test
   public void testSecondaryUnits()
   {
      test("1 Main St Apt 2E", "1 MAIN STREET Apartment 2E");
   }

   @Test
   public void testSecondaryUnits_noConversion()
   {
      test("1 Apt St Apt 2E", "1 APT STREET Apartment 2E");
   }

   @Test
   public void testSecondaryUnits_invalid()
   {
      test("1 Main St Ap 2E", "1 MAIN STREET AP 2E");
   }

   @Test
   public void testLine2()
   {
      test("Apt 2E", "Apartment 2E");
   }

   @Test
   public void testLine2_invalid()
   {
      test("Ap 2E", "AP 2E");
   }

   @Test
   public void testTrim()
   {
      test(" 1  Main   St  ", "1 MAIN STREET");
   }

   @Test
   public void testInvalid()
   {
      test("Blah", "BLAH");
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

   private void test(String inputLine, String expectedPronouncedLine)
   {
      String actualPronouncedLine = componentUnderTest.pronounce(inputLine);

      assertThat(actualPronouncedLine, equalTo(expectedPronouncedLine));
   }
}

