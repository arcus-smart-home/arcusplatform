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

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.messages.type.Invitation;

@Singleton
public class CreateInvitationHandler implements ContextualRequestMessageHandler<Place> {

   private static final String OWNER_DEFAULT_TMPL = "Great news! %s would like to give you access to their smart home at the place called %s.";
   private static final String GUEST_DEFAULT_TMPL = "Great news! %s would like to give you access to %s's smart home at the place called %s.";

   private final PersonDAO personDao;
   private final AccountDAO accountDao;

   @Inject
   public CreateInvitationHandler(PersonDAO personDao, AccountDAO accountDao) {
      this.personDao = personDao;
      this.accountDao = accountDao;
   }

   @Override
   public String getMessageType() {
      return PlaceCapability.CreateInvitationRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Place context, PlatformMessage msg) {
      MessageBody body = msg.getValue();
      validate(body);

      Person actor = getPersonFromActor(msg.getActor());

      Invitation invitation = new Invitation();

      invitation.setInviteeFirstName(PlaceCapability.CreateInvitationRequest.getFirstName(body));
      invitation.setInviteeLastName(PlaceCapability.CreateInvitationRequest.getLastName(body));
      invitation.setInviteeEmail(PlaceCapability.CreateInvitationRequest.getEmail(body));
      invitation.setRelationship(PlaceCapability.CreateInvitationRequest.getRelationship(body));

      if(Objects.equals(actor.getAccountId(), context.getAccount())) {
         invitation.setInvitationText(
               String.format(
                     OWNER_DEFAULT_TMPL,
                     displayName(actor),
                     context.getName()));
      } else {
         Person owner = getOwner(context);
         invitation.setInvitationText(
               String.format(
                     GUEST_DEFAULT_TMPL,
                     displayName(actor),
                     displayName(owner),
                     context.getName()));
      }

      Map<String,Object> invitationMap = invitation.toMap()
            .entrySet().stream().filter((e) -> { return e.getValue() != null; })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      return PlaceCapability.CreateInvitationResponse.builder().withInvitation(invitationMap).build();
   }

   private String displayName(Person person) {
      if(StringUtils.isBlank(person.getFirstName())) {
         return StringUtils.isBlank(person.getLastName()) ? person.getEmail() : person.getLastName();
      }
      if(StringUtils.isBlank(person.getLastName())) {
         return person.getFirstName();
      }
      return person.getFirstName() + " " + person.getLastName();
   }

   private Person getOwner(Place place) {
      Account account = accountDao.findById(place.getAccount());
      return personDao.findById(account.getOwner());
   }

   private Person getPersonFromActor(Address addr) {
      Person actor = personDao.findByAddress(addr);
      if(actor == null) {
         throw new ErrorEventException(Errors.CODE_NOT_FOUND, "no person found from actor " + (addr == null ? "null" : addr.getRepresentation()));
      }
      return actor;
   }

   private void validate(MessageBody body) {
      if(StringUtils.isBlank(PlaceCapability.CreateInvitationRequest.getFirstName(body))) {
         throw new ErrorEventException(Errors.CODE_MISSING_PARAM, "firstName is required");
      }
      if(StringUtils.isBlank(PlaceCapability.CreateInvitationRequest.getLastName(body))) {
         throw new ErrorEventException(Errors.CODE_MISSING_PARAM, "lastName is required");
      }
      if(StringUtils.isBlank(PlaceCapability.CreateInvitationRequest.getEmail(body))) {
         throw new ErrorEventException(Errors.CODE_MISSING_PARAM, "email is required");
      }
   }
}

