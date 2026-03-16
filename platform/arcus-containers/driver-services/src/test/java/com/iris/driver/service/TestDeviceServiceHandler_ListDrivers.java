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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.protocol.ipcd.IpcdDeviceDao;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverDefinition;
import com.iris.driver.service.executor.DriverExecutorRegistry;
import com.iris.driver.service.registry.DriverRegistry;
import com.iris.messages.MessageBody;
import com.iris.model.Version;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({
   DeviceDAO.class,
   HubDAO.class,
   IpcdDeviceDao.class,
   PersonDAO.class,
   PersonPlaceAssocDAO.class,
   DriverRegistry.class,
   DriverExecutorRegistry.class,
   DeviceService.class
})
@Modules({ InMemoryMessageModule.class })
public class TestDeviceServiceHandler_ListDrivers extends IrisMockTestCase {

   @Inject private DeviceDAO devDao;
   @Inject private HubDAO hubDao;
   @Inject private IpcdDeviceDao ipcdDevDao;
   @Inject private PersonDAO personDao;
   @Inject private PersonPlaceAssocDAO personPlaceAssocDao;
   @Inject private DeviceService service;
   @Inject private DriverRegistry driverRegistry;
   @Inject private InMemoryPlatformMessageBus platformBus;

   private DeviceServiceHandler handler;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      handler = new DeviceServiceHandler(
         platformBus, new DriverServiceConfig(), devDao, hubDao,
         ipcdDevDao, personDao, personPlaceAssocDao, service, null, driverRegistry
      );
   }

   @Override
   public void tearDown() throws Exception {
      verify();
      super.tearDown();
   }

   @Test
   public void testListDriversNoFilter() throws Exception {
      DeviceDriver driver1 = createMockDriver("ZWaveSwitchDriver", "1.0", "Z-Wave Switch", Collections.emptyList());
      DeviceDriver driver2 = createMockDriver("ZigBeeDimmerDriver", "2.1", "ZigBee Dimmer", Collections.emptyList());

      EasyMock.expect(driverRegistry.listDrivers())
         .andReturn(Arrays.asList(driver1, driver2));
      replay();

      MessageBody request = com.iris.messages.service.DeviceService.ListDriversRequest.builder().build();
      MessageBody response = handler.listDrivers(request);

      List<Map<String, Object>> drivers = (List<Map<String, Object>>) response.getAttributes().get("drivers");
      assertNotNull(drivers);
      assertEquals(2, drivers.size());
      assertEquals("ZWaveSwitchDriver", drivers.get(0).get("name"));
      assertEquals("ZigBeeDimmerDriver", drivers.get(1).get("name"));
   }

   @Test
   public void testListDriversWithPopulationFilter() throws Exception {
      DeviceDriver driver1 = createMockDriver("ZWaveSwitchDriver", "1.0", "Z-Wave Switch", Arrays.asList("general"));
      DeviceDriver driver2 = createMockDriver("ZigBeeDimmerDriver", "2.1", "ZigBee Dimmer", Arrays.asList("beta"));

      EasyMock.expect(driverRegistry.listDrivers())
         .andReturn(Arrays.asList(driver1, driver2));
      replay();

      MessageBody request = com.iris.messages.service.DeviceService.ListDriversRequest.builder()
         .withPopulation("general")
         .build();
      MessageBody response = handler.listDrivers(request);

      List<Map<String, Object>> drivers = (List<Map<String, Object>>) response.getAttributes().get("drivers");
      assertNotNull(drivers);
      assertEquals(1, drivers.size());
      assertEquals("ZWaveSwitchDriver", drivers.get(0).get("name"));
   }

   @Test
   public void testListDriversUniversalDriverIncludedWithFilter() throws Exception {
      DeviceDriver driver1 = createMockDriver("ZWaveSwitchDriver", "1.0", "Z-Wave Switch", Arrays.asList("general"));
      DeviceDriver driver2 = createMockDriver("FallbackDriver", "1.0", "Fallback", Collections.emptyList());

      EasyMock.expect(driverRegistry.listDrivers())
         .andReturn(Arrays.asList(driver1, driver2));
      replay();

      MessageBody request = com.iris.messages.service.DeviceService.ListDriversRequest.builder()
         .withPopulation("general")
         .build();
      MessageBody response = handler.listDrivers(request);

      List<Map<String, Object>> drivers = (List<Map<String, Object>>) response.getAttributes().get("drivers");
      assertNotNull(drivers);
      assertEquals(2, drivers.size());
   }

   @Test
   public void testListDriversEmpty() throws Exception {
      EasyMock.expect(driverRegistry.listDrivers())
         .andReturn(Collections.emptyList());
      replay();

      MessageBody request = com.iris.messages.service.DeviceService.ListDriversRequest.builder().build();
      MessageBody response = handler.listDrivers(request);

      List<Map<String, Object>> drivers = (List<Map<String, Object>>) response.getAttributes().get("drivers");
      assertNotNull(drivers);
      assertTrue(drivers.isEmpty());
   }

   @Test
   public void testListDriversResponseStructure() throws Exception {
      DeviceDriver driver = createMockDriver("ZWaveSwitchDriver", "1.0", "Z-Wave Switch", Arrays.asList("general"));

      EasyMock.expect(driverRegistry.listDrivers())
         .andReturn(Arrays.asList(driver));
      replay();

      MessageBody request = com.iris.messages.service.DeviceService.ListDriversRequest.builder().build();
      MessageBody response = handler.listDrivers(request);

      List<Map<String, Object>> drivers = (List<Map<String, Object>>) response.getAttributes().get("drivers");
      assertNotNull(drivers);
      assertEquals(1, drivers.size());

      Map<String, Object> driverInfo = drivers.get(0);
      assertEquals("ZWaveSwitchDriver", driverInfo.get("name"));
      assertEquals("1.0", driverInfo.get("version"));
      assertEquals("Z-Wave Switch", driverInfo.get("description"));
      assertNotNull(driverInfo.get("populations"));
      assertNotNull(driverInfo.get("capabilities"));
   }

   private DeviceDriver createMockDriver(String name, String version, String description, List<String> populations) {
      DeviceDriverDefinition def = DeviceDriverDefinition.builder()
         .withName(name)
         .withVersion(Version.fromRepresentation(version))
         .withDescription(description)
         .withPopulations(populations)
         .create();

      DeviceDriver driver = EasyMock.createMock(DeviceDriver.class);
      EasyMock.expect(driver.getDefinition()).andReturn(def).anyTimes();
      EasyMock.expect(driver.getBaseAttributes()).andReturn(AttributeMap.newMap()).anyTimes();
      EasyMock.replay(driver);
      return driver;
   }
}
