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

import com.iris.common.subsystem.event.SubsystemStartedEvent;
import com.iris.messages.event.ModelAddedEvent;
import com.iris.messages.model.Model;

/**
 * 
 */
public class TestClimateSubsystem_Thermostats extends ClimateSubsystemTestCase {
   Map<String, Object> thermostatAttributes = ClimateFixtures.buildThermostatAttributes().create();

   @Test
   public void testAdded() {
      start();
      
      Model thermostatModel = addModel(thermostatAttributes);
      
      assertTrue(context.model().getAvailable());
      assertControlEquals(thermostatModel.getAddress().getRepresentation());
      assertThermostatsEquals(thermostatModel.getAddress().getRepresentation());
      assertTemperatureEmpty();
      assertHumidityEmpty();
   }
   
   @Test
   public void testRemoved() {
      Model thermostatModel = addModel(thermostatAttributes);
      
      start();
      
      removeModel(thermostatModel.getAddress());
      
      assertFalse(context.model().getAvailable());
      assertAllEmpty();
   }
   
   @Test
   public void testSyncOnStartWithOne() {
      Model thermostatModel = addModel(thermostatAttributes);
      
      start();
      
      assertTrue(context.model().getAvailable());
      assertControlEquals(thermostatModel.getAddress().getRepresentation());
      assertThermostatsEquals(thermostatModel.getAddress().getRepresentation());
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

