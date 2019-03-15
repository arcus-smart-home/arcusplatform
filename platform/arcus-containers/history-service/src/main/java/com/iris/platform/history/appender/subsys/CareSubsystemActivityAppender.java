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
package com.iris.platform.history.appender.subsys;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.CareSubsystemCapability;
import com.iris.platform.history.ActivityEvent;
import com.iris.platform.history.HistoryActivityDAO;
import com.iris.platform.history.appender.HistoryAppender;

@Singleton
public class CareSubsystemActivityAppender implements HistoryAppender {
	private final HistoryActivityDAO activityDao;
	
   @Inject
	protected CareSubsystemActivityAppender(HistoryActivityDAO activityDao) {
	   this.activityDao = activityDao;
   }
   
	@Override
	public boolean append(PlatformMessage message) {
   	if(
   			message.getDestination().isBroadcast() &&
   			CareSubsystemCapability.NAMESPACE.equals(message.getSource().getGroup()) &&
   			Capability.EVENT_VALUE_CHANGE.equals(message.getMessageType())
		) {
   		MessageBody body = message.getValue();
   		if(
   				body.getAttributes().containsKey(CareSubsystemCapability.ATTR_TRIGGEREDDEVICES) ||
   				body.getAttributes().containsKey(CareSubsystemCapability.ATTR_INACTIVEDEVICES)
			) {
   			append(
   					message.getPlaceId(),
   					message.getTimestamp(),
   					CareSubsystemCapability.getTriggeredDevices(body),
   					CareSubsystemCapability.getInactiveDevices(body)
				);
   			return true;
   		}
   	}
   	return false;
   }

	private void append(String placeId, Date timestamp, Set<String> triggeredDevices, Set<String> inactiveDevices) {
		ActivityEvent event = new ActivityEvent();
		event.setPlaceId(UUID.fromString(placeId));
		event.setTimestamp(timestamp);
		event.setActiveDevices(triggeredDevices);
		event.setInactiveDevices(inactiveDevices);
		activityDao.append(event);
	}

}

