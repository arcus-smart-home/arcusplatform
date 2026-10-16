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
package com.iris.driver.service;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.driver.DeviceDriverStateHolder;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.driver.DeviceDriver;
import com.iris.driver.Drivers;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.service.executor.DefaultDriverExecutor;
import com.iris.driver.service.executor.DriverExecutor;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.messages.type.Population;
import com.iris.model.Version;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({ DeviceDAO.class, PersonDAO.class, PersonPlaceAssocDAO.class, PlacePopulationCacheManager.class })
@Modules({ InMemoryMessageModule.class, AttributeMapTransformModule.class })
public class MispairedRecoveryTest extends IrisMockTestCase {
   private static final Map<String, String> MISPAIRED_ERRORS =
         ImmutableMap.of(DeviceAdvancedCapability.PairingFailedException.CODE_PAIRING_FAILED, "Driver could not be determined.");

   @Inject InMemoryPlatformMessageBus messages;
   @Inject DeviceDAO mockDeviceDao;
   @Inject CapabilityRegistry capabilities;
   @Inject PlacePopulationCacheManager mockPopulationCacheMgr;

   Device device = Fixtures.createDevice();
   DeviceDriver fallbackDriver;
   DeviceDriver realDriver;

   @Before
   public void initializeDrivers() {
      fallbackDriver = createDriver("Fallback");
      realDriver = createDriver("Driver");
      EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
   }

   @Test
   public void testRecoversRealDriverWithStaleErrors() throws Exception {
      DriverExecutor executor = createExecutor(realDriver, Device.STATE_ACTIVE_UNSUPPORTED, MISPAIRED_ERRORS);
      Capture<Device> saved = expectSaveDevice();
      expectUpdateDriverState();
      replay();

      assertTrue(MispairedRecovery.recover(executor));

      assertEquals(Device.STATE_ACTIVE_SUPPORTED, saved.getValue().getState());
      Map<String, String> errors = executor.context().getAttributeValue(DeviceAdvancedCapability.KEY_ERRORS);
      assertTrue(errors.isEmpty());

      PlatformMessage changed = messages.take();
      assertEquals(Capability.EVENT_VALUE_CHANGE, changed.getMessageType());
      Map<String, Object> attributes = changed.getValue().getAttributes();
      // read the way DeviceChangeListener does, which normalizes the enum casing
      assertEquals(DeviceAdvancedCapability.DRIVERSTATE_ACTIVE, DeviceAdvancedCapability.getDriverstate(changed.getValue()));
      assertEquals(ImmutableMap.of(), attributes.get(DeviceAdvancedCapability.ATTR_ERRORS));
      verify();
   }

   @Test
   public void testKeepsOtherErrors() throws Exception {
      Map<String, String> errors = ImmutableMap.<String, String>builder()
            .putAll(MISPAIRED_ERRORS)
            .put("other.error", "Something else")
            .build();
      DriverExecutor executor = createExecutor(realDriver, Device.STATE_ACTIVE_UNSUPPORTED, errors);
      expectSaveDevice();
      expectUpdateDriverState();
      replay();

      assertTrue(MispairedRecovery.recover(executor));

      assertEquals(ImmutableMap.of("other.error", "Something else"), executor.context().getAttributeValue(DeviceAdvancedCapability.KEY_ERRORS));
      verify();
   }

   @Test
   public void testIgnoresFallbackDriver() throws Exception {
      DriverExecutor executor = createExecutor(fallbackDriver, Device.STATE_ACTIVE_UNSUPPORTED, MISPAIRED_ERRORS);
      replay();

      assertFalse(MispairedRecovery.recover(executor));

      assertEquals(MISPAIRED_ERRORS, executor.context().getAttributeValue(DeviceAdvancedCapability.KEY_ERRORS));
      assertNull(messages.poll());
      verify();
   }

   @Test
   public void testIgnoresHealthyDevice() throws Exception {
      DriverExecutor executor = createExecutor(realDriver, Device.STATE_ACTIVE_SUPPORTED, null);
      replay();

      assertFalse(MispairedRecovery.recover(executor));

      assertNull(messages.poll());
      verify();
   }

   @Test
   public void testIgnoresUnsupportedDeviceWithoutPairingError() throws Exception {
      DriverExecutor executor = createExecutor(realDriver, Device.STATE_ACTIVE_UNSUPPORTED, ImmutableMap.of("other.error", "Something else"));
      replay();

      assertFalse(MispairedRecovery.recover(executor));

      assertNull(messages.poll());
      verify();
   }

   private DeviceDriver createDriver(String name) {
      return Drivers
            .builder()
            .withName(name)
            .withVersion(Version.fromRepresentation("1.0"))
            .withMatcher((a) -> false)
            .withPopulations(ImmutableList.<String>of(Population.NAME_GENERAL))
            .addCapabilityDefinition(capabilities.getCapabilityDefinitionByNamespace(DeviceCapability.NAMESPACE))
            .create(true);
   }

   private DriverExecutor createExecutor(DeviceDriver driver, String state, Map<String, String> errors) {
      device.setDriverId(driver.getDriverId());
      device.setState(state);
      PlatformDeviceDriverContext context = new PlatformDeviceDriverContext(device, driver, mockPopulationCacheMgr);
      if(errors != null) {
         context.setAttributeValue(DeviceAdvancedCapability.KEY_ERRORS, errors);
      }
      context.clearDirty();
      return new DefaultDriverExecutor(driver, context, null, 100);
   }

   private Capture<Device> expectSaveDevice() {
      Capture<Device> deviceRef = Capture.newInstance();
      EasyMock.expect(mockDeviceDao.save(EasyMock.capture(deviceRef)))
         .andAnswer(() -> {
            Device device = deviceRef.getValue().copy();
            device.setModified(new Date());
            return device;
         });
      return deviceRef;
   }

   private void expectUpdateDriverState() {
      mockDeviceDao.updateDriverState(EasyMock.anyObject(Device.class), EasyMock.anyObject(DeviceDriverStateHolder.class));
      EasyMock.expectLastCall();
   }
}
