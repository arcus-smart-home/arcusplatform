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
package com.iris.driver;

import java.util.Map;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.core.driver.DeviceDriverStateHolder;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.driver.service.TestDriverModule;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.messages.type.Population;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 * A pairing device's state is derived from both the driver state and the errors, so
 * a value change to either one carries both.
 */
@Mocks({ DeviceDAO.class, PersonDAO.class, HubDAO.class, PersonPlaceAssocDAO.class, PlaceDAO.class, PopulationDAO.class, PlacePopulationCacheManager.class })
@Modules({ InMemoryMessageModule.class, TestDriverModule.class, AttributeMapTransformModule.class, CapabilityRegistryModule.class })
public class PlatformDeviceDriverContextPairingStateTest extends IrisMockTestCase {
   private static final Map<String, String> MISCONFIGURED =
         ImmutableMap.of(DeviceAdvancedCapability.PairingMisconfiguredException.CODE_PAIRING_MISCONFIGURED, "Unable to fully configure device");

   @Inject CapabilityRegistry registry;
   @Inject InMemoryPlatformMessageBus messages;
   @Inject DeviceDAO mockDeviceDao;
   @Inject PlacePopulationCacheManager mockPopulationCacheMgr;

   private DeviceDriver driver;
   private Device device = Fixtures.createDevice();

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      driver =
            Drivers
               .builder()
               .withName("Test")
               .withMatcher(Predicates.alwaysTrue())
               .withPopulations(ImmutableList.<String>of(Population.NAME_GENERAL))
               .addCapabilityDefinition(registry.getCapabilityDefinitionByNamespace(DeviceCapability.NAMESPACE))
               .addCapabilityDefinition(registry.getCapabilityDefinitionByNamespace(DeviceAdvancedCapability.NAMESPACE))
               .create(true);
      EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
      EasyMock.expect(mockDeviceDao.save(EasyMock.anyObject(Device.class))).andAnswer(() -> ((Device) EasyMock.getCurrentArguments()[0]).copy()).anyTimes();
      mockDeviceDao.updateDriverState(EasyMock.anyObject(Device.class), EasyMock.anyObject(DeviceDriverStateHolder.class));
      EasyMock.expectLastCall().anyTimes();
   }

   @Test
   public void testErrorsChangeCarriesDriverState() throws Exception {
      device.setState(Device.STATE_ACTIVE_SUPPORTED);
      PlatformDeviceDriverContext context = context();

      context.setAttributeValue(DeviceAdvancedCapability.KEY_ERRORS, MISCONFIGURED);
      context.commit();

      MessageBody changed = takeValueChange();
      assertEquals(DeviceAdvancedCapability.DRIVERSTATE_ACTIVE, DeviceAdvancedCapability.getDriverstate(changed));
      assertEquals(MISCONFIGURED, DeviceAdvancedCapability.getErrors(changed));
   }

   @Test
   public void testDriverStateChangeCarriesErrors() throws Exception {
      // a lost device that was misconfigured comes back
      device.setState(Device.STATE_LOST_RECOVERABLE);
      PlatformDeviceDriverContext context = context();
      context.setAttributeValue(DeviceAdvancedCapability.KEY_ERRORS, MISCONFIGURED);
      context.commit();
      takeValueChange();

      context.setAttributeValue(DeviceAdvancedCapability.KEY_DRIVERSTATE, DeviceAdvancedCapability.DRIVERSTATE_ACTIVE);
      context.commit();

      // without the errors, a listener would take this for a device with no errors
      MessageBody changed = takeValueChange();
      assertEquals(DeviceAdvancedCapability.DRIVERSTATE_ACTIVE, DeviceAdvancedCapability.getDriverstate(changed));
      assertEquals(MISCONFIGURED, DeviceAdvancedCapability.getErrors(changed));
   }

   @Test
   public void testDriverStateChangeWithoutErrors() throws Exception {
      device.setState(Device.STATE_ACTIVE_PROVISIONING);
      PlatformDeviceDriverContext context = context();

      context.setAttributeValue(DeviceAdvancedCapability.KEY_DRIVERSTATE, DeviceAdvancedCapability.DRIVERSTATE_ACTIVE);
      context.commit();

      MessageBody changed = takeValueChange();
      assertEquals(DeviceAdvancedCapability.DRIVERSTATE_ACTIVE, DeviceAdvancedCapability.getDriverstate(changed));
      assertFalse(changed.getAttributes().containsKey(DeviceAdvancedCapability.ATTR_ERRORS));
   }

   @Test
   public void testOtherChangesLeavePairingStateOut() throws Exception {
      device.setState(Device.STATE_ACTIVE_SUPPORTED);
      PlatformDeviceDriverContext context = context();
      context.setAttributeValue(DeviceAdvancedCapability.KEY_ERRORS, MISCONFIGURED);
      context.commit();
      takeValueChange();

      context.setAttributeValue(DeviceCapability.KEY_NAME, "Renamed");
      context.commit();

      MessageBody changed = takeValueChange();
      assertEquals("Renamed", changed.getAttributes().get(DeviceCapability.ATTR_NAME));
      assertFalse(changed.getAttributes().containsKey(DeviceAdvancedCapability.ATTR_DRIVERSTATE));
      assertFalse(changed.getAttributes().containsKey(DeviceAdvancedCapability.ATTR_ERRORS));
   }

   private PlatformDeviceDriverContext context() {
      replay();
      return new PlatformDeviceDriverContext(device, driver, mockPopulationCacheMgr);
   }

   // read through the same coercing getters the pairing device's listener uses
   private MessageBody takeValueChange() throws Exception {
      PlatformMessage message = messages.take();
      assertEquals(Capability.EVENT_VALUE_CHANGE, message.getMessageType());
      return message.getValue();
   }
}
