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
package com.iris.common.subsystem.climate;

import java.util.Map;

import org.junit.Test;

import com.iris.messages.capability.TemperatureCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.test.ModelFixtures;

public class TestClimateSubsystem_Temperature extends ClimateSubsystemTestCase {
   Map<String, Object> device1 = ModelFixtures.buildDeviceAttributes(TemperatureCapability.NAMESPACE).create();
   Map<String, Object> device2 = ModelFixtures.buildDeviceAttributes(TemperatureCapability.NAMESPACE).create();

   @Test
   public void testAdded() {
      start();
      
      assertFalse(context.model().getAvailable());

      Model temperatureModel1 = addModel(device1);
      
      assertTrue(context.model().getAvailable());
      assertControlEmpty();
      assertThermostatsEmpty();
      assertTemperatureEquals(temperatureModel1.getAddress().getRepresentation());
      assertHumidityEmpty();

      Model temperatureModel2 = addModel(device2);
      
      assertTrue(context.model().getAvailable());
      assertControlEmpty();
      assertThermostatsEmpty();
      assertTemperatureEquals(
            temperatureModel1.getAddress().getRepresentation(),
            temperatureModel2.getAddress().getRepresentation()
      );
      assertHumidityEmpty();
   }
   
   @Test
   public void testRemoved() {
      Model temperatureModel1 = addModel(device1);
      Model temperatureModel2 = addModel(device2);
      
      start();
      
      assertTrue(context.model().getAvailable());

      removeModel(temperatureModel1.getAddress());
      
      assertTrue(context.model().getAvailable());
      assertControlEmpty();
      assertThermostatsEmpty();
      assertTemperatureEquals(temperatureModel2.getAddress().getRepresentation());
      assertHumidityEmpty();
      
      removeModel(temperatureModel2.getAddress());

      assertFalse(context.model().getAvailable());
      assertAllEmpty();
      
   }
   
   @Test
   public void testSyncOnStartWithTwo() {
      Model temperatureModel1 = addModel(device1);
      Model temperatureModel2 = addModel(device2);
      
      start();
      
      assertTrue(context.model().getAvailable());
      assertControlEmpty();
      assertThermostatsEmpty();
      assertTemperatureEquals(
            temperatureModel1.getAddress().getRepresentation(),
            temperatureModel2.getAddress().getRepresentation()
      );
      assertHumidityEmpty();
   }
   
   @Test
   public void testSyncOnStartWithNone() {
      start();
      
      assertFalse(context.model().getAvailable());
      assertAllEmpty();
   }

}

