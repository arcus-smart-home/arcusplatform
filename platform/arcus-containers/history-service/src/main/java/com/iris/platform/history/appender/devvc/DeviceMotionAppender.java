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

import static com.iris.messages.capability.MotionCapability.ATTR_MOTION;
import static com.iris.messages.capability.MotionCapability.MOTION_DETECTED;
import static com.iris.messages.capability.MotionCapability.MOTION_NONE;

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
@EnumValue(attr=ATTR_MOTION, val=MOTION_DETECTED, tpl="device.motion.detected", critical=false)
@EnumValue(attr=ATTR_MOTION, val=MOTION_NONE,     tpl="device.motion.none",     critical=false)
public class DeviceMotionAppender extends AnnotatedDeviceValueChangeAppender {

	@Inject
	public DeviceMotionAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
		super(appender, cache);
	}

}

