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
package com.iris.core.template;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;

import org.apache.commons.lang3.StringUtils;

import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.iris.util.Net;
import com.iris.util.UnitConversion;

public class HandlebarsHelpersSource {
	
	public static CharSequence section(String sectionName, Options options) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("---%s---", sectionName))
		.append(options.fn())
		.append(String.format("!--%s---", sectionName));
		return sb.toString();
	}
	public static CharSequence timestampFormat(String timestampVar, Options options) throws IOException {
		Date timestampDateValue = new Date(Long.parseLong(timestampVar));
		return StringHelpers.dateFormat.apply(timestampDateValue, options);
	}
     
	public static CharSequence fallback(String main, String fallback, Options options) throws IOException {
	   return main!=null?main:fallback;
   }
	
	public static CharSequence celsiusToFahrenheit(String temperatureInC, Options options) throws IOException {
		String formattedValue = "";
		if(StringUtils.isNoneBlank(temperatureInC)) {
			double temperatureInCValue = Double.valueOf(temperatureInC);
			formattedValue = BigDecimal.valueOf(UnitConversion.tempCtoF(temperatureInCValue)).setScale(0, BigDecimal.ROUND_HALF_DOWN).toString();			 
		}
		return formattedValue;
	}
	
   public static CharSequence urlEncode(String s) {
      return Net.urlEncode(s);
   }
}

