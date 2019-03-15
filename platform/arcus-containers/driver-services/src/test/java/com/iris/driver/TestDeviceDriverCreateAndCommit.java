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
package com.iris.driver;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.common.scheduler.ExecutorScheduler;
import com.iris.common.scheduler.Scheduler;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.driver.DeviceDriverStateHolder;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.device.attributes.AttributeMap;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DevicePowerCapability;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.messages.type.Population;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 *
 */
@Modules({InMemoryMessageModule.class, AttributeMapTransformModule.class, CapabilityRegistryModule.class})
@Mocks({DeviceDAO.class, PersonDAO.class, PersonPlaceAssocDAO.class, PlaceDAO.class, PlacePopulationCacheManager.class})
public class TestDeviceDriverCreateAndCommit extends IrisMockTestCase {
   @Inject DeviceDAO mockDeviceDao;
   @Inject InMemoryPlatformMessageBus platformBus;
   @Inject CapabilityRegistry registry;
   @Inject PlaceDAO mockPlaceDao;
   @Inject PlacePopulationCacheManager populationCacheMgr;
   
   DeviceDriver driver;
   PlatformDeviceDriverContext context;
   AttributeMap initialAttributes;

   @Provides
   public Scheduler scheduler() {
      return new ExecutorScheduler(Executors.newScheduledThreadPool(1));
   }

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();

      initialAttributes = AttributeMap.newMap();
      initialAttributes.set(DevicePowerCapability.KEY_LINECAPABLE, false);
      initialAttributes.add(DeviceConnectionCapability.KEY_STATE.valueOf(DeviceConnectionCapability.STATE_ONLINE));

      driver =
            Drivers
               .builder()
               .withName("Test")
               .withMatcher(Predicates.alwaysTrue())
               .withPopulations(ImmutableList.<String>of(Population.NAME_GENERAL, Population.NAME_BETA, Population.NAME_QA))
               .addAttribute(DevicePowerCapability.KEY_LINECAPABLE, false)
               .addCapabilityDefinition(registry.getCapabilityDefinitionByNamespace(DeviceCapability.NAMESPACE))
               .create(true);

      Device device = Fixtures.createDevice();
      //device.setPlace(UUID.randomUUID());
      device.setName(PlatformDeviceDriverContext.FALLBACK_DEVICE_NAME);
      device.setState(Device.STATE_ACTIVE_UNSUPPORTED);
      
      
      context = new PlatformDeviceDriverContext(device, driver, populationCacheMgr);
      context.setAttributeValue(DeviceConnectionCapability.KEY_STATE.valueOf(DeviceConnectionCapability.STATE_ONLINE));
      context.clearDirty();
      
      EasyMock.expect(populationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
      //EasyMock.expect(populationCacheMgr.getPopulationByPlaceId(null)).andReturn(Population.NAME_GENERAL).anyTimes();

   }

   @Test
   public void testCreateNoDeviceUpdates() throws Exception {
      Device device = context.getDevice().copy();

      mockDeviceDao.replaceDriverState(device, new DeviceDriverStateHolder(initialAttributes));
      EasyMock.expectLastCall();
      
      replay();

      context.create();

      PlatformMessage message = platformBus.take();

      MessageBody addedEvent = message.getValue();

      assertEquals(device.getAddress(), addedEvent.getAttributes().get(Capability.ATTR_ADDRESS));
      assertEquals(device.getId().toString(), addedEvent.getAttributes().get(Capability.ATTR_ID));
      assertEquals(ImmutableList.of("dev"), addedEvent.getAttributes().get(Capability.ATTR_CAPS));
      assertEquals(ImmutableList.of(), addedEvent.getAttributes().get(Capability.ATTR_TAGS));
      assertEquals(DeviceCapability.NAMESPACE, addedEvent.getAttributes().get(Capability.ATTR_TYPE));

      verify();
   }

   @Test
   public void testCreateWithDeviceUpdates() throws Exception {
      Device device = context.getDevice().copy();
      device.setVendor("vendor");
      device.setModel("model");

      EasyMock.expect(mockDeviceDao.save(device)).andReturn(device);
      mockDeviceDao.replaceDriverState(context.getDevice(), new DeviceDriverStateHolder(initialAttributes));
      EasyMock.expectLastCall();
      replay();

      context.setAttributeValue(DeviceCapability.KEY_VENDOR, "vendor");
      context.setAttributeValue(DeviceCapability.KEY_MODEL, "model");

      context.create();

      PlatformMessage message = platformBus.take();
      MessageBody addedEvent = message.getValue();
      // spot check
      assertEquals(device.getId().toString(), addedEvent.getAttributes().get(Capability.ATTR_ID));
      assertEquals("dev", addedEvent.getAttributes().get(Capability.ATTR_TYPE));
      assertEquals("vendor", addedEvent.getAttributes().get(DeviceCapability.ATTR_VENDOR));
      assertEquals("model", addedEvent.getAttributes().get(DeviceCapability.ATTR_MODEL));

      verify();
   }

   @Test
   public void testNoUpdates() throws Exception {
      replay();

      context.commit();

      assertNull(platformBus.poll());

      verify();
   }

   @Test
   public void testDeviceUpdates() throws Exception {
      Device device = context.getDevice().copy();
      device.setName("My new name");

      EasyMock
         .expect(mockDeviceDao.save(device))
         .andReturn(device);
      replay();

      context.getDevice().setName("My new name");
      context.commit();

      assertNull(platformBus.poll());

      verify();
   }

   @Test
   public void testAttributeUpdates() throws Exception {
      Device device = context.getDevice().copy();

      Map<String,String> images = Collections.<String,String>singletonMap("icon", UUID.randomUUID().toString());

      AttributeMap expected = AttributeMap.newMap();
      expected.set(DeviceCapability.KEY_NAME, "My new name");
      expected.set(Capability.KEY_IMAGES, images);

      device.setName("My new name");
      device.setImages(images.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> UUID.fromString(e.getValue()))));

      EasyMock.expect(mockDeviceDao.save(device)).andReturn(device);
      replay();

      context.setAttributeValue(DeviceCapability.KEY_NAME, "My new name");
      context.setAttributeValue(Capability.KEY_IMAGES, images);
      context.commit();

      PlatformMessage message = platformBus.take();
      MessageBody event = message.getValue();
      assertEquals(Capability.EVENT_VALUE_CHANGE, event.getMessageType());
      assertEquals(expected.toMap(), event.getAttributes());

      verify();
   }

   @Test
   public void testUpdates() throws Exception {
      Device device = context.getDevice().copy();

      Map<String,String> images = Collections.<String,String>singletonMap("icon", UUID.randomUUID().toString());
      device.setName("My new name");
      device.setImages(images.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> UUID.fromString(e.getValue()))));

      AttributeMap expected = AttributeMap.newMap();
      expected.set(DeviceCapability.KEY_NAME, "My new name");
      expected.set(Capability.KEY_IMAGES, images);

      EasyMock.expect(mockDeviceDao.save(device)).andReturn(device);
      replay();

      context.setAttributeValue(DeviceCapability.KEY_NAME, "My new name");
      context.setAttributeValue(Capability.KEY_IMAGES, images);
      context.commit();

      PlatformMessage message = platformBus.take();
      MessageBody event = message.getValue();
      assertEquals(Capability.EVENT_VALUE_CHANGE, event.getMessageType());
      assertEquals(expected.toMap(), event.getAttributes());

      verify();
   }

   // this is testing context timing out, but
   @Ignore
   @Test
   public void testMultiOperationSetAttributesErrorsOut() throws Exception {
      Device device = context.getDevice().copy();

      Map<String,String> images = Collections.<String,String>singletonMap("icon", UUID.randomUUID().toString());

      AttributeMap setAttributes = AttributeMap.newMap();
      setAttributes.set(DeviceCapability.KEY_NAME, "My new name");
      setAttributes.set(Capability.KEY_IMAGES, images);

      PlatformMessage message =
            PlatformMessage
               .builder()
               .from(Fixtures.createClientAddress())
               .to(Address.fromString(device.getAddress()))
               .withPayload(MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, setAttributes.toMap()))
               .withCorrelationId("correlation-id")
               .create();
      context.setMessageContext(message);

      // no changes should result in no saves, no events
      {
         reset();
         replay();

         context.commit();
         assertFalse(context.hasDirtyAttributes());
         assertTrue(context.hasMessageContext());
         assertNull(platformBus.poll());

         verify();
      }

      // one of two attributes set, the attributes should be updated and a value change should happen
      {
         reset();

         mockDeviceDao.updateDriverState(device, new DeviceDriverStateHolder(AttributeMap.mapOf(DeviceCapability.KEY_NAME.valueOf("My new name"))));
         EasyMock.expectLastCall();

         replay();

         context.setAttributeValue(DeviceCapability.KEY_NAME, "My new name");
         context.commit();

         assertFalse(context.hasDirtyAttributes());
         assertTrue(context.hasMessageContext());

         message = platformBus.poll();
         assertEquals(Capability.EVENT_VALUE_CHANGE, message.getMessageType());
//         assertEquals("correlation-id", response.getCorrelationId());

         MessageBody event = message.getValue();
         assertEquals("My new name", event.getAttributes().get(DeviceCapability.ATTR_NAME));

         assertNull(platformBus.poll());

         verify();
      }

      // now an error gets set, clearing out the context
      {
         reset();
         replay();

         context.respondToPlatform(ErrorEvent.fromCode("Timeout", "Request timed out"));
         context.commit();

         assertFalse(context.hasDirtyAttributes());
         assertFalse(context.hasMessageContext());

         // response to the request is an error
         message = platformBus.take();
         assertFalse(message.getDestination().isBroadcast());
         assertEquals("correlation-id", message.getCorrelationId());
         ErrorEvent errorEvent = (ErrorEvent) message.getValue();
         assertEquals("Timeout", errorEvent.getCode());

         assertNull(platformBus.poll());

         verify();
      }
   }

}

