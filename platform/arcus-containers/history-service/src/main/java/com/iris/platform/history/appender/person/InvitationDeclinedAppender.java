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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PersonCapability.InvitationRejectedEvent;
import com.iris.messages.type.Invitation;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.appender.MessageContext;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.annotation.Event;
import com.iris.platform.history.appender.annotation.Group;
import com.iris.platform.history.appender.matcher.MatchResults;

@Singleton
@Group(PersonCapability.NAMESPACE)
@Event(event = PersonCapability.InvitationRejectedEvent.NAME)
public class InvitationDeclinedAppender extends AnnotatedPersonAppender {
	private static final Logger logger = LoggerFactory.getLogger(InvitationDeclinedAppender.class);
	private final ObjectNameCache cache;
	
	private static final String KEY_FULLACCESS_REJECTED  = "person.fullaccess.declined";
	
	@Inject
	public InvitationDeclinedAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
		super(appender, cache);
		this.cache = cache;
	}	

	@Override
	protected List<HistoryLogEntry> translate(PlatformMessage message, MessageContext context, MatchResults matchResults) {		
		String msgKey = KEY_FULLACCESS_REJECTED;		
		// {0} = person name  {1} = inviter name	{2} = place name	
		MessageBody msgBody = message.getValue();
		String personName = "";
		String placeName = "";
		String inviterName = "";
		Map<String, Object> invitationMap = InvitationRejectedEvent.getInvitation(msgBody);
		if(invitationMap != null) {
			Invitation invitation = new Invitation(invitationMap);
			personName = getPersonName(invitation.getInviteeFirstName(), invitation.getInviteeLastName());
			placeName = invitation.getPlaceName();
			inviterName = getPersonName(invitation.getInvitorFirstName(), invitation.getInvitorLastName());
		}
		return creteLogEntries(message, context, msgKey, new String[] {personName,inviterName, placeName});					
	}

}

