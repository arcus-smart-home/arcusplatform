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
package com.iris.common.subsystem.climate;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableList;
import com.iris.messages.capability.FanCapability;
import com.iris.messages.capability.VentCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.test.ModelFixtures;

/**
 * 
 */
@RunWith(Parameterized.class)
public class TestClimateSubsystem_ControlDevices extends ClimateSubsystemTestCase {

   @Parameters(name="{0}")
   public static Iterable<Object []> attributes() {
      return ImmutableList.of(
            new Object [] { FanCapability.NAMESPACE, ClimateFixtures.buildFanAttributes().create() },
            new Object [] { VentCapability.NAMESPACE, ClimateFixtures.buildVentAttributes().create() },
            // test multi-namespace capability, probably not a realistic device
            new Object [] { "VentFan", ModelFixtures.buildDeviceAttributes(FanCapability.NAMESPACE, VentCapability.NAMESPACE).create() }
      );
   }
   
   private Map<String, Object> attributes;
   
   public TestClimateSubsystem_ControlDevices(String name, Map<String, Object> attributes) {
      this.attributes = attributes;
   }
   
   @Test
   public void testAdded() {
      start();
      
      Model model = addModel(attributes);
      
      assertTrue(context.model().getAvailable());
      assertControlEquals(model.getAddress().getRepresentation());
      assertThermostatsEmpty();
      assertTemperatureEmpty();
      assertHumidityEmpty();
   }
   
   @Test
   public void testRemoved() {
      Model model = addModel(attributes);
      
      start();
      
      removeModel(model.getAddress());
      
      assertFalse(context.model().getAvailable());
      assertAllEmpty();
   }
   
   @Test
   public void testSyncOnStartWithOne() {
      Model model = addModel(attributes);
      
      start();
      
      assertTrue(context.model().getAvailable());
      assertControlEquals(model.getAddress().getRepresentation());
      assertThermostatsEmpty();
      assertTemperatureEmpty();
      assertHumidityEmpty();
   }
   
   @Test
   public void testSyncOnStartWithNone() {
      start();
      
      assertFalse(context.model().getAvailable());
      assertAllEmpty();
   }

}

