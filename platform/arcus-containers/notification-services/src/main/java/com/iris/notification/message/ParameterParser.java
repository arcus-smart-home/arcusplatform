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
package com.iris.notification.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.iris.messages.address.Address;

public class ParameterParser {
	private static Logger logger = LoggerFactory.getLogger(ParameterParser.class);
	private static Pattern lookupPattern = Pattern.compile("^\\{(.*)\\}\\.(.*)$");
	
	private final ValueLookup valueLookup;
	
	@Inject
	ParameterParser(ValueLookup valueLookup) {
		this.valueLookup = valueLookup;
	}
	
	public String parse(String value) {
		List<String> values = splitter(preProcess(value));
		return formatValues(values);
	}
	
	private List<String> splitter(String value) {
		if (value.startsWith("[")) {
			if (value.endsWith("]")) {
				String stripped = strip(value);
				String[] values = stripped.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
				if (values != null && values.length > 0) {
					List<String> list = new ArrayList<>(values.length);
					for (String s : values) {
						list.add(process(s));
					}
					return list;
				}
				else {
					return Collections.emptyList();
				}
			}
			else {
				logger.error("Notification parameter starts with '[' but doesn't end with ']'");
			}
		}
		// Not a list.
		List<String> results = new ArrayList<>(1);
		results.add(process(value));
		return results;
	}
	
	private String process(String value) {
		String preProcessedString = preProcess(value);
		if (isQuoted(preProcessedString)) {
			return strip(preProcessedString);
		}
		Matcher lookupMatcher = lookupPattern.matcher(preProcessedString);
		if (lookupMatcher.matches()) {
			System.out.println("MATCHES!! " + preProcessedString);
			return lookupValue(lookupMatcher.group(1), lookupMatcher.group(2));
		}
		return preProcessedString;
	}
	
	private String lookupValue(String lookup, String attribute) {
		//Only Addresses are currently supported.
		try {
			Address address = Address.fromString(lookup.trim());
			System.out.println("Do Lookup " + address + " - " + attribute);
			return valueLookup.get(address, attribute);
		} 
		catch (IllegalArgumentException iae) {
			logger.error("Illegal value for parameter parser lookup in notification: {}", lookup);
			return "";
		}
	}
	
	private static String preProcess(String value) {
		if(value != null) {
			return value.trim();
		}else {
			return "";
		}
	}
	
	private static boolean isQuoted(String value) {
		return value.startsWith("\"") && value.endsWith("\"");
	}
	
	private static String strip(String value) {
		if (value != null && value.length() > 1) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}
	
	private static String formatValues(List<String> values) {
		if (values == null || values.isEmpty()) {
			return "";
		}
		if (values.size() == 1) {
			return values.get(0);
		}
		if (values.size() == 2) {
			return values.get(0) + " and " + values.get(1);
		}
		int numberOfValues = values.size();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < numberOfValues; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			if (i == (numberOfValues - 1)) {
				sb.append("and ");
			}
			sb.append(values.get(i));
		}
		return sb.toString();
	}
}

