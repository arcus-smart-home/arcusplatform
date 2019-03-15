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
package com.iris.driver.groovy.zigbee;

import java.util.Map;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.handler.AbstractDispatchingHandler;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;
import com.iris.protocol.zigbee.msg.ZigbeeMessage.Protocol;

public class ZigbeeMessageHandler
   extends AbstractDispatchingHandler<ZigbeeMessage.Protocol>
   implements ContextualEventHandler<ProtocolMessage>
{
   public static ZigbeeMessageHandler.Builder builder() {
      return new Builder();
   }

   protected ZigbeeMessageHandler(Map<String, ContextualEventHandler<? super ZigbeeMessage.Protocol>> handlers) {
      super(handlers);
   }

   @Override
   public boolean handleEvent(DeviceDriverContext context, ProtocolMessage event) throws Exception {

      ZigbeeMessage.Protocol message = event.getValue(ZigbeeProtocol.INSTANCE);
      if (ZigbeeProtocol.isZcl(message)) {
         ZigbeeMessage.Zcl zclMessage = ZigbeeProtocol.getZclMessage(message);
         short clusterId = zclMessage.rawClusterId();
         byte zclMessageId = zclMessage.rawZclMessageId();
         boolean clusterSpecific = (zclMessage.getFlags() & ZigbeeMessage.Zcl.CLUSTER_SPECIFIC) != 0;
         boolean fromServer = (zclMessage.getFlags() & ZigbeeMessage.Zcl.FROM_SERVER) != 0;

         Byte group = clusterSpecific
               ? (fromServer ? ZigbeeContext.GROUP_SERVER : ZigbeeContext.GROUP_CLIENT)
               : ZigbeeContext.GROUP_GENERAL;

         if (deliver(HandlerKey.makeZclKey(clusterId, zclMessageId, group), context, message)) {
            return true;
         }

         if (clusterSpecific) {
            if (deliver(HandlerKey.makeZclKey(clusterId, zclMessageId, ZigbeeContext.GROUP_GENERAL), context, message)) {
               return true;
            }
         }

         if (deliver(HandlerKey.makeZclKey(clusterId, null, null), context, message)) {
            return true;
         }

         if (deliver(HandlerKey.makeZclKey(null, null, null), context, message)) {
            return true;
         }
      }
      else if (ZigbeeProtocol.isZdp(message)) {
         ZigbeeMessage.Zdp zdpMessage = ZigbeeProtocol.getZdpMessage(message);
         short zdpMessageId = zdpMessage.rawZdpMessageId();

         if (deliver(HandlerKey.makeZdpKey(zdpMessageId), context, message)) {
            return true;
         }

         if (deliver(HandlerKey.makeZdpKey(null), context, message)) {
            return true;
         }
      }

      if (deliver(WILDCARD, context, message)) {
         return true;
      }

      return false;
   }

   public static class Builder extends AbstractDispatchingHandler.Builder<ZigbeeMessage.Protocol, ZigbeeMessageHandler> {
      private Builder() {}

      public Builder addWildcardHandler(ContextualEventHandler<? super ZigbeeMessage.Protocol> handler) {
         doAddHandler(WILDCARD, handler);
         return this;
      }

      public Builder addHandler(String key, ContextualEventHandler<? super ZigbeeMessage.Protocol> handler) {
         doAddHandler(key, handler);
         return this;
      }

      @Override
      protected ZigbeeMessageHandler create(Map<String, ContextualEventHandler<? super Protocol>> handlers) {
         return new ZigbeeMessageHandler(handlers);
      }

   }

   public static class HandlerKey {
      private final Byte messageType;
      private final Short clusterId;
      private final Byte zclMessageId;
      private final Short zdpMessageId;
      private final Byte group;

      static String makeZclKey(Short clusterId, Byte messageId, Byte group) {
         HandlerKey key = new HandlerKey((byte)ZigbeeMessage.Zcl.ID, clusterId, messageId, null, group);
         return String.valueOf(key.hashCode());
      }

      static String makeZdpKey(Short messageId) {
         HandlerKey key = new HandlerKey((byte)ZigbeeMessage.Zdp.ID, null, null, messageId, null);
         return String.valueOf(key.hashCode());
      }

      private HandlerKey(Byte messageType, Short clusterId, Byte zclMessageId,
            Short zdpMessageId, Byte group) {
         this.messageType = messageType;
         this.clusterId = clusterId;
         this.zclMessageId = zclMessageId;
         this.zdpMessageId = zdpMessageId;
         this.group = group;
      }

      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result
               + ((clusterId == null) ? 0 : clusterId.hashCode());
         result = prime * result + ((group == null) ? 0 : group.hashCode());
         result = prime * result
               + ((messageType == null) ? 0 : messageType.hashCode());
         result = prime * result
               + ((zclMessageId == null) ? 0 : zclMessageId.hashCode());
         result = prime * result
               + ((zdpMessageId == null) ? 0 : zdpMessageId.hashCode());
         return result;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         HandlerKey other = (HandlerKey) obj;
         if (clusterId == null) {
            if (other.clusterId != null)
               return false;
         } else if (!clusterId.equals(other.clusterId))
            return false;
         if (group == null) {
            if (other.group != null)
               return false;
         } else if (!group.equals(other.group))
            return false;
         if (messageType == null) {
            if (other.messageType != null)
               return false;
         } else if (!messageType.equals(other.messageType))
            return false;
         if (zclMessageId == null) {
            if (other.zclMessageId != null)
               return false;
         } else if (!zclMessageId.equals(other.zclMessageId))
            return false;
         if (zdpMessageId == null) {
            if (other.zdpMessageId != null)
               return false;
         } else if (!zdpMessageId.equals(other.zdpMessageId))
            return false;
         return true;
      }
   }
}

