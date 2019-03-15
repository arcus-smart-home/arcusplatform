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
package com.iris.driver.groovy.ipcd;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.handler.AbstractDispatchingHandler;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.ipcd.message.IpcdMessage;
import com.iris.protocol.ipcd.message.model.IpcdResponse;
import com.iris.protocol.ipcd.message.model.MessageType;
import com.iris.protocol.ipcd.message.model.StatusType;

public class IpcdMessageHandler 
   extends AbstractDispatchingHandler<IpcdMessage>
   implements ContextualEventHandler<ProtocolMessage> 
{
   public static IpcdMessageHandler.Builder builder() {
      return new Builder();
   }

   protected IpcdMessageHandler(Map<String, ContextualEventHandler<? super IpcdMessage>> handlers) {
      super(handlers);
   }

   @Override
   public boolean handleEvent(DeviceDriverContext context, ProtocolMessage event) throws Exception {
      
      IpcdMessage message = event.getValue(IpcdProtocol.INSTANCE);
      MessageType messageType = message.getMessageType();
      if (messageType == MessageType.response) {
         IpcdResponse response = (IpcdResponse) message;
         StatusType statusType = response.getStatus().getResult();
         // Check Catch All Buckets First
         if (deliver(makeCatchAllKey(statusType), context, message)) {
            return true;
         }
         String commandName = response.getRequest().getCommand();
               
         if (deliver(makeIpcdKey(messageType, commandName, statusType), context, message)) {
            return true;
         }
         // Broaden
         if (deliver(makeIpcdKey(messageType, commandName, null), context, message)) {
            return true;
         }
      }
      
      if (deliver(makeIpcdKey(messageType), context, message)) {
         return true;
      }
      
      if (deliver(WILDCARD, context, message)) {
         return true;
      }
      
      return false;
   }
   
   public static class Builder extends AbstractDispatchingHandler.Builder<IpcdMessage, IpcdMessageHandler> {
      private Builder() {}
      
      public Builder addCatchAllHandler(StatusType statusType, ContextualEventHandler<? super IpcdMessage> handler) {
         Preconditions.checkArgument(statusType != null, "A catch all handler requires a status type.");
         doAddHandler(makeCatchAllKey(statusType), handler);
         return this;
      }
      
      public Builder addHandler(
            MessageType messageType, 
            String commandName, 
            StatusType statusType, 
            ContextualEventHandler<? super IpcdMessage> handler) {
         doAddHandler(makeIpcdKey(messageType, commandName, statusType), handler);
         return this;
      }

      @Override
      protected IpcdMessageHandler create(Map<String, ContextualEventHandler<? super IpcdMessage>> handlers) {
         return new IpcdMessageHandler(handlers);
      }
      
   }
   
   static String makeCatchAllKey(StatusType statusType) {
      return makeIpcdKey(MessageType.response.name(), "catch_all_" + statusType.name(), null);
   }
   
   static String makeIpcdKey(MessageType messageType) {
      return makeIpcdKey(messageType, null, null);
   }
   
   static String makeIpcdKey(MessageType messageType, String commandName, StatusType statusType) {
      return makeIpcdKey((messageType != null ? messageType.name() : null), commandName, (statusType != null ? statusType.name() : null));
   }
   
   static String makeIpcdKey(String messageType, String commandName, String statusType) {
      if (messageType == null && statusType == null && commandName == null) {
         return WILDCARD;
      }
      StringBuffer sb = new StringBuffer();
      sb.append(messageType != null ? messageType.toLowerCase() : "ANY");
      sb.append(":");
      sb.append(commandName != null ? commandName.toLowerCase() : "ANY");
      sb.append(":");
      sb.append(statusType != null ? statusType.toLowerCase() : "ANY");
      return sb.toString();
   }
}

