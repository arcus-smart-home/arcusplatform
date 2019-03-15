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
package com.iris.platform.services.account.handlers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.notification.Notifications;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.type.Population;
import com.iris.platform.address.StreetAddress;
import com.iris.platform.address.updater.AddressUpdaterFactory;
import com.iris.platform.subscription.SubscriptionUpdateException;
import com.iris.platform.subscription.SubscriptionUpdater;
import com.iris.security.authz.AuthorizationGrant;

@Singleton
public class AddPlaceHandler implements ContextualRequestMessageHandler<Account> {

   private static final Logger logger = LoggerFactory.getLogger(AddPlaceHandler.class);

   private final BeanAttributesTransformer<Place> placeTransformer;
   private final AccountDAO accountDao;
   private final PlaceDAO placeDao;
   private final AuthorizationGrantDAO grantDao;
   private final SubscriptionUpdater subscriptionUpdater;
   private final PlatformMessageBus bus;
   private final AddressUpdaterFactory updaterFactory;

   @Inject
   public AddPlaceHandler(
         BeanAttributesTransformer<Place> placeTransformer,
         AccountDAO accountDao,
         PlaceDAO placeDao,
         AuthorizationGrantDAO grantDao,
         SubscriptionUpdater subscriptionUpdater,
         PlatformMessageBus bus,
         AddressUpdaterFactory updaterFactory) {

      this.placeTransformer = placeTransformer;
      this.accountDao = accountDao;
      this.placeDao = placeDao;
      this.grantDao = grantDao;
      this.subscriptionUpdater = subscriptionUpdater;
      this.bus = bus;
      this.updaterFactory = updaterFactory;
   }

   @Override
   public String getMessageType() {
      return AccountCapability.AddPlaceRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Account context, PlatformMessage msg) {
      MessageBody body = msg.getValue();
      Map<String,Object> placeAttrs = AccountCapability.AddPlaceRequest.getPlace(body);
      Place place = placeTransformer.transform(placeAttrs);

      if(StringUtils.isBlank(place.getName())) {
         return Errors.fromCode(Errors.CODE_MISSING_PARAM, "Missing required argument place:name");
      }

      if(StringUtils.isBlank(AccountCapability.AddPlaceRequest.getServiceLevel(body))) {
         return Errors.missingParam(AccountCapability.AddPlaceRequest.ATTR_SERVICELEVEL);
      }

      ServiceLevel serviceLevel = ServiceLevel.valueOf(AccountCapability.AddPlaceRequest.getServiceLevel(body));

      if(ServiceLevel.isPromon(serviceLevel)) {
         return Errors.fromCode(Errors.CODE_INVALID_REQUEST, "A place cannot be added at a promonitoring level.");
      }
      if(hasPremiumFree(context)) {
    	  if(serviceLevel == ServiceLevel.PREMIUM) {
    		  serviceLevel = ServiceLevel.PREMIUM_FREE;
    	  }
      }else{
    	  if(serviceLevel == ServiceLevel.PREMIUM_FREE) {
    		  //can not have a premium free if there is not an existing one
    		  return Errors.fromCode(Errors.CODE_INVALID_REQUEST, "Cannot add a PREMIUM FREE place without an existing PREMIUM FREE place or subscription.");
    	  }
      }

      // bail out if we don't have a billing but are trying to create a place at premium
      if(!context.hasBillingAccount() && serviceLevel == ServiceLevel.PREMIUM) {
         return Errors.fromCode(Errors.CODE_INVALID_REQUEST, "Billing information required to add a place at " + serviceLevel);
      }

      Map<String, Boolean> addons = mapAddons(AccountCapability.AddPlaceRequest.getAddons(body), place);
      //population is always assigned to be general in AddPlace
      String population = Population.NAME_GENERAL;
      
      place.setPopulation(population);
      place.setAccount(context.getId());
      
      // Accounts that have billing information provisioned will have service level written by SubscriptionUpdater
      if (!context.hasBillingAccount()) {
    	  place.setServiceLevel(serviceLevel);
      }

      Map<String,Object> updates = updaterFactory.updaterFor(place).updateAddress(place, StreetAddress.fromPlace(place));
      if(updates != null && !updates.isEmpty()) {
         placeTransformer.merge(place, updates);
      }
      
      place = placeDao.save(place);

      try {
         context = updateAccount(context, place, false);

         // bailed out earlier if creating a place at premium without a billing account, so only
         // update subscriptions if a billing account is present
         if(context.hasBillingAccount()) {
            subscriptionUpdater.updateSubscription(context, place, serviceLevel, addons, false);
         }

         createGrant(context, place);

      }
      catch(Exception e) {
         logger.error("Unable to create new place!", e);
         updateAccount(context, place, true);
         placeDao.delete(place);
         if(e instanceof SubscriptionUpdateException) {
            return Errors.fromCode("unable.to.update.recurly", "Billing information could not be updated.");
         }
         return Errors.fromException(e);
      }

      place.setServiceLevel(serviceLevel);
      MessageBody addedEvent = MessageBody.buildMessage(Capability.EVENT_ADDED, placeTransformer.transform(place));
      PlatformMessage eventMsg =
            PlatformMessage
               .buildBroadcast(addedEvent, Address.fromString(place.getAddress()))
               // subsystems and rules need the new place id
               .withPlaceId(place.getId())
               .withPopulation(place.getPopulation())
               .create();
      bus.send(eventMsg);

      notification(context.getOwner(), place.getId(), place.getPopulation());

      return AccountCapability.AddPlaceResponse.builder().withPlace(placeTransformer.transform(place)).build();
   }

   private void notification(UUID personId, UUID placeId, String population) {
      PlatformMessage msg = Notifications.builder()
            .withPersonId(personId)
            .withPlaceId(placeId)
            .withPopulation(population)
            .withSource(Address.platformService(AccountCapability.NAMESPACE))
            .withPriority(NotificationCapability.NotifyRequest.PRIORITY_LOW)
            .withMsgKey(Notifications.PlaceAdded.KEY)
            .create();
      bus.send(msg);
   }

   private Account updateAccount(Account account, Place newPlace, boolean rollback) {
      Set<UUID> placeIds = new HashSet<>(account.getPlaceIDs());

      if(rollback) {
         placeIds.remove(newPlace.getId());
      } else {
         placeIds.add(newPlace.getId());
      }

      account.setPlaceIDs(placeIds);
      return accountDao.save(account);
   }

   private boolean hasPremiumFree(Account account) {
      if(account.getSubscriptionIDs() != null && !account.getSubscriptionIDs().isEmpty()) {
         return account.getSubscriptionIDs().containsKey(ServiceLevel.PREMIUM_FREE) || account.getSubscriptionIDs().containsKey(ServiceLevel.PREMIUM_PROMON_FREE);
      }
      // skipped billing creation case requires looking up the places unfortunately
      List<Place> places = placeDao.findByPlaceIDIn(account.getPlaceIDs());
      return places.stream().anyMatch((p) -> { return ServiceLevel.isNonBasicFree(p.getServiceLevel()); });
   }

   private void createGrant(Account account, Place newPlace) {
      AuthorizationGrant grant = new AuthorizationGrant();
      grant.setAccountId(account.getId());
      grant.setAccountOwner(true);
      grant.setEntityId(account.getOwner());
      grant.setPlaceId(newPlace.getId());
      grant.setPlaceName(newPlace.getName());
      grant.addPermissions("*:*:*");
      grantDao.save(grant);
   }

   private Map<String, Boolean> mapAddons(Map<String, Object> addons, Place place) {
      Map<String, Boolean> updatedMap = addons != null
            ? addons.entrySet().stream().collect(Collectors.toMap((e) -> e.getKey(), (e) -> (Boolean)e.getValue()))
            : new HashMap<>();
      Set<String> placeAddons = place.getServiceAddons();
      if (placeAddons != null) {
         for (String addon : placeAddons) {
            if (updatedMap.get(addon) == null) {
               updatedMap.put(addon, true);
            }
         }
      }
      return updatedMap;
   }
}

