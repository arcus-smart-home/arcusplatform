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
package com.iris.platform.services.person;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.dao.PersonDAO;
import com.iris.core.platform.ContextualEventMessageHandler;
import com.iris.core.platform.ContextualPlatformMessageDispatcher;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.platform.PlatformService;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Person;
import com.iris.messages.services.PlatformConstants;

@Singleton
public class PersonService extends ContextualPlatformMessageDispatcher<Person> implements PlatformService {
   public static final String PROP_THREADPOOL = "platform.service.person.threadpool";

   private static final Logger logger = LoggerFactory.getLogger(PersonService.class);
   private static final Address address = Address.platformService(PlatformConstants.SERVICE_PEOPLE);

   private final PersonDAO personDao;

   @Inject
   public PersonService(
         PlatformMessageBus platformBus,
         @Named(PROP_THREADPOOL) Executor executor,
         Set<ContextualRequestMessageHandler<Person>> handlers,
         PersonDAO personDao) {

      super(platformBus, executor, handlers, Collections.<ContextualEventMessageHandler<Person>>emptySet());
      this.personDao = personDao;
   }

   @Override
   public Address getAddress() {
      return address;
   }

   @Override
   public void handleMessage(PlatformMessage message) {
      super.handleMessage(message);
   }

   // RoleAuthorizer holds a client's self methods to its own actor, and PersonAccess checks a
   // client's actor against the person's places. hub-bridge runs no authorizer, forwards a hub's
   // requests anywhere and keeps whatever actor the hub set, and nothing on a hub reads or changes
   // people, so a hub's requests are refused.
   @Override
   protected MessageBody handleRequest(PlatformMessage message) throws Exception {
      if(message.getSource() != null && message.getSource().getHubId() != null) {
         logger.warn("Refusing [{}] for [{}] from hub [{}]", message.getMessageType(), message.getDestination(), message.getSource().getHubId());
         throw new UnauthorizedRequestException(message.getDestination(), "Hubs may not send person requests");
      }
      return super.handleRequest(message);
   }

   @Override
   protected Person loadContext(Object contextId, Integer qualifier) {
      if(!(contextId instanceof UUID)) {
         throw new IllegalArgumentException("The context ID must be a UUID");
      }
      return personDao.findById((UUID) contextId);
   }
}

