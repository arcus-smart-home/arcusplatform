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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Singleton;
import com.iris.capability.definition.ProtocolDefinition;
import com.iris.device.attributes.LegacyAttributeConverter;
import com.iris.device.model.AttributeDefinition;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.groovy.plugin.ProtocolPlugin;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.driver.metadata.EventMatcher;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.BridgeChildCapability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.model.Device;
import com.iris.messages.service.BridgeService;
import com.iris.messages.services.PlatformConstants;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.ipcd.message.IpcdMessage;

@Singleton
public class IpcdProtocolPlugin extends ProtocolPlugin {
   private final Map<String, AttributeDefinition> attributes;
   private final IpcdContext ipcdContext;

   public IpcdProtocolPlugin() {
      ProtocolDefinition protocolDef = IpcdProtocol.INSTANCE.getDefinition();
      Map<String, AttributeDefinition> attributeDefs = new HashMap<>();
      for (com.iris.capability.definition.AttributeDefinition def : protocolDef.getAttributes()) {
         attributeDefs.put(def.getName(), LegacyAttributeConverter.convertToLegacyAttributeDef(def));
      }
      attributes = Collections.unmodifiableMap(attributeDefs);
      ipcdContext = new IpcdContext();
   }

   @Override
   protected void addRootProperties(EnvironmentBinding binding) {
      binding.setProperty("Ipcd", ipcdContext);
      binding.setProperty("ipcd", ipcdContext);
      binding.setProperty("onIpcdMessage", new OnIpcdClosure(binding));
      binding.setProperty("onIpcdEvent", new OnEventClosure(binding));
      binding.setProperty("onIpcdReport", new OnReportClosure(binding));
      binding.setProperty("onIpcdValueChange", new OnValueChangeClosure(binding));
   }

   @Override
   protected void addContextProperties(EnvironmentBinding binding) {
      // There are no context properites for Ipcd.

   }

   @Override
   public IpcdProtocol getProtocol() {
      return IpcdProtocol.INSTANCE;
   }

   @Override
   public Map<String, AttributeDefinition> getMatcherAttributes() {
      return attributes;
   }

   @Override
   public ContextualEventHandler<ProtocolMessage> createHandler(List<EventMatcher> matchers) {
      IpcdMessageHandler.Builder builder = null;
      for (EventMatcher matcher : matchers) {
         if (!(matcher instanceof IpcdProtocolEventMatcher)) {
            continue;
         }
         if (builder == null) {
            builder = IpcdMessageHandler.builder();
         }
         IpcdProtocolEventMatcher ipcdMatcher = (IpcdProtocolEventMatcher) matcher;
         builder.addHandler(
               ipcdMatcher.getMessageType(),
               ipcdMatcher.getCommandName(),
               ipcdMatcher.getStatusType(),
               ipcdMatcher.getHandler());
      }
      return builder != null ? builder.build() : null;
   }

   @Override
   public void postProcessEnvironment(EnvironmentBinding binding) {
      super.postProcessEnvironment(binding);
      ipcdContext.setDispatchHandler(createDispatchHandler(binding.getBuilder().getEventMatchers()));
   }

   private ContextualEventHandler<IpcdMessage> createDispatchHandler(List<EventMatcher> matchers) {
      IpcdDispatchHandler.Builder builder = null;
      for (EventMatcher matcher : matchers) {
         if (matcher instanceof IpcdDispatchMatcher) {
            if (builder == null) {
               builder = IpcdDispatchHandler.builder();
            }
            IpcdDispatchMatcher dispatchMatcher = (IpcdDispatchMatcher) matcher;
            builder.addHandler(dispatchMatcher.getType(),
                  dispatchMatcher.getEvent(),
                  dispatchMatcher.getParameter(),
                  dispatchMatcher.getHandler());
         }
      }
      return builder != null ? builder.build() : null;
   }

}

