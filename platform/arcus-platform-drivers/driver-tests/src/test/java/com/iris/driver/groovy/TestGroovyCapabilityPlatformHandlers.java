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
package com.iris.driver.groovy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.service.executor.DefaultDriverExecutor;
import com.iris.driver.service.executor.DriverExecutors;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.model.Fixtures;


/**
 *
 */
@RunWith(Parameterized.class)
public class TestGroovyCapabilityPlatformHandlers extends GroovyDriverTestCase {
   private String driverFile;
   private DeviceDriver driver;
   private DeviceDriverContext context;

   @Parameters(name="{0}")
   public static Iterable<Object []> files() {
      return Arrays.asList(
            new Object [] { "PlatformMessageHandlerObjectsCapability.driver" },
            new Object [] { "PlatformMessageHandlerStringsCapability.driver" }
      );
   }

   public TestGroovyCapabilityPlatformHandlers(String driverFile) {
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
   public void testMatchSetAttributes() throws Exception {
      PlatformMessage message =
         PlatformMessage
            .builder()
            .from(Fixtures.createClientAddress())
            .to(Fixtures.createDeviceAddress())
            .withPayload(MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, Collections.<String, Object>singletonMap(DoorLockCapability.ATTR_LOCKSTATE, DoorLockCapability.LOCKSTATE_LOCKED)))
            .create();

      driver.handlePlatformMessage(message, context);
      assertEquals(true, context.getVariable("handledByNamespace"));
      assertEquals(true, context.getVariable("handledByWildcard"));
   }

   @Test
   public void testLongNameMatch() throws Exception {
      PlatformMessage message =
         PlatformMessage
            .builder()
            .from(Fixtures.createClientAddress())
            .to(Fixtures.createDeviceAddress())
            .withPayload(MessageBody.buildMessage(DoorLockCapability.AuthorizePersonRequest.NAME, new HashMap<String,Object>()))
            .create();

      driver.handlePlatformMessage(message, context);
      assertEquals("doorlock:AuthorizePerson", context.getVariable("handledBy"));
   }

   @Test
   public void testShortNameMatch() throws Exception {
      PlatformMessage message =
         PlatformMessage
            .builder()
            .from(Fixtures.createClientAddress())
            .to(Fixtures.createDeviceAddress())
            .withPayload(MessageBody.buildMessage(DoorLockCapability.DeauthorizePersonRequest.NAME, new HashMap<String,Object>()))
            .create();

      driver.handlePlatformMessage(message, context);
      assertEquals("DeauthorizePerson", context.getVariable("handledBy"));
   }

   @Test
   public void testWildcardMatch() throws Exception {
      PlatformMessage message =
         PlatformMessage
            .builder()
            .from(Fixtures.createClientAddress())
            .to(Fixtures.createDeviceAddress())
            .withPayload(MessageBody.buildMessage("doorlock:ListPins", new HashMap<String,Object>()))
            .create();

      driver.handlePlatformMessage(message, context);
      assertEquals("wildcard", context.getVariable("handledBy"));
   }

   @Test
   public void testWrongNamespace() throws Exception {
      PlatformMessage message =
         PlatformMessage
            .builder()
            .from(Fixtures.createClientAddress())
            .to(Fixtures.createDeviceAddress())
            .withPayload(MessageBody.buildMessage("swit:switch", new HashMap<String,Object>()))
            .create();

      DriverExecutors.dispatch(message, new DefaultDriverExecutor(driver, context, null, 100));
      assertEquals(null, context.getVariable("handledBy"));
   }

   @Test
   public void testNoMatch() throws Exception {
      PlatformMessage message =
         PlatformMessage
            .builder()
            .from(Fixtures.createClientAddress())
            .to(Fixtures.createDeviceAddress())
            .withPayload(MessageBody.buildMessage("vendor:Magic", new HashMap<String,Object>()))
            .create();

      DriverExecutors.dispatch(message, new DefaultDriverExecutor(driver, context, null, 100));
      assertEquals(null, context.getVariable("handledBy"));
   }

}

