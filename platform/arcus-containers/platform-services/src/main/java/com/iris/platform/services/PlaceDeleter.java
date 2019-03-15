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
package com.iris.platform.services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PreferencesDAO;
import com.iris.core.notification.Notifications;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.services.PlatformConstants;
import com.iris.messages.type.EmailRecipient;
import com.iris.platform.person.InvitationHandlerHelper;
import com.iris.platform.subscription.SubscriptionUpdateException;
import com.iris.platform.subscription.SubscriptionUpdater;
import com.iris.security.authz.AuthorizationGrant;

@Singleton
public class PlaceDeleter {
	private static final Logger logger = LoggerFactory.getLogger(PlaceDeleter.class);

   private final AccountDAO accountDao;
   private final PlaceDAO placeDao;
   private final PersonDAO personDao;
   private final AuthorizationGrantDAO grantDao;
   private final PreferencesDAO preferencesDao;
   private final SubscriptionUpdater subscriptionUpdater;
   private final PersonDeleter personDeleter;
   private final PlatformMessageBus bus;

   @Inject
   public PlaceDeleter(
         AccountDAO accountDao,
         PlaceDAO placeDao,
         PersonDAO personDao,
         AuthorizationGrantDAO grantDao,
         PreferencesDAO preferencesDao,
         SubscriptionUpdater subscriptionUpdater,
         PersonDeleter personDeleter,
         PlatformMessageBus bus
   ) {
      this.accountDao = accountDao;
      this.placeDao = placeDao;
      this.personDao = personDao;
      this.grantDao = grantDao;
      this.preferencesDao = preferencesDao;
      this.subscriptionUpdater = subscriptionUpdater;
      this.personDeleter = personDeleter;
      this.bus = bus;
   }
   
   
   public Place getPlace(UUID placeId) {
	   return placeDao.findById(placeId);
   }

   public MessageBody deletePlace(Place place, boolean sendNotification) {

      Account account = accountDao.findById(place.getAccount());

      if(account != null && place.isPrimary()) {
         return Errors.fromCode("account.primary_place.deletion", "The primary place for an account cannot be deleted without closing the account");
      }

      if(!updateBilling(account, place)) {
         return Errors.fromCode("unable.to.update.recurly", "Billing information could not be updated.");
      }
      Person ownerPerson = account!=null?personDao.findById(account.getOwner()):null;
      removePeopleAndSendNotification(account, place, sendNotification, ownerPerson);
      grantDao.removeForPlace(place.getId());
      placeDao.delete(place);
      emitDeletedEvent(place);
      if(sendNotification && ownerPerson != null) {
    	  //send notification to owner
    	  InvitationHandlerHelper.sendEmailNotification(bus, ownerPerson.getId().toString(), place.getId().toString(), place.getPopulation(),
					Notifications.PlaceRemovedNotifyOwner.KEY,
					ImmutableMap.<String, String>of(Notifications.PlaceRemovedNotifyOwner.PARAM_PLACE_NAME, place.getName()));
      }
      return PlaceCapability.DeleteResponse.instance();
   }

   private boolean updateBilling(Account account, Place place) {
      // check for null due to concurrency, the account may have been removed and thus subscription
      // canceled before the place delete
      if(account != null) {
         updateAccount(account, place, false);
         try {
            // only update the subscriptions if a billing account is present
            if(account.hasBillingAccount()) {
               subscriptionUpdater.removeSubscriptionForPlace(account, place);
            }
         } catch(SubscriptionUpdateException sue) {
            updateAccount(account, place, true);
            logger.error("Unable to remove place due to billing update error.", sue);
            return false;
         }
      }

      return true;
   }

   private void updateAccount(Account account, Place place, boolean rollback) {
      Set<UUID> placeIds = new HashSet<>(account.getPlaceIDs());
      if(rollback) {
         placeIds.add(place.getId());
      } else {
         placeIds.remove(place.getId());
      }
      account.setPlaceIDs(placeIds);
      accountDao.save(account);
   }

   private List<AuthorizationGrant> removePeopleAndSendNotification(Account account, Place place, boolean sendNotification, Person ownerPerson) {
      List<AuthorizationGrant> grants = grantDao.findForPlace(place.getId());
      for (AuthorizationGrant grant : grants) {
         if (!grant.isAccountOwner()) {
           	Person curPerson = personDao.findById(grant.getEntityId());
   			if (curPerson != null) {
   				if (sendNotification) {
   					EmailRecipient recipient = new EmailRecipient();
   					recipient.setEmail(curPerson.getEmail());
   					recipient.setFirstName(curPerson.getFirstName());
   					recipient.setLastName(curPerson.getLastName());
   					Notifications.sendEmailNotification(bus, recipient, place.getId().toString(), place.getPopulation(), Notifications.PlaceRemovedNotifyPerson.KEY,
   							ImmutableMap.<String, String>of(Notifications.PlaceRemovedNotifyPerson.PARAM_PLACE_NAME, place.getName(),
   									Notifications.PlaceRemovedNotifyPerson.PARAM_ACTOR_FIRSTNAME, ownerPerson!=null?ownerPerson.getFirstName():"",
   									Notifications.PlaceRemovedNotifyPerson.PARAM_ACTOR_LASTNAME, ownerPerson!=null?ownerPerson.getLastName():""),
   							Address.platformService(PlatformConstants.SERVICE_PLACES));
   				}
   				personDeleter.removePersonFromPlace(account, place, curPerson, null, false);
   			} else {
   				logger.warn(String.format("Unable to send notification with key %s for person %s because it no longer exists",  Notifications.PlaceRemovedNotifyPerson.KEY, grant.getEntityId()));
   			}
         }
         else {
           	Person curPerson = personDao.findById(grant.getEntityId());
            if (curPerson != null) {
               preferencesDao.delete(curPerson.getId(), place.getId());
            }
         }
      }
      if (ownerPerson != null) {
         personDeleter.clearPinForPlace(place.getId(), ownerPerson);
      }
      return grants;
   }

   private void emitDeletedEvent(Place place) {
      MessageBody body = MessageBody.buildMessage(Capability.EVENT_DELETED, ImmutableMap.of());
      PlatformMessage event = PlatformMessage.buildBroadcast(body, Address.fromString(place.getAddress()))
            .withPlaceId(place.getId())
            .withPopulation(place.getPopulation())
            .create();
      bus.send(event);
   }
}

