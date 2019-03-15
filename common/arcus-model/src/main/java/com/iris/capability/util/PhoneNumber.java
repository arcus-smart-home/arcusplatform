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

import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * Currently the class only supports US phone number 
 * @author daniellep
 *
 */
public class PhoneNumber implements Cloneable {
	
	
	public static final String DEFAULT_COUNTRY_CODE = "1";	// default US country code
	
	private final String countryCode;
	private final String areaCode;
	private final String numberPart1;  //first 3 digits of the phone number
	private final String numberPart2;	//last 4 digits of the phone number
	
	
	PhoneNumber(String areaCode, String number) {
		this(DEFAULT_COUNTRY_CODE, areaCode, number);
	}
	
	PhoneNumber(String countryCode, String areaCode, String number) {
		this.countryCode = countryCode;
		this.areaCode = areaCode;
		this.numberPart1 = number.substring(0, 3);
		this.numberPart2 = number.substring(3);
	}
	
	public String getCountryCode() {
		return countryCode;
	}

	public String getAreaCode() {
		return areaCode;
	}

	public String getNumber() {
		return numberPart1+numberPart2;
	}
	
	public String getNumberPart1() {
		return numberPart1;
	}

	public String getNumberPart2() {
		return numberPart2;
	}
	
	
	@Override
	public String toString() {
		return "PhoneNumber [countryCode=" + countryCode + ", areaCode=" + areaCode + ", numberPart1=" + numberPart1 + ", numberPart2="
				+ numberPart2 + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((countryCode == null) ? 0 : countryCode.hashCode());
		result = prime * result + ((areaCode == null) ? 0 : areaCode.hashCode());
		result = prime * result + ((numberPart1 == null) ? 0 : numberPart1.hashCode());
		result = prime * result + ((numberPart2 == null) ? 0 : numberPart2.hashCode());
		return result;
	}

   @Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PhoneNumber other = (PhoneNumber) obj;
		if (countryCode == null) {
			if (other.countryCode != null)
				return false;
		} else if (!countryCode.equals(other.countryCode))
			return false;
		if (areaCode == null) {
			if (other.areaCode != null)
				return false;
		} else if (!areaCode.equals(other.areaCode))
			return false;
		if (numberPart1 == null) {
			if (other.numberPart1 != null)
				return false;
		} else if (!numberPart1.equals(other.numberPart1))
			return false;
		if (numberPart2 == null) {
			if (other.numberPart2 != null)
				return false;
		} else if (!numberPart2.equals(other.numberPart2))
			return false;
		return true;
	}

	public Map<String, Object> toMap(){
		return ImmutableMap.<String, Object>of(
			"countryCode", this.countryCode,
			"areaCode", this.areaCode,
			"numberPart1", this.numberPart1,
			"numberPart2", this.numberPart2
		);
	}

}

