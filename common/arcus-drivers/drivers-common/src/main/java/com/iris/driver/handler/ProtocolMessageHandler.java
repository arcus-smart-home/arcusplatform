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
import com.iris.protocol.ProtocolMessage;

/**
 *
 */
public class ProtocolMessageHandler
   extends AbstractDispatchingHandler<ProtocolMessage>
   implements ContextualEventHandler<ProtocolMessage>
{

   public static ProtocolMessageHandler.Builder builder() {
      return new Builder();
   }

   protected ProtocolMessageHandler(Map<String, ContextualEventHandler<? super ProtocolMessage>> handlers) {
      super(handlers);
   }

   /* (non-Javadoc)
    * @see com.iris.driver.DriverEventHandler#handleEvent(com.iris.core.driver.DeviceDriverContext, java.lang.Object)
    */
   @Override
   public boolean handleEvent(DeviceDriverContext context, ProtocolMessage message) throws Exception {

      // try protocol match
      String protocol = message.getMessageType();
      if(deliver(protocol, context, message)) {
         return true;
      }

      // try wildcard match
      if(deliver(WILDCARD, context, message)) {
         return true;
      }

      return false;
   }

   public static class Builder extends AbstractDispatchingHandler.Builder<ProtocolMessage, ProtocolMessageHandler> {

      public Builder addWildcardHandler(ContextualEventHandler<? super ProtocolMessage> handler) {
         return addHandler(WILDCARD, handler);
      }

      public Builder addHandler(String protocolName, ContextualEventHandler<? super ProtocolMessage> handler) {
         doAddHandler(protocolName, handler);
         return this;
      }

      @Override
      protected ProtocolMessageHandler create(Map<String, ContextualEventHandler<? super ProtocolMessage>> handlers) {
         return new ProtocolMessageHandler(handlers);
      }
   }

}

