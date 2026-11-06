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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.protocol.ipcd.IpcdDeviceDao;
import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverDefinition;
import com.iris.driver.MatchCriterion;
import com.iris.driver.MatchCriterion.Comparison;
import com.iris.driver.service.executor.DriverExecutorRegistry;
import com.iris.driver.service.registry.DriverRegistry;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.service.DeviceService.ListDriversRequest;
import com.iris.messages.service.DeviceService.ListDriversResponse;
import com.iris.messages.type.DriverMatchCriterion;
import com.iris.messages.type.DriverMatcher;
import com.iris.messages.type.DriverSummary;
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
public class DeviceServiceHandlerListDriversTest extends IrisMockTestCase {

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
   public void testNoPopulationListsEveryVersionByNameNewestFirst() throws Exception {
      DeviceDriver switchOld = driver("Switch", "2.9", "general");
      DeviceDriver switchNew = driver("Switch", "2.10", "general");
      DeviceDriver retired = driver("RetiredDimmer", "1.0");
      DeviceDriver beta = driver("BetaSensor", "1.0", "beta");
      EasyMock.expect(driverRegistry.listDrivers()).andReturn(Arrays.asList(switchOld, retired, switchNew, beta));
      replay();

      List<DriverSummary> drivers = listDrivers(null);

      assertEquals(Arrays.asList("BetaSensor 1.0", "RetiredDimmer 1.0", "Switch 2.10", "Switch 2.9"), ids(drivers));
   }

   @Test
   public void testBlankPopulationIsTheSameAsNone() throws Exception {
      EasyMock.expect(driverRegistry.listDrivers()).andReturn(Arrays.asList(driver("RetiredDimmer", "1.0")));
      replay();

      assertEquals(Arrays.asList("RetiredDimmer 1.0"), ids(listDrivers("")));
   }

   @Test
   public void testPopulationListsWhatTheRegistryPicksForEachName() throws Exception {
      DeviceDriver switchOld = driver("Switch", "2.9", "general");
      DeviceDriver switchNew = driver("Switch", "2.10", "general");
      DeviceDriver retired = driver("RetiredDimmer", "1.0");
      DeviceDriver beta = driver("BetaSensor", "1.0", "beta");
      EasyMock.expect(driverRegistry.listDrivers()).andReturn(Arrays.asList(switchOld, retired, switchNew, beta));
      // once per name, with no hub firmware limit
      EasyMock.expect(driverRegistry.loadDriverByName("general", "Switch", Integer.MAX_VALUE)).andReturn(switchNew);
      EasyMock.expect(driverRegistry.loadDriverByName("general", "RetiredDimmer", Integer.MAX_VALUE)).andReturn(null);
      EasyMock.expect(driverRegistry.loadDriverByName("general", "BetaSensor", Integer.MAX_VALUE)).andReturn(null);
      replay();

      assertEquals(Arrays.asList("Switch 2.10"), ids(listDrivers("general")));
   }

   @Test
   public void testNoDrivers() throws Exception {
      EasyMock.expect(driverRegistry.listDrivers()).andReturn(Collections.<DeviceDriver>emptyList());
      replay();

      assertTrue(listDrivers(null).isEmpty());
   }

   @Test
   public void testSummary() throws Exception {
      DeviceDriverDefinition definition = DeviceDriverDefinition.builder()
         .withName("Switch")
         .withVersion(Version.fromRepresentation("2.10"))
         .withDescription("A switch")
         .withHash("abc123")
         .withPopulations(Arrays.asList("general", "beta"))
         .addCapability(capability("swit"))
         .addCapability(capability("devpow"))
         .addMatcher(Arrays.asList(
            new MatchCriterion("ZWAV:Manufacturer", Comparison.EQUALS, "99"),
            new MatchCriterion("ZWAV:ProductId", Comparison.IN, "[1, 2]")
         ))
         .addMatcher(Arrays.asList(new MatchCriterion("dev:model", Comparison.REGEX, "^3315-")))
         .create();
      EasyMock.expect(driverRegistry.listDrivers()).andReturn(Arrays.asList(driver(definition)));
      replay();

      DriverSummary summary = listDrivers(null).get(0);

      assertEquals("Switch", summary.getName());
      assertEquals("2.10", summary.getVersion());
      assertEquals("A switch", summary.getDescription());
      assertEquals("abc123", summary.getHash());
      assertEquals(Arrays.asList("general", "beta"), summary.getPopulations());
      assertEquals(Arrays.asList("swit", "devpow"), summary.getCapabilities());

      List<Map<String, Object>> matchers = summary.getMatchers();
      assertEquals(2, matchers.size());
      List<Map<String, Object>> first = new DriverMatcher(matchers.get(0)).getCriteria();
      assertEquals(2, first.size());
      assertCriterion("ZWAV:Manufacturer", DriverMatchCriterion.COMPARISON_EQUALS, "99", first.get(0));
      assertCriterion("ZWAV:ProductId", DriverMatchCriterion.COMPARISON_IN, "[1, 2]", first.get(1));
      List<Map<String, Object>> second = new DriverMatcher(matchers.get(1)).getCriteria();
      assertEquals(1, second.size());
      assertCriterion("dev:model", DriverMatchCriterion.COMPARISON_REGEX, "^3315-", second.get(0));
   }

   @Test
   public void testDriverWithoutMatchersHasAnEmptyList() throws Exception {
      EasyMock.expect(driverRegistry.listDrivers()).andReturn(Arrays.asList(driver("Fallback", "1.0")));
      replay();

      assertEquals(Collections.emptyList(), listDrivers(null).get(0).getMatchers());
   }

   @Test
   public void testHandleRequest() throws Exception {
      EasyMock.expect(driverRegistry.listDrivers()).andReturn(Arrays.asList(driver("Switch", "2.10", "general")));
      replay();

      PlatformMessage request = PlatformMessage
         .buildRequest(
            ListDriversRequest.builder().build(),
            Address.clientAddress("test", "session"),
            Address.platformService(DeviceServiceHandler.NAME)
         )
         .create();
      MessageBody response = handler.handleRequest(request);

      assertEquals(ListDriversResponse.NAME, response.getMessageType());
      assertEquals(1, ListDriversResponse.getDrivers(response).size());
   }

   private List<DriverSummary> listDrivers(String population) {
      MessageBody response = handler.listDrivers(ListDriversRequest.builder().withPopulation(population).build());
      List<DriverSummary> drivers = new ArrayList<>();
      for(Map<String, Object> attributes : ListDriversResponse.getDrivers(response)) {
         drivers.add(new DriverSummary(attributes));
      }
      return drivers;
   }

   private static List<String> ids(List<DriverSummary> drivers) {
      List<String> ids = new ArrayList<>();
      for(DriverSummary driver : drivers) {
         ids.add(driver.getName() + " " + driver.getVersion());
      }
      return ids;
   }

   private static void assertCriterion(String attribute, String comparison, String value, Map<String, Object> attributes) {
      DriverMatchCriterion criterion = new DriverMatchCriterion(attributes);
      assertEquals(attribute, criterion.getAttribute());
      assertEquals(comparison, criterion.getComparison());
      assertEquals(value, criterion.getValue());
   }

   private static CapabilityDefinition capability(String namespace) {
      return new CapabilityDefinition(namespace, namespace, null, namespace,
         Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
   }

   private static DeviceDriver driver(String name, String version, String... populations) {
      return driver(
         DeviceDriverDefinition.builder()
            .withName(name)
            .withVersion(Version.fromRepresentation(version))
            .withPopulations(Arrays.asList(populations))
            .create()
      );
   }

   private static DeviceDriver driver(DeviceDriverDefinition definition) {
      DeviceDriver driver = EasyMock.createMock(DeviceDriver.class);
      EasyMock.expect(driver.getDefinition()).andReturn(definition).anyTimes();
      EasyMock.replay(driver);
      return driver;
   }
}
