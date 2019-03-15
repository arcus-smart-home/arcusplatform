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
package com.iris.driver.groovy.mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.iris.capability.definition.ProtocolDefinition;
import com.iris.device.attributes.LegacyAttributeConverter;
import com.iris.device.model.AttributeDefinition;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.groovy.plugin.ProtocolPlugin;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.driver.metadata.EventMatcher;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceCapability.DeviceRemovedEvent;
import com.iris.messages.model.Device;
import com.iris.protocol.Protocol;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.mock.MockProtocol;
import com.iris.validators.Validator;

public class MockProtocolPlugin extends ProtocolPlugin {
   private final Map<String, AttributeDefinition> attributes;
   
   public MockProtocolPlugin() {
      ProtocolDefinition protocolDef = MockProtocol.INSTANCE.getDefinition();
      Map<String, AttributeDefinition> attributeDefs = new HashMap<>();
      for (com.iris.capability.definition.AttributeDefinition def : protocolDef.getAttributes()) {
         attributeDefs.put(def.getName(), LegacyAttributeConverter.convertToLegacyAttributeDef(def));
      }
      attributes = Collections.unmodifiableMap(attributeDefs);
   }

   @Override
   protected void addRootProperties(EnvironmentBinding binding) {
   }

   @Override
   protected void addContextProperties(EnvironmentBinding binding) {
   }

   @Override
   public Protocol<?> getProtocol() {
      return MockProtocol.INSTANCE;
   }

   @Override
   public Map<String, AttributeDefinition> getMatcherAttributes() {
      return attributes;
   }

   @Override
   public ContextualEventHandler<ProtocolMessage> createHandler(List<EventMatcher> matcher) {
      return null;
   }

}

