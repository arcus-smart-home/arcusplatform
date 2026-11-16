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
package com.iris.platform.services;

import java.util.Collection;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.core.platform.ContextualEventHandler;
import com.iris.core.platform.ContextualPlatformMessageDispatcher;
import com.iris.core.platform.ContextualRequestHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.errors.UnauthorizedRequestException;

/**
 * A service that hubs have no business with. hub-bridge runs no authorizer: it forwards a hub's
 * messages to any address and keeps whatever actor the hub set, so the checks RoleAuthorizer makes
 * for clients never happen for hubs. Nothing on a hub talks to the services that extend this, so a
 * hub's requests to them are refused and its events addressed to one of their records are dropped.
 * Broadcasts still reach their static handlers, since hubs broadcast their own changes.
 */
public abstract class HubExcludingDispatcher<T> extends ContextualPlatformMessageDispatcher<T> {
   private static final Logger logger = LoggerFactory.getLogger(HubExcludingDispatcher.class);

   protected HubExcludingDispatcher(
         PlatformMessageBus bus,
         Executor executor,
         Collection<? extends ContextualRequestHandler<PlatformMessage,T>> requestHandlers,
         Collection<? extends ContextualEventHandler<PlatformMessage,T>> eventHandlers
   ) {
      super(bus, executor, requestHandlers, eventHandlers);
   }

   @Override
   protected MessageBody handleRequest(PlatformMessage message) throws Exception {
      if(isFromHub(message)) {
         logger.warn("Refusing [{}] for [{}] from hub [{}]", message.getMessageType(), message.getDestination(), message.getSource().getHubId());
         throw new UnauthorizedRequestException(message.getDestination(), "Hubs may not send this request");
      }
      return super.handleRequest(message);
   }

   @Override
   protected void handleEvent(PlatformMessage message) {
      Address destination = message.getDestination();
      if(isFromHub(message) && destination != null && !destination.isBroadcast()) {
         logger.warn("Dropping [{}] for [{}] from hub [{}]", message.getMessageType(), destination, message.getSource().getHubId());
         return;
      }
      super.handleEvent(message);
   }

   private static boolean isFromHub(PlatformMessage message) {
      return message.getSource() != null && message.getSource().isHubAddress();
   }
}
