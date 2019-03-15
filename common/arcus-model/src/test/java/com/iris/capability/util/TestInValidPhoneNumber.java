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

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public class TestInValidPhoneNumber
{
	@Parameters(name="phoneNumber[{0}], expectedCountryCode[{2}], expectedAreaCode[{3}],expectedNumberPart1[{4}],expectedNumberPart2[{5}], comparedPhoneNumber[{6}]")
   public static List<Object []> files() {
      return Arrays.<Object[]>asList(
            new Object [] { "122223334444"},  //too many digits
            new Object [] { "223334444"},	//too few digits
            new Object [] { "62223334444"}	//wrong country code            
      );
   }
   
   private final String phoneNumber;
	
	
	public TestInValidPhoneNumber(String phoneNumberStr) {
		this.phoneNumber = phoneNumberStr;
	}
	
	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {
	}
	

	@Test
	public void testParsePhoneNumber() {
		PhoneNumber phone1 = null;
		try {
			phone1 = PhoneNumbers.fromString(this.phoneNumber);	
			fail(this.phoneNumber + " should be invalid");
		}catch(IllegalArgumentException e) {			
			//ok
		}
		
	}
	
	
}

