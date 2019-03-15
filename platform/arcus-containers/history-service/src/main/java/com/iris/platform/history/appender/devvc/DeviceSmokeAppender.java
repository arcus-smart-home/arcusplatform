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
/**
 *
 */
package com.iris.platform.history.appender.devvc;

import static com.iris.messages.capability.SmokeCapability.ATTR_SMOKE;
import static com.iris.messages.capability.SmokeCapability.SMOKE_DETECTED;
import static com.iris.messages.capability.SmokeCapability.SMOKE_SAFE;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.capability.DeviceCapability;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.appender.AnnotatedDeviceValueChangeAppender;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.annotation.AutoTranslate;
import com.iris.platform.history.appender.annotation.EnumValue;
import com.iris.platform.history.appender.annotation.Group;

@Singleton
@Group(DeviceCapability.NAMESPACE)
@AutoTranslate()
@EnumValue(attr=ATTR_SMOKE, val=SMOKE_DETECTED, tpl="device.smoke.detected", critical=true)
@EnumValue(attr=ATTR_SMOKE, val=SMOKE_SAFE,     tpl="device.smoke.none",     critical=false)
public class DeviceSmokeAppender extends AnnotatedDeviceValueChangeAppender {

	@Inject
	public DeviceSmokeAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
		super(appender, cache);
	}

}

