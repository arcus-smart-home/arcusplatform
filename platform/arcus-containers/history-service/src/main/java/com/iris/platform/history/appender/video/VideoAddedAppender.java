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
package com.iris.platform.history.appender.video;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.RecordingCapability;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.appender.AnnotatedAppender;
import com.iris.platform.history.appender.MessageContext;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.annotation.Event;
import com.iris.platform.history.appender.annotation.Group;
import com.iris.platform.history.appender.matcher.MatchResults;

@Singleton
@Group(RecordingCapability.NAMESPACE)
@Event(event = Capability.EVENT_ADDED)
public class VideoAddedAppender extends AnnotatedAppender {
	private static final Logger logger = LoggerFactory.getLogger(VideoAddedAppender.class);
	private static final String KEY_SECURITY_RECORDING  = "alarm.security.recording";
	private final ObjectNameCache cache;
	
	@Inject
	public VideoAddedAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
		super(appender, cache);
		this.cache = cache;
	}

	@Override
	protected MatchResults matches(PlatformMessage message) {
		MatchResults results = super.matches(message);
		Address actor = message.getActor();
		if(results.isMatch() && actor != null && AlarmIncidentCapability.NAMESPACE.equals(actor.getGroup())) {
			return results;
		}
		return MatchResults.FALSE;
	}

	@Override
	protected List<HistoryLogEntry> translate(PlatformMessage message, MessageContext context, MatchResults matchResults) {
		Address actor = message.getActor();
		UUID incidentId = (UUID) actor.getId();
		
		String cameraId = RecordingCapability.getCameraid(message.getValue());
		if(StringUtils.isEmpty(cameraId)) {
			logger.warn("Recording added with no camera specified, message: [{}]", message);
			return null;
		}
		Address cameraAddress = Address.platformDriverAddress(UUID.fromString(cameraId));
		String cameraName = cache.getName(cameraAddress);
		
		HistoryLogEntry dashboard = criticalPlaceEvent(message.getTimestamp().getTime(), context.getPlaceId(), KEY_SECURITY_RECORDING, message.getSource(), cameraName);
		HistoryLogEntry details   = detailedPlaceEvent(message.getTimestamp().getTime(), context.getPlaceId(), KEY_SECURITY_RECORDING, message.getSource(), cameraName);
		HistoryLogEntry incident  = detailedAlarmEvent(message.getTimestamp().getTime(), incidentId, KEY_SECURITY_RECORDING, message.getSource(), cameraName);
		HistoryLogEntry device    = detailedDeviceEvent(message.getTimestamp().getTime(), (UUID) cameraAddress.getId(), KEY_SECURITY_RECORDING, message.getSource(), cameraName);
		return ImmutableList.of(dashboard, details, incident, device);
	}

}

