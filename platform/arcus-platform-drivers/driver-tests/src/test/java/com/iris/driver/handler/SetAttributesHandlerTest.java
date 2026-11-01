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
package com.iris.driver.handler;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.groovy.GroovyDriverTestCase;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.model.Fixtures;

public class SetAttributesHandlerTest extends GroovyDriverTestCase {
   @Inject
   private InMemoryPlatformMessageBus platformBus;

   private DeviceDriver driver;
   private DeviceDriverContext context;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      driver = factory.load("SetAttributesNullDouble.driver");
      context = new PlatformDeviceDriverContext(createDevice(driver), driver, mockPopulationCacheMgr);
   }

   @Test
   public void testNullDoubleIsRejectedBeforeReachingTheDriver() throws Exception {
      driver.handlePlatformMessage(setHeatSetPoint(null), context);

      assertNull(context.getVariable("setpoint"));
      assertEquals(Capability.CMD_SET_ATTRIBUTES + "Error", platformBus.take().getMessageType());
   }

   @Test
   public void testDoubleReachesTheDriver() throws Exception {
      driver.handlePlatformMessage(setHeatSetPoint(21.0), context);

      assertEquals(21.0, context.getVariable("setpoint"));
   }

   private PlatformMessage setHeatSetPoint(Double value) {
      return PlatformMessage
            .builder()
            .from(Fixtures.createClientAddress())
            .to(Fixtures.createDeviceAddress())
            .isRequestMessage(true)
            .withPayload(MessageBody.buildMessage(
                  Capability.CMD_SET_ATTRIBUTES,
                  Collections.<String, Object>singletonMap(ThermostatCapability.ATTR_HEATSETPOINT, value)
            ))
            .create();
   }

}
