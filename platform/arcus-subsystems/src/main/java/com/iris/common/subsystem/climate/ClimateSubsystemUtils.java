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

import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.FanCapability;
import com.iris.messages.capability.RelativeHumidityCapability;
import com.iris.messages.capability.SchedulableCapability;
import com.iris.messages.capability.SchedulerCapability;
import com.iris.messages.capability.SpaceHeaterCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.capability.TemperatureCapability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.capability.VentCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.FanModel;
import com.iris.messages.model.dev.SpaceHeaterModel;
import com.iris.messages.model.dev.SwitchModel;
import com.iris.messages.model.dev.VentModel;
import com.iris.model.query.expression.ExpressionCompiler;

/**
 *
 */
public class ClimateSubsystemUtils {
   public static final Set<String> CONTROL_DEVICE_NAMESPACES =
         ImmutableSet.of(ThermostatCapability.NAMESPACE, FanCapability.NAMESPACE, VentCapability.NAMESPACE);

   // public so they can be included in annotations
   public static final String IS_CONTROL_DEVICE =
         Capability.ATTR_CAPS + " contains '" + ThermostatCapability.NAMESPACE + "' OR " +
         Capability.ATTR_CAPS + " contains '" + FanCapability.NAMESPACE + "' OR " +
         Capability.ATTR_CAPS + " contains '" + SpaceHeaterCapability.NAMESPACE + "' OR " +
         Capability.ATTR_CAPS + " contains '" + VentCapability.NAMESPACE + "'";
   public static final String IS_TEMPERATURE_DEVICE =
         Capability.ATTR_CAPS + " contains '" + TemperatureCapability.NAMESPACE + "'";
   public static final String IS_HUMIDITY_DEVICE =
         Capability.ATTR_CAPS + " contains '" + RelativeHumidityCapability.NAMESPACE + "'";
   public static final String IS_THERMOSTAT =
         Capability.ATTR_CAPS + " contains '" + ThermostatCapability.NAMESPACE + "'";
   public static final String IS_SPACE_HEATER =
	         Capability.ATTR_CAPS + " contains '" + SpaceHeaterCapability.NAMESPACE + "'";
   public static final String IS_CLIMATE_DEVICE =
         IS_TEMPERATURE_DEVICE + " OR " +
         IS_HUMIDITY_DEVICE + " OR " +
         IS_CONTROL_DEVICE + " OR " +
         IS_SPACE_HEATER;
   public static final String IS_SCHEDULER = Capability.ATTR_TYPE + " = '" + SchedulerCapability.NAMESPACE+ "'";
   

   private static final Predicate<Model> isScheduler = ExpressionCompiler.compile(IS_SCHEDULER);
   private static final Predicate<Model> isControlDevice = ExpressionCompiler.compile(IS_CONTROL_DEVICE);
   private static final Predicate<Model> isTemperatureDevice = ExpressionCompiler.compile(IS_TEMPERATURE_DEVICE);
   private static final Predicate<Model> isHumidityDevice = ExpressionCompiler.compile(IS_HUMIDITY_DEVICE);
   private static final Predicate<Model> isThermostat = ExpressionCompiler.compile(IS_THERMOSTAT);
   private static final Predicate<Model> isActiveFan = new Predicate<Model>() {
      @Override
      public boolean apply(Model model) {
         Integer fanSpeed = FanModel.getSpeed(model,0);
         String switchState = SwitchModel.getState(model,SwitchCapability.STATE_ON);
         return switchState.equals(SwitchCapability.STATE_ON) && fanSpeed > 0;
      }
   };
   private static final Predicate<Model> isActiveHeater = new Predicate<Model>() {
      @Override
      public boolean apply(Model model) {
    	 if(model.supports(SpaceHeaterCapability.NAMESPACE)) {
	         String state = SpaceHeaterModel.getHeatstate(model, SpaceHeaterCapability.HEATSTATE_OFF);
	         return SpaceHeaterCapability.HEATSTATE_ON.equals(state);
    	 }else{
    		 return false;
    	 }
      }
   };
   
   private static final Predicate<Model> isClosedVent = new Predicate<Model>() {
      @Override
      public boolean apply(Model model) {
         Integer ventLevel = VentModel.getLevel(model);
         return ventLevel != null && ventLevel.intValue() <= 25;
      }
   };
   private static final Predicate<Model> isSchedulable = new Predicate<Model>() {
      @Override
      public boolean apply(Model input) {
         return input.getCapabilities().contains(SchedulableCapability.NAMESPACE);
      }
   };

   public static Predicate<Model> isControlDevice() {
      return isControlDevice;
   }

   public static boolean isControlDevice(Model model) {
      return isControlDevice.apply(model);
   }

   public static Predicate<Model> isTemperatureDevice() {
      return isTemperatureDevice;
   }

   public static boolean isTemperatureDevice(Model model) {
      return isTemperatureDevice.apply(model);
   }

   public static Predicate<Model> isHumidityDevice() {
      return isHumidityDevice;
   }

   public static boolean isHumidityDevice(Model model) {
      return isHumidityDevice.apply(model);
   }

   public static Predicate<Model> isThermostat() {
      return isThermostat;
   }

   public static boolean isThermostat(Model model) {
      return isThermostat.apply(model);
   }

   public static boolean isSchedulable(Model model) {
      return isSchedulable.apply(model);
   }

   public static Predicate<Model> isClosedVent() {
      return isClosedVent;
   }

   public static boolean isClosedVent(Model model) {
      return isClosedVent.apply(model);
   }
   
   public static Predicate<Model> isActiveHeater() {
      return isActiveHeater;
   }

   public static boolean isActiveHeater(Model model) {
      return isActiveHeater.apply(model);
   }

   public static Predicate<Model> isActiveFan() {
      return isActiveFan;
   }

   public static boolean isActiveFan(Model model) {
      return isActiveFan.apply(model);
   }
   
   public static Predicate<Model> isScheduler() {
      return isScheduler;
   }
   
   public static boolean isScheduler(Model model) {
      return isScheduler.apply(model);
   }
   
   

   private ClimateSubsystemUtils() {
      // TODO Auto-generated constructor stub
   }

}

