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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.security.authz.AuthorizationGrant;

@Singleton
public class PromoteToAccountHandler implements ContextualRequestMessageHandler<Person> {

   private final PersonDAO personDao;
   private final AccountDAO accountDao;
   private final PlaceDAO placeDao;
   private final AuthorizationGrantDAO authGrantDao;
   private final BeanAttributesTransformer<Account> accountTransform;
   private final BeanAttributesTransformer<Place> placeTransform;
   private final PlatformMessageBus bus;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject(optional = true) @Named("default.service.level")
   private String defaultServiceLevel = "basic";

   @Inject
   public PromoteToAccountHandler(
         PersonDAO personDao,
         AccountDAO accountDao,
         PlaceDAO placeDao,
         AuthorizationGrantDAO authGrantDao,
         BeanAttributesTransformer<Account> accountTransform,
         BeanAttributesTransformer<Place> placeTransform,
         PlatformMessageBus bus,
         PlacePopulationCacheManager populationCacheMgr) {

      this.personDao = personDao;
      this.accountDao = accountDao;
      this.placeDao = placeDao;
      this.authGrantDao = authGrantDao;
      this.accountTransform = accountTransform;
      this.placeTransform = placeTransform;
      this.bus = bus;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public String getMessageType() {
      return PersonCapability.PromoteToAccountRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Person context, PlatformMessage msg) {
      MessageBody body = msg.getValue();
      List<AuthorizationGrant> grants = new LinkedList<>(authGrantDao.findForEntity(context.getId()));
      validate(body, context, grants, msg.getActor());

      UUID placeId = UUID.randomUUID();
      UUID accountId= UUID.randomUUID();

      boolean succeeded = false;
      Account account = null;
      Place place = null;
      try {
         account = new Account();
         account.setId(accountId);
         account.setState("ABOUT_YOUR_HOME");
         account.setBillable(true);

         Set<UUID> placeIDs = new HashSet<>();
         placeIDs.add(placeId);
         account.setPlaceIDs(placeIDs);
         account.setOwner(context.getId());
         account = accountDao.create(account);

         place = placeTransform.transform(PersonCapability.PromoteToAccountRequest.getPlace(body));
         place.setId(placeId);
         place.setServiceLevel(ServiceLevel.fromString(defaultServiceLevel));
         place.setAccount(account.getId());
         place.setPrimary(true);
         place = placeDao.create(place);

         AuthorizationGrant defaultGrant = new AuthorizationGrant();
         defaultGrant.setAccountId(account.getId());
         defaultGrant.setAccountOwner(true);
         defaultGrant.setEntityId(context.getId());
         defaultGrant.setPlaceId(place.getId());
         defaultGrant.setPlaceName(place.getName());
         defaultGrant.addPermissions("*:*:*");
         authGrantDao.save(defaultGrant);

         grants.add(defaultGrant);

         context.setCurrPlace(placeId);
         personDao.update(context);
         sendCurrPlaceValueChange(context, grants);
         emitEvent(account, place);
         succeeded = true;
      }
      finally {
         if(!succeeded) {
            if(place != null && place.isPersisted()) {
               placeDao.delete(place);
            }
            if(account != null && account.isPersisted()) {
               accountDao.delete(account);
            }
            context.setCurrPlace(null);
            personDao.update(context);
            throw new ErrorEventException(Errors.CODE_GENERIC, "unable to create account");
         }
      }

      return PersonCapability.PromoteToAccountResponse.builder()
            .withAccount(accountTransform.transform(account))
            .withPlace(placeTransform.transform(place))
            .build();
   }

   private void emitEvent(Account account, Place place) {
	   //emit added event for the account
	   MessageBody body = MessageBody.buildMessage(Capability.EVENT_ADDED, accountTransform.transform(account));
	   PlatformMessage event = PlatformMessage.buildBroadcast(body, Address.fromString(account.getAddress()))
            .withPlaceId(place.getId())
            .withPopulation(place.getPopulation())
            .create();
	   bus.send(event);		
	   
	   //emit added event for the place
	   MessageBody eventBodyForPlace = MessageBody.buildMessage(Capability.EVENT_ADDED, placeTransform.transform(place));
	   PlatformMessage eventForPlace = PlatformMessage.buildBroadcast(eventBodyForPlace, Address.fromString(place.getAddress()))
            .withPlaceId(place.getId())
            .withPopulation(place.getPopulation())
            .create();
	   bus.send(eventForPlace);
   }
   
   private void sendCurrPlaceValueChange(Person person, List<AuthorizationGrant> grants) {
      MessageBody body = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, ImmutableMap.of(PersonCapability.ATTR_CURRPLACE, person.getCurrPlace().toString()));
      Address addr = Address.fromString(person.getAddress());
      grants.forEach((g) -> {
         PlatformMessage msg = PlatformMessage.buildBroadcast(body, addr)
         		.withPlaceId(g.getPlaceId())
         		.withPopulation(populationCacheMgr.getPopulationByPlaceId(g.getPlaceId()))
         		.create();
         bus.send(msg);
      });
   }

   private void validate(MessageBody body, Person context, List<AuthorizationGrant> grants, Address actor) {
      Errors.assertRequiredParam(PersonCapability.PromoteToAccountRequest.getPlace(body), "place");
      if(!context.getHasLogin()) {
         throw new ErrorEventException(Errors.invalidRequest("person does not have a login"));
      }
      if(grants.stream().anyMatch((g) -> { return g.isAccountOwner(); })) {
         throw new ErrorEventException(Errors.invalidRequest("person is already an account owner"));
      }
      if(!actor.getRepresentation().contains("icst") && !Objects.equal(context.getAddress(), actor.getRepresentation())) {
         throw new ErrorEventException(Errors.invalidRequest("a person may only be promoted to an account by themselves or icst"));
      }
   }

}

