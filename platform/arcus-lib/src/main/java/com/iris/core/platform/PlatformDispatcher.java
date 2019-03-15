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
/**
 * 
 */
package com.iris.core.platform;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.iris.core.messaging.MessageListener;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.AddressMatcher;

/**
 * 
 */
public class PlatformDispatcher implements MessageListener<PlatformMessage> {
   private final Set<AddressMatcher> matchers;
   private final Map<String, Function<PlatformMessage, ? extends MessageBody>> requestHandlers;
   private final Function<PlatformMessage, ? extends MessageBody> fallbackHandler;
   private final Map<String, Consumer<? super PlatformMessage>> eventHandlers;
   private final Consumer<? super PlatformMessage> wildcardEventHandler;
   private final PlatformMessageBus messageBus;
   
   protected PlatformDispatcher(
         Set<AddressMatcher> matchers,
         Map<String, Function<PlatformMessage, ? extends MessageBody>> requestHandlers,
         Map<String, Consumer<? super PlatformMessage>> eventHandlers,
         PlatformMessageBus messageBus
   ) {
      this.matchers = ImmutableSet.copyOf(matchers);
      this.requestHandlers = requestHandlers;
      this.fallbackHandler = requestHandlers.get(MessageConstants.MSG_ANY_MESSAGE_TYPE); 
      this.eventHandlers = eventHandlers;
      this.wildcardEventHandler = eventHandlers.get(MessageConstants.MSG_ANY_MESSAGE_TYPE);
      this.messageBus = messageBus;
   }
   
   public void start() {
      messageBus.addMessageListener(matchers, this);
   }
   
   public void stop() {
   }
   
   public Set<AddressMatcher> matchers() {
      return matchers;
   }

   @Override
   public void onMessage(PlatformMessage message) {
      if(!message.isRequest()) {
         handleEvent(message);
      } else {
         handleRequest(message);
      }
   }

   private void handleRequest(PlatformMessage message) {
      Function<PlatformMessage, ? extends MessageBody> handler = requestHandlers.get(message.getMessageType());
      if(handler != null) {
         messageBus.invokeAndSendResponse(message, handler);
      }
      else if(fallbackHandler != null) {
      	messageBus.invokeAndSendResponse(message, fallbackHandler);
      }
      // TODO else return unsupported?
   }

   private void handleEvent(PlatformMessage message) {
      Consumer<? super PlatformMessage> consumer = eventHandlers.get(message.getMessageType());
      if(consumer != null) {
         consumer.accept(message);
      }
      if(wildcardEventHandler != null) {
      	wildcardEventHandler.accept(message);
      }
   }


}

