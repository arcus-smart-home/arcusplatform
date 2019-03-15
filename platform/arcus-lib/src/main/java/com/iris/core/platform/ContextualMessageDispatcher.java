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
package com.iris.core.platform;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.messages.Message;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.address.PlatformServiceAddress;
import com.iris.messages.errors.Errors;


public abstract class ContextualMessageDispatcher<M extends Message,T> extends AbstractMessageListener<M> {
   private static final Logger logger = LoggerFactory.getLogger(ContextualMessageDispatcher.class);

   private final Map<String, ContextualRequestHandler<M,T>> requestDispatchTable;
   private final Map<String, ContextualEventHandler<M,T>> eventDispatchTable;

   protected ContextualMessageDispatcher(
         RequestResponseMessageBus<M> bus,
         Executor executor,
         Collection<? extends ContextualRequestHandler<M,T>> requestHandlers,
         Collection<? extends ContextualEventHandler<M,T>> eventHandlers
   ) {
      super(bus, executor);
      
      Map<String,ContextualRequestHandler<M,T>> tmpRequestDispatchTable = new HashMap<>();
      for(ContextualRequestHandler<M,T> handler : requestHandlers) {
         tmpRequestDispatchTable.put(handler.getMessageType(), handler);
      }
      requestDispatchTable = Collections.unmodifiableMap(tmpRequestDispatchTable);

      Map<String, ContextualEventHandler<M,T>> tmpEventDispatchTable = new HashMap<>();
      for(ContextualEventHandler<M,T> handler : eventHandlers) {
         tmpEventDispatchTable.put(handler.getEventType(), handler);
      }
      eventDispatchTable = Collections.unmodifiableMap(tmpEventDispatchTable);
   }

   protected abstract String getMessageType(M message);
   protected abstract Address getMessageDestination(M message);
   protected abstract M getResponse(M message, MessageBody response);

   @Override
   protected MessageBody handleRequest(M message) throws Exception {
      String type = getMessageType(message);
      ContextualRequestHandler<M,T> handler = requestDispatchTable.get(type);
      if(handler == null) {
         return super.handleRequest(message);
      }

      Address destination = getMessageDestination(message);
      if(Address.ZERO_UUID.equals(destination.getId())) {
         return handler.handleStaticRequest(message);
      }

      T context = loadContext(destination);
      if(context == null) {
         return Errors.notFound(destination);
      }

      return handler.handleRequest(context, message);
   }

   @Override
   protected void handleEvent(M message) {
      Address destination;
      boolean isBroadcast;
      try {
         destination = getMessageDestination(message);
         isBroadcast = destination == null || destination.isBroadcast();
      } catch (Exception e) {
         logger.warn("Error handling event [{}]", e);
         return;
      }

      try {
         String type = getMessageType(message);
         ContextualEventHandler<M,T> handler = eventDispatchTable.get(type);
         if(handler == null) {
            super.handleEvent(message);
            return;
         }
         // broadcast events could come from anywhere so invoke static method
         if(isBroadcast) {
            handler.handleStaticEvent(message);
         } else {
            T context = loadContext(destination);
            handler.handleEvent(context, message);
         }
      } catch(Exception e) {
         // it doesn't really make sense to reply with an error for a broadcast event, so just log it
         if(isBroadcast) {
            logger.warn("Error handling broadcast event [{}]", e);
         } else {
            sendMessage(getResponse(message, Errors.fromException(e)));
         }
      }
   }

   private T loadContext(Address address) {
      if(!(address instanceof PlatformServiceAddress)) {
         throw new IllegalArgumentException("Invalid address type [{}] when a PlatformServiceAddress was expected");
      }

      PlatformServiceAddress platformServiceAddress = (PlatformServiceAddress) address;
      if(Address.ZERO_UUID.equals(platformServiceAddress.getContextId())) {
         return null;
      }

      return loadContext(platformServiceAddress.getContextId(), platformServiceAddress.getContextQualifier());
   }

   protected abstract T loadContext(Object id, Integer qualifier);
}

