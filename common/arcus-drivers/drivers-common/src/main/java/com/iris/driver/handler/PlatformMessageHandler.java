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

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.handler.AbstractDispatchingHandler;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;

/**
 *
 */
public class PlatformMessageHandler extends
      AbstractDispatchingHandler<PlatformMessage>
      implements ContextualEventHandler<PlatformMessage>
{
   public static PlatformMessageHandler.Builder builder() {
      return new Builder();
   }

   protected PlatformMessageHandler(Map<String, ContextualEventHandler<? super PlatformMessage>> handlers) {
      super(handlers);
   }

   /* (non-Javadoc)
    * @see com.iris.driver.DriverEventHandler#handleEvent(com.iris.core.driver.DeviceDriverContext, java.lang.Object)
    */
   @Override
   public boolean handleEvent(DeviceDriverContext context, PlatformMessage message) throws Exception {
      return deliver(WILDCARD, context, message);
   }

   public static class Builder extends AbstractDispatchingHandler.Builder<PlatformMessage, PlatformMessageHandler> {

      public Builder addWildcardHandler(ContextualEventHandler<? super PlatformMessage> handler) {
         doAddHandler(WILDCARD, handler);
         return this;
      }

      public Builder addHandler(Class<? extends MessageBody> type, ContextualEventHandler<? super PlatformMessage> handler) {
         if(type == null) {
            return addWildcardHandler(handler);
         }
         doAddHandler(type.getName(), handler);
         return this;
      }

      @Override
      protected PlatformMessageHandler create(Map<String, ContextualEventHandler<? super PlatformMessage>> handlers) {
         return new PlatformMessageHandler(handlers);
      }
   }

}

