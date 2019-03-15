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

import static com.iris.messages.capability.HubPowerCapability.ATTR_SOURCE;
import static com.iris.messages.capability.HubPowerCapability.SOURCE_MAINS;
import static com.iris.messages.capability.HubPowerCapability.SOURCE_BATTERY;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.appender.AnnotatedHubValueChangeAppender;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.annotation.AutoTranslate;
import com.iris.platform.history.appender.annotation.EnumValue;


@Singleton
@AutoTranslate()
@EnumValue(attr=ATTR_SOURCE, val=SOURCE_MAINS, tpl="hub.power.mains", critical=true)
@EnumValue(attr=ATTR_SOURCE, val=SOURCE_BATTERY,  tpl="hub.power.battery", critical=true)
public class HubBatteryAppender extends AnnotatedHubValueChangeAppender {
	
	@Inject
	public HubBatteryAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
		super(appender, cache);
	}
	
}

