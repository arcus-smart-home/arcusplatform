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
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.InvitationDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Place;
import com.iris.messages.type.Invitation;

@Singleton
public class PendingInvitationsHandler implements ContextualRequestMessageHandler<Place> {

   private final InvitationDAO invitationDao;

   @Inject
   public PendingInvitationsHandler(InvitationDAO invitationDao) {
      this.invitationDao = invitationDao;
   }

   @Override
   public String getMessageType() {
      return PlaceCapability.PendingInvitationsRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Place context, PlatformMessage msg) {
      List<Invitation> invitations = invitationDao.listForPlace(context.getId());
      return PlaceCapability.PendingInvitationsResponse.builder()
            .withInvitations(
                  invitations.stream()
                  .filter((i) -> { return i.getAccepted() == null && i.getRejected() == null; })
                  .map((i) -> { return i.toMap(); }).collect(Collectors.toList()))
            .build();
   }

}

