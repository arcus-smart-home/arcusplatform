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
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.model.Fixtures;


/**
 *
 */
@RunWith(Parameterized.class)
public class TestGroovyDriverPlatformHandlers extends GroovyDriverTestCase {
   private String driverFile;
   private DeviceDriver driver;
   private DeviceDriverContext context;

   @Parameters(name="{0}")
   public static Iterable<Object []> files() {
      return Arrays.asList(
            new Object [] { "PlatformMessageHandlerObjects.driver" },
            new Object [] { "PlatformMessageHandlerStrings.driver" }
      );
   }

   public TestGroovyDriverPlatformHandlers(String driverFile) {
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
   public void testExactMatch() throws Exception {
      PlatformMessage message =
         PlatformMessage
            .builder()
            .from(Fixtures.createClientAddress())
            .to(Fixtures.createDeviceAddress())
            .withPayload(Capability.GetAttributesRequest.builder().build())
            .create();

      driver.handlePlatformMessage(message, context);
      assertEquals("exact", context.getVariable("handledBy"));
   }

   @Test
   public void testNamespaceMatch() throws Exception {
      PlatformMessage message =
         PlatformMessage
            .builder()
            .from(Fixtures.createClientAddress())
            .to(Fixtures.createDeviceAddress())
            .withPayload(MessageBody.buildMessage("doorlock:AddPin", new HashMap<String,Object>()))
            .create();

      driver.handlePlatformMessage(message, context);
      assertEquals("namespace", context.getVariable("handledBy"));
      assertEquals(1, context.getVariable("eventCount"));
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

      driver.handlePlatformMessage(message, context);
      assertEquals("wildcard", context.getVariable("handledBy"));
   }

}

