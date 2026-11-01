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
package com.iris.driver.groovy.customizer;

import org.junit.Before;
import org.junit.Test;

import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.groovy.GroovyDriverTestCase;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.ClockCapability;
import com.iris.messages.capability.DeviceMockCapability;
import com.iris.messages.model.Fixtures;

/**
 * Drivers name a capability with a bare word, and Clock is also a class Groovy
 * imports by default: {@link java.time.Clock}. The thermostat drivers depend on
 * it being the capability, and none of their tests exercise it.
 */
public class DriverCompilationCustomizerTest extends GroovyDriverTestCase {
   private DeviceDriver driver;
   private DeviceDriverContext context;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      driver = factory.load("ClockCapability.driver");
      context = new PlatformDeviceDriverContext(createDevice(driver), driver, mockPopulationCacheMgr);
   }

   @Test
   public void testBareClockDeclaresTheCapability() throws Exception {
      assertTrue(driver.getDefinition().getCapabilities().contains(
            registry.getCapabilityDefinitionByNamespace(ClockCapability.NAMESPACE)
      ));
   }

   @Test
   public void testBareClockReadsAndWritesAttributesFromHandlers() throws Exception {
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

      assertEquals(Integer.valueOf(7), context.getAttributeValue(ClockCapability.KEY_HOUR));
      assertEquals("read in a condition", Integer.valueOf(8), context.getAttributeValue(ClockCapability.KEY_MINUTE));
      assertEquals("written in a nested closure", Integer.valueOf(9), context.getAttributeValue(ClockCapability.KEY_SECOND));
      assertEquals("read in a method", Integer.valueOf(7), context.getAttributeValue(ClockCapability.KEY_DAY));
   }

}
