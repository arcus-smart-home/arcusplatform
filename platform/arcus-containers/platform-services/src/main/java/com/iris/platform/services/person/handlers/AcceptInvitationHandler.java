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
import java.util.Objects;
import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.InvitationDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.messages.type.Invitation;
import com.iris.platform.person.InvitationHandlerHelper;
import com.iris.security.authz.AuthorizationGrant;

@Singleton
public class AcceptInvitationHandler implements ContextualRequestMessageHandler<Person> {

   private final InvitationDAO invitationDao;
   private final PlaceDAO placeDao;
   private final AuthorizationGrantDAO authGrantDao;
   private final BeanAttributesTransformer<Person> personTransformer;
   private final PlatformMessageBus bus;

   @Inject
   public AcceptInvitationHandler(
         InvitationDAO invitationDao,
         PlaceDAO placeDao,
         AuthorizationGrantDAO authGrantDao,
         BeanAttributesTransformer<Person> personTransformer,
         PlatformMessageBus bus) {

      this.invitationDao = invitationDao;
      this.placeDao = placeDao;
      this.authGrantDao = authGrantDao;
      this.personTransformer = personTransformer;
      this.bus = bus;
   }

   @Override
   public String getMessageType() {
      return PersonCapability.AcceptInvitationRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Person context, PlatformMessage msg) {
      MessageBody body = msg.getValue();
      validate(body);

      String code = PersonCapability.AcceptInvitationRequest.getCode(body);
      String email = PersonCapability.AcceptInvitationRequest.getInviteeEmail(body);
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

      Place place = placeDao.findById(UUID.fromString(invite.getPlaceId()));

      if(place == null) {
         throw new ErrorEventException(Errors.CODE_NOT_FOUND, "place " + invite.getPlaceId() + " not found");
      }

      List<AuthorizationGrant> grants = authGrantDao.findForEntity(context.getId());
      // user is already associated with this place
      if(grants.stream().anyMatch((g) -> { return Objects.equals(g.getPlaceId(), place.getId()); })) {
         invitationDao.accept(invite.getCode(), context.getId());
         return PersonCapability.AcceptInvitationResponse.instance();
      }

      InvitationHandlerHelper.createGrantsUponAcceptingInvitation(authGrantDao, context, place);
      invitationDao.accept(invite.getCode(), context.getId());

      // TODO:  notifications/events
      InvitationHandlerHelper.emitPersonAddedEvent(bus, personTransformer, context, place);
      InvitationHandlerHelper.sendInvitationAcceptedNotifications(bus, invite, place.getPopulation());
      return PersonCapability.AcceptInvitationResponse.instance();
   }

   private void validate(MessageBody body) {
      Errors.assertRequiredParam(PersonCapability.AcceptInvitationRequest.getCode(body), "code");
      Errors.assertRequiredParam(PersonCapability.AcceptInvitationRequest.getInviteeEmail(body), "inviteeEmail");

   }


}

