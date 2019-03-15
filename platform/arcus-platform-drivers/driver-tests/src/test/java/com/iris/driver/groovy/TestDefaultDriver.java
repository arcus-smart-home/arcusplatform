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
package com.iris.driver.groovy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.messaging.memory.InMemoryProtocolMessageBus;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.event.DriverEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.ClientAddress;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;

/**
 * Tests the default settings for a driver.
 * 
 * NOTE commit is disabled in the context so that DeviceDAO does not generally need to be mocked
 */
public class TestDefaultDriver extends GroovyDriverTestCase {
   @Inject InMemoryProtocolMessageBus protocolBus;
   @Inject InMemoryPlatformMessageBus platformBus;

   Address driverAddress = Fixtures.createDeviceAddress();
   Address protocolAddress = Fixtures.createProtocolAddress();

   ClientAddress clientAddress;
   Device device;
   DeviceDriver driver;
   DeviceDriverContext context;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();

      clientAddress = Fixtures.createClientAddress();
      driver = factory.load("DefaultDriver.driver");
      device = createDevice(driver);
      device.setAddress(driverAddress.getRepresentation());
      for(CapabilityDefinition def: driver.getDefinition().getCapabilities()) {
         device.getCaps().add(def.getNamespace());
      }
      device.setDrivername(driver.getDefinition().getName());
      device.setDriverversion(driver.getDefinition().getVersion());
      context = new PlatformDeviceDriverContext(device, driver, mockPopulationCacheMgr) {

         /* (non-Javadoc)
          * @see com.iris.driver.PlatformDeviceDriverContext#commit()
          */
         @Override
         public void commit() {
            // no-op
         }
         
      };
   }

   protected void sendToDevice(MessageBody request) {
      sendToDevice(request, null);
   }

   protected void sendToDevice(MessageBody request, String correlationId) {
      PlatformMessage message =
            PlatformMessage
               .builder()
               .from(clientAddress)
               .to(driverAddress)
               .withPayload(request)
               .withCorrelationId(correlationId)
               .create()
               ;

      driver.handlePlatformMessage(message, context);
   }

   protected void sendToDevice(DriverEvent event) throws Exception {
      driver.handleDriverEvent(event, context);
   }

   @Ignore
   @Test
   public void testHash() throws Exception {
      driver.onRestored(context);
      
      String hash = "smgcQfSWq6M3Wz1NgFw00vdxNPQ";
      assertEquals(hash, driver.getDefinition().getHash());
      assertEquals(hash, context.getAttributeValue(DeviceAdvancedCapability.KEY_DRIVERHASH));
   }
   
   @Test
   public void testGetAllAttributes() throws Exception {
      MessageBody command = Capability.GetAttributesRequest.builder().build();
      sendToDevice(command);

      PlatformMessage response = platformBus.take();

      MessageBody event = response.getValue();
      assertEquals(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, event.getMessageType());
      assertEquals(17, event.getAttributes().size());
      assertEquals(
            Arrays.asList(DeviceCapability.NAMESPACE, DeviceAdvancedCapability.NAMESPACE, Capability.NAMESPACE, DeviceConnectionCapability.NAMESPACE),
            event.getAttributes().get(Capability.ATTR_CAPS)
      );
      assertEquals("Test", event.getAttributes().get(DeviceCapability.ATTR_DEVTYPEHINT));
      assertEquals("Unknown", event.getAttributes().get(DeviceCapability.ATTR_VENDOR));
      assertEquals("Unknown", event.getAttributes().get(DeviceCapability.ATTR_MODEL));
      assertEquals("DefaultDriver", event.getAttributes().get(DeviceAdvancedCapability.ATTR_DRIVERNAME));
      assertEquals("1.0", event.getAttributes().get(DeviceAdvancedCapability.ATTR_DRIVERVERSION));
   }

   // FIXME GetAttributesRequestHandler in drivers is looking at the wrong attribute name
   @Ignore @Test
   public void testGetDeviceAttributes() throws Exception {
      MessageBody command = Capability.GetAttributesRequest.builder()
         .withNames(ImmutableSet.of(DeviceCapability.NAMESPACE, Capability.NAMESPACE))
         .build();
      sendToDevice(command);

      PlatformMessage response = platformBus.take();

      MessageBody event = response.getValue();
      assertEquals(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, event.getMessageType());
      assertEquals(12, event.getAttributes().size());
      assertEquals(
            Arrays.asList(DeviceCapability.NAMESPACE, DeviceAdvancedCapability.NAMESPACE, DeviceConnectionCapability.NAMESPACE, Capability.NAMESPACE),
            event.getAttributes().get(Capability.ATTR_CAPS)
      );
      assertEquals("Test", event.getAttributes().get(DeviceCapability.ATTR_DEVTYPEHINT));
      assertEquals("Unknown", event.getAttributes().get(DeviceCapability.ATTR_VENDOR));
      assertEquals("Unknown", event.getAttributes().get(DeviceCapability.ATTR_MODEL));
   }

   // FIXME GetAttributesRequestHandler in drivers is looking at the wrong attribute name
   @Ignore @Test
   public void testGetAdvancedAttributes() throws Exception {
      MessageBody command = Capability.GetAttributesRequest.builder()
         .withNames(ImmutableSet.of(DeviceAdvancedCapability.NAMESPACE))
         .build();
      sendToDevice(command);

      PlatformMessage response = platformBus.take();

      MessageBody event = response.getValue();
      assertEquals(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, event.getMessageType());
      assertEquals(5, event.getAttributes().size());
      assertEquals("DefaultDriver", event.getAttributes().get(DeviceAdvancedCapability.ATTR_DRIVERNAME));
      assertEquals("1.0", event.getAttributes().get(DeviceAdvancedCapability.ATTR_DRIVERVERSION));
   }

   // FIXME GetAttributesRequestHandler in drivers is looking at the wrong attribute name
   @Ignore @Test
   public void testGetUnknownAttributes() throws Exception {
      MessageBody command = Capability.GetAttributesRequest.builder()
         .withNames(ImmutableSet.of("foo"))
         .build();
      sendToDevice(command);

      PlatformMessage response = platformBus.take();

      // TODO errors in the future?

      MessageBody event = response.getValue();
      assertEquals(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, event.getMessageType());
      assertEquals(0, event.getAttributes().size());
   }

   @Test
   public void testSetDeviceAttributes() throws Exception {
      sendToDevice(MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES,
            ImmutableMap.of(DeviceCapability.ATTR_NAME, "test")));

      // the command runner handles the empty response...

      assertEquals("test", context.getAttributeValue(DeviceCapability.KEY_NAME));
   }

   @Test
   public void testSetReadOnlyAttributes() throws Exception {
      sendToDevice(MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES,
            ImmutableMap.of("dev:model", "A new model")));

      PlatformMessage message = platformBus.take();
      MessageBody event = message.getValue();
      assertEquals(Capability.EVENT_SET_ATTRIBUTES_ERROR, event.getMessageType());

      assertEquals("Unknown", context.getAttributeValue(DeviceCapability.KEY_MODEL));
   }

   @Test
   public void testSetNonExistentAttributes() throws Exception {
      sendToDevice(MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES,
            ImmutableMap.of("dev:nosuchattribute", "missing")));

      PlatformMessage message = platformBus.take();
      MessageBody event = message.getValue();
      assertEquals(Capability.EVENT_SET_ATTRIBUTES_ERROR, event.getMessageType());
      // nothing was set
      assertFalse(context.hasDirtyAttributes());
   }

   @Test
   public void testMixedSet() {
      Map<String, Object> attributes = new HashMap<>();
      attributes.put(DeviceCapability.ATTR_NAME, "a new name");
      attributes.put(DeviceCapability.ATTR_MODEL, "a new model"); // shouldn't take, read-only
      attributes.put("dev:nosuchattribute", "missing"); // shouldn't take, doesn't exist
      sendToDevice(MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, attributes));

      assertEquals("a new name", context.getAttributeValue(DeviceCapability.KEY_NAME));
      assertEquals("Unknown", context.getAttributeValue(DeviceCapability.KEY_MODEL));
   }

   @Test
   public void testAdd() throws Exception {
      sendToDevice(DriverEvent.createAssociated(AttributeMap.emptyMap()));
      assertEquals("Test Device", context.getAttributeValue(DeviceCapability.KEY_NAME));
   }
   
   @Test
   public void testAddTags() throws Exception {
      Set<String> tags = ImmutableSet.of("test1", "test2", "test3");
      sendToDevice(
            Capability.AddTagsRequest.builder()
               .withTags(tags)
               .build()
      );
      assertEquals(tags, context.getAttributeValue(Capability.KEY_TAGS));
      assertTrue(context.hasDirtyAttributes());
      assertEquals(ImmutableSet.of(Capability.KEY_TAGS), context.getDirtyAttributes().keySet());
   }

   @Test
   public void testRemoveAllTags() throws Exception {
      Set<String> tags = ImmutableSet.of("test1", "test2", "test3");
      context.setAttributeValue(Capability.KEY_TAGS, new HashSet<>(tags));
      sendToDevice(
            Capability.RemoveTagsRequest.builder()
               .withTags(tags)
               .build()
      );
      
      assertEquals(ImmutableSet.of(), context.getAttributeValue(Capability.KEY_TAGS));
      assertTrue(context.hasDirtyAttributes());
      assertEquals(ImmutableSet.of(Capability.KEY_TAGS), context.getDirtyAttributes().keySet());
   }


   @Test
   public void testAddAndRemoveTags() throws Exception {
      Set<String> toAdd = ImmutableSet.of("test1", "test2", "test3");
      Set<String> toRemove = ImmutableSet.of("test3", "test4", "test5");
      sendToDevice(
            Capability.AddTagsRequest.builder()
               .withTags(toAdd)
               .build()
      );
      sendToDevice(
            Capability.RemoveTagsRequest.builder()
               .withTags(toRemove)
               .build()
      );
      assertEquals(ImmutableSet.of("test1", "test2"), context.getAttributeValue(Capability.KEY_TAGS));
      assertTrue(context.hasDirtyAttributes());
      assertEquals(ImmutableSet.of(Capability.KEY_TAGS), context.getDirtyAttributes().keySet());
   }

}

