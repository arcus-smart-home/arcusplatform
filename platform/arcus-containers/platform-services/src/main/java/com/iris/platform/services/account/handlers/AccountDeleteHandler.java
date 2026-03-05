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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.dao.PreferencesDAO;
import com.iris.core.notification.Notifications;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.messages.services.PlatformConstants;
import com.iris.messages.type.EmailRecipient;
import com.iris.platform.services.PlaceDeleter;
import com.iris.population.PlacePopulationCacheManager;

@Singleton
public class AccountDeleteHandler implements ContextualRequestMessageHandler<Account> {

   private static final Logger logger = LoggerFactory.getLogger(AccountDeleteHandler.class);

   private final AccountDAO accountDao;
   private final PersonDAO personDao;
   private final PersonPlaceAssocDAO personPlaceAssocDao;
   private final AuthorizationGrantDAO authGrantDao;
   private final PreferencesDAO preferencesDao;
   private final PlatformMessageBus bus;
   private final PlaceDeleter placeDeleter;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public AccountDeleteHandler(
         AccountDAO accountDao,
         PersonDAO personDao,
         PersonPlaceAssocDAO personPlaceAssocDao,
         AuthorizationGrantDAO authGrantDao,
         PreferencesDAO preferencesDao,
         PlaceDeleter placeDeleter,
         PlatformMessageBus bus,
         PlacePopulationCacheManager populationCacheMgr) {

      this.accountDao = accountDao;
      this.personDao = personDao;
      this.personPlaceAssocDao = personPlaceAssocDao;
      this.authGrantDao = authGrantDao;
      this.preferencesDao = preferencesDao;
      this.placeDeleter = placeDeleter;
      this.bus = bus;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public String getMessageType() {
      return AccountCapability.DeleteRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Account context, PlatformMessage msg) {
      Boolean deleteLogin = AccountCapability.DeleteRequest.getDeleteOwnerLogin(msg.getValue());
      deleteLogin = deleteLogin == null ? false : deleteLogin;

      try {
         doDeleteAccount(context, msg.getPlaceId(), deleteLogin);
         return AccountCapability.DeleteResponse.instance();
      }  catch (IllegalArgumentException iae) {
          logger.error("Invalid argument ", iae);
          return Errors.fromCode("invalid.argument", iae.getMessage());
      } catch (Throwable t) {
          logger.error("Failed to delete account", t);
          return Errors.fromCode("account.delete.failed", "Error deleting the account.");
      }
   }

   private void doDeleteAccount(Account account, String placeId, boolean deleteLogin) {
      accountDao.delete(account);

      Person owner = personDao.findById(account.getOwner());
      sendNotifications(account, owner, placeId);

      account.getPlaceIDs().forEach((p) -> deletePlace(account.getAddress(), p));

      if(deleteLogin) {
         deleteAccountOwner(account, owner, placeId);
      } else {
         owner.setCurrPlace(null);
         owner.setAccountId(null);
         personDao.update(owner);
         emitOwnerValueChange(owner);
      }
   }

   private void emitOwnerValueChange(Person owner) {
      MessageBody body = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, ImmutableMap.of(
         PersonCapability.ATTR_CURRPLACE, "",
         PersonCapability.ATTR_HASPIN, false,
         PersonCapability.ATTR_PLACESWITHPIN, owner.getPlacesWithPin()
      ));
      authGrantDao.findForEntity(owner.getId()).forEach((g) -> {
         PlatformMessage evt = PlatformMessage.buildBroadcast(body, Address.fromString(owner.getAddress()))
               .withPlaceId(g.getPlaceId())
               .withPopulation(populationCacheMgr.getPopulationByPlaceId(g.getPlaceId()))
               .create();
         bus.send(evt);
      });
   }

   private void sendNotifications(Account account, Person owner, String placeId) {
	   EmailRecipient recipient = new EmailRecipient();
	   recipient.setEmail(owner.getEmail());
	   recipient.setFirstName(owner.getFirstName());
	   recipient.setLastName(owner.getLastName());
	   Notifications.sendEmailNotification(bus, recipient, placeId, populationCacheMgr.getPopulationByPlaceId(placeId), Notifications.AccountRemoved.KEY,
				ImmutableMap.<String, String>of(Notifications.AccountRemoved.PARAM_PERSON_FIRSTNAME, owner.getFirstName() == null ? "" : owner.getFirstName(),
						Notifications.AccountRemoved.PARAM_PERSON_LASTNAME, owner.getLastName() == null ? "" : owner.getLastName()),
				Address.platformService(PlatformConstants.SERVICE_ACCOUNTS));

   }

   private void emitAccountDeletedEvent(String accountAddress, String placeId) {
      MessageBody body = MessageBody.buildMessage(Capability.EVENT_DELETED, ImmutableMap.of());
      PlatformMessage event = PlatformMessage.buildBroadcast(body, Address.fromString(accountAddress))
            .withPlaceId(placeId)
            .withPopulation(populationCacheMgr.getPopulationByPlaceId(placeId))
            .create();
      bus.send(event);
   }

   private void deletePlace(String accountAddress, UUID placeId) {
	   if(placeId != null) {
		   Place curPlace = placeDeleter.getPlace(placeId);
		   if(curPlace != null) {
			   placeDeleter.deletePlace(curPlace, false);
			   emitAccountDeletedEvent(accountAddress, placeId.toString());
		   }
	   }
   }

	private void deleteAccountOwner(Account account, Person owner, String placeId) {
		List<String> places = personPlaceAssocDao
				.listPlaceAccessForPerson(owner.getId()).stream()
				.map(pad -> pad.getPlaceId()).collect(Collectors.toList());

		if (!places.contains(placeId)) {
			places.add(placeId);
		}

		preferencesDao.deleteForPerson(owner.getId());
		authGrantDao.removeGrantsForEntity(owner.getId());
		personDao.delete(owner);

		for (String thisPlace : places) {
			MessageBody body = MessageBody.buildMessage(Capability.EVENT_DELETED, ImmutableMap.of("bootSession", true));

			PlatformMessage evt = PlatformMessage
					.buildBroadcast(body,Address.fromString(owner.getAddress()))
					.withPlaceId(thisPlace)
					.withPopulation(populationCacheMgr.getPopulationByPlaceId(thisPlace))
					.create();

			bus.send(evt);
		}
	}
}
