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

import com.iris.messages.PlatformMessage;
import com.iris.platform.history.appender.MessageContext;
import com.iris.platform.history.appender.matcher.MatchResults;

public interface ValueGetter {
	public final static String PLACE_NAME = "vc_place_name";
	public final static String ATTR_VALUE = "vc_attr_value";
	public final static String ATTR_VALUE_AS_FAHRENHEIT = "vc_attr_value_as_fahrenheit";
	
	String get(PlatformMessage message, MessageContext context, MatchResults matchResults);
}

