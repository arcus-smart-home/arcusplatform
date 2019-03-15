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
package com.iris.platform.services.place.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.InvitationDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.notification.Notifications;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.messages.services.PlatformConstants;
import com.iris.messages.type.EmailRecipient;
import com.iris.messages.type.Invitation;
import com.iris.platform.person.InvitationHandlerHelper;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.security.authz.AuthorizationGrant;

@Singleton
public class SendInvitationHandler implements ContextualRequestMessageHandler<Place> {

   private final PersonDAO personDao;
   private final AccountDAO accountDao;
   private final InvitationDAO invitationDao;
   private final AuthorizationGrantDAO authGrantDao;
   private final PlatformMessageBus bus;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public SendInvitationHandler(
         PersonDAO personDao,
         AccountDAO accountDao,
         InvitationDAO invitationDao,
         AuthorizationGrantDAO authGrantDao,
         PlatformMessageBus bus,
         PlacePopulationCacheManager populationCacheMgr) {

      this.personDao = personDao;
      this.accountDao = accountDao;
      this.invitationDao = invitationDao;
      this.authGrantDao = authGrantDao;
      this.bus = bus;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public String getMessageType() {
      return PlaceCapability.SendInvitationRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Place context, PlatformMessage msg) {
      Invitation inv = new Invitation(PlaceCapability.SendInvitationRequest.getInvitation(msg.getValue()));
      validate(inv);

      Person invitor = InvitationHandlerHelper.getActorFromMessage(msg, personDao);
      Person accountOwner = invitor;

      if(!Objects.equals(invitor.getAccountId(), context.getAccount())) {
         accountOwner = getOwner(context.getAccount());
      }

      Person invitee = findInvitee(inv.getInviteeEmail());

      inv.setPlaceId(context.getId().toString());
      inv.setPlaceName(context.getName());
      inv.setStreetAddress1(context.getStreetAddress1());
      inv.setStreetAddress2(context.getStreetAddress2());
      inv.setCity(context.getCity());
      inv.setStateProv(context.getStateProv());
      inv.setZipCode(context.getZipCode());

      if(invitee != null) {
         inv.setInviteeId(invitee.getId().toString());
      }

      inv.setInvitorId(invitor.getId().toString());
      inv.setInvitorFirstName(invitor.getFirstName());
      inv.setInvitorLastName(invitor.getLastName());

      inv.setPlaceOwnerId(accountOwner.getId().toString());
      inv.setPlaceOwnerFirstName(accountOwner.getFirstName());
      inv.setPlaceOwnerLastName(accountOwner.getLastName());

      invitationDao.insert(inv);

      emitInvitationPending(context, invitee);
      sendInvite(inv);
      if(!InvitationHandlerHelper.isInviterSameAsOwner(inv)) {
    	  //inviter is not the account owner, so send notification to the account owner
    	  sendNotificationToAccountOwner(invitor, inv);
      }

      return PlaceCapability.SendInvitationResponse.instance();
   }

   private void sendNotificationToAccountOwner(Person invitor, Invitation inv) {
	   
	   InvitationHandlerHelper.sendEmailNotification(bus, inv.getPlaceOwnerId(), inv.getPlaceId(), 
	   		populationCacheMgr.getPopulationByPlaceId(inv.getPlaceId()),
				Notifications.PersonInvitedToJoinNotifyOwner.KEY, 
				ImmutableMap.<String, String> of(Notifications.PersonInvitedToJoinNotifyOwner.PARAM_INVITEE_FIRSTNAME, Notifications.ensureNotNull(inv.getInviteeFirstName())
						, Notifications.PersonInvitedToJoinNotifyOwner.PARAM_INVITEE_LASTNAME, Notifications.ensureNotNull(inv.getInviteeLastName())
						, Notifications.PersonInvitedToJoinNotifyOwner.PARAM_INVITER_FIRSTNAME, Notifications.ensureNotNull(invitor.getFirstName())
						, Notifications.PersonInvitedToJoinNotifyOwner.PARAM_INVITER_LASTNAME, Notifications.ensureNotNull(invitor.getLastName())));
   }

   private void emitInvitationPending(Place context, Person invitee) {
      if(invitee == null) {
         return;
      }
      // send event to every place because we don't know which one the user is logged into if any
      List<AuthorizationGrant> grants = authGrantDao.findForEntity(invitee.getId());
      grants.forEach((g) -> { emitInvitationPending(context, invitee, g.getPlaceId()); });
   }

   private void emitInvitationPending(Place context, Person invitee, UUID placeId) {
      PlatformMessage msg = PlatformMessage.buildBroadcast(
            PersonCapability.InvitationPendingEvent.instance(),
            Address.fromString(context.getAddress()))
            .withPlaceId(placeId)
            .withPopulation(populationCacheMgr.getPopulationByPlaceId(placeId))
            .create();
      bus.send(msg);
   }

   private void sendInvite(Invitation invitation) {
      Map<String,String> args = new HashMap<>();
      args.put(Notifications.PersonInvitedToJoin.PARAM_CODE, invitation.getCode());
      args.put(Notifications.PersonInvitedToJoin.PARAM_INVITATIONTEXT, invitation.getInvitationText());
      args.put(Notifications.PersonInvitedToJoin.PARAM_INVITEEFIRSTNAME, invitation.getInviteeFirstName());
      args.put(Notifications.PersonInvitedToJoin.PARAM_INVITEELASTNAME, invitation.getInviteeLastName());
      args.put(Notifications.PersonInvitedToJoin.PARAM_INVITORFIRSTNAME, invitation.getInvitorFirstName());
      args.put(Notifications.PersonInvitedToJoin.PARAM_INVITORLASTNAME, invitation.getInvitorLastName());
      args.put(Notifications.PersonInvitedToJoin.PARAM_PERSONALIZEDGREETING, invitation.getPersonalizedGreeting() == null ? "" : invitation.getPersonalizedGreeting());

      InvitationHandlerHelper.sendEmailNotificationToInvitee(bus, invitation, populationCacheMgr.getPopulationByPlaceId(invitation.getPlaceId()), Notifications.PersonInvitedToJoin.KEY, args);
   }

   private Person getOwner(UUID accountID) {
      Account account = accountDao.findById(accountID);
      return personDao.findById(account.getOwner());
   }

   private Person findInvitee(String email) {
      return personDao.findByEmail(email);
   }

   

   private void validate(Invitation inv) {
      if(inv == null) {
         throw new ErrorEventException(Errors.CODE_MISSING_PARAM, "invitation is required");
      }
      Errors.assertValidRequest(!StringUtils.isBlank(inv.getInviteeEmail()), "inviteeEmail is required");
      Errors.assertValidRequest(!StringUtils.isBlank(inv.getInviteeFirstName()), "inviteeFirstName is required");
      Errors.assertValidRequest(!StringUtils.isBlank(inv.getInviteeLastName()), "inviteeLastName is required");
   }
}

