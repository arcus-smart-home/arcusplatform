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
package com.iris.driver.groovy.context;

import java.util.Collections;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.capability.registry.StaticCapabilityRegistryImpl;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.driver.DeviceDriverStateHolder;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.DeviceDriverDefinition;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.groovy.ClasspathResourceConnector;
import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.GroovyContextObject;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;

/**
 *
 */
@Mocks({PersonDAO.class, PersonPlaceAssocDAO.class, PlacePopulationCacheManager.class, PlaceDAO.class})
@Modules({ CapabilityRegistryModule.class })
public class TestGroovyCapabilityDefinition extends IrisMockTestCase {
   @Inject CapabilityRegistry registry;
   @Inject private PlacePopulationCacheManager mockPopulationCacheMgr;
   private GroovyScriptEngine engine;
   private Script script;
   private DeviceDriverContext context;
   private DriverBinding binding;

   private String accountId = String.valueOf(UUID.randomUUID());
   private String placeId = String.valueOf(UUID.randomUUID());

   protected CapabilityDefinition deviceCapability() {
      return registry.getCapabilityDefinitionByNamespace(DeviceCapability.NAMESPACE);
   }
   
   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      AttributeMap attributes = AttributeMap.newMap();
      attributes.set(DeviceCapability.KEY_DEVTYPEHINT, "device");
      attributes.set(DeviceCapability.KEY_MODEL, "model");
      attributes.set(DeviceCapability.KEY_VENDOR, "vendor");

      Device device = Fixtures.createDevice();
      device.setAccount(UUID.fromString(accountId));
      device.setPlace(UUID.fromString(placeId));
      device.setDevtypehint("device");
      device.setModel("model");
      device.setName("name");
      device.setVendor("vendor");

      engine = new GroovyScriptEngine(new ClasspathResourceConnector(TestGroovyCapabilityDefinition.class));
      context = new PlatformDeviceDriverContext(device, DeviceDriverDefinition.builder().withName("TestDriver").create(), new DeviceDriverStateHolder(attributes), mockPopulationCacheMgr);
      binding = new DriverBinding(new StaticCapabilityRegistryImpl(Collections.singleton(deviceCapability())), null);
      script = engine.createScript("TestGroovyCapabilityDefinition.gscript", binding);
      script.setProperty(DeviceCapability.NAME, new GroovyCapabilityDefinition(deviceCapability(), binding));
      script.run();

      GroovyContextObject.setContext(context);
   }

   @Override
   @After
   public void tearDown() throws Exception {
      // don't leak to the next test case
      GroovyContextObject.clearContext();
      super.tearDown();
   }

   @Test
   public void testGetCapability() {
      GroovyCapabilityDefinition context = (GroovyCapabilityDefinition) script.invokeMethod("getProperty", DeviceCapability.NAME);
      assertEquals(deviceCapability(), context.getDefinition());
   }

   @Test
   public void testGetAttribute() {
      assertEquals(accountId,  script.invokeMethod("getAttributeValue", new Object [] { DeviceCapability.NAME, "account" }));
      assertEquals("device",   script.invokeMethod("getAttributeValue", new Object [] { DeviceCapability.NAME, "devtypehint" }));
      assertEquals("model",    script.invokeMethod("getAttributeValue", new Object [] { DeviceCapability.NAME, "model" }));
      assertEquals("name",     script.invokeMethod("getAttributeValue", new Object [] { DeviceCapability.NAME, "name" }));
      assertEquals(placeId,    script.invokeMethod("getAttributeValue", new Object [] { DeviceCapability.NAME, "place" }));
      assertEquals("vendor",   script.invokeMethod("getAttributeValue", new Object [] { DeviceCapability.NAME, "vendor" }));
   }

   @Test
   public void testSetAttribute() throws Exception {
      // TODO make some properties in device base read-only (can't change id, placeId, etc)
      script.invokeMethod("setAttributeValue", new Object [] { DeviceCapability.NAME, "name", "testing" });
      assertEquals("testing", context.getAttributeValue(DeviceCapability.KEY_NAME));
   }

   @Test
   public void testReadOnlyProperty() throws Exception {

      try {
         script.invokeMethod("setAttributeValue", new Object [] { DeviceCapability.NAME, "id", UUID.randomUUID() });
         fail("Allowed a read-only attribute to be written");
      }
      catch(Exception e) {
         // expected
         e.printStackTrace(System.out);
      }
   }

   @Test
   public void testGetProperty() {
      assertEquals(deviceCapability(), script.invokeMethod("getAttributeProperty", new Object [] { DeviceCapability.NAME, "definition" } ));
      assertEquals(DeviceCapability.NAME, script.invokeMethod("getAttributeProperty", new Object [] { DeviceCapability.NAME, "capabilityName" } ));
      assertEquals(DeviceCapability.NAMESPACE, script.invokeMethod("getAttributeProperty", new Object [] { DeviceCapability.NAME, "namespace" } ));
      assertEquals(deviceCapability().getAttributes().values(), script.invokeMethod("getAttributeProperty", new Object [] { DeviceCapability.NAME, "attributes" } ));
      assertEquals(deviceCapability().getCommands().values(), script.invokeMethod("getAttributeProperty", new Object [] { DeviceCapability.NAME, "commands" } ));
      assertEquals(deviceCapability().getEvents().values(), script.invokeMethod("getAttributeProperty", new Object [] { DeviceCapability.NAME, "events" } ));
   }

   @Test
   public void testUnknownProperty() throws Exception {
      try {
         script.invokeMethod("setAttributeValue", new Object [] { DeviceCapability.NAME, "noSuchAttribute", "value" });
         fail("Allowed a non-existent attribute to be set");
      }
      catch(Exception e) {
         // expected
         e.printStackTrace(System.out);
      }
   }

}

