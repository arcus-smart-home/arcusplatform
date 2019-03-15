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

import com.iris.messages.capability.FanCapability;
import com.iris.messages.capability.RelativeHumidityCapability;
import com.iris.messages.capability.SpaceHeaterCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.capability.TemperatureCapability;
import com.iris.messages.model.Model;

/**
 * 
 */
public class TestClimateSubsystem_AllDevices extends ClimateSubsystemTestCase {
   Map<String, Object> thermostatWithTempHumAttributes = 
         ClimateFixtures
            .buildThermostatAttributes()
            .addTemperature()
            .addHumidity()
            .create();
   Map<String, Object> ventWithTempHumAttributes = 
         ClimateFixtures
            .buildVentAttributes()
            .addTemperature()
            .addHumidity()
            .create();
   Map<String, Object> fanAttributes = 
         ClimateFixtures
            .buildFanAttributes()
            .create();
   Map<String, Object> contactWithTempSensor =
         ClimateFixtures
            .buildContactAttributes()
            .addTemperature()
            .create();
   Map<String, Object> motionWithTempSensor =
         ClimateFixtures
            .buildMotionAttributes()
            .addTemperature()
            .create();
   Map<String, Object> temperatureSensor =
         ClimateFixtures
            .buildDeviceAttributes(TemperatureCapability.NAMESPACE)
            .addTemperature() // sets the attributes
            .create();
   Map<String, Object> humiditySensor =
         ClimateFixtures
            .buildDeviceAttributes(RelativeHumidityCapability.NAMESPACE)
            .addHumidity() // sets the attributes
            .create();
   
   Map<String, Object> heaterSensor = 
		   ClimateFixtures.buildHeaterAttributes(SpaceHeaterCapability.HEATSTATE_OFF)
		   .create();
   
   
         
   String thermostatWithTempHumAddress;
   String ventWithTempHumAddress;
   String fanAddress;
   String contactWithTempAddress;
   String motionWithTempAddress;
   String temperatureAddress;
   String humiditiyAddress;
   String heaterAddress;
   String activeHeaterAddress;
   
   protected void addAll() {
      thermostatWithTempHumAddress = addModel(thermostatWithTempHumAttributes).getAddress().getRepresentation();
      ventWithTempHumAddress = addModel(ventWithTempHumAttributes).getAddress().getRepresentation();
      fanAddress = addModel(fanAttributes).getAddress().getRepresentation();
      contactWithTempAddress = addModel(contactWithTempSensor).getAddress().getRepresentation();
      motionWithTempAddress = addModel(motionWithTempSensor).getAddress().getRepresentation();
      temperatureAddress = addModel(temperatureSensor).getAddress().getRepresentation();
      humiditiyAddress = addModel(humiditySensor).getAddress().getRepresentation();
      heaterAddress = addModel(heaterSensor).getAddress().getRepresentation();
      activeHeaterAddress = addModel(ClimateFixtures.buildHeaterAttributes(SpaceHeaterCapability.HEATSTATE_ON)
   		   .create()).getAddress().getRepresentation();
   }
   
   
   @Test
   public void testSyncOnLoad() {
      addAll();
      start();
      
      assertTrue(context.model().getAvailable());
      assertControlEquals(thermostatWithTempHumAddress, ventWithTempHumAddress, fanAddress, activeHeaterAddress,heaterAddress);
      assertThermostatsEquals(thermostatWithTempHumAddress);
      assertTemperatureEquals(
            thermostatWithTempHumAddress, 
            ventWithTempHumAddress, 
            contactWithTempAddress, 
            motionWithTempAddress, 
            temperatureAddress
      );
      assertHumidityEquals(thermostatWithTempHumAddress, ventWithTempHumAddress, humiditiyAddress);
      assertFalse(context.model().getActiveHeaters().contains(heaterAddress));
      assertTrue(context.model().getActiveHeaters().contains(activeHeaterAddress));
   }
   
   @Test
   public void testRemoveControlDevices() {
      addAll();
      
      start();
      
      // remove all control devices
      removeModel(thermostatWithTempHumAddress);
      removeModel(ventWithTempHumAddress);
      removeModel(fanAddress);
      removeModel(activeHeaterAddress);
      removeModel(heaterAddress);
      
      // still has temperature devices
      assertTrue(context.model().getAvailable());
      
      assertControlEmpty();
      assertThermostatsEmpty();
      assertTemperatureEquals(
            contactWithTempAddress, 
            motionWithTempAddress, 
            temperatureAddress
      );
      assertHumidityEquals(humiditiyAddress);
   }

   @Test
   public void testAddWhileRunning() {
      start();
      
      addAll();
      assertTrue(context.model().getAvailable());
      assertControlEquals(thermostatWithTempHumAddress, ventWithTempHumAddress, fanAddress,activeHeaterAddress,heaterAddress);
      assertThermostatsEquals(thermostatWithTempHumAddress);
      assertTemperatureEquals(
            thermostatWithTempHumAddress, 
            ventWithTempHumAddress, 
            contactWithTempAddress, 
            motionWithTempAddress, 
            temperatureAddress
      );
      assertHumidityEquals(thermostatWithTempHumAddress, ventWithTempHumAddress, humiditiyAddress);
   }
   
   @Test
   public void testFanActive() {
      start();
      Model fan = addModel(fanAttributes);
      assertTrue(context.model().getActiveFans().contains(fan.getAddress().getRepresentation()));
      fan.setAttribute(FanCapability.ATTR_SPEED, 0);
      updateModel(fan.getAddress(), fan.toMap());
      assertFalse(context.model().getActiveFans().contains(fan.getAddress().getRepresentation()));
      fan.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF);
      fan.setAttribute(FanCapability.ATTR_SPEED, 10);
      updateModel(fan.getAddress(), fan.toMap());
      assertFalse(context.model().getActiveFans().contains(fan.getAddress().getRepresentation()));
   }
   
   @Test
   public void testHeaterActive() {
      start();
      Map<String, Object> activeHeaterSensor = ClimateFixtures.buildHeaterAttributes(SpaceHeaterCapability.HEATSTATE_ON).create();
      
      Model activeHeater = addModel(activeHeaterSensor);
      assertTrue(context.model().getActiveHeaters().contains(activeHeater.getAddress().getRepresentation()));
      activeHeater.setAttribute(SpaceHeaterCapability.ATTR_HEATSTATE, SpaceHeaterCapability.HEATSTATE_OFF);
      updateModel(activeHeater.getAddress(), activeHeater.toMap());
      assertFalse(context.model().getActiveHeaters().contains(activeHeater.getAddress().getRepresentation()));
      
      
   }
}

