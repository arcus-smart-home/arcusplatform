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

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.InvitationDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.notification.Notifications;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.messages.type.Invitation;
import com.iris.platform.person.InvitationHandlerHelper;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.security.authz.AuthorizationGrant;

@Singleton
public class CancelInvitationHandler implements ContextualRequestMessageHandler<Place> {

   private final InvitationDAO invitationDao;
   private final PlatformMessageBus bus;
   private final PersonDAO personDao;
   private final AuthorizationGrantDAO authGrantDao;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public CancelInvitationHandler(InvitationDAO invitationDao, 
   		PersonDAO personDao, 
   		AuthorizationGrantDAO authGrantDao,
   		PlatformMessageBus bus,
   		PlacePopulationCacheManager populationCacheMgr) {
      this.invitationDao = invitationDao;
      this.personDao = personDao;
      this.authGrantDao = authGrantDao;
      this.bus = bus;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public String getMessageType() {
      return PlaceCapability.CancelInvitationRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Place context, PlatformMessage msg) {
      MessageBody body = msg.getValue();
      validate(body);

      String code = PlaceCapability.CancelInvitationRequest.getCode(body);
      Invitation invitation = invitationDao.find(code);
      if(invitation != null) {
         if(invitation.getAccepted() != null || invitation.getRejected() != null) {
            return PlaceCapability.CancelInvitationResponse.instance();
         }
         if(!Objects.equals(context.getId().toString(), invitation.getPlaceId())) {
            throw new ErrorEventException(Errors.CODE_INVALID_REQUEST, "invitation " + code + " not for place " + context.getId());
         }
         invitationDao.cancel(invitation);

         emitInvitationCancelled(context, invitation);

         Person invitationCancelledByPerson = InvitationHandlerHelper.getActorFromMessage(msg, personDao);
         if(invitationCancelledByPerson != null) {
            sendNotifications(invitation, invitationCancelledByPerson);
         }
      }

      return PlaceCapability.CancelInvitationResponse.instance();
   }

   private void emitInvitationCancelled(Place context, Invitation invitation) {
      if (invitation.getInviteeId() == null) {
         return;
      }
      // Send event to every place because we don't know which one the user is logged into, if any
      List<AuthorizationGrant> grants = authGrantDao.findForEntity(UUID.fromString(invitation.getInviteeId()));
      grants.forEach(g -> emitInvitationCancelled(context, g.getPlaceId()));
   }

   private void emitInvitationCancelled(Place context, UUID placeId) {
      PlatformMessage msg = PlatformMessage.buildBroadcast(
         PersonCapability.InvitationCancelledEvent.instance(),
         Address.fromString(context.getAddress()))
            .withPlaceId(placeId)
            .withPopulation(populationCacheMgr.getPopulationByPlaceId(placeId))
            .create();
      bus.send(msg);
   }

   private void sendNotifications(Invitation invite, Person invitationCancelledByPerson) {
   	String population = populationCacheMgr.getPopulationByPlaceId(invite.getPlaceId());
	   //Send notification to inviter
	   InvitationHandlerHelper.sendEmailNotification(bus, invite.getInvitorId(), invite.getPlaceId(), population,
				Notifications.PersonInvitationCancelled.KEY,
				ImmutableMap.<String, String> of(Notifications.PersonInvitationCancelled.PARAM_INVITEE_FIRSTNAME, invite.getInviteeFirstName()
						, Notifications.PersonInvitationCancelled.PARAM_INVITEE_LASTNAME, invite.getInviteeLastName()
						, Notifications.PersonInvitationCancelled.PARAM_PERSON_CANCELLED_FIRSTNAME, invitationCancelledByPerson.getFirstName()
						, Notifications.PersonInvitationCancelled.PARAM_PERSON_CANCELLED_LASTNAME, invitationCancelledByPerson.getLastName()));

	    //If owner is not the same as inviter, send notification to account owner
	    if(!InvitationHandlerHelper.isInviterSameAsOwner(invite)) {
	 	   InvitationHandlerHelper.sendEmailNotification(bus, invite.getPlaceOwnerId(), invite.getPlaceId(), population,
					Notifications.PersonInvitationCancelled.KEY,
					ImmutableMap.<String, String> of(Notifications.PersonInvitationCancelled.PARAM_INVITEE_FIRSTNAME, invite.getInviteeFirstName()
							, Notifications.PersonInvitationCancelled.PARAM_INVITEE_LASTNAME, invite.getInviteeLastName()
							, Notifications.PersonInvitationCancelled.PARAM_PERSON_CANCELLED_FIRSTNAME, invitationCancelledByPerson.getFirstName()
							, Notifications.PersonInvitationCancelled.PARAM_PERSON_CANCELLED_LASTNAME, invitationCancelledByPerson.getLastName()));
	    }

	    //Send notification to the invitee
	    InvitationHandlerHelper.sendEmailNotificationToInvitee(bus, invite, population, Notifications.PersonInvitationCancelledNotifyInvitee.KEY,
	    		ImmutableMap.<String, String> of(Notifications.PersonInvitationCancelledNotifyInvitee.PARAM_INVITEE_FIRSTNAME, invite.getInviteeFirstName()
						, Notifications.PersonInvitationCancelledNotifyInvitee.PARAM_INVITEE_LASTNAME, invite.getInviteeLastName()
						, Notifications.PersonInvitationCancelledNotifyInvitee.PARAM_INVITER_FIRSTNAME, invite.getInvitorFirstName()
						, Notifications.PersonInvitationCancelledNotifyInvitee.PARAM_INVITER_LASTNAME, invite.getInvitorLastName()));

	}

   private void validate(MessageBody body) {
      Errors.assertRequiredParam(PlaceCapability.CancelInvitationRequest.getCode(body), "code");
   }

}

