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
package com.iris.platform.history.appender;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.appender.annotation.Event;
import com.iris.platform.history.appender.annotation.Group;
import com.iris.platform.history.appender.matcher.MatchResults;
import com.iris.platform.history.appender.translator.TranslateOptions;

@Singleton
@Group(DeviceCapability.NAMESPACE)
@Event(event=Capability.EVENT_DELETED, tpl="device.removed", critical=true)
@Event(event=Capability.EVENT_ADDED,   tpl="device.added",   critical=true)
public class DeviceEventAppender extends AnnotatedAppender {

	@Inject
	public DeviceEventAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
	   super(appender, cache);
   }

	@Override
   protected List<HistoryLogEntry> doTranslate(PlatformMessage message, MessageContext context, MatchResults matchResults, TranslateOptions options) {
		String placeName = context.getPlaceName();
      String deviceName = DeviceCapability.getName(message.getValue());
      context.setDeviceId((UUID)message.getSource().getId());
      
      List<HistoryLogEntry> entries = new ArrayList<HistoryLogEntry>();
      if (options.isCritical()) {
      	entries.add(criticalPlaceEvent(context.getTimestamp(), context.getPlaceId(), options.getTemplate(), context.getSubjectAddress(), deviceName, placeName));
      }
      entries.add(detailedPlaceEvent(context.getTimestamp(), context.getPlaceId(), options.getTemplate(), context.getSubjectAddress(), deviceName, placeName));
      entries.add(detailedDeviceEvent(context.getTimestamp(), context.getDeviceId(), options.getTemplate(), context.getSubjectAddress(), deviceName, placeName));
      return entries;
   }
	
}

