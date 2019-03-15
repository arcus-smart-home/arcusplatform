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
package com.iris.platform.history.appender.translator;

import java.util.Map;

import com.iris.platform.history.appender.matcher.MatchResults;
import com.iris.util.IrisCollections;

public class ValueGetterFactory {
	private final static Map<String, ValueGetter> getters = IrisCollections.<String, ValueGetter>immutableMap()
			.put(ValueGetter.PLACE_NAME, (msg, ctx, results) -> ctx.getPlaceName())
			.put(ValueGetter.ATTR_VALUE, (msg, ctx, results) -> String.valueOf(getValue(results)))
			.put(ValueGetter.ATTR_VALUE_AS_FAHRENHEIT, (msg, ctx, results) -> convertCelciusToFahrenheit(getValue(results)))
			.create();
	
	public static ValueGetter findGetter(String getterType) {
		return getters.get(getterType);
	}
	
	public static Object getValue(MatchResults results) {
		return results.getFoundValue() != null ? results.getFoundValue() : "";
	}
	
	public static String convertCelciusToFahrenheit(Object value) {
		return value instanceof String ? (String)value : String.valueOf(Math.round((((double)value * 9 / 5.0) + 32)));
	}
}

