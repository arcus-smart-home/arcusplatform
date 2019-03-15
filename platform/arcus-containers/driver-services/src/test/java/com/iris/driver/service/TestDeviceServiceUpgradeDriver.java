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
package com.iris.driver.service;

import java.util.Date;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IExpectationSetters;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
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
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.Drivers;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.groovy.GroovyProtocolPluginModule;
import com.iris.driver.service.DeviceService.UpgradeDriverResponse;
import com.iris.driver.service.executor.DefaultDriverExecutor;
import com.iris.driver.service.executor.DriverExecutor;
import com.iris.driver.service.executor.DriverExecutorRegistry;
import com.iris.driver.service.registry.DriverRegistry;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.Device;
import com.iris.messages.model.DriverId;
import com.iris.messages.model.Fixtures;
import com.iris.messages.type.Population;
import com.iris.model.Version;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;
import com.iris.util.Initializer;

/**
 *
 */
@Mocks({ DeviceDAO.class, HubDAO.class, PersonDAO.class, PersonPlaceAssocDAO.class, DriverRegistry.class, DriverExecutorRegistry.class, PlaceDAO.class, PopulationDAO.class, PlacePopulationCacheManager.class })
@Modules({ InMemoryMessageModule.class, AttributeMapTransformModule.class, GroovyProtocolPluginModule.class })
public class TestDeviceServiceUpgradeDriver extends IrisMockTestCase {

   // unit under test
   @Inject DeviceService uut;

   // mocks
   @Inject DriverRegistry registry;
   @Inject InMemoryPlatformMessageBus messages;
   @Inject DeviceDAO mockDeviceDao;
   @Inject DriverRegistry mockRegistry;
   @Inject DriverExecutorRegistry mockExecutorRegistry;
   @Inject HubDAO hubDao;
   @Inject CapabilityRegistry capabilities;
   @Inject private PlacePopulationCacheManager mockPopulationCacheMgr;

   // fixtures
   Device device = Fixtures.createDevice();
   DeviceDriverStateHolder state = new DeviceDriverStateHolder(AttributeMap.mapOf(DeviceConnectionCapability.KEY_STATE.valueOf(DeviceConnectionCapability.STATE_ONLINE)));
   DeviceDriver fallbackDriver;
   DeviceDriver switchDriver;
   
   @Before
   public void initializeDrivers() {
      fallbackDriver =
            Drivers
               .builder()
               .withName("Fallback")
               .withVersion(Version.fromRepresentation("1.0"))
               .withMatcher((a) -> false)
               .withPopulations(ImmutableList.<String>of(Population.NAME_GENERAL, Population.NAME_BETA, Population.NAME_QA))
               .addCapabilityDefinition(capabilities.getCapabilityDefinitionByNamespace(DeviceCapability.NAMESPACE))
               .create(true)
               ;
      
      switchDriver =
            Drivers
               .builder()
               .withName("Driver")
               .withVersion(Version.fromRepresentation("3.0"))
               .withMatcher((a) -> false)
               .withPopulations(ImmutableList.<String>of(Population.NAME_GENERAL, Population.NAME_BETA, Population.NAME_QA))
               .addCapabilityDefinition(capabilities.getCapabilityDefinitionByNamespace(DeviceCapability.NAMESPACE))
               .addCapabilityDefinition(capabilities.getCapabilityDefinitionByNamespace(SwitchCapability.NAMESPACE))
               .create(true)
               ;

      EasyMock
         .expect(registry.getFallback())
         .andReturn(fallbackDriver)
         .anyTimes();
      EasyMock
         .expect(registry.loadDriverById(switchDriver.getDriverId()))
         .andReturn(switchDriver)
         .anyTimes();
      EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
   }

   @Test
   public void testUpgradeFromFallbackToBestDriver() throws Exception {
      expectLoadConsumerWithDriver(fallbackDriver);
      expectFindByProtocolAttributes().andReturn(switchDriver);
      expectAssociateDriver(switchDriver);
      expectResyncToHub(hubDao);
      expectLoadHubModelForPlace(hubDao);
      Capture<Device> saved = expectSaveDevice();
      replay();

      UpgradeDriverResponse response = uut.upgradeDriver(Address.fromString(device.getAddress()));
      assertTrue(response.isUpgraded());
      assertEquals(switchDriver.getDriverId(), response.getDriverId());
      assertEquals(Device.STATE_ACTIVE_SUPPORTED, saved.getValue().getState());
      
      // note all the changes to the device object are controlled by the executor registry which we have mocked out
      // so verifying value changes / device save updates isn't worth a lot

      verify();
   }

   @Test
   public void testUpgradeFromFallbackToRequestedDriver() throws Exception {
      // currently on fallback
      expectLoadConsumerWithDriver(fallbackDriver);
      expectAssociateDriver(switchDriver);
      expectResyncToHub(hubDao);
      expectLoadHubModelForPlace(hubDao);
      Capture<Device> saved = expectSaveDevice();
      replay();

      UpgradeDriverResponse response = uut.upgradeDriver(
            Address.fromString(device.getAddress()),
            switchDriver.getDriverId()
      );
      assertTrue(response.isUpgraded());
      assertEquals(switchDriver.getDriverId(), response.getDriverId());
      assertEquals(Device.STATE_ACTIVE_SUPPORTED, saved.getValue().getState());

      verify();
   }

   @Test
   public void testUpgradeToSameBestDriver() throws Exception {
      // currently on switch
      expectLoadConsumerWithDriver(switchDriver);
      expectFindByProtocolAttributes().andReturn(switchDriver);
      expectLoadHubModelForPlace(hubDao);
      replay();

      UpgradeDriverResponse response = uut.upgradeDriver(Address.fromString(device.getAddress()));
      assertFalse(response.isUpgraded());
      assertEquals(switchDriver.getDriverId(), response.getDriverId());

      verify();
   }

   @Test
   public void testUpgradeToSameRequestedDriver() throws Exception {
      expectLoadConsumerWithDriver(switchDriver);
      expectLoadHubModelForPlace(hubDao);
      replay();

      UpgradeDriverResponse response = uut.upgradeDriver(
            Address.fromString(device.getAddress()),
            switchDriver.getDriverId()
      );
      assertFalse(response.isUpgraded());
      assertEquals(switchDriver.getDriverId(), response.getDriverId());
      assertNull(messages.poll());

      verify();
   }

   @Test
   public void testUpgradeToNonExistentRequestedDriver() throws Exception {
      DriverId id = new DriverId("InvalidDriverName", Version.fromRepresentation("1.0"));
      expectLoadConsumerWithDriver(switchDriver);
      EasyMock
         .expect(mockRegistry.loadDriverById(id))
         .andReturn(null);
      replay();

      try {
         uut.upgradeDriver(Address.fromString(device.getAddress()), id);
         fail();
      }
      catch(ErrorEventException e) {
         assertEquals("NoSuchDriver", e.getCode());
      }
      assertNull(messages.poll());

      verify();
   }

   /*
   @Test
   public void testUpgradeFromNonExistentToIpcdDriver() throws Exception {
      // currently on fallback
      device.setDriverId(new DriverId("NoSuchDriver", Version.fromRepresentation("1.0")));

      // TODO check these captures for the proper state
      expectLoadDeviceAndState();
      expectLoadDeviceState();
      expectSaveDevice();
      expectReplaceState();
      replay();

      UpgradeDriverResponse response =
         uut.upgradeDriver(Address.fromString(device.getAddress()));
      assertTrue(response.isUpgraded());
      assertEquals(IpcdBlackboxSwitchDriver.NAME, response.getDriverId().getName());

      {
         PlatformMessage message = messages.take();
         assertEquals(Capability.EVENT_VALUE_CHANGE, message.getMessageType());
         assertEquals(IpcdBlackboxSwitchDriver.NAME, message.getValue().getAttributes().get(DeviceAdvancedCapability.ATTR_DRIVERNAME));
         assertEquals("1.0", message.getValue().getAttributes().get(DeviceAdvancedCapability.ATTR_DRIVERVERSION));
      }
      assertNull(messages.poll());

      verify();
   }
   */

   private void expectLoadConsumerWithDriver(DeviceDriver driver) {
      device.setDriverId(driver.getDriverId());
      EasyMock
         .expect(mockExecutorRegistry.loadConsumer(Address.fromString(device.getAddress())))
         .andReturn(new DefaultDriverExecutor(driver, createContext(device, driver), null, 100));
   }
   
   private IExpectationSetters<DeviceDriver> expectFindByProtocolAttributes() {
      AttributeMap protocolAttributes = AttributeMap.mapOf(
            DeviceService.DEVICE_ADV_PROTCOL_KEY.valueOf(device.getProtocol()),
            DeviceService.DEVICE_ADV_SUBPROTCOL_KEY.valueOf(device.getSubprotocol()),
            DeviceService.DEVICE_ADV_PROTOCOLID_KEY.valueOf(device.getProtocolid())
      );
      return
         EasyMock
            .expect(mockRegistry.findDriverFor("general", protocolAttributes, 0));
   }
   
   private void expectAssociateDriver(DeviceDriver driver) {
      Capture<Initializer<DriverExecutor>> executorRef = EasyMock.newCapture();
      EasyMock
         .expect(mockExecutorRegistry.associate(EasyMock.eq(device), EasyMock.eq(driver), EasyMock.capture(executorRef)))
         .andAnswer(() -> {
            DefaultDriverExecutor executor = new DefaultDriverExecutor(driver, createContext(device, driver), null, 100);
            executorRef.getValue().initialize(executor);
            return executor;
         });
   }
    
   private void expectResyncToHub(HubDAO hubDao) {
      EasyMock.expect(hubDao.findHubForPlace(null))
         .andReturn(null);
   }

   private void expectLoadHubModelForPlace(HubDAO hubDao) {
      EasyMock
         .expect(hubDao.findHubModelForPlace(null))
         .andReturn(null)
         .anyTimes();
   }
   
   private DeviceDriverContext createContext(Device device2, DeviceDriver driver) {
      PlatformDeviceDriverContext context = new PlatformDeviceDriverContext(device, driver, mockPopulationCacheMgr);
      context.setAttributeValue(DeviceConnectionCapability.KEY_STATE.valueOf(DeviceConnectionCapability.STATE_ONLINE));
      context.clearDirty();
      return context;
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

}

