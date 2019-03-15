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
package com.iris.capability.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.iris.capability.util.PhoneNumbers.PhoneNumberFormat;



@RunWith(value = Parameterized.class)
public class TestValidPhoneNumber
{
	@Parameters(name="phoneNumber[{0}], expectedCountryCode[{2}], expectedAreaCode[{3}],expectedNumberPart1[{4}],expectedNumberPart2[{5}], comparedPhoneNumber[{6}]")
   public static List<Object []> files() {
      return Arrays.<Object[]>asList(
            new Object [] { "2223334444", "1", "222", "333", "4444", "12223334444"},
            new Object [] { "12223334444", "1", "222", "333", "4444", "(222)333-4444"},
            new Object [] { "(222)333-4444", "1", "222", "333", "4444", "(222) 333-4444"},
            new Object [] { "(222) 333-4444", "1", "222", "333", "4444", "+1(222)333-4444"},
            new Object [] { "+1(222)333-4444", "1", "222", "333", "4444", "+1(222) 333-4444"},
            new Object [] { "+1(222) 333-4444", "1", "222", "333", "4444", "222-333-4444"},
            new Object [] { "222-333-4444", "1", "222", "333", "4444", "1-222-333-4444"},
            new Object [] { "1-222-333-4444", "1", "222", "333", "4444", "2223334444"}
      );
   }
   
   private final String phoneNumber;
   private final String expectedCountryCode;
   private final String expectedAreaCode;
   private final String expectedNumberPart1;
   private final String expectedNumberPart2;
   private PhoneNumber phone1;
   private final String comparedNumber;

	
	
	public TestValidPhoneNumber(String phoneNumberStr, String expectedCountryCode, String expectedAreaCode, String expectedNumberPart1, String expectedNumberPart2, String comparedNumber) {
		this.phoneNumber = phoneNumberStr;
		this.expectedCountryCode = expectedCountryCode;
		this.expectedAreaCode = expectedAreaCode;
		this.expectedNumberPart1 = expectedNumberPart1;
		this.expectedNumberPart2 = expectedNumberPart2;
		this.comparedNumber = comparedNumber;
	}
	
	@Before
	public void setUp() throws Exception {
		try {
			phone1 = PhoneNumbers.fromString(this.phoneNumber);			
		}catch(Exception e) {			
			fail(this.phoneNumber + " should be valid");
		}
	}

	@After
	public void tearDown() throws Exception {
	}
	

	@Test
	public void testParsePhoneNumber() {
		assertEquals(expectedCountryCode, phone1.getCountryCode());
		assertEquals(expectedAreaCode, phone1.getAreaCode());
		assertEquals(expectedNumberPart1+expectedNumberPart2, phone1.getNumber());
	}
	
	@Test
	public void testFormat() throws IOException {
		assertEquals(String.format("%s%s", phone1.getAreaCode(), phone1.getNumber()), PhoneNumbers.format(phone1, PhoneNumberFormat.NUMBERS));
		assertEquals(String.format("%s%s%s", phone1.getCountryCode(), phone1.getAreaCode(), phone1.getNumber()), PhoneNumbers.format(phone1, PhoneNumberFormat.NUMBERS_WITH_COUNTRY_CODE));
		assertEquals(String.format("%s-%s-%s", phone1.getAreaCode(), phone1.getNumber().substring(0, 3), phone1.getNumber().substring(3)), PhoneNumbers.format(phone1, PhoneNumberFormat.DASHES));
		assertEquals(String.format("%s-%s-%s-%s", phone1.getCountryCode(), phone1.getAreaCode(), phone1.getNumber().substring(0, 3), phone1.getNumber().substring(3)), PhoneNumbers.format(phone1, PhoneNumberFormat.DASHES_WITH_COUNTRY_CODE));
		assertEquals(String.format("(%s) %s-%s", phone1.getAreaCode(), phone1.getNumber().substring(0, 3), phone1.getNumber().substring(3)), PhoneNumbers.format(phone1, PhoneNumberFormat.PARENS));
		assertEquals(String.format("+%s(%s) %s-%s", phone1.getCountryCode(), phone1.getAreaCode(), phone1.getNumber().substring(0, 3), phone1.getNumber().substring(3)), PhoneNumbers.format(phone1, PhoneNumberFormat.PARENS_WITH_COUNTRY_CODE));			
	}
	
	@Test
	public void testEquals() {
		PhoneNumber phone2 = PhoneNumbers.fromString(comparedNumber);
		assertEquals(phone1, phone2);
	}

}

