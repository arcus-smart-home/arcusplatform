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

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Person;

/**
 * Who a client may read or change. The bridges can't decide this because it depends on the places
 * the person belongs to. Messages from other services aren't checked.
 *
 * A stranger gets not found rather than unauthorized so these can't be used to test person ids.
 */
@Singleton
public class PersonAccess {
   private static final Logger logger = LoggerFactory.getLogger(PersonAccess.class);

   private final PersonPlaceAssocDAO assocDao;

   @Inject
   public PersonAccess(PersonPlaceAssocDAO assocDao) {
      this.assocDao = assocDao;
   }

   /** The person themselves, or anyone sharing a place with them. */
   public void assertReadable(Person person, PlatformMessage msg) {
      if(isClientRequest(msg)) {
         assertSharedPlace(person, actorOf(msg));
      }
   }

   /**
    * The person themselves. Someone with a login manages their own record, because their email is
    * their username and changing it decides who a password reset reaches. Someone without a login
    * can't, so anyone sharing a place may maintain them.
    */
   public void assertEditable(Person person, PlatformMessage msg) {
      if(!isClientRequest(msg)) {
         return;
      }
      Address actor = actorOf(msg);
      if(isSelf(person, actor)) {
         return;
      }
      assertSharedPlace(person, actor);
      if(person.getHasLogin()) {
         logger.warn("Actor [{}] tried to change [{}], who has a login -- denied", actor, person.getAddress());
         throw new UnauthorizedRequestException(msg.getDestination(), "People with a login manage their own information");
      }
   }

   public void assertSharedPlace(Person person, Address actor) {
      if(isSelf(person, actor)) {
         return;
      }

      Set<UUID> places;
      if(person.getHasLogin()) {
         places = assocDao.findPlaceIdsByPerson(person.getId());
      }
      else {
         places = ImmutableSet.of(person.getCurrPlace());
      }
      Set<UUID> actorPlaces = assocDao.findPlaceIdsByPerson((UUID) actor.getId());
      if(Sets.intersection(places, actorPlaces).isEmpty()) {
         logger.warn("Actor [{}] does not have any places in common with [{}] -- denied", actor, person.getAddress());
         throw new NotFoundException(Address.fromString(person.getAddress()));
      }
   }

   private static boolean isSelf(Person person, Address actor) {
      return Objects.equals(person.getId(), actor.getId());
   }

   private static boolean isClientRequest(PlatformMessage msg) {
      return MessageConstants.CLIENT.equals(msg.getSource().getNamespace());
   }

   private static Address actorOf(PlatformMessage msg) {
      if(msg.getActor() == null) {
         throw new UnauthorizedRequestException(msg.getDestination(), "Client request has no actor");
      }
      return msg.getActor();
   }
}
