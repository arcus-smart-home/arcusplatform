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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Singleton;
import com.iris.capability.definition.ProtocolDefinition;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.device.attributes.LegacyAttributeConverter;
import com.iris.device.model.AttributeDefinition;
import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.groovy.plugin.ProtocolPlugin;
import com.iris.driver.metadata.EventMatcher;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.model.Device;
import com.iris.messages.services.PlatformConstants;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;

@Singleton
public class ZigbeeProtocolPlugin extends ProtocolPlugin {
   private final Map<String, AttributeDefinition> attributes;

   public ZigbeeProtocolPlugin() {
      ProtocolDefinition protocolDef = ZigbeeProtocol.INSTANCE.getDefinition();
      Map<String, AttributeDefinition> attributeDefs = new HashMap<>();
      for (com.iris.capability.definition.AttributeDefinition def : protocolDef.getAttributes()) {
         attributeDefs.put(def.getName(), LegacyAttributeConverter.convertToLegacyAttributeDef(def));
      }
      attributes = Collections.unmodifiableMap(attributeDefs);
   }

   @Override
   protected void addRootProperties(EnvironmentBinding binding) {
      ZigbeeContext zbCtx = new ZigbeeContext();
      binding.setProperty("Zigbee", zbCtx);
      binding.setProperty("zigbee", zbCtx);
      binding.setProperty("onZigbeeMessage", new OnZigbeeClosure(binding));
   }

   @Override
   public void postProcessEnvironment(EnvironmentBinding binding) {
      if (binding instanceof DriverBinding) {
         ZigbeeContext zbctx = (ZigbeeContext)binding.getProperty("Zigbee");

         DriverBinding drvBinding = (DriverBinding)binding;
         zbctx.processReflexes(drvBinding);
         zbctx.processConfiguration(drvBinding);
      } 

      super.postProcessEnvironment(binding);
   }

   @Override
   public ZigbeeProtocol getProtocol() {
      return ZigbeeProtocol.INSTANCE;
   }

   @Override
   public Map<String, AttributeDefinition> getMatcherAttributes() {
      return attributes;
   }

   @Override
   public ContextualEventHandler<ProtocolMessage> createHandler(List<EventMatcher> matchers) {
      ZigbeeMessageHandler.Builder builder = null;
      for (EventMatcher matcher: matchers) {
         if (!(matcher instanceof ZigbeeProtocolEventMatcher)) {
            continue;
         }
         if (builder == null) {
            builder = ZigbeeMessageHandler.builder();
         }
         ZigbeeProtocolEventMatcher zigbeeMatcher = (ZigbeeProtocolEventMatcher) matcher;
         if (zigbeeMatcher.matchesAnyMessageType()) {
            builder.addWildcardHandler(zigbeeMatcher.getHandler());
         }
         else {
            if (zigbeeMatcher.getMessageType() == ZigbeeMessage.Zcl.ID) {
               builder.addHandler(
                     ZigbeeMessageHandler.HandlerKey.makeZclKey(zigbeeMatcher.getClusterOrMessageId(),
                           zigbeeMatcher.getZclMessageId(),
                           zigbeeMatcher.getGroup()),
                     zigbeeMatcher.getHandler());
            }
            else if (zigbeeMatcher.getMessageType() == ZigbeeMessage.Zdp.ID) {
               builder.addHandler(
                     ZigbeeMessageHandler.HandlerKey.makeZdpKey(zigbeeMatcher.getClusterOrMessageId()),
                     zigbeeMatcher.getHandler());
            }
         }
      }
      if (builder == null) {
         return null;
      }
      return builder.build();
   }

   @Override
   protected void addContextProperties(EnvironmentBinding binding) {
      // The Zigbee object is a root-level property.
   }

}

