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
package com.iris.service.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.common.sunrise.SunriseSunsetCalc;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.capability.SchedulerCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.model.Device;
import com.iris.messages.model.Place;
import com.iris.messages.model.serv.SchedulerModel;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.scheduler.SchedulerModelDao;
import com.iris.population.PlacePopulationCacheManager;

/**
 * The first request naming a target creates its scheduler for the requester's place, so the
 * registry has to refuse a target at another place before anything is created. The scheduler
 * DAO is a strict mock: a refused target that still created a scheduler fails the test.
 */
public class PlatformSchedulerRegistryTest {
   private final UUID place = UUID.randomUUID();
   private final UUID otherPlace = UUID.randomUUID();
   private final UUID deviceId = UUID.randomUUID();
   private final Address device = Address.platformDriverAddress(deviceId);

   private SchedulerModelDao schedulerDao;
   private DeviceDAO deviceDao;
   private PlaceDAO placeDao;
   private PlatformSchedulerRegistry registry;

   @Before
   public void setUp() {
      schedulerDao = EasyMock.createMock(SchedulerModelDao.class);
      deviceDao = EasyMock.createMock(DeviceDAO.class);
      placeDao = EasyMock.createNiceMock(PlaceDAO.class);
      EasyMock.expect(placeDao.findById(place)).andReturn(new Place()).anyTimes();
      registry = new PlatformSchedulerRegistry(
            EasyMock.createNiceMock(SunriseSunsetCalc.class),
            schedulerDao,
            placeDao,
            deviceDao,
            new InMemoryPlatformMessageBus(),
            EasyMock.createNiceMock(EventSchedulerService.class),
            EasyMock.createNiceMock(DefinitionRegistry.class),
            EasyMock.createNiceMock(PlacePopulationCacheManager.class));
   }

   @After
   public void tearDown() {
      EasyMock.verify(schedulerDao, deviceDao);
   }

   private void replay() {
      EasyMock.replay(schedulerDao, deviceDao, placeDao);
   }

   private Device deviceAt(UUID placeId) {
      Device d = new Device();
      d.setId(deviceId);
      d.setPlace(placeId);
      return d;
   }

   private ModelEntity schedulerFor(Address target) {
      return new ModelEntity(ImmutableMap.<String, Object>of(
            Capability.ATTR_ID, UUID.randomUUID().toString(),
            Capability.ATTR_TYPE, SchedulerCapability.NAMESPACE,
            SchedulerCapability.ATTR_PLACEID, place.toString(),
            SchedulerCapability.ATTR_TARGET, target.getRepresentation()));
   }

   @Test
   public void testAnotherPlacesDeviceIsNotFoundAndNoSchedulerIsCreated() {
      EasyMock.expect(deviceDao.findById(deviceId)).andReturn(deviceAt(otherPlace));
      replay();
      try {
         registry.loadOrCreateByAddress(place, device);
         fail("expected another place's device to be refused");
      }
      catch(NotFoundException e) {
         // expected
      }
   }

   @Test
   public void testAnUnknownDeviceIsNotFoundAndNoSchedulerIsCreated() {
      EasyMock.expect(deviceDao.findById(deviceId)).andReturn(null);
      replay();
      try {
         registry.loadOrCreateByAddress(place, device);
         fail("expected an unknown device to be refused");
      }
      catch(NotFoundException e) {
         // expected
      }
   }

   @Test
   public void testHubsGetNoScheduler() {
      replay();
      for(Address hub : new Address[] { Address.hubAddress("ABC-1234"), Address.hubService("ABC-1234", HubCapability.NAMESPACE) }) {
         try {
            registry.loadOrCreateByAddress(place, hub);
            fail("expected " + hub + " to be refused");
         }
         catch(ErrorEventException e) {
            assertEquals(Errors.CODE_INVALID_REQUEST, e.getCode());
         }
      }
   }

   @Test
   public void testTheOwnPlacesDeviceGetsAScheduler() {
      EasyMock.expect(deviceDao.findById(deviceId)).andReturn(deviceAt(place));
      EasyMock.expect(schedulerDao.findOrCreateByTarget(place, device)).andReturn(schedulerFor(device));
      replay();

      SchedulerCapabilityDispatcher executor = registry.loadOrCreateByAddress(place, device);
      assertNotNull(executor);
      assertEquals(place.toString(), SchedulerModel.getPlaceId(executor.getScheduler()));
   }

   @Test
   public void testASceneTargetNeedsNoDeviceLookup() {
      Address scene = Address.platformService(place, SceneCapability.NAMESPACE, 1);
      EasyMock.expect(schedulerDao.findOrCreateByTarget(place, scene)).andReturn(schedulerFor(scene));
      replay();

      assertNotNull(registry.loadOrCreateByAddress(place, scene));
   }
}
