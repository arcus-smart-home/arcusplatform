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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DevicePowerCapability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.model.Fixtures;


/**
 *
 */
@RunWith(Parameterized.class)
public class TestMultiInstance extends GroovyDriverTestCase {
   private String driverFile;
   private DeviceDriver driver;
   private DeviceDriverContext context;
   
   @Inject InMemoryPlatformMessageBus bus;

   @Parameters(name="{0}")
   public static List<Object []> files() {
      return Arrays.<Object[]>asList(
            new Object [] { "MultiInstanceStrings.driver" },
            new Object [] { "MultiInstanceObjects.driver" }
      );
   }

   public TestMultiInstance(String driverFile) {
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
   public void testCaps() {
      assertEquals(
            ImmutableSet.of("base", "dev", "devadv", "devconn"), 
            driver.getBaseAttributes().get(Capability.KEY_CAPS)
      );
   }
   
   @Test
   public void testInstances() {
      Map<String, Set<String>> instances = driver.getBaseAttributes().get(Capability.KEY_INSTANCES);
      assertEquals(
            ImmutableSet.of("plug1", "plug2", "lock"),
            instances.keySet()
      );
      assertEquals(
            ImmutableSet.of("swit", "devpow"),
            instances.get("plug1")
      );
      assertEquals(
            ImmutableSet.of("swit", "devpow"),
            instances.get("plug2")
      );
      assertEquals(
            ImmutableSet.of("doorlock"),
            instances.get("lock")
      );
   }
   
   @Test
   public void testBaseAttributes() {
      assertEquals("line", driver.getBaseAttributes().get(DevicePowerCapability.KEY_SOURCE.instance("plug1")));
      assertEquals("battery", driver.getBaseAttributes().get(DevicePowerCapability.KEY_SOURCE.instance("plug2")));
      assertEquals(true, driver.getBaseAttributes().get(DevicePowerCapability.KEY_LINECAPABLE.instance("plug1")));
      assertEquals(false, driver.getBaseAttributes().get(DevicePowerCapability.KEY_LINECAPABLE.instance("plug2")));
      assertEquals("deadbolt", driver.getBaseAttributes().get(DoorLockCapability.KEY_TYPE.instance("lock")));
   }
   
   @Test
   public void testGetAttributes() {
      PlatformMessage message =
            PlatformMessage
               .builder()
               .to(context.getDriverAddress())
               .from(Fixtures.createClientAddress())
               .isRequestMessage(true)
               .withPayload(Capability.CMD_GET_ATTRIBUTES)
               .create();
      
      driver.handlePlatformMessage(message, context);
      
      MessageBody response = bus.poll().getValue();
      assertEquals(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, response.getMessageType());
      assertEquals("line", response.getAttributes().get(DevicePowerCapability.KEY_SOURCE.instance("plug1").getName()));
      assertEquals("battery", response.getAttributes().get(DevicePowerCapability.KEY_SOURCE.instance("plug2").getName()));
      assertEquals(true, response.getAttributes().get(DevicePowerCapability.KEY_LINECAPABLE.instance("plug1").getName()));
      assertEquals(false, response.getAttributes().get(DevicePowerCapability.KEY_LINECAPABLE.instance("plug2").getName()));
      assertEquals("deadbolt", response.getAttributes().get("doorlock:type:lock"));
   }
   
   @Test
   public void testSetAttributes() {
      PlatformMessage message =
            PlatformMessage
               .builder()
               .to(context.getDriverAddress())
               .from(Fixtures.createClientAddress())
               .isRequestMessage(true)
               .withPayload(
                     Capability.CMD_SET_ATTRIBUTES,
                     ImmutableMap.of(
                           "swit:state:plug1", "ON",
                           "swit:state:plug2", "OFF"
                     )
               )
               .create();
      
      driver.handlePlatformMessage(message, context);
      
      MessageBody response = bus.poll().getValue();
      assertEquals(MessageBody.emptyMessage(), response);
      
      MessageBody valueChange = bus.poll().getValue();
      assertEquals(Capability.EVENT_VALUE_CHANGE, valueChange.getMessageType());
      assertEquals("ON", valueChange.getAttributes().get("swit:state:plug1"));
      assertEquals("OFF", valueChange.getAttributes().get("swit:state:plug2"));
   }
   
   @Test
   public void testCaseStatement() {
      PlatformMessage message =
            PlatformMessage
               .builder()
               .to(context.getDriverAddress())
               .from(Fixtures.createClientAddress())
               .isRequestMessage(true)
               .withPayload(
                     Capability.CMD_SET_ATTRIBUTES,
                     ImmutableMap.of(
                           "swit:state:plug1", "ON",
                           "swit:state:plug2", "OFF"
                     )
               )
               .create();
      
      driver.handlePlatformMessage(message, context);
      
      assertEquals("ON", context.getVariable("plug1"));
      assertEquals("OFF", context.getVariable("state"));
   }
   
   @Test
   public void testCustomMethodInstanceMatch() {
      PlatformMessage message =
            PlatformMessage
               .builder()
               .to(context.getDriverAddress())
               .from(Fixtures.createClientAddress())
               .isRequestMessage(true)
               .withPayload(
                  MessageBody
                     .messageBuilder(DoorLockCapability.AuthorizePersonRequest.NAME + ":lock")
                     .withAttribute(DoorLockCapability.AuthorizePersonRequest.ATTR_PERSONID, UUID.randomUUID().toString())
                     .create()
               )
               .create();
      
      driver.handlePlatformMessage(message, context);
      
      assertEquals("doorlock:AuthorizePerson:lock", context.getVariable("handledBy"));
   }

   @Test
   public void testCustomMethodMatch() {
      PlatformMessage message =
            PlatformMessage
               .builder()
               .to(context.getDriverAddress())
               .from(Fixtures.createClientAddress())
               .isRequestMessage(true)
               .withPayload(
                     DoorLockCapability.DeauthorizePersonRequest
	                     .builder()
	                     .withPersonId(UUID.randomUUID().toString())
	                     .build()
               )
               .create();
      
      driver.handlePlatformMessage(message, context);
      
      assertEquals("doorlock:DeauthorizePerson", context.getVariable("handledBy"));
   }

}

