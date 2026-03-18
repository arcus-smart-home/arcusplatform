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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.codehaus.groovy.control.customizers.ImportCustomizer;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.groovy.control.ControlProtocolPlugin;
import com.iris.driver.groovy.devicesettings.DeviceSettingsPlugin;
import com.iris.driver.groovy.ipcd.IpcdProtocolPlugin;
import com.iris.driver.groovy.mock.MockProtocolPlugin;
import com.iris.driver.groovy.pin.PinManagementPlugin;
import com.iris.driver.groovy.plugin.GroovyDriverPlugin;
import com.iris.driver.groovy.reflex.ReflexPlugin;
import com.iris.driver.groovy.scheduler.SchedulerPlugin;
import com.iris.driver.groovy.zigbee.ZigbeeProtocolPlugin;
import com.iris.driver.groovy.zwave.ZWaveProtocolPlugin;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.model.Fixtures;

/**
 * Smoke test that loads every production driver and exercises its
 * setAttributes handlers.  This catches runtime errors like
 * MissingPropertyException that the Groovy compiler cannot detect
 * at compile time.
 */
@RunWith(Parameterized.class)
public class DriverSetAttributesSmokeTest extends GroovyDriverTestCase {
   private static final Logger log = LoggerFactory.getLogger(DriverSetAttributesSmokeTest.class);

   private static final String DRIVER_DIR =
      "../../arcus-containers/driver-services/src/main/resources";

   @Override
   protected Set<GroovyDriverPlugin> getPlugins() {
      return ImmutableSet.of(
         new SchedulerPlugin(),
         new ControlProtocolPlugin(),
         new ZWaveProtocolPlugin(),
         new ZigbeeProtocolPlugin(),
         new IpcdProtocolPlugin(),
         new MockProtocolPlugin(),
         new ReflexPlugin(),
         new PinManagementPlugin(),
         new DeviceSettingsPlugin()
      );
   }

   @Override
   protected void configure(Binder binder) {
      super.configure(binder);
      Multibinder.newSetBinder(binder, org.codehaus.groovy.control.customizers.CompilationCustomizer.class)
         .addBinding()
         .toInstance(new ImportCustomizer()
            .addImports("groovy.transform.Field")
            .addStaticStars("java.util.concurrent.TimeUnit")
         );
   }

   // Capabilities with writable attributes commonly handled by setAttributes.
   // Maps namespace -> (attribute name -> sample value)
   private static final Map<String, Map<String, Object>> CAPABILITY_SAMPLE_VALUES = new HashMap<>();
   static {
      // swit (Switch)
      Map<String, Object> swit = new HashMap<>();
      swit.put("swit:state", "ON");
      CAPABILITY_SAMPLE_VALUES.put("swit", swit);

      // dim (Dimmer)
      Map<String, Object> dim = new HashMap<>();
      dim.put("dim:brightness", 50);
      CAPABILITY_SAMPLE_VALUES.put("dim", dim);

      // therm (Thermostat)
      Map<String, Object> therm = new HashMap<>();
      therm.put("therm:hvacmode", "AUTO");
      therm.put("therm:coolsetpoint", 25.0);
      therm.put("therm:heatsetpoint", 20.0);
      therm.put("therm:fanmode", 1);
      CAPABILITY_SAMPLE_VALUES.put("therm", therm);

      // doorlock (DoorLock)
      Map<String, Object> doorlock = new HashMap<>();
      doorlock.put("doorlock:lockstate", "LOCKED");
      CAPABILITY_SAMPLE_VALUES.put("doorlock", doorlock);

      // color (Color)
      Map<String, Object> color = new HashMap<>();
      color.put("color:hue", 180);
      color.put("color:saturation", 100);
      CAPABILITY_SAMPLE_VALUES.put("color", color);

      // colortemp (ColorTemperature)
      Map<String, Object> colortemp = new HashMap<>();
      colortemp.put("colortemp:colortemp", 4000);
      CAPABILITY_SAMPLE_VALUES.put("colortemp", colortemp);

      // fanspeed (Fan)
      Map<String, Object> fan = new HashMap<>();
      fan.put("fan:speed", 1);
      CAPABILITY_SAMPLE_VALUES.put("fan", fan);

      // shade (Shade)
      Map<String, Object> shade = new HashMap<>();
      shade.put("shade:level", 50);
      CAPABILITY_SAMPLE_VALUES.put("shade", shade);

      // cont (Contact)
      Map<String, Object> cont = new HashMap<>();
      cont.put("cont:usehint", "DOOR");
      CAPABILITY_SAMPLE_VALUES.put("cont", cont);

      // motdoor (MotorizedDoor)
      Map<String, Object> motdoor = new HashMap<>();
      motdoor.put("motdoor:doorstate", "OPEN");
      CAPABILITY_SAMPLE_VALUES.put("motdoor", motdoor);

      // pres (Presence)
      Map<String, Object> pres = new HashMap<>();
      pres.put("pres:usehint", "PERSON");
      CAPABILITY_SAMPLE_VALUES.put("pres", pres);

      // petdoor (PetDoor)
      Map<String, Object> petdoor = new HashMap<>();
      petdoor.put("petdoor:lockstate", "LOCKED");
      CAPABILITY_SAMPLE_VALUES.put("petdoor", petdoor);

      // waterheater (WaterHeater)
      Map<String, Object> waterheater = new HashMap<>();
      waterheater.put("waterheater:setpoint", 49.0);
      waterheater.put("waterheater:hotwaterlevel", "MEDIUM");
      CAPABILITY_SAMPLE_VALUES.put("waterheater", waterheater);

      // irrcont (IrrigationController)
      Map<String, Object> irrcont = new HashMap<>();
      irrcont.put("irrcont:rainDelayDuration", 1440);
      CAPABILITY_SAMPLE_VALUES.put("irrcont", irrcont);

      // indicator (Indicator)
      Map<String, Object> indicator = new HashMap<>();
      indicator.put("indicator:enabled", true);
      indicator.put("indicator:inverted", false);
      CAPABILITY_SAMPLE_VALUES.put("indicator", indicator);

      // twinstar (Twinstar)
      Map<String, Object> twinstar = new HashMap<>();
      twinstar.put("twinstar:ecomode", "ENABLED");
      CAPABILITY_SAMPLE_VALUES.put("twinstar", twinstar);

      // somfyv1 (SomfyV1)
      Map<String, Object> somfyv1 = new HashMap<>();
      somfyv1.put("somfyv1:mode", "SHADE");
      somfyv1.put("somfyv1:reversed", "NORMAL");
      CAPABILITY_SAMPLE_VALUES.put("somfyv1", somfyv1);

      // keenvent (Vent)
      Map<String, Object> vent = new HashMap<>();
      vent.put("vent:level", 50);
      CAPABILITY_SAMPLE_VALUES.put("vent", vent);
   }

   private final String driverFile;
   private DeviceDriver driver;
   private DeviceDriverContext context;

   @Parameters(name = "{0}")
   public static Collection<Object[]> driverFiles() {
      List<Object[]> drivers = new ArrayList<>();
      File dir = new File(DRIVER_DIR);
      if (!dir.exists() || !dir.isDirectory()) {
         throw new RuntimeException("Driver directory not found: " + dir.getAbsolutePath());
      }
      File[] files = dir.listFiles((d, name) ->
         name.endsWith(".driver") && !name.startsWith("_") && !name.startsWith("Fallback")
      );
      if (files != null) {
         for (File f : files) {
            drivers.add(new Object[] { f.getName() });
         }
      }
      return drivers;
   }

   public DriverSetAttributesSmokeTest(String driverFile) {
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
   public void testSetAttributesDoesNotThrow() throws Exception {
      Set<CapabilityDefinition> capabilities = driver.getDefinition().getCapabilities();

      boolean testedAny = false;
      for (CapabilityDefinition cap : capabilities) {
         String namespace = cap.getNamespace();
         Map<String, Object> sampleValues = CAPABILITY_SAMPLE_VALUES.get(namespace);
         if (sampleValues == null) {
            continue;
         }

         for (Map.Entry<String, Object> entry : sampleValues.entrySet()) {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put(entry.getKey(), entry.getValue());

            MessageBody body = MessageBody.buildMessage(
               Capability.CMD_SET_ATTRIBUTES, attrs
            );

            PlatformMessage message = PlatformMessage.builder()
               .from(Fixtures.createClientAddress())
               .to(Fixtures.createDeviceAddress())
               .withPayload(body)
               .create();

            try {
               driver.handlePlatformMessage(message, context);
               testedAny = true;
            } catch (Exception e) {
               throw new AssertionError(
                  "Driver " + driverFile + " threw exception handling " +
                  "SetAttributes for " + entry.getKey() + "=" + entry.getValue() +
                  ": " + e.getMessage(), e
               );
            }
         }
      }

      if (testedAny) {
         log.info("Driver {} - setAttributes smoke test passed", driverFile);
      } else {
         log.debug("Driver {} - no testable setAttributes capabilities found", driverFile);
      }
   }
}
