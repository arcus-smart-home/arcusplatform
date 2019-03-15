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
package com.iris.driver.groovy.scheduler;

import java.util.Arrays;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.event.DeviceConnectedEvent;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.event.ScheduledDriverEvent;
import com.iris.driver.groovy.GroovyDriverTestCase;
import com.iris.messages.model.Fixtures;

@RunWith(Parameterized.class)
public class TestSchedulerHandler extends GroovyDriverTestCase {
   private String driverFile;
   private DeviceDriver driver;
   private DeviceDriverContext context;

   @Parameters(name="{0}")
   public static Iterable<Object []> files() {
      return Arrays.asList(
            new Object [] { "SchedulerHandler.driver" },
            new Object [] { "SchedulerHandlerWithCapability.driver" }
      );
   }

   public TestSchedulerHandler(String driverFile) {
      this.driverFile = driverFile;
   }

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      driver = factory.load(driverFile);
      context = new PlatformDeviceDriverContext(createDevice(driver), driver, mockPopulationCacheMgr);
   }

   @Test
   public void testWildcardMatch() throws Exception {
      ScheduledDriverEvent event =
            DriverEvent.createScheduledEvent("wildcard", null, null, new Date(System.currentTimeMillis() - 1000));
      // note we're not actually going through the scheduler so the event will be delivered immediately
      driver.handleDriverEvent(event, context);
      assertEquals("wildcard", context.getVariable("handledBy"));
      assertEquals(1, context.getVariable("eventCount"));
   }

   @Test
   public void testNameMatch() throws Exception {
      ScheduledDriverEvent event =
            DriverEvent.createScheduledEvent("TestEvent", null, null, new Date());
      driver.handleDriverEvent(event, context);
      assertEquals("TestEvent", context.getVariable("handledBy"));
      assertEquals(1, context.getVariable("eventCount"));
   }
   
   @Test
   public void testStaticMethods() throws Exception {
      DeviceConnectedEvent event = DriverEvent.createConnected(0);
      driver.handleDriverEvent(event, context);
      assertEquals("onConnected", context.getVariable("handledBy"));
      assertEquals(1, context.getVariable("eventCount"));
   }

}

