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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.appender.MessageContext;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.annotation.Event;
import com.iris.platform.history.appender.annotation.Group;
import com.iris.platform.history.appender.matcher.MatchResults;

@Singleton
@Group(PersonCapability.NAMESPACE)
@Event(event = Capability.EVENT_DELETED)
public class PersonRemovedAppender extends AnnotatedPersonAppender {
	private static final Logger logger = LoggerFactory.getLogger(PersonRemovedAppender.class);
	private final ObjectNameCache cache;
	
	private static final String KEY_HOBBIT_REMOVED  = "person.hobbit.removed";
	private static final String KEY_FULLACCESS_REMOVED  = "person.fullaccess.removed";
	
	@Inject
	public PersonRemovedAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
		super(appender, cache);
		this.cache = cache;
	}	

	@Override
	protected List<HistoryLogEntry> translate(PlatformMessage message, MessageContext context, MatchResults matchResults) {
		boolean isFullAccess = PersonCapability.getHasLogin(message.getValue(), Boolean.FALSE);
		String msgKey = KEY_HOBBIT_REMOVED;
		if(isFullAccess) {
			msgKey =  KEY_FULLACCESS_REMOVED;
		}

		// {0} = person name  {1} = place name	{2} = inviter name
		MessageBody msgBody = message.getValue();
		String personName = getPersonName(PersonCapability.getFirstName(msgBody), PersonCapability.getLastName(msgBody));	
		String placeName = getPlaceNameFromHeader(message);
		String inviterName=personName;
		if(!message.getSource().equals(message.getActor())) {
			//Not Deleted by the person himself
			inviterName = getActorName(message);
		}
			
		return creteLogEntries(message, context, msgKey, new String[] {personName, placeName, inviterName});
	}

}

