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
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.event.DriverEvent;
import com.iris.messages.capability.DeviceCapability;


/**
 * Tests loading the generic events: added, connected,
 * disconnected, removed.
 */
@RunWith(Parameterized.class)
public class TestGroovyDriverHandlers extends GroovyDriverTestCase {
   private DeviceDriver driver;
   private DeviceDriverContext context;
   
   private String curFile;
   private int curExpectedEventCount;
   
   @Parameters
   public static Collection<Object[]> data() {
       return Arrays.asList(new Object[][] {
                { "DriverEventHandlerWithCapability2.driver", 3 }, { "DriverEventHandlerWithCapability.driver", 2 }, { "DriverEventHandler.driver", 1 }  
          });
   }
   
   public TestGroovyDriverHandlers(String file, int count) {
	   this.curFile = file;
	   this.curExpectedEventCount = count;			   
   }
   
  

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      driver = factory.load(curFile);
      context = new PlatformDeviceDriverContext(createDevice(driver), driver, mockPopulationCacheMgr);
   }

   @Test
   public void testOnAdded() throws Exception {
      driver.handleDriverEvent(DriverEvent.createAssociated(AttributeMap.emptyMap()), context);
      assertEquals("onAdded", context.getVariable("handledBy"));
      assertEquals(curExpectedEventCount, context.getVariable("eventCount"));
   }

   @Test
   public void testOnAddedWithAttributes() throws Exception {
      AttributeMap attributes = AttributeMap.newMap();
      attributes.set(DeviceCapability.KEY_NAME, "Name");
      attributes.set(DeviceCapability.KEY_MODEL, "Model");
      driver.handleDriverEvent(DriverEvent.createAssociated(attributes), context);
      assertEquals("onAdded", context.getVariable("handledBy"));
      assertEquals("Name", context.getVariable("name"));
      assertEquals("Model", context.getVariable("model"));
   }

   @Test
   public void testOnConnected() throws Exception {
      driver.handleDriverEvent(DriverEvent.createConnected(0), context);
      assertEquals("onConnected", context.getVariable("handledBy"));
      assertEquals(curExpectedEventCount, context.getVariable("eventCount"));
   }

   @Test
   public void testOnDisconnected() throws Exception {
      driver.handleDriverEvent(DriverEvent.createDisconnected(0), context);
      assertEquals("onDisconnected", context.getVariable("handledBy"));
      assertEquals(curExpectedEventCount, context.getVariable("eventCount"));
   }

   @Test
   public void testOnRemoved() throws Exception {
      driver.handleDriverEvent(DriverEvent.createDisassociated(), context);
      assertEquals("onRemoved", context.getVariable("handledBy"));
      assertEquals(curExpectedEventCount, context.getVariable("eventCount"));
   }

}

