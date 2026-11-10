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
package com.iris.platform.services.place;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.platform.ContextualEventMessageHandler;
import com.iris.core.platform.ContextualPlatformMessageDispatcher;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.platform.PlatformService;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Place;
import com.iris.messages.services.PlatformConstants;

@Singleton
public class PlaceService extends ContextualPlatformMessageDispatcher<Place> implements PlatformService {
   public static final String PROP_THREADPOOL = "platform.service.place.threadpool";

   private static final Logger logger = LoggerFactory.getLogger(PlaceService.class);
   private static final Address address = Address.platformService(PlatformConstants.SERVICE_PLACES);

   private final PlaceDAO placeDao;

   @Inject
   public PlaceService(
         PlatformMessageBus platformBus,
         @Named(PROP_THREADPOOL) Executor executor,
         Set<ContextualRequestMessageHandler<Place>> handlers,
         Set<ContextualEventMessageHandler<Place>> eventHandlers,
         PlaceDAO placeDao
   ) {
      super(platformBus, executor, handlers, eventHandlers);
      this.placeDao = placeDao;
   }

   @Override
   public Address getAddress() {
      return address;
   }

   @Override
   public void handleMessage(PlatformMessage message) {
      super.handleMessage(message);
   }

   // RoleAuthorizer keeps a client to its own places. hub-bridge runs no authorizer and forwards a
   // hub's requests anywhere with the hub's place as the place header, so a hub is kept to that place.
   @Override
   protected MessageBody handleRequest(PlatformMessage message) throws Exception {
      Address source = message.getSource();
      if(source != null && source.getHubId() != null && !Objects.equals(String.valueOf(message.getDestination().getId()), message.getPlaceId())) {
         logger.warn("Refusing [{}] for [{}] from hub [{}] at place [{}]", message.getMessageType(), message.getDestination(), source.getHubId(), message.getPlaceId());
         throw new UnauthorizedRequestException(message.getDestination(), "A hub may only send requests to its own place");
      }
      return super.handleRequest(message);
   }

   @Override
   protected Place loadContext(Object contextId, Integer qualifier) {
      if(!(contextId instanceof UUID)) {
         throw new IllegalArgumentException("The context ID must be a UUID");
      }
      return placeDao.findById((UUID) contextId);
   }
}

