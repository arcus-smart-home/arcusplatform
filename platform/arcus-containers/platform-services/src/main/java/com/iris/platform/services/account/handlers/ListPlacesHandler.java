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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
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
import com.iris.messages.type.PlaceAccessDescriptor;

@Singleton
public class ListPlacesHandler implements ContextualRequestMessageHandler<Account> {

   public static final String MESSAGE_TYPE = AccountCapability.ListPlacesRequest.NAME;

   @Inject(optional = true) @Named("account.listplaces.filterByAccount")
   private boolean filterByAccount = true;
   
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
      if(
      		actor != null && 
      		PersonCapability.NAMESPACE.equals(actor.getNamespace())
		) {
      	places = getAssociatedPlaces(context, (UUID) actor.getId());
      }
      else {
      	places = placeDAO.findByPlaceIDIn(context.getPlaceIDs());
      }
      
      List<Map<String,Object>> transformed = places.stream().map((p) -> placeTransformer.transform(p)).collect(Collectors.toList());
      Map<String,Object> response = new HashMap<>();
      response.put(AccountCapability.ListPlacesResponse.ATTR_PLACES, transformed);
      return MessageBody.buildResponse(msg.getValue(), response);
   }

   private List<Place> getAssociatedPlaces(Account context, UUID personId) {
      Set<UUID> placeIds = new HashSet<>();
      for(PlaceAccessDescriptor descriptor: associationDAO.listPlaceAccessForPerson(personId)) {
      	if(descriptor == null) {
      		continue;
      	}
      	if(StringUtils.isEmpty(descriptor.getPlaceId())) {
      		continue;
      	}
      	placeIds.add(UUID.fromString(descriptor.getPlaceId()));
      }
      List<Place> places = placeDAO.findByPlaceIDIn(placeIds);
      if(filterByAccount) {
      	places = new ArrayList<>(places);
      	Iterator<Place> it = places.iterator();
      	while(it.hasNext()) {
      		if(!Objects.equals(context.getId(), it.next().getAccount())) {
      			it.remove();
      		}
      	}
      }
      return places;
   }
}

