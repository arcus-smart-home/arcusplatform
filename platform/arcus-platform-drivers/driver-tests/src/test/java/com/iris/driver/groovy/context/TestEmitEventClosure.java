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
package com.iris.driver.groovy.context;

import java.util.Date;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.groovy.GroovyDriverTestCase;
import com.iris.driver.service.executor.DefaultDriverExecutor;
import com.iris.driver.service.executor.DriverExecutor;
import com.iris.driver.service.executor.DriverExecutors;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.model.Fixtures;

@RunWith(Parameterized.class)
public class TestEmitEventClosure extends GroovyDriverTestCase {
   private String driverFile;
   private DeviceDriver driver;
   private DeviceDriverContext context;
   private DriverExecutor executor;
   
   @Inject InMemoryPlatformMessageBus messages;

   @Parameters(name="{0}")
   public static Iterable<Object []> files() {
      return ImmutableList.of(
            new Object [] { "EmitEvents.driver" },
            new Object [] { "EmitEventsCapability.driver" },
            new Object [] { "EmitEventStrings.driver" },
            new Object [] { "EmitEventStringsCapability.driver" }
      );
   }

   public TestEmitEventClosure(String driverFile) {
      this.driverFile = driverFile;
   }

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      driver = factory.load(driverFile);
      context = new PlatformDeviceDriverContext(Fixtures.createDevice(), driver, mockPopulationCacheMgr);
      executor = new DefaultDriverExecutor(driver, context, null, 10);
   }

   @Test
   public void testWithoutParams() throws Exception {
      DriverExecutors.dispatch(DriverEvent.createScheduledEvent("NoParams", null, null, new Date()), executor);
      PlatformMessage message = messages.take();
      assertEquals(DeviceCapability.DeviceConnectedEvent.NAME, message.getMessageType());
      assertEquals(ImmutableMap.of(), message.getValue().getAttributes());
   }

   @Test
   public void testWithParams() throws Exception {
      DriverExecutors.dispatch(DriverEvent.createScheduledEvent("WithParams", null, null, new Date()), executor);
      PlatformMessage message = messages.take();
      assertEquals(DeviceCapability.DeviceDisconnectedEvent.NAME, message.getMessageType());
      assertEquals(ImmutableMap.of("param", "value"), message.getValue().getAttributes());
   }

}

