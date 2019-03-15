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

import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PreferencesDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Person;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.security.authz.AuthorizationGrant;

@Singleton
public class DeleteLoginHandler implements ContextualRequestMessageHandler<Person> {

   private final PersonDAO personDao;
   private final AuthorizationGrantDAO authGrantDao;
   private final PreferencesDAO preferencesDao;
   private final PlatformMessageBus bus;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public DeleteLoginHandler(PersonDAO personDao, 
   		AuthorizationGrantDAO authGrantDao, 
   		PreferencesDAO preferencesDao,
   		PlatformMessageBus bus,
   		PlacePopulationCacheManager populationCacheMgr) {
      this.personDao = personDao;
      this.authGrantDao = authGrantDao;
      this.preferencesDao = preferencesDao;
      this.bus = bus;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public String getMessageType() {
      return PersonCapability.DeleteLoginRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Person context, PlatformMessage msg) {
      List<AuthorizationGrant> grants = authGrantDao.findForEntity(context.getId());
      validate(context, grants, msg.getActor());
      preferencesDao.deleteForPerson(context.getId());
      authGrantDao.removeGrantsForEntity(context.getId());
      personDao.delete(context);
      emitDeleted(context, grants);
      return PersonCapability.DeleteLoginResponse.instance();
   }

   private void emitDeleted(Person person, List<AuthorizationGrant> grants) {
      MessageBody body = MessageBody.buildMessage(Capability.EVENT_DELETED, ImmutableMap.of("bootSession", true));
      Address addr = Address.fromString(person.getAddress());

      grants.forEach((g) -> {
         PlatformMessage evt = PlatformMessage.buildBroadcast(body, addr)
               .withPlaceId(g.getPlaceId())
               .withPopulation(populationCacheMgr.getPopulationByPlaceId(g.getPlaceId()))
               .create();
         bus.send(evt);
      });
   }

   private void validate(Person context, List<AuthorizationGrant> grants, Address actor) {
      if(!context.getHasLogin()) {
         throw new ErrorEventException(Errors.invalidRequest("person does not have a login"));
      }
      if(grants.stream().anyMatch((g) -> { return g.isAccountOwner(); })) {
         throw new ErrorEventException("account.owner.deletion", "The account owner cannot be deleted without closing the account.");
      }
      if(!Objects.equal(context.getAddress(), actor.getRepresentation())) {
         throw new ErrorEventException(Errors.invalidRequest("a login may only be deleted by themselves"));
      }
   }
}

