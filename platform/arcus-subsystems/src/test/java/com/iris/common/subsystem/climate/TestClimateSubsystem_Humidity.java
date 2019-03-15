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

import com.iris.messages.capability.RelativeHumidityCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.test.ModelFixtures;

public class TestClimateSubsystem_Humidity extends ClimateSubsystemTestCase {
   Map<String, Object> device1 = ModelFixtures.buildDeviceAttributes(RelativeHumidityCapability.NAMESPACE).create();
   Map<String, Object> device2 = ModelFixtures.buildDeviceAttributes(RelativeHumidityCapability.NAMESPACE).create();

   @Test
   public void testAdded() {
      start();
      
      Model humidityModel1 = addModel(device1);
      
      assertFalse(context.model().getAvailable());
      assertControlEmpty();
      assertThermostatsEmpty();
      assertHumidityEquals(humidityModel1.getAddress().getRepresentation());
      assertTemperatureEmpty();

      Model humidityModel2 = addModel(device2);
      
      assertFalse(context.model().getAvailable());
      assertControlEmpty();
      assertThermostatsEmpty();
      assertHumidityEquals(
            humidityModel1.getAddress().getRepresentation(),
            humidityModel2.getAddress().getRepresentation()
      );
      assertTemperatureEmpty();
   }
   
   @Test
   public void testRemoved() {
      Model humidityModel1 = addModel(device1);
      Model humidityModel2 = addModel(device2);
      
      start();
      
      removeModel(humidityModel1.getAddress());
      
      assertFalse(context.model().getAvailable());
      assertControlEmpty();
      assertThermostatsEmpty();
      assertHumidityEquals(humidityModel2.getAddress().getRepresentation());
      assertTemperatureEmpty();
      
      removeModel(humidityModel2.getAddress());

      assertFalse(context.model().getAvailable());
      assertAllEmpty();
      
   }
   
   @Test
   public void testSyncOnStartWithTwo() {
      Model humidityModel1 = addModel(device1);
      Model humidityModel2 = addModel(device2);
      
      start();
      
      assertFalse(context.model().getAvailable());
      assertControlEmpty();
      assertThermostatsEmpty();
      assertHumidityEquals(
            humidityModel1.getAddress().getRepresentation(),
            humidityModel2.getAddress().getRepresentation()
      );
      assertTemperatureEmpty();
   }
   
   @Test
   public void testSyncOnStartWithNone() {
      start();
      
      assertFalse(context.model().getAvailable());
      assertAllEmpty();
   }

}

