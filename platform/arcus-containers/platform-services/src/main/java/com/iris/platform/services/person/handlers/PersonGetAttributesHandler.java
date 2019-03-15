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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.model.Person;
import com.iris.platform.services.AbstractGetAttributesPlatformMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Singleton
public class PersonGetAttributesHandler extends AbstractGetAttributesPlatformMessageHandler<Person> {
   private static final Logger logger = LoggerFactory.getLogger(PersonGetAttributesHandler.class);

   private final PersonPlaceAssocDAO assocDao;

   @Inject
   public PersonGetAttributesHandler(BeanAttributesTransformer<Person> personTransformer, PersonPlaceAssocDAO assocDao) {
      super(personTransformer);
      this.assocDao = assocDao;
   }

   @Override
   public MessageBody handleRequest(Person context, PlatformMessage msg) {
      Errors.assertFound(context, msg.getDestination());
      assertSharedPlace(context, msg.getActor());

      MessageBody body = super.handleRequest(context, msg);
      if(context.getCurrPlace() == null) {
         return MessageBody.messageBuilder(body.getMessageType())
               .withAttributes(body.getAttributes())
               .withAttribute(PersonCapability.ATTR_CURRPLACE, "")
               .create();
      }
      return body;
   }

   private void assertSharedPlace(Person context, Address actor) {
      if(Objects.equals(context.getId(), actor.getId())) {
         // can always access myself
      	return;
      }
      
      Set<UUID> places;
      if(context.getHasLogin()) {
         places = assocDao.findPlaceIdsByPerson(context.getId());
      }
      else {
         places = ImmutableSet.of(context.getCurrPlace());
      }
      Set<UUID> actorPlaces = assocDao.findPlaceIdsByPerson((UUID) actor.getId());
      if(Sets.intersection(places, actorPlaces).isEmpty()) {
         logger.warn("Actor [{}] does not have any places in common with [{}] -- denied", actor, context.getAddress());
         throw new NotFoundException(Address.fromString(context.getAddress()));
      }
   }
}

