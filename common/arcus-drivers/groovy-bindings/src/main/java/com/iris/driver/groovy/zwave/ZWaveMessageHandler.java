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
package com.iris.driver.groovy.zwave;

import java.util.Map;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.handler.AbstractDispatchingHandler;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.message.ZWaveMessage;
import com.iris.protocol.zwave.message.ZWaveNodeInfoMessage;
import com.iris.protocol.zwave.model.ZWaveCommand;

public class ZWaveMessageHandler
   extends AbstractDispatchingHandler<ZWaveMessage>
   implements ContextualEventHandler<ProtocolMessage>
{

   public static ZWaveMessageHandler.Builder builder() {
      return new Builder();
   }

   protected ZWaveMessageHandler(Map<String, ContextualEventHandler<? super ZWaveMessage>> handlers) {
      super(handlers);
   }

   @Override
   public boolean handleEvent(DeviceDriverContext context, ProtocolMessage event) throws Exception {
      // TODO it would be nice to have a different key type from string here

      ZWaveMessage message = event.getValue(ZWaveProtocol.INSTANCE);
      if (message instanceof ZWaveNodeInfoMessage) {
         String key = encode(message.getMessageType());
         if (deliver(key, context, message)) {
            return true;
         }
      }
      else if (message instanceof ZWaveCommandMessage) {
         ZWaveCommand command = ((ZWaveCommandMessage) message).getCommand();

         String key = encode(message.getMessageType(), command.commandClass, command.commandNumber);
         if(deliver(key, context, message)) {
            return true;
         }

         key = encode(message.getMessageType(), command.commandClass);
         if(deliver(key, context, message)) {
            return true;
         }
         
         // Technically speaking, there isn't a way to define a handler for this, but maybe there
         // will be in the future so this is here just in case.
         key = encode(message.getMessageType());
         if (deliver(key, context, message)) {
            return true;
         }

      }

      if(deliver(WILDCARD, context, message)) {
         return true;
      }

      return false;
   }

   private static String encode(String messageType, byte commandClass, byte commandNumber) {
      int value = ((commandClass & 0xff) << 8) | (commandNumber & 0xff);
      // TODO consider caching this?
      return messageType + String.valueOf(value);
   }

   private static String encode(String messageType, byte commandClass) {
      int value = (commandClass & 0xff) << 16;
      // TODO consider caching this?
      return messageType + String.valueOf(value);
   }
   
   private static String encode(String messageType) {
      return messageType;
   }

   public static class Builder extends AbstractDispatchingHandler.Builder<ZWaveMessage, ZWaveMessageHandler> {
      private Builder() {
      }

      public Builder addWildcardHandler(ContextualEventHandler<? super ZWaveMessage> handler) {
         doAddHandler(WILDCARD, handler);
         return this;
      }
      
      public Builder addHandler(String messageType, ContextualEventHandler<? super ZWaveMessage> handler) {
         doAddHandler(encode(messageType), handler);
         return this;
      }

      public Builder addHandler(String messageType, byte commandClass, ContextualEventHandler<? super ZWaveMessage> handler) {
         doAddHandler(encode(messageType, commandClass), handler);
         return this;
      }

      public Builder addHandler(String messageType, byte commandClass, byte commandNumber, ContextualEventHandler<? super ZWaveMessage> handler) {
         doAddHandler(encode(messageType, commandClass, commandNumber), handler);
         return this;
      }

      @Override
      protected ZWaveMessageHandler create(Map<String, ContextualEventHandler<? super ZWaveMessage>> handlers) {
         return new ZWaveMessageHandler(handlers);
      }

   }
}

