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
package com.iris.driver.groovy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableList;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.DeviceMockCapability;
import com.iris.messages.model.Fixtures;

@RunWith(Parameterized.class)
public class TestConnectDisconnectHandlers extends GroovyDriverTestCase {
   private String driverFile;
   private DeviceDriver driver;
   private DeviceDriverContext context;

   @Parameters(name="{0}")
   public static Iterable<Object []> files() {
      return ImmutableList.of(
            new Object [] { "ConnectDisconnect.driver" },
            new Object [] { "ConnectDisconnectCapability.driver" }
      );
   }

   public TestConnectDisconnectHandlers(String driverFile) {
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
   public void testOnConnected() throws Exception {
      context.setDisconnected();
      PlatformMessage message = 
            PlatformMessage
               .builder()
               .from(Fixtures.createClientAddress())
               .to(Fixtures.createDeviceAddress())
               .isRequestMessage(true)
               .withPayload(DeviceMockCapability.ConnectRequest.instance())
               .create();
      driver.handlePlatformMessage(message, context);
      assertEquals(true, context.getVariable("onConnected"));
      assertEquals(null, context.getVariable("onDisconnected"));
      assertEquals(1, context.getVariable("eventCount"));
      
   }

   @Test
   public void testOnDisconnected() throws Exception {
      context.setConnected();
      PlatformMessage message = 
            PlatformMessage
               .builder()
               .from(Fixtures.createClientAddress())
               .to(Fixtures.createDeviceAddress())
               .isRequestMessage(true)
               .withPayload(DeviceMockCapability.DisconnectRequest.instance())
               .create();
      driver.handlePlatformMessage(message, context);
      assertEquals(null, context.getVariable("onConnected"));
      assertEquals(true, context.getVariable("onDisconnected"));
      assertEquals(1, context.getVariable("eventCount"));
   }

}

