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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.NotFoundException;

public class RequestHandlers {

   public static <T> PlatformRequestMessageHandler toRequestHandler(
         Function<PlatformMessage, T> loader, 
         ContextualRequestMessageHandler<? super T> handler
   ) {
      return new LoadAndDispatchMessageHandler<T>(loader, handler);
   }
   
   public static Consumer<PlatformMessage> toDispatcher(PlatformMessageBus platformBus, PlatformRequestMessageHandler... handlers) {
      return toDispatcher(platformBus, Arrays.asList(handlers));
   }
   
   public static Consumer<PlatformMessage> toDispatcher(PlatformMessageBus platformBus, Collection<PlatformRequestMessageHandler> handlers) {
      return toDispatcher(platformBus, handlers, UnsupportedMessageHandler.Instance);
   }

   public static Consumer<PlatformMessage> toDispatcher(PlatformMessageBus platformBus, Collection<PlatformRequestMessageHandler> handlers, PlatformRequestMessageHandler fallback) {
      Map<String, PlatformRequestMessageHandler> dispatchTable = 
            handlers
               .stream()
               .collect(Collectors.toMap(PlatformRequestMessageHandler::getMessageType, Function.identity()))
               ;
      return new PlatformMessageConsumer(platformBus, dispatchTable, fallback);
   }
   
   public static <T> BiConsumer<PlatformMessage, T> toContextualDispatcher(PlatformMessageBus platformBus, Collection<ContextualRequestMessageHandler<? super T>> handlers) {
      return toContextualDispatcher(platformBus, handlers, UnsupportedMessageHandler.Instance);
   }

   public static <T> BiConsumer<PlatformMessage, T> toContextualDispatcher(PlatformMessageBus platformBus, Collection<ContextualRequestMessageHandler<? super T>> handlers, ContextualRequestMessageHandler<? super T> fallback) {
      Map<String, ContextualRequestMessageHandler<? super T>> dispatchTable = 
            handlers
               .stream()
               .collect(Collectors.toMap(ContextualRequestMessageHandler::getMessageType, Function.identity()))
               ;
      return new PlatformMessageBiConsumer<T>(platformBus, dispatchTable, fallback);
   }

   private static class UnsupportedMessageHandler implements PlatformRequestMessageHandler, ContextualRequestMessageHandler<Object> {
      private static final UnsupportedMessageHandler Instance = new UnsupportedMessageHandler();
      
      @Override
      public String getMessageType() {
         return "*";
      }

      @Override
      public MessageBody handleRequest(Object context, PlatformMessage message) {
         return Errors.unsupportedMessageType(message.getMessageType());
      }

      @Override
      public MessageBody handleMessage(PlatformMessage message) throws Exception {
         return Errors.unsupportedMessageType(message.getMessageType());
      }
      
   }
   
   private static class PlatformMessageConsumer implements Consumer<PlatformMessage> {
      private final PlatformMessageBus platformBus;
      private final PlatformRequestMessageHandler fallback;
      private final Map<String, PlatformRequestMessageHandler> handlers;
      
      public PlatformMessageConsumer(
            PlatformMessageBus platformBus, 
            Map<String, PlatformRequestMessageHandler> handlers,
            PlatformRequestMessageHandler fallback
      ) {
         this.platformBus = platformBus;
         this.handlers = handlers;
         this.fallback = fallback;
      }
      
      @Override
      public void accept(PlatformMessage message) {
         platformBus.invokeAndSendIfNotNull(message, () -> dispatch(message));
      }

      private Optional<MessageBody> dispatch(PlatformMessage message) throws Exception {
         String type = message.getMessageType();
         PlatformRequestMessageHandler handler = handlers.getOrDefault(type, fallback);
         
         MessageBody response = handler.handleMessage(message);
         if(!handler.isAsync() && response == null) {
            return Optional.of( MessageBody.emptyMessage() );
         }
         return Optional.ofNullable(response);
      }

      /* (non-Javadoc)
       * @see java.lang.Object#toString()
       */
      @Override
      public String toString() {
         return "PlatformMessageConsumer [platformBus=" + platformBus
               + ", fallback=" + fallback + ", handles=" + handlers + "]";
      }

   }

   private static class PlatformMessageBiConsumer<T> implements BiConsumer<PlatformMessage, T> {
      private final PlatformMessageBus platformBus;
      private final ContextualRequestMessageHandler<? super T> fallback;
      private final Map<String, ContextualRequestMessageHandler<? super T>> handlers;
      
      public PlatformMessageBiConsumer(
            PlatformMessageBus platformBus, 
            Map<String, ContextualRequestMessageHandler<? super T>> handlers,
            ContextualRequestMessageHandler<? super T> fallback
      ) {
         this.platformBus = platformBus;
         this.handlers = handlers;
         this.fallback = fallback;
      }
      
      @Override
      public void accept(PlatformMessage message, T context) {
         platformBus.invokeAndSendIfNotNull(message, () -> dispatch(message, context));
      }

      private Optional<MessageBody> dispatch(PlatformMessage message, T context) throws Exception {
         String type = message.getMessageType();
         ContextualRequestMessageHandler<? super T> handler = handlers.getOrDefault(type, fallback);
         
         MessageBody response = handler.handleRequest(context, message);
         if(response == null) {
            return Optional.of( MessageBody.emptyMessage() );
         }
         return Optional.ofNullable(response);
      }

      /* (non-Javadoc)
       * @see java.lang.Object#toString()
       */
      @Override
      public String toString() {
         return "PlatformMessageConsumer [platformBus=" + platformBus
               + ", fallback=" + fallback + ", handles=" + handlers.keySet() + "]";
      }

   }
   
   private static class LoadAndDispatchMessageHandler<T> implements PlatformRequestMessageHandler {
      private final Function<PlatformMessage, T> loader;
      private final ContextualRequestMessageHandler<? super T> delegate;
      
      LoadAndDispatchMessageHandler(
            Function<PlatformMessage, T> loader,
            ContextualRequestMessageHandler<? super T> delegate
      ) {
         Preconditions.checkNotNull(loader, "loader may not be null");
         Preconditions.checkNotNull(delegate, "delegate may not be null");
         this.loader = loader;
         this.delegate = delegate;
      }
      
      @Override
      public String getMessageType() {
         return delegate.getMessageType();
      }

      @Override
      public MessageBody handleMessage(PlatformMessage message) throws Exception {
         T context = loader.apply(message);
         if(context == null) {
            throw new NotFoundException(message.getDestination());
         }
         return delegate.handleRequest(context, message);
      }
      
   }

}

