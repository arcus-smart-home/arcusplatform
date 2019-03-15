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
package com.iris.platform.person;

import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.notification.Notifications;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.messages.services.PlatformConstants;
import com.iris.messages.type.EmailRecipient;
import com.iris.messages.type.Invitation;
import com.iris.security.authz.AuthorizationGrant;

public class InvitationHandlerHelper {

	public static AuthorizationGrant createGrantsUponAcceptingInvitation(AuthorizationGrantDAO authGrantDao, Person person, Place place) {
      AuthorizationGrant grant = new AuthorizationGrant();
      grant.setAccountId(place.getAccount());
      grant.setAccountOwner(false);
      grant.setEntityId(person.getId());
      grant.setPlaceId(place.getId());
      grant.setPlaceName(place.getName());
      grant.addPermissions("*:*:*");
      authGrantDao.save(grant);
      return grant;
    }

	public static void emitPersonAddedEvent(PlatformMessageBus bus, BeanAttributesTransformer<Person> personTransformer, Person person, Place place) {
      MessageBody addedBody = MessageBody.buildMessage(Capability.EVENT_ADDED, personTransformer.transform(person));
      PlatformMessage addedMsg = PlatformMessage.buildBroadcast(addedBody, Address.fromString(person.getAddress()))
            .withPlaceId(place.getId())
            .withPopulation(place.getPopulation())
            .create();
      bus.send(addedMsg);
	}

	public static void sendInvitationAcceptedNotifications(PlatformMessageBus bus, Invitation invite, String population) {
		//Send notification to inviter
		sendEmailNotification(bus, invite.getInvitorId(), invite.getPlaceId(), population, 
				Notifications.PersonAcceptedToJoinNotifyInviter.KEY,
				ImmutableMap.<String, String> of(Notifications.PersonAcceptedToJoinNotifyInviter.PARAM_INVITEE_FIRSTNAME, invite.getInviteeFirstName()
						, Notifications.PersonAcceptedToJoinNotifyInviter.PARAM_INVITEE_LASTNAME, invite.getInviteeLastName()));
		//Send notification to account owner if inviter is not the owner
		if(!Objects.equals(invite.getInvitorId(), invite.getPlaceOwnerId())) {
			sendEmailNotification(bus, invite.getPlaceOwnerId(), invite.getPlaceId(), population, 
					Notifications.PersonAcceptedToJoinNotifyOwner.KEY,
					ImmutableMap.<String, String> of(Notifications.PersonAcceptedToJoinNotifyOwner.PARAM_INVITEE_FIRSTNAME, invite.getInviteeFirstName()
							, Notifications.PersonAcceptedToJoinNotifyOwner.PARAM_INVITEE_LASTNAME, invite.getInviteeLastName()));
		}
   }

	public static void sendEmailNotification(PlatformMessageBus bus, String personId, String placeId, String population, String msgKey, Map<String, String> params) {
		Notifications.sendEmailNotification(bus, personId, placeId, population, msgKey, params, Address.platformService(PlatformConstants.SERVICE_PLACES));
	}

	public static void sendEmailNotificationToInvitee(PlatformMessageBus bus, Invitation invitation, String population, String msgKey, Map<String, String> params) {

	      EmailRecipient recipient = new EmailRecipient();
	      recipient.setEmail(invitation.getInviteeEmail());
	      recipient.setFirstName(invitation.getInviteeFirstName());
	      recipient.setLastName(invitation.getInviteeLastName());

	      Notifications.sendEmailNotification(bus, recipient, invitation.getPlaceId(), population, msgKey, params, Address.platformService(PlatformConstants.SERVICE_PLACES));
	}

	public static boolean isInviterSameAsOwner(Invitation invitation) {
		return Objects.equals( invitation.getInvitorId(), invitation.getPlaceOwnerId());
	}

	public static Person getActorFromMessage(PlatformMessage msg, PersonDAO personDao) {
		Address addr = msg.getActor();
		return getActorFromAddress(addr, personDao);
	}
	
	public static Person getActorFromAddress(Address actorAddress, PersonDAO personDao) {
		if(actorAddress != null) {
			Person invitor = personDao.findByAddress(actorAddress);
			if(invitor != null) {
				return invitor;
			}
		}
		throw new ErrorEventException(Errors.CODE_NOT_FOUND, "no person found from actor " + (actorAddress == null ? "null" : actorAddress.getRepresentation()));
	}
}

