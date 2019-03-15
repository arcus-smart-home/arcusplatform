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
package com.iris.platform.rule.environment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.common.rule.event.AttributeValueChangedEvent;
import com.iris.common.rule.event.ModelAddedEvent;
import com.iris.common.rule.event.ModelRemovedEvent;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.RecordingCapability;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules(CapabilityRegistryModule.class)
public class TestModelStore extends IrisTestCase {
	@Inject DefinitionRegistry registry;
	
   String id = UUID.randomUUID().toString();
   Address address = Address.platformService(UUID.fromString(id), DeviceCapability.NAMESPACE);
   Map<String, Object> attributes =
         ImmutableMap.of(
               Capability.ATTR_ID, id, 
               Capability.ATTR_TYPE, DeviceCapability.NAMESPACE,
               Capability.ATTR_CAPS, ImmutableSet.of(Capability.NAMESPACE, DeviceCapability.NAMESPACE, "test"),
               Capability.ATTR_ADDRESS, address.getRepresentation()
         );
   
   @Test
   public void testEmptyModelStore() {
      Address address = Address.platformService(id, DeviceCapability.NAMESPACE);
      
      RuleModelStore store = new RuleModelStore();
      store.setRegistry(registry);
      assertFalse(store.getModels().iterator().hasNext());
      assertEquals(null, store.getModelByAddress(address));
      assertEquals(null, store.getAttributeValue(address, "test:Attribute"));
   }


   @Test
   public void testAddObjectMessage() {
      MessageBody body =
            MessageBody
               .buildMessage(
                     Capability.EVENT_ADDED, 
                     attributes
               );
      PlatformMessage message = PlatformMessage.createBroadcast(body, address);
      
      RuleModelStore store = new RuleModelStore();
      store.update(message);
      
      assertEquals(1, store.getModels().size());
      assertNotNull(store.getModelByAddress(address));
      assertEquals(id, store.getAttributeValue(address, Capability.ATTR_ID));
      assertEquals(DeviceCapability.NAMESPACE, store.getAttributeValue(address, Capability.ATTR_TYPE));
      assertEquals(null, store.getAttributeValue(address, "test:Attribute"));
   }
   
   @Test
   public void testAddObjectMessageFiltered() {
      Map<String, Object> attributes = new HashMap<String, Object>(this.attributes);
      attributes.put(Capability.ATTR_TYPE, RecordingCapability.NAMESPACE);
      MessageBody body =
            MessageBody
               .buildMessage(
                     Capability.EVENT_ADDED, 
                     attributes
               );
      PlatformMessage message = PlatformMessage.createBroadcast(body, address);
      
      RuleModelStore store = new RuleModelStore();
      store.update(message);
      
      assertEquals(0, store.getModels().size());
      assertEquals(null, store.getModelByAddress(address));
      assertEquals(null, store.getAttributeValue(address, "test:Attribute"));
   }
   
   @Test
   public void testAddObjectListener() {
      Capture<RuleEvent> eventRef = EasyMock.newCapture();
      Consumer<RuleEvent> listener = EasyMock.createMock(Consumer.class);
      listener.accept(EasyMock.capture(eventRef));
      EasyMock.expectLastCall();
      EasyMock.replay(listener);
      
      MessageBody body =
            MessageBody
               .buildMessage(
                     Capability.EVENT_ADDED, 
                     attributes
               );
      PlatformMessage message = PlatformMessage.createBroadcast(body, address);
      
      RuleModelStore store = new RuleModelStore();
      store.addListener(listener);
      store.update(message);
      
      ModelAddedEvent event = (ModelAddedEvent) eventRef.getValue();
      assertEquals(address, event.getAddress());
      assertEquals(RuleEventType.MODEL_ADDED, event.getType());
      
      EasyMock.verify(listener);
   }
   
   @Test
   public void testUpdateObjectMessage() {
      MessageBody body =
            MessageBody
               .buildMessage(
                     Capability.EVENT_VALUE_CHANGE, 
                     ImmutableMap.of("test:Attribute", "newValue")
               );
      PlatformMessage message = PlatformMessage.createBroadcast(body, address);
      
      RuleModelStore store = new RuleModelStore();
      store.addModel(ImmutableList.of(attributes));
      store.update(message);
      
      assertEquals(1, store.getModels().size());
      assertNotNull(store.getModelByAddress(address));
      assertEquals(id, store.getAttributeValue(address, Capability.ATTR_ID));
      assertEquals(DeviceCapability.NAMESPACE, store.getAttributeValue(address, Capability.ATTR_TYPE));
      assertEquals("newValue", store.getAttributeValue(address, "test:Attribute"));
   }
   
   @Test
   public void testUpdateObjectListener() {
      Capture<RuleEvent> eventRef = EasyMock.newCapture();
      Consumer<RuleEvent> listener = EasyMock.createMock(Consumer.class);
      listener.accept(EasyMock.capture(eventRef));
      EasyMock.expectLastCall();
      EasyMock.replay(listener);

      MessageBody body =
            MessageBody
               .buildMessage(
                     Capability.EVENT_VALUE_CHANGE, 
                     ImmutableMap.of("test:Attribute", "newValue")
               );
      PlatformMessage message = PlatformMessage.createBroadcast(body, address);
      
      RuleModelStore store = new RuleModelStore();
      store.setRegistry(registry);
      store.addModel(ImmutableList.of(attributes));
      store.addListener(listener);
      store.update(message);
      
      AttributeValueChangedEvent event = (AttributeValueChangedEvent) eventRef.getValue();
      assertEquals(RuleEventType.ATTRIBUTE_VALUE_CHANGED, event.getType());
      assertEquals(address, event.getAddress());
      assertEquals("test:Attribute", event.getAttributeName());
      assertEquals("newValue", event.getAttributeValue());
      assertEquals(null, event.getOldValue());
      
      EasyMock.verify(listener);
   }
   
   @Test
   public void testRemoveObjectMessage() {
      MessageBody body =
            MessageBody
               .buildMessage(Capability.EVENT_DELETED, ImmutableMap.of());
      PlatformMessage message = PlatformMessage.createBroadcast(body, address);
      
      RuleModelStore store = new RuleModelStore();
      store.addModel(ImmutableList.of(attributes));
      store.update(message);
      
      assertEquals(0, store.getModels().size());
      assertNull(store.getModelByAddress(address));
      assertNull(store.getAttributeValue(address, Capability.ATTR_ID));
   }
   
   @Test
   public void testRemoveObjectListener() {
      Capture<RuleEvent> eventRef = EasyMock.newCapture();
      Consumer<RuleEvent> listener = EasyMock.createMock(Consumer.class);
      listener.accept(EasyMock.capture(eventRef));
      EasyMock.expectLastCall();
      EasyMock.replay(listener);

      MessageBody body =
            MessageBody
               .buildMessage(Capability.EVENT_DELETED, ImmutableMap.of());
      PlatformMessage message = PlatformMessage.createBroadcast(body, address);
      
      RuleModelStore store = new RuleModelStore();
      store.addModel(ImmutableList.of(attributes));
      store.addListener(listener);
      store.update(message);
      
      ModelRemovedEvent event = (ModelRemovedEvent) eventRef.getValue();
      assertEquals(RuleEventType.MODEL_REMOVED, event.getType());
      assertEquals(address, event.getModel().getAddress());
      assertEquals(id, event.getModel().getAttribute(Capability.ATTR_ID));
      assertEquals(DeviceCapability.NAMESPACE, event.getModel().getAttribute(Capability.ATTR_TYPE));
      
      EasyMock.verify(listener);
   }
   
}

