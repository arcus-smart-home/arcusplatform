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
package com.iris.platform.history.appender.apikey;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.service.ApiKeyService.CreatedEvent;
import com.iris.messages.service.ApiKeyService.RevokedEvent;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.appender.MessageContext;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.annotation.Event;
import com.iris.platform.history.appender.annotation.Group;
import com.iris.platform.history.appender.matcher.MatchResults;
import com.iris.platform.history.appender.person.AnnotatedPersonAppender;

@Singleton
@Group(PlaceCapability.NAMESPACE)
@Event(event = CreatedEvent.NAME)
@Event(event = RevokedEvent.NAME)
public class ApiKeyAppender extends AnnotatedPersonAppender {

   private static final String KEY_CREATED = "apikey.created";
   private static final String KEY_REVOKED = "apikey.revoked";

   @Inject
   public ApiKeyAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
      super(appender, cache);
   }

   @Override
   protected List<HistoryLogEntry> translate(PlatformMessage message, MessageContext context, MatchResults matchResults) {
      MessageBody body = message.getValue();
      boolean created = CreatedEvent.NAME.equals(body.getMessageType());
      String msgKey = created ? KEY_CREATED : KEY_REVOKED;
      String label = created ? CreatedEvent.getLabel(body) : RevokedEvent.getLabel(body);

      // {0} = API key label  {1} = place name  {2} = person name
      return creteLogEntries(message, context, msgKey, new String[] { label, context.getPlaceName(), context.getActorName() });
   }

}
