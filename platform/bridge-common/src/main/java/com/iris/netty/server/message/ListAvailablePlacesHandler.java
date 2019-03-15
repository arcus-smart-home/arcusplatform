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
package com.iris.netty.server.message;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import com.google.inject.Inject;
import com.iris.bridge.server.session.Session;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.service.SessionService;
import com.iris.messages.service.SessionService.ListAvailablePlacesRequest;
import com.iris.messages.service.SessionService.ListAvailablePlacesResponse;
import com.iris.messages.type.PlaceAccessDescriptor;

@Singleton
public class ListAvailablePlacesHandler extends BaseClientRequestHandler
{
   private final PersonPlaceAssocDAO personPlaceAssocDao;

   @Inject
   public ListAvailablePlacesHandler(PersonPlaceAssocDAO personPlaceAssocDao)
   {
      super(directExecutor());

      this.personPlaceAssocDao = personPlaceAssocDao;
   }

   @Override
   public String getRequestType()
   {
      return ListAvailablePlacesRequest.NAME;
   }

   @Override
   protected MessageBody doHandle(ClientMessage request, Session session)
   {
      UUID personId = session.getClient().getPrincipalId();

      List<Map<String, Object>> places = personPlaceAssocDao.listPlaceAccessForPerson(personId)
         .stream().map(PlaceAccessDescriptor::toMap).collect(Collectors.toList());

      return ListAvailablePlacesResponse.builder()
         .withPlaces(places)
         .build();
   }

   @Override
   protected Address address()
   {
   	return SessionService.ADDRESS;
   }
}

