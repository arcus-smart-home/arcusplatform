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
package com.iris.driver.capability;

import java.util.List;

import com.iris.device.attributes.AttributeMap;
import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.driver.handler.GetAttributesProvider;
import com.iris.driver.handler.SetAttributesConsumer;
import com.iris.messages.PlatformMessage;
import com.iris.messages.model.CapabilityId;
import com.iris.model.Version;
import com.iris.protocol.ProtocolMessage;

/**
 *
 */
public class Capability {
   private CapabilityId capabilityId;
   private CapabilityDefinition capabilityDefinition;
   private String hash;
   private AttributeMap attributes;
   private ContextualEventHandler<DriverEvent> driverEventHandler;
   private ContextualEventHandler<PlatformMessage> platformMessageHandler;
   private ContextualEventHandler<ProtocolMessage> protocolMessageHandler;
   private List<GetAttributesProvider> getAttributesProviders;
   private List<SetAttributesConsumer> setAttributesConsumers;

   public Capability(
      CapabilityId capabilityId,
      CapabilityDefinition capabilityDefinition,
      String hash,
      AttributeMap attributes,
      ContextualEventHandler<DriverEvent> driverEventHandler,
      ContextualEventHandler<PlatformMessage> platformMessageHandler,
      ContextualEventHandler<ProtocolMessage> protocolMessageHandler,
      List<GetAttributesProvider> getAttributesProviders,
      List<SetAttributesConsumer> setAttributesConsumers
   ) {
      this.capabilityId = capabilityId;
      this.capabilityDefinition = capabilityDefinition;
      this.hash = hash;
      this.attributes = AttributeMap.unmodifiableCopy(attributes);
      this.driverEventHandler = driverEventHandler;
      this.platformMessageHandler = platformMessageHandler;
      this.protocolMessageHandler = protocolMessageHandler;
      this.getAttributesProviders = getAttributesProviders;
      this.setAttributesConsumers = setAttributesConsumers;
   }

   public String getNamespace() {
      return capabilityDefinition.getNamespace();
   }

   public String getCapabilityName() {
      return capabilityId.getCapabilityName();
   }

   public String getName() {
      return capabilityId.getImplementationName();
   }

   public Version getVersion() {
      return capabilityId.getVersion();
   }

   public CapabilityId getCapabilityId() {
      return capabilityId;
   }

   public String getHash() {
      return hash;
   }
   
   public CapabilityDefinition getCapabilityDefinition() {
      return capabilityDefinition;
   }

   public AttributeMap getAttributes() {
      return attributes;
   }

   public ContextualEventHandler<DriverEvent> getDriverEventHandler() {
      return driverEventHandler;
   }

   public ContextualEventHandler<PlatformMessage> getPlatformMessageHandler() {
      return platformMessageHandler;
   }

   public ContextualEventHandler<ProtocolMessage> getProtocolMessageHandler() {
      return protocolMessageHandler;
   }

   public List<GetAttributesProvider> getAttributeProviders() {
      return getAttributesProviders;
   }

   public List<SetAttributesConsumer> getAttributeConsumers() {
      return setAttributesConsumers;
   }

   @Override
   public String toString() {
      return "Capability [" + capabilityId.getRepresentation() + "]";
   }

}

