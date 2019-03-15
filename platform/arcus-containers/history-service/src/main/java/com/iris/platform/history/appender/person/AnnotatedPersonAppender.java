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
package com.iris.platform.history.appender.person;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.iris.core.notification.Notifications;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PersonCapability;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.appender.AnnotatedAppender;
import com.iris.platform.history.appender.MessageContext;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.matcher.MatchResults;

public abstract class AnnotatedPersonAppender extends AnnotatedAppender {
 
	public AnnotatedPersonAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
		super(appender, cache);
	}
	
	@Override
	protected MatchResults matches(PlatformMessage message) {
		MatchResults results = super.matches(message);
		Address actor = message.getActor();
		if(results.isMatch() && StringUtils.isNotBlank(message.getPlaceId()) && actor != null && PersonCapability.NAMESPACE.equals(actor.getGroup())) {
			return results;
		}
		return MatchResults.FALSE;
	}
	
	
	protected String getPersonName(String firstName, String lastName) {
		return Notifications.ensureNotNull(firstName) + " "+ Notifications.ensureNotNull(lastName);
	}
	
	protected List<HistoryLogEntry> creteLogEntries(PlatformMessage message, MessageContext context, String msgKey, String[] params) {
		HistoryLogEntry dashboard = criticalPlaceEvent(message.getTimestamp().getTime(), context.getPlaceId(), msgKey, message.getSource(), params);
		HistoryLogEntry details   = detailedPlaceEvent(message.getTimestamp().getTime(), context.getPlaceId(), msgKey, message.getSource(), params);
		return ImmutableList.of(dashboard, details);
	}
	
	
}

