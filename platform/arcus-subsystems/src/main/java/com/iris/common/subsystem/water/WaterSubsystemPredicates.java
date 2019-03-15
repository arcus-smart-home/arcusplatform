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
package com.iris.common.subsystem.water;

import com.google.common.base.Predicate;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.EcowaterWaterSoftenerCapability;
import com.iris.messages.capability.ValveCapability;
import com.iris.messages.capability.WaterHeaterCapability;
import com.iris.messages.capability.WaterSoftenerCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.EcowaterWaterSoftenerModel;
import com.iris.messages.model.dev.ValveModel;
import com.iris.messages.model.dev.WaterSoftenerModel;
import com.iris.model.query.expression.ExpressionCompiler;

class WaterSubsystemPredicates {

   private WaterSubsystemPredicates() {
   }

   static final String QUERY_WATERHEATER_DEVICES = containCapability(WaterHeaterCapability.NAMESPACE);
   static final String QUERY_WATERSOFTENER_DEVICES = containCapability(WaterSoftenerCapability.NAMESPACE);
   static final String QUERY_VALVE_DEVICES = containCapability(ValveCapability.NAMESPACE);
   static final String QUERY_WATER_DEVICES = QUERY_WATERHEATER_DEVICES + " OR " + QUERY_WATERSOFTENER_DEVICES + " OR " + QUERY_VALVE_DEVICES ;
   static final String QUERY_ECOWATER_DEVICES = containCapability(EcowaterWaterSoftenerCapability.NAMESPACE);
   
   static final Predicate<Model> IS_WATERHEATER = ExpressionCompiler.compile(QUERY_WATERHEATER_DEVICES);
   static final Predicate<Model> IS_WATERSOFTENER = ExpressionCompiler.compile(QUERY_WATERSOFTENER_DEVICES);
   static final Predicate<Model> IS_VALVE = ExpressionCompiler.compile(QUERY_VALVE_DEVICES);
   static final Predicate<Model> IS_ECOWATER = ExpressionCompiler.compile(QUERY_ECOWATER_DEVICES);
   static final int LOW_SALT_THRESHOLD = 25;
   
   static final Predicate<Model> IS_CLOSED_VALVE = new Predicate<Model>() {
      @Override
      public boolean apply(Model arg0) {
         return ValveModel.isValvestateCLOSED(arg0);
      }
   };
   
   static final Predicate<Model> IS_CONTINUOUS_WATER_USE = new Predicate<Model>() {
      @Override
      public boolean apply(Model model) {
         boolean retValue = EcowaterWaterSoftenerModel.getContinuousUse(model, false) && EcowaterWaterSoftenerModel.getAlertOnContinuousUse(model, false);
         return retValue;
      }
   };
   
   static final Predicate<Model> IS_EXCESSIVE_WATER_USE = new Predicate<Model>() {
      @Override
      public boolean apply(Model model) {
         return EcowaterWaterSoftenerModel.getExcessiveUse(model, false) && EcowaterWaterSoftenerModel.getAlertOnExcessiveUse(model, false);
      }
   };
   
   static final Predicate<Model> IS_LOW_SALT = new Predicate<Model>() {
      @Override
      public boolean apply(Model model) {
         Integer saltLevel = WaterSoftenerModel.getCurrentSaltLevel(model);
         if(saltLevel != null) {
            return WaterSoftenerModel.getSaltLevelEnabled(model, false) && saltLevel.intValue() < LOW_SALT_THRESHOLD;
         }else{
            return false;
         }          
      }
   };
   
   
   private static String containCapability(String capName) {
	   return Capability.ATTR_CAPS + " contains '" + capName + "'";
   }
}

