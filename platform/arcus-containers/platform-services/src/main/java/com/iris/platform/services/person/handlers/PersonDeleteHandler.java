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
package com.iris.platform.services.person.handlers;

import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.model.Person;
import com.iris.platform.services.PersonDeleter;

@Singleton
public class PersonDeleteHandler implements ContextualRequestMessageHandler<Person> {

   private final PersonDeleter deleter;

   @Inject
   public PersonDeleteHandler(PersonDeleter deleter) {
      this.deleter = deleter;
   }

   @Override
   public String getMessageType() {
      return PersonCapability.DeleteRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Person context, PlatformMessage msg) {
      deleter.removePersonFromPlace(UUID.fromString(msg.getPlaceId()), context, msg.getActor(), true);
      return PersonCapability.DeleteResponse.instance();
   }
}

