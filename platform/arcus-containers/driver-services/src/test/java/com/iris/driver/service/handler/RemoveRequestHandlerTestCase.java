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
package com.iris.driver.service.handler;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.device.model.AttributeDefinition;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.groovy.plugin.GroovyDriverPlugin;
import com.iris.driver.groovy.plugin.ProtocolPlugin;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.driver.metadata.EventMatcher;
import com.iris.driver.service.DeviceService;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.DeviceAdvancedCapability.RemovedDeviceEvent;
import com.iris.messages.model.Device;
import com.iris.protocol.Protocol;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.mock.MockProtocol;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({
   DeviceDAO.class,
   DeviceService.class
})
@Modules({ InMemoryMessageModule.class})
public class RemoveRequestHandlerTestCase extends IrisMockTestCase {

   @Provides
   @Singleton
   public Set<GroovyDriverPlugin> plugins() {
      ProtocolPlugin plugin = new ProtocolPlugin() {
         
         @Override
         public Protocol<?> getProtocol() {
            return MockProtocol.INSTANCE;
         }
         
         @Override
         public Map<String, AttributeDefinition> getMatcherAttributes() {
            return ImmutableMap.of();
         }
         
         @Override
         public ContextualEventHandler<ProtocolMessage> createHandler(List<EventMatcher> matcher) {
            throw new UnsupportedOperationException();
         }
         
         @Override
         protected void addRootProperties(EnvironmentBinding binding) {
            throw new UnsupportedOperationException();
         }
         
         @Override
         protected void addContextProperties(EnvironmentBinding binding) {
            throw new UnsupportedOperationException();
         }
      };
      return ImmutableSet.of(plugin);
   }
   
   protected void assertDeviceRemovedMessage(Device device, PlatformMessage message) {
      assertEquals(com.iris.messages.service.DeviceService.ADDRESS, message.getDestination());
      assertFalse(message.isRequest());
      assertFalse(message.isError());
      assertEquals(device.getPlace().toString(), message.getPlaceId());
      
      MessageBody payload = message.getValue();
      assertEquals(RemovedDeviceEvent.NAME, message.getMessageType());
      assertEquals(device.getAccount().toString(), RemovedDeviceEvent.getAccountId(payload));
      assertEquals(device.getProtocol(), RemovedDeviceEvent.getProtocol(payload));
      assertEquals(device.getProtocolid(), RemovedDeviceEvent.getProtocolId(payload));
   }


}

