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
package com.iris.platform.history.appender.incident;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.definition.AttributeType;
import com.iris.capability.definition.AttributeTypes;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.type.HistoryLog;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.HistoryLogEntryType;
import com.iris.platform.history.appender.AnnotatedAppender;
import com.iris.platform.history.appender.MessageContext;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.annotation.Event;
import com.iris.platform.history.appender.annotation.Group;
import com.iris.platform.history.appender.matcher.MatchResults;
import com.iris.util.IrisUUID;

@Singleton
@Group(AlarmIncidentCapability.NAMESPACE)
@Event(event = AlarmIncidentCapability.HistoryAddedEvent.NAME)
public class HistoryAddedAppender extends AnnotatedAppender {
	private static final Logger logger = LoggerFactory.getLogger(HistoryAddedAppender.class);
	private static final AttributeType StringList = AttributeTypes.listOf(AttributeTypes.stringType());
	
   @Inject
   public HistoryAddedAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
	   super(appender, cache);
   }

   @Override
   protected List<HistoryLogEntry> translate(PlatformMessage message, MessageContext context, MatchResults matchResults) {
   	List<Map<String, Object>> logs = AlarmIncidentCapability.HistoryAddedEvent.getEvents(message.getValue());
   	if(logs == null || logs.isEmpty()) {
   		logger.warn("Received incident history with no history events", message);
   		return ImmutableList.of();
   	}
   	
   	Preconditions.checkArgument(Objects.equals(AlarmIncidentCapability.NAMESPACE, message.getSource().getGroup()));
   	
   	int i = 0;
   	List<HistoryLogEntry> entries = new ArrayList<>(logs.size());
   	for(Map<String, Object> log: logs) {
   		HistoryLog hl = new HistoryLog(log);
   		HistoryLogEntry entry = new HistoryLogEntry();
   		entry.setId(message.getSource().getId());
   		entry.setType(HistoryLogEntryType.DETAILED_ALARM_LOG);
   		entry.setMessageKey(hl.getKey());
   		entry.setSubjectAddress(hl.getSubjectAddress());
   		entry.setValues((List<String>) StringList.coerce(log.get("values")));
   		
   		// maintain ordering received in this event
   		// assume any events that share a timestamp will arrive in the same event
   		entry.setTimestamp(IrisUUID.timeUUID(hl.getTimestamp(), i++));
   		entries.add(entry);
   	}
   	return entries;
   }

}

