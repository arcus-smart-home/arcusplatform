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
package com.iris.driver.service.registry;

import groovy.util.GroovyScriptEngine;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.annotations.WarmUp;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.attributes.AttributeValue;
import com.iris.driver.DeviceDriver;
import com.iris.driver.LightweightDeviceDriver;
import com.iris.driver.groovy.GroovyDriverFactory;
import com.iris.driver.groovy.GroovyProtocolPluginModule;
import com.iris.driver.groovy.customizer.DriverCompilationCustomizer;
import com.iris.driver.registry.GroovyDriverRegistry;
import com.iris.driver.service.DriverConfig;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.model.DriverId;
import com.iris.model.Version;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules({ CapabilityRegistryModule.class, GroovyProtocolPluginModule.class })
public class TestGroovyDriverRegistryLazy extends IrisTestCase {
   private GroovyDriverRegistry registry;

   @Provides
   @Singleton
   public GroovyDriverRegistry provideGroovyDriverRegistry(LazyDriverConfig config, GroovyDriverFactory factory) {
      return new GroovyDriverRegistry(config, factory);
   }

   @Provides
   @Singleton
   public GroovyScriptEngine provideGroovyScriptEngine(LazyDriverConfig driverConfig, CapabilityRegistry capRegistry) throws MalformedURLException {
      File driverDir = new File(driverConfig.getDriverDirectory());
      GroovyScriptEngine engine = new GroovyScriptEngine(new URL[] {
            driverDir.toURI().toURL(),
            new File("src/main/resources").toURI().toURL()
      });
      engine.getConfig().addCompilationCustomizers(new DriverCompilationCustomizer(capRegistry));
      return engine;
   }

   @Provides
   public LazyDriverConfig provideLazyDriverConfig() {
      return new LazyDriverConfig(getDriverDir().getAbsolutePath());
   }

   @WarmUp
   public void warmUp() throws Exception {
      registry = ServiceLocator.getInstance(GroovyDriverRegistry.class);
      registry.warmUp();
   }

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
   }

   @Test
   public void testListDriversReturnsLightweight() {
      Collection<DeviceDriver> drivers = registry.listDrivers();
      assertNotNull(drivers);
      assertFalse(drivers.isEmpty());
      for(DeviceDriver driver : drivers) {
         assertTrue(
               "Expected LightweightDeviceDriver but got " + driver.getClass().getSimpleName(),
               driver instanceof LightweightDeviceDriver
         );
         // Definition should be accessible
         assertNotNull(driver.getDefinition());
         assertNotNull(driver.getDriverId());
         assertNotNull(driver.getBaseAttributes());
      }
   }

   @Test
   public void testLoadDriverByIdCompilesOnDemand() {
      DriverId id = new DriverId("Driver1", new Version(1));

      // Before loading, the registry should hold a lightweight driver
      Collection<DeviceDriver> allDrivers = registry.listDrivers();
      DeviceDriver inRegistry = null;
      for(DeviceDriver d : allDrivers) {
         if(d.getDriverId().equals(id)) {
            inRegistry = d;
            break;
         }
      }
      assertNotNull("Driver1 v1 should be in registry", inRegistry);
      assertTrue("Should be lightweight before loadDriverById", inRegistry instanceof LightweightDeviceDriver);

      // loadDriverById should return a full driver
      DeviceDriver loaded = registry.loadDriverById(id);
      assertNotNull(loaded);
      assertFalse("loadDriverById should return a full driver", loaded instanceof LightweightDeviceDriver);
      assertEquals(id, loaded.getDriverId());
   }

   @Test
   public void testLoadDriverByIdCachesResult() {
      DriverId id = new DriverId("Driver2", new Version(1));

      DeviceDriver first = registry.loadDriverById(id);
      DeviceDriver second = registry.loadDriverById(id);
      assertNotNull(first);
      assertSame("Second call should return cached instance", first, second);
   }

   @Test
   public void testLoadDriverByIdReturnsNullForUnknown() {
      DeviceDriver driver = registry.loadDriverById(new DriverId("NonExistent", new Version(1)));
      assertNull(driver);
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   @Test
   public void testFindDriverForCompilesOnDemand() {
      AttributeMap attributes = AttributeMap.mapOf(
            new AttributeValue(DeviceCapability.KEY_VENDOR, "Iris"),
            new AttributeValue(DeviceCapability.KEY_MODEL, "nifty-002")
      );
      DeviceDriver driver = registry.findDriverFor("general", attributes, 0);
      assertNotNull(driver);
      assertFalse("findDriverFor should return a full driver", driver instanceof LightweightDeviceDriver);
      assertEquals(new DriverId("Driver2", new Version(1)), driver.getDriverId());
   }

   @Test
   public void testLoadDriverByNameCompilesOnDemand() {
      DeviceDriver driver = registry.loadDriverByName("general", "Driver1", 0);
      assertNotNull(driver);
      assertFalse("loadDriverByName should return a full driver", driver instanceof LightweightDeviceDriver);
      assertEquals(new DriverId("Driver1", new Version(2)), driver.getDriverId());
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   @Test
   public void testDiscoveryMatchingWorksWithLightweight() {
      // Verify all the same matching cases work as in the non-lazy test
      AttributeMap noMatch = AttributeMap.emptyMap();
      assertNull(registry.findDriverFor("general", noMatch, 0));

      AttributeMap matchOne = AttributeMap.mapOf(
            new AttributeValue(DeviceCapability.KEY_VENDOR, "Iris"),
            new AttributeValue(DeviceCapability.KEY_MODEL, "nifty-002")
      );
      assertEquals(new DriverId("Driver2", new Version(1)), registry.findDriverFor("general", matchOne, 0).getDriverId());

      AttributeMap matchTwo = AttributeMap.mapOf(
            new AttributeValue(DeviceCapability.KEY_VENDOR, "Iris"),
            new AttributeValue(DeviceCapability.KEY_MODEL, "nifty-001")
      );
      assertEquals(new DriverId("Driver1", new Version(2)), registry.findDriverFor("general", matchTwo, 0).getDriverId());

      // Regex matcher
      AttributeMap matchThree = AttributeMap.mapOf(
            new AttributeValue(DeviceCapability.KEY_VENDOR, "Iris"),
            new AttributeValue(DeviceCapability.KEY_MODEL, "nifty-003BCE")
      );
      assertEquals(new DriverId("Driver3", new Version(1)), registry.findDriverFor("general", matchThree, 0).getDriverId());
   }

   @Test
   public void testMultipleDriversCompileIndependently() {
      DriverId id1 = new DriverId("Driver1", new Version(1));
      DriverId id2 = new DriverId("Driver2", new Version(1));

      // Initially all lightweight
      Collection<DeviceDriver> allDrivers = registry.listDrivers();
      for(DeviceDriver d : allDrivers) {
         assertTrue(d instanceof LightweightDeviceDriver);
      }

      // Compile one driver
      DeviceDriver driver1 = registry.loadDriverById(id1);
      assertNotNull(driver1);
      assertFalse(driver1 instanceof LightweightDeviceDriver);

      // The other should still be lightweight in the registry
      DeviceDriver inRegistry = null;
      for(DeviceDriver d : registry.listDrivers()) {
         if(d.getDriverId().equals(id2)) {
            inRegistry = d;
            break;
         }
      }
      assertTrue("Driver2 should still be lightweight", inRegistry instanceof LightweightDeviceDriver);

      // Now compile the second one
      DeviceDriver driver2 = registry.loadDriverById(id2);
      assertNotNull(driver2);
      assertFalse(driver2 instanceof LightweightDeviceDriver);

      // Both should be cached
      assertSame(driver1, registry.loadDriverById(id1));
      assertSame(driver2, registry.loadDriverById(id2));
   }

   private File getDriverDir() {
      return new File("src/test/resources");
   }

   private static class LazyDriverConfig extends DriverConfig {
      private final String directory;

      LazyDriverConfig(String directory) {
         this.directory = directory;
         setLazyLoading(true);
      }

      @Override
      public String getDriverDirectory() {
         return directory;
      }
   }
}
