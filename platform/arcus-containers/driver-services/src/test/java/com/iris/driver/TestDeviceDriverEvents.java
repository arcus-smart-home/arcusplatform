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

import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.event.DeviceAssociatedEvent;
import com.iris.driver.event.DeviceConnectedEvent;
import com.iris.driver.event.DeviceDisassociatedEvent;
import com.iris.driver.event.DeviceDisconnectedEvent;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.model.Fixtures;
import com.iris.messages.type.Population;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

/**
 *
 */
@Mocks({DeviceDAO.class, PersonDAO.class, PersonPlaceAssocDAO.class, PlaceDAO.class, PlacePopulationCacheManager.class})
public class TestDeviceDriverEvents extends IrisMockTestCase {
   DeviceDriver driver;
   ContextualEventHandler<DriverEvent> eventHandler1;
   ContextualEventHandler<DriverEvent> eventHandler2;
   PlatformDeviceDriverContext context;
   @Inject PlacePopulationCacheManager populationCacheMgr;


   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      eventHandler1 = EasyMock.createMock(ContextualEventHandler.class);
      eventHandler2 = EasyMock.createMock(ContextualEventHandler.class);

      driver =
            Drivers
               .builder()
               .withName("Test")
               .withMatcher(Predicates.alwaysTrue())
               .withPopulations(ImmutableList.<String>of(Population.NAME_GENERAL, Population.NAME_BETA, Population.NAME_QA))
               .addDriverEventHandler(DeviceAssociatedEvent.class, eventHandler1)
               .addDriverEventHandler(DeviceConnectedEvent.class, eventHandler1)
               .addDriverEventHandler(DeviceDisconnectedEvent.class, eventHandler1)
               .addDriverEventHandler(DeviceDisassociatedEvent.class, eventHandler1)
               .addDriverEventHandler(DeviceAssociatedEvent.class, eventHandler2)
               .addDriverEventHandler(DeviceConnectedEvent.class, eventHandler2)
               .addDriverEventHandler(DeviceDisconnectedEvent.class, eventHandler2)
               .addDriverEventHandler(DeviceDisassociatedEvent.class, eventHandler2)
               .addCapabilityDefinition(new CapabilityDefinition("Device", "dev", "base", "", ImmutableMap.of(), ImmutableMap.of(), ImmutableMap.of()))
               .create(true);

      EasyMock.expect(populationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
      context = new PlatformDeviceDriverContext(Fixtures.createDevice(), driver, populationCacheMgr);
      context.setAttributeValue(DeviceConnectionCapability.KEY_STATE.valueOf(DeviceConnectionCapability.STATE_ONLINE));
      context.clearDirty();
   }

   @Override
   public void replay() {
      super.replay();
      EasyMock.replay(eventHandler1, eventHandler2);
   }

   @Override
   public void verify() {
      EasyMock.verify(eventHandler1, eventHandler2);
      super.verify();
   }

   @Test
   public void testAssociated() throws Exception {
      DriverEvent event = DriverEvent.createAssociated(AttributeMap.emptyMap());
      EasyMock.expect(eventHandler1.handleEvent(context, event)).andReturn(true).once();
      EasyMock.expect(eventHandler2.handleEvent(context, event)).andReturn(true).once();
      EasyMock.expect(eventHandler1.handleEvent(context, event)).andReturn(false).once();
      EasyMock.expect(eventHandler2.handleEvent(context, event)).andReturn(false).once();
      replay();

      driver.handleDriverEvent(event, context);
      driver.handleDriverEvent(event, context);

      verify();
   }

   /**
    * Each listener should be notified regardless of exceptions being thrown
    * @throws Exception
    */
   @Test
   public void testAssociatedThrowsException() throws Exception {
      DriverEvent event = DriverEvent.createAssociated(AttributeMap.emptyMap());
      EasyMock.expect(eventHandler1.handleEvent(context, event)).andThrow(new RuntimeException("BOOM")).once();
      EasyMock.expect(eventHandler2.handleEvent(context, event)).andThrow(new RuntimeException("BOOM")).once();
      replay();

      driver.handleDriverEvent(event, context);

      verify();
   }

   @Test
   public void testAllEvents() throws Exception {
      DriverEvent associated = DriverEvent.createAssociated(AttributeMap.emptyMap());
      DriverEvent connected = DriverEvent.createConnected(0);
      DriverEvent disconnected = DriverEvent.createDisconnected(0);
      DriverEvent disassociated = DriverEvent.createDisassociated();

      EasyMock.expect(eventHandler1.handleEvent(context, associated)).andReturn(true).once();
      EasyMock.expect(eventHandler2.handleEvent(context, associated)).andReturn(true).once();
      EasyMock.expect(eventHandler1.handleEvent(context, connected)).andReturn(true).once();
      EasyMock.expect(eventHandler2.handleEvent(context, connected)).andReturn(true).once();
      EasyMock.expect(eventHandler1.handleEvent(context, disconnected)).andReturn(true).once();
      EasyMock.expect(eventHandler2.handleEvent(context, disconnected)).andReturn(true).once();
      EasyMock.expect(eventHandler1.handleEvent(context, disassociated)).andReturn(true).once();
      EasyMock.expect(eventHandler2.handleEvent(context, disassociated)).andReturn(true).once();

      replay();

      driver.handleDriverEvent(associated, context);
      driver.handleDriverEvent(connected, context);
      driver.handleDriverEvent(disconnected, context);
      driver.handleDriverEvent(disassociated, context);

      verify();
   }
}

