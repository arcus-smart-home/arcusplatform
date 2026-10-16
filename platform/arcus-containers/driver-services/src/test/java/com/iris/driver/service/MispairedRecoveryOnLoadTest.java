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

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.core.driver.DeviceDriverStateHolder;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriver;
import com.iris.driver.Drivers;
import com.iris.driver.groovy.GroovyProtocolPluginModule;
import com.iris.driver.service.executor.DriverExecutor;
import com.iris.driver.service.executor.DriverExecutorRegistry;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.model.Device;
import com.iris.messages.model.DriverId;
import com.iris.messages.model.Fixtures;
import com.iris.messages.type.Population;
import com.iris.model.Version;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 * Loading a device's executor clears the mispaired state a fallback driver left on it.
 */
@Mocks({ DeviceDAO.class, HubDAO.class, PersonDAO.class, PersonPlaceAssocDAO.class, PlaceDAO.class, PopulationDAO.class })
@Modules({ InMemoryMessageModule.class, TestDriverModule.class, AttributeMapTransformModule.class, GroovyProtocolPluginModule.class, MispairedRecoveryOnLoadTest.RealDriverModule.class })
public class MispairedRecoveryOnLoadTest extends IrisMockTestCase {
   private static final DriverId REAL_DRIVER = new DriverId("RealDriver", Version.fromRepresentation("1.0"));
   private static final Map<String, String> MISPAIRED_ERRORS =
         ImmutableMap.of(DeviceAdvancedCapability.PairingFailedException.CODE_PAIRING_FAILED, "Driver could not be determined.");

   public static class RealDriverModule extends AbstractIrisModule {
      @Override
      protected void configure() {
      }

      @Provides
      @Named("RealDriver")
      public DeviceDriver realDriver(CapabilityRegistry registry) {
         return
               Drivers
                  .builder()
                  .withName(REAL_DRIVER.getName())
                  .withVersion(REAL_DRIVER.getVersion())
                  .withMatcher((a) -> false)
                  .withPopulations(ImmutableList.<String>of(Population.NAME_GENERAL, Population.NAME_BETA, Population.NAME_QA))
                  .addCapabilityDefinition(registry.getCapabilityDefinitionByNamespace(DeviceCapability.NAMESPACE))
                  .addCapabilityDefinition(registry.getCapabilityDefinitionByNamespace(DeviceAdvancedCapability.NAMESPACE))
                  .create(true)
                  ;
      }
   }

   @Inject DriverExecutorRegistry executors;
   @Inject @Named("RealDriver") DeviceDriver realDriver;
   @Inject InMemoryPlatformMessageBus messages;
   @Inject DeviceDAO mockDeviceDao;

   Device device = Fixtures.createDevice();

   @Test
   public void testLoadClearsStalePairingState() throws Exception {
      device.setDriverId(REAL_DRIVER);
      device.setState(Device.STATE_ACTIVE_UNSUPPORTED);
      expectLoad(MISPAIRED_ERRORS);
      Capture<Device> saved = Capture.newInstance();
      EasyMock.expect(mockDeviceDao.save(EasyMock.capture(saved))).andAnswer(() -> saved.getValue().copy());
      mockDeviceDao.updateDriverState(EasyMock.anyObject(Device.class), EasyMock.anyObject(DeviceDriverStateHolder.class));
      EasyMock.expectLastCall();
      replay();

      DriverExecutor executor = executors.loadConsumer(Address.fromString(device.getAddress()));

      assertEquals(Device.STATE_ACTIVE_SUPPORTED, saved.getValue().getState());
      assertEquals(ImmutableMap.of(), executor.context().getAttributeValue(DeviceAdvancedCapability.KEY_ERRORS));
      PlatformMessage changed = messages.take();
      assertEquals(Capability.EVENT_VALUE_CHANGE, changed.getMessageType());
      assertEquals(DeviceAdvancedCapability.DRIVERSTATE_ACTIVE, DeviceAdvancedCapability.getDriverstate(changed.getValue()));
      assertEquals(ImmutableMap.of(), changed.getValue().getAttributes().get(DeviceAdvancedCapability.ATTR_ERRORS));
      verify();
   }

   @Test
   public void testLoadLeavesHealthyDeviceAlone() throws Exception {
      device.setDriverId(REAL_DRIVER);
      device.setState(Device.STATE_ACTIVE_SUPPORTED);
      expectLoad(null);
      replay();

      executors.loadConsumer(Address.fromString(device.getAddress()));

      assertNull(messages.poll());
      verify();
   }

   @Test
   public void testAssociateLeavesRepairToTheUpgrade() throws Exception {
      // the device is on the fallback, and an upgrade associates the real driver
      device.setDriverId(new DriverId("Fallback", Version.fromRepresentation("1.0")));
      device.setState(Device.STATE_ACTIVE_UNSUPPORTED);
      expectLoad(MISPAIRED_ERRORS);
      EasyMock.expect(mockDeviceDao.save(EasyMock.anyObject(Device.class))).andAnswer(() -> ((Device) EasyMock.getCurrentArguments()[0]).copy()).anyTimes();
      mockDeviceDao.updateDriverState(EasyMock.anyObject(Device.class), EasyMock.anyObject(DeviceDriverStateHolder.class));
      EasyMock.expectLastCall().anyTimes();
      replay();

      AtomicReference<Map<String, String>> errorsAtInitialize = new AtomicReference<>();
      executors.associate(device, realDriver, (executor) -> errorsAtInitialize.set(executor.context().getAttributeValue(DeviceAdvancedCapability.KEY_ERRORS)));

      // not cleared, saved or broadcast by the registry: DeviceService clears it before the new driver's onAdded
      assertEquals(MISPAIRED_ERRORS, errorsAtInitialize.get());
      verify();
   }

   private void expectLoad(Map<String, String> errors) {
      AttributeMap attributes = AttributeMap.newMap();
      if(errors != null) {
         attributes.set(DeviceAdvancedCapability.KEY_ERRORS, errors);
      }
      EasyMock.expect(mockDeviceDao.findById(device.getId())).andReturn(device);
      EasyMock.expect(mockDeviceDao.loadDriverState(device)).andReturn(new DeviceDriverStateHolder(attributes));
   }
}
