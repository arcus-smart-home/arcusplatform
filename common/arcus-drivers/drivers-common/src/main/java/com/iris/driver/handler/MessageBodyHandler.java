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
package com.iris.driver.handler;

import java.util.Map;

import com.iris.capability.key.NamespacedKey;
import com.iris.driver.DeviceDriverContext;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;

/**
 *
 */
public class MessageBodyHandler
   extends AbstractDispatchingHandler<MessageBody>
   implements ContextualEventHandler<PlatformMessage>
{

   public static MessageBodyHandler.Builder builder() {
      return new Builder();
   }

   protected MessageBodyHandler(Map<String, ContextualEventHandler<? super MessageBody>> handlers) {
      super(handlers);
   }

   /* (non-Javadoc)
    * @see com.iris.driver.DriverEventHandler#handleEvent(com.iris.core.driver.DeviceDriverContext, java.lang.Object)
    */
   @Override
   public boolean handleEvent(DeviceDriverContext context, PlatformMessage message) throws Exception {
      MessageBody body = message.getValue();

      NamespacedKey name = NamespacedKey.parse(body.getMessageType());
      
      // try an instance match
      if(name.isInstanced() && deliver(name.getRepresentation(), context, body)) {
         return true;
      }

      // try command match
      if(name.isNamed() && deliver(name.getNamedRepresentation(), context, body)) {
         return true;
      }
      
      // try namespace match
      if(deliver(name.getNamespace(), context, body)) {
         return true;
      }

      // try wildcard match
      if(deliver(WILDCARD, context, body)) {
         return true;
      }

      return false;
   }

   public static class Builder extends AbstractDispatchingHandler.Builder<MessageBody, MessageBodyHandler> {

      public Builder addWildcardHandler(ContextualEventHandler<? super MessageBody> handler) {
         doAddHandler(WILDCARD, handler);
         return this;
      }

      public Builder addHandler(NamespacedKey name, ContextualEventHandler<? super MessageBody> handler) {
         doAddHandler(name.getRepresentation(), handler);
         return this;
      }

      @Override
      protected MessageBodyHandler create(Map<String, ContextualEventHandler<? super MessageBody>> handlers) {
         return new MessageBodyHandler(handlers);
      }

   }
}

