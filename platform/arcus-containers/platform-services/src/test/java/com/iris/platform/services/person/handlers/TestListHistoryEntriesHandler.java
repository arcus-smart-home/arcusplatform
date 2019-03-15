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
package com.iris.platform.services.person.handlers;

import java.util.Date;
import java.util.UUID;

import com.google.inject.Inject;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Person;
import com.iris.platform.history.HistoryLogEntryType;
import com.iris.test.ListHistoryEntriesHandlerTestCase;

/**
 * 
 */
public class TestListHistoryEntriesHandler extends ListHistoryEntriesHandlerTestCase<Person> {
   @Inject ListHistoryEntriesHandler handler;
   
   /* (non-Javadoc)
    * @see com.iris.platform.services.ListHistoryEntriesHandlerTestCase#createContext()
    */
   @Override
   protected Person createContext() {
      Person context = Fixtures.createPerson();
      context.setId(UUID.randomUUID());
      context.setCreated(new Date());
      context.setModified(new Date());
      return context;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.services.ListHistoryEntriesHandlerTestCase#handleRequest(com.iris.messages.model.BaseEntity, com.iris.messages.PlatformMessage)
    */
   @Override
   protected MessageBody handleRequest(Person context, PlatformMessage message) {
      return handler.handleRequest(context, message);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.services.ListHistoryEntriesHandlerTestCase#createListRequest(com.iris.messages.model.BaseEntity, java.lang.Integer, java.lang.String)
    */
   @Override
   protected MessageBody createListRequest(Person context, Integer limit, String token) {
      return 
            PersonCapability.ListHistoryEntriesRequest
               .builder()
               .withLimit(limit)
               .withToken(token)
               .build();
   }

   /* (non-Javadoc)
    * @see com.iris.platform.services.ListHistoryEntriesHandlerTestCase#getType()
    */
   @Override
   protected HistoryLogEntryType getType() {
      return HistoryLogEntryType.DETAILED_PERSON_LOG;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.services.ListHistoryEntriesHandlerTestCase#getDefaultLimit()
    */
   @Override
   protected int getDefaultLimit() {
      return ListHistoryEntriesHandler.DFLT_LIMIT;
   }
   
}

