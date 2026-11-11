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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.Utils;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.model.Account;
import com.iris.messages.model.Place;

@Singleton
public class ListPlacesHandler implements ContextualRequestMessageHandler<Account> {

   public static final String MESSAGE_TYPE = AccountCapability.ListPlacesRequest.NAME;

   private final PlaceDAO placeDAO;
   private final PersonPlaceAssocDAO associationDAO;
   private final BeanAttributesTransformer<Place> placeTransformer;

   @Inject
   public ListPlacesHandler(
   		PlaceDAO placeDAO,
   		PersonPlaceAssocDAO associationDAO,
   		BeanAttributesTransformer<Place> placeTransformer
	) {
      this.placeDAO = placeDAO;
      this.associationDAO = associationDAO;
      this.placeTransformer = placeTransformer;
   }

   @Override
   public String getMessageType() {
      return MESSAGE_TYPE;
   }

   @Override
   public MessageBody handleRequest(Account context, PlatformMessage msg) {
      Utils.assertNotNull(context, "The account is required");
      
      List<Place> places;
      Address actor = msg.getActor();
      // a person actor is SERV:person:<id>; the owner gets every place on the account. Anyone
      // without a person actor (internal callers, API keys, which api-bridge narrows to the key's
      // place in ApiPlatformBusListener) gets them all too.
      if(actor != null && PersonCapability.NAMESPACE.equals(actor.getGroup()) && !Objects.equals(context.getOwner(), actor.getId())) {
         places = getAssociatedPlaces(context, actor.getId());
      }
      else {
         places = placeDAO.findByPlaceIDIn(context.getPlaceIDs());
      }

      List<Map<String,Object>> transformed = places.stream().map((p) -> placeTransformer.transform(p)).collect(Collectors.toList());
      Map<String,Object> response = new HashMap<>();
      response.put(AccountCapability.ListPlacesResponse.ATTR_PLACES, transformed);
      return MessageBody.buildResponse(msg.getValue(), response);
   }

   // the places on this account the person has a grant on; a person id that isn't a UUID has none
   private List<Place> getAssociatedPlaces(Account context, Object personId) {
      if(!(personId instanceof UUID)) {
         return Collections.emptyList();
      }
      Set<UUID> placeIds = new HashSet<>(associationDAO.findPlaceIdsByPerson((UUID) personId));
      placeIds.retainAll(context.getPlaceIDs());
      return placeIds.isEmpty() ? Collections.emptyList() : placeDAO.findByPlaceIDIn(placeIds);
   }
}

