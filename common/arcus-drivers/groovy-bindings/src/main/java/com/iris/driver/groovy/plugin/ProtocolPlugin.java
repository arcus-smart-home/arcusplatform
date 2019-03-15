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
package com.iris.driver.groovy.plugin;

import java.util.List;
import java.util.Map;

import com.iris.device.model.AttributeDefinition;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DriverConstants;
import com.iris.driver.capability.Capability;
import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.binding.CapabilityEnvironmentBinding;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.driver.metadata.EventMatcher;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.model.Device;
import com.iris.protocol.Protocol;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.RemoveProtocolRequest;

/**
 * Base class for plugins that expose Protocol specific functionality.
 */
public abstract class ProtocolPlugin implements GroovyDriverPlugin {

   @Override
   public void enhanceEnvironment(EnvironmentBinding binding) {
      if(binding instanceof DriverBinding) {
         ((DriverBinding) binding).getBuilder().addMatchAttributes(getMatcherAttributes());
      }
      addRootProperties(binding);
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   @Override
   public void postProcessEnvironment(EnvironmentBinding binding) {
      final ContextualEventHandler<ProtocolMessage> handler = createHandler(binding.getBuilder().getEventMatchers());
      if(handler != null) {
         binding.getBuilder().addProtocolHandler(getProtocol().getNamespace(), (ContextualEventHandler) handler);
      }
   }

   @Override
   public void enhanceDriver(DriverBinding binding, DeviceDriver driver) {
      String protocolName = driver.getBaseAttributes().get(DriverConstants.DEVADV_ATTR_PROTOCOL);
      if(protocolName != null && protocolName.equals(getProtocol().getNamespace())) {
         addContextProperties(binding);
      }
   }

   @Override
   public void enhanceCapability(CapabilityEnvironmentBinding binding, Capability capability) {
      addContextProperties(binding);
   }
   
   // FIXME this should be on the driver where we have full context
   public PlatformMessage handleRemove(Device device, long duration, boolean force) {
      RemoveProtocolRequest request = new RemoveProtocolRequest(device);
      request.setTimeoutMs(duration);
      request.setForceRemove(force);
      return getProtocol().remove(request);
   }

   protected abstract void addRootProperties(EnvironmentBinding binding);

   protected abstract void addContextProperties(EnvironmentBinding binding);

   public abstract Protocol<?> getProtocol();

   /**
    * The set of {@link AttributeDefinition}s that will
    * be provided by this protocol on startup.
    * @return
    */
   public abstract Map<String, AttributeDefinition> getMatcherAttributes();

   public abstract ContextualEventHandler<ProtocolMessage> createHandler(List<EventMatcher> matcher);

}

