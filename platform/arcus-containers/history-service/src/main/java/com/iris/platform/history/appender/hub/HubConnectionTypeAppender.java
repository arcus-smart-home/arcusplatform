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
package com.iris.platform.history.appender.hub;

import static com.iris.messages.capability.HubNetworkCapability.ATTR_TYPE;
import static com.iris.messages.capability.HubNetworkCapability.TYPE_ETH;
import static com.iris.messages.capability.HubNetworkCapability.TYPE_3G;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.appender.AnnotatedHubValueChangeAppender;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.annotation.AutoTranslate;
import com.iris.platform.history.appender.annotation.EnumValue;


@Singleton
@AutoTranslate()
@EnumValue(attr=ATTR_TYPE, val=TYPE_ETH, tpl="hub.connectiontype.ethernet", critical=true)
@EnumValue(attr=ATTR_TYPE, val=TYPE_3G,  tpl="hub.connectiontype.cellular", critical=true)
public class HubConnectionTypeAppender extends AnnotatedHubValueChangeAppender {
	
	@Inject
	public HubConnectionTypeAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
		super(appender, cache);
	}
	
}

