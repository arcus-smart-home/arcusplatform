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

import java.util.Objects;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.InvitationDAO;
import com.iris.core.notification.Notifications;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Person;
import com.iris.messages.type.Invitation;
import com.iris.platform.person.InvitationHandlerHelper;
import com.iris.population.PlacePopulationCacheManager;

@Singleton
public class RejectInvitationHandler implements ContextualRequestMessageHandler<Person> {

   private final InvitationDAO invitationDao;
   private final PlatformMessageBus bus;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public RejectInvitationHandler(InvitationDAO invitationDao, PlatformMessageBus bus, PlacePopulationCacheManager populationCacheMgr) {
      this.invitationDao = invitationDao;
      this.bus = bus;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public String getMessageType() {
      return PersonCapability.RejectInvitationRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Person context, PlatformMessage msg) {
      MessageBody body = msg.getValue();
      validate(body);

      String code = PersonCapability.RejectInvitationRequest.getCode(body);
      String email = PersonCapability.RejectInvitationRequest.getInviteeEmail(body);
      Invitation invite = invitationDao.find(code);

      if(invite == null) {
         throw new ErrorEventException(Errors.CODE_NOT_FOUND, "invitation " + code + " not found");
      }
      if(!Objects.equals(email, invite.getInviteeEmail())) {
         throw new ErrorEventException(Errors.CODE_NOT_FOUND, "invitation " + code + " for " + email + " not found");
      }
      if(invite.getInviteeId() != null && !Objects.equals(invite.getInviteeId(), context.getId().toString())) {
         throw new ErrorEventException(Errors.CODE_INVALID_REQUEST, "invitation " + code + " not for person " + context.getId());
      }

      if(invite.getAccepted() != null || invite.getRejected() != null) {
         throw new ErrorEventException(Errors.CODE_INVALID_REQUEST, "invitation " + code + " has already been accepted or rejected.");
      }

      invitationDao.reject(code, PersonCapability.RejectInvitationRequest.getReason(body));

      //send notifications and events
      sendNotifications(invite);
      emitEvent(context, invite);
      return PersonCapability.RejectInvitationResponse.instance();
   }

   private void emitEvent(Person context, Invitation invite) {
   	UUID placeId = UUID.fromString(invite.getPlaceId());
   	PlatformMessage msg = PlatformMessage.buildBroadcast(
            PersonCapability.InvitationRejectedEvent.builder().withInvitation(invite.toMap()).build(),
            Address.fromString(context.getAddress()))
               .withPlaceId(placeId)
               .withPopulation(populationCacheMgr.getPopulationByPlaceId(placeId))
               .withActor(Address.fromString(context.getAddress()))
               .create();
         bus.send(msg);
		
	}

	private void sendNotifications(Invitation invite) {
   	String population = populationCacheMgr.getPopulationByPlaceId(invite.getPlaceId());
	   //Send notification to inviter
	   InvitationHandlerHelper.sendEmailNotification(bus, invite.getInvitorId(), invite.getPlaceId(), population,
				Notifications.PersonDeclinedToJoinNotifyInviter.KEY, 
				ImmutableMap.<String, String> of(Notifications.PersonDeclinedToJoinNotifyInviter.PARAM_INVITEE_FIRSTNAME, invite.getInviteeFirstName()
						, Notifications.PersonDeclinedToJoinNotifyInviter.PARAM_INVITEE_LASTNAME, invite.getInviteeLastName()));
	   
	
	    //If owner is not the same as inviter, send notification to account owner
	    if(!InvitationHandlerHelper.isInviterSameAsOwner(invite)) {
	    	InvitationHandlerHelper.sendEmailNotification(bus, invite.getPlaceOwnerId(), invite.getPlaceId(), population, 
					Notifications.PersonDeclinedToJoinNotifyOwner.KEY, 
					ImmutableMap.<String, String> of(Notifications.PersonDeclinedToJoinNotifyOwner.PARAM_INVITEE_FIRSTNAME, invite.getInviteeFirstName()
							, Notifications.PersonDeclinedToJoinNotifyOwner.PARAM_INVITEE_LASTNAME, invite.getInviteeLastName()));
	    }
		
   }

   private void validate(MessageBody body) {
      Errors.assertRequiredParam(PersonCapability.RejectInvitationRequest.getCode(body), "code");
      Errors.assertRequiredParam(PersonCapability.RejectInvitationRequest.getInviteeEmail(body), "inviteeEmail");
   }
}

