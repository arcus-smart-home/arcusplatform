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

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Currently the class only supports US phone number 
 * @author daniellep
 *
 */
public class PhoneNumbers {
	private final static Pattern NUMBERS_10_PATTERN = Pattern.compile("\\d{10}");  //number only, 10 digit
	private final static Pattern NUMBERS_11_PATTERN = Pattern.compile("\\d{11}");  //number only, 11 digit with the country code
	private final static Pattern NUMBERS_10_DASHES_PATTERN = Pattern.compile("\\d{3}[-\\s]\\d{3}[-\\s]\\d{4}");  //10 digit with dashes/space. i.e. 111-222-3333
	private final static Pattern NUMBERS_11_DASHES_PATTERN = Pattern.compile("1[-\\s]\\d{3}[-\\s]\\d{3}[-\\s]\\d{4}");  //11 digit with dashes/space and country code. i.e. 1-111-222-3333
	private final static Pattern NUMBERS_10_PARENS_PATTERN = Pattern.compile("\\(\\d{3}\\)[-\\s]\\d{3}[-\\s]\\d{4}");  //10 digit with parentheses. i.e. (111) 222-3333
	private final static Pattern NUMBERS_11_PARENS_PATTERN = Pattern.compile("\\+1\\(\\d{3}\\)[-\\s]\\d{3}[-\\s]\\d{4}");  //11 digit with parentheses with country code. i.e. +1(111) 222-3333
	
	
	public enum PhoneNumberFormat {
		NUMBERS(NUMBERS_10_PATTERN, "%s%s%s"),
		NUMBERS_WITH_COUNTRY_CODE(NUMBERS_11_PATTERN,"1%s%s%s"),
		PARENS(NUMBERS_10_PARENS_PATTERN, "(%s) %s-%s"),
		PARENS_WITH_COUNTRY_CODE(NUMBERS_11_PARENS_PATTERN, "+1(%s) %s-%s"),
		DASHES(NUMBERS_10_DASHES_PATTERN, "%s-%s-%s"),
		DASHES_WITH_COUNTRY_CODE(NUMBERS_11_DASHES_PATTERN, "1-%s-%s-%s");
		
		private final Pattern matchingPattern;
		private final String formatString;
		
		PhoneNumberFormat(Pattern p, String formatString) {
			this.matchingPattern = p;
			this.formatString = formatString;
		}
		
		public Pattern getPattern() {
			return matchingPattern;
		}
		
		public boolean matches(String str) {
			return matchingPattern.matcher(str).matches();
		}

		public String getFormatString()
		{
			return formatString;
		}
	}
	
	
	@Nullable
	public static PhoneNumber fromString(String phoneNumber) {
		if(StringUtils.isNotBlank(phoneNumber)) {
			String cleansedPhone1 = phoneNumber.replaceAll("[^0-9]", "");
			int len = cleansedPhone1.length();
			if(len == 10) {
				return new PhoneNumber(cleansedPhone1.substring(0, 3), cleansedPhone1.substring(3));
			}else if(len == 11) {
				String countryCode = cleansedPhone1.substring(0, 1);
				if(PhoneNumber.DEFAULT_COUNTRY_CODE.equals(countryCode)) {
					return new PhoneNumber(cleansedPhone1.substring(1, 4), cleansedPhone1.substring(4));
				}else{
					throw new IllegalArgumentException(phoneNumber + " is not a valid US phone number");
				}				
			}else{
				//invalid number
				throw new IllegalArgumentException(phoneNumber + " is not a valid US phone number");
			}
		}else{
			return null;
		}
	}
	
	
	public static String format(PhoneNumber phone, PhoneNumberFormat format) {
		return String.format(format.formatString, phone.getAreaCode(), phone.getNumberPart1(), phone.getNumberPart2());
	}
	


}

