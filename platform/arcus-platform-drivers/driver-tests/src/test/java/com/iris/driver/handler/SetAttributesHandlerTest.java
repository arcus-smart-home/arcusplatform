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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.model.Fixtures;

public class SetAttributesHandlerTest extends GroovyDriverTestCase {
   private static final String SET_ATTRIBUTES_ERROR = Capability.CMD_SET_ATTRIBUTES + "Error";

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
      driver.handlePlatformMessage(setAttributes(ThermostatCapability.ATTR_HEATSETPOINT, null), context);

      assertNull(context.getVariable("setpoint"));
      assertEquals(SET_ATTRIBUTES_ERROR, platformBus.take().getMessageType());
   }

   @Test
   public void testDoubleReachesTheDriver() throws Exception {
      driver.handlePlatformMessage(setAttributes(ThermostatCapability.ATTR_HEATSETPOINT, 21.0), context);

      assertEquals(21.0, context.getVariable("setpoint"));
   }

   @Test
   public void testNameIsSet() throws Exception {
      driver.handlePlatformMessage(setAttributes(DeviceCapability.ATTR_NAME, "Kitchen Light"), context);

      assertEquals("Kitchen Light", context.getAttributeValue(DeviceCapability.KEY_NAME));
      assertEquals(MessageBody.emptyMessage().getMessageType(), platformBus.take().getMessageType());
   }

   @Test
   public void testBlankNameIsRefused() throws Exception {
      String name = context.getAttributeValue(DeviceCapability.KEY_NAME);
      assertNotNull(name);

      for(String blank: new String[] { "", "   ", "\t" }) {
         driver.handlePlatformMessage(setAttributes(DeviceCapability.ATTR_NAME, blank), context);

         assertEquals(name, context.getAttributeValue(DeviceCapability.KEY_NAME));
         assertErrors(platformBus.take(), 1);
      }
   }

   @Test
   public void testNullNameIsRefused() throws Exception {
      String name = context.getAttributeValue(DeviceCapability.KEY_NAME);

      driver.handlePlatformMessage(setAttributes(DeviceCapability.ATTR_NAME, null), context);

      assertEquals(name, context.getAttributeValue(DeviceCapability.KEY_NAME));
      assertErrors(platformBus.take(), 1);
   }

   @Test
   public void testOtherAttributesApplyWhenTheNameIsRefused() throws Exception {
      String name = context.getAttributeValue(DeviceCapability.KEY_NAME);
      Map<String, Object> attributes = new HashMap<>();
      attributes.put(DeviceCapability.ATTR_NAME, "");
      attributes.put(ThermostatCapability.ATTR_HEATSETPOINT, 21.0);

      driver.handlePlatformMessage(setAttributes(attributes), context);

      assertEquals(name, context.getAttributeValue(DeviceCapability.KEY_NAME));
      assertEquals(21.0, context.getVariable("setpoint"));
      assertErrors(platformBus.take(), 1);
   }

   /** Drivers can set the name themselves, and a hub can report it, without a SetAttributes. */
   @Test
   public void testContextIgnoresABlankName() throws Exception {
      String name = context.getAttributeValue(DeviceCapability.KEY_NAME);

      context.setAttributeValue(DeviceCapability.KEY_NAME, "  ");
      assertEquals(name, context.getAttributeValue(DeviceCapability.KEY_NAME));

      context.setAttributeValue(DeviceCapability.KEY_NAME, "Hall Light");
      assertEquals("Hall Light", context.getAttributeValue(DeviceCapability.KEY_NAME));
   }

   /** The response, and nothing after it that changes the name. */
   private void assertErrors(PlatformMessage response, int count) {
      assertEquals(SET_ATTRIBUTES_ERROR, response.getMessageType());
      assertEquals(count, ((List<?>) response.getValue().getAttributes().get("errors")).size());
      for(PlatformMessage message = platformBus.poll(); message != null; message = platformBus.poll()) {
         assertFalse(message.getValue().getAttributes().containsKey(DeviceCapability.ATTR_NAME));
      }
   }

   private PlatformMessage setAttributes(String name, Object value) {
      return setAttributes(Collections.<String, Object>singletonMap(name, value));
   }

   private PlatformMessage setAttributes(Map<String, Object> attributes) {
      return PlatformMessage
            .builder()
            .from(Fixtures.createClientAddress())
            .to(Fixtures.createDeviceAddress())
            .isRequestMessage(true)
            .withPayload(MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, attributes))
            .create();
   }

}
