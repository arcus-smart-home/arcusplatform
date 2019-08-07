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
package com.iris.platform.scene.resolver;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Precision;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.common.rule.action.Action;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.action.ActionList;
import com.iris.common.rule.action.SendAction;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ClimateSubsystemCapability;
import com.iris.messages.capability.ClimateSubsystemCapability.EnableSchedulerRequest;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.ThermostatModel;
import com.iris.messages.type.ActionSelector;
import com.iris.messages.type.ThermostatAction;
import com.iris.model.predicate.Predicates;

/**
 * 
 */
public class ThermostatResolver extends BaseResolver {
   private final Predicate<Model> isThermostat =
         Predicates.isA(ThermostatCapability.NAMESPACE);
   
   private final static double PRECISION = 0.01;

   /**
    * @param id
    * @param name
    * @param typeHint
    */
   public ThermostatResolver() {
      super("thermostat", "Set Thermostat", "thermostat");
   }

   /* (non-Javadoc)
    * @see com.iris.platform.scene.resolver.BaseResolver#resolve(com.iris.common.rule.action.ActionContext, com.iris.messages.model.Model)
    */
   @Override
   protected List<ActionSelector> resolve(ActionContext context, Model model) {
      if(!isThermostat.apply(model)) {
         return ImmutableList.of();
      }
      
      ActionSelector selector = new ActionSelector();
      selector.setName("thermostat");
      selector.setType(ActionSelector.TYPE_THERMOSTAT);
      return ImmutableList.of(selector);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.scene.resolver.ActionResolver#generate(com.iris.common.rule.action.ActionContext, com.iris.messages.address.Address, java.util.Map)
    */
   @Override
   public Action generate(ActionContext context, Address target, Map<String, Object> variables) {
      ThermostatAction action = new ThermostatAction((Map<String, Object>) variables.get("thermostat"));
      if(action.getScheduleEnabled()) {
         return generateScheduleEnabled(context, target, true);
      }
      else {
         Action disable = generateScheduleEnabled(context, target, false);
         Action setAttributes = generateSetAttributes(context, target, action);
         return
               new ActionList.Builder()
                  .addAction(disable)
                  .addAction(setAttributes)
                  .build();
      }
   }

   private Action generateScheduleEnabled(ActionContext context, Address thermostat, boolean enable) {
      Address address = Address.platformService(context.getPlaceId(), ClimateSubsystemCapability.NAMESPACE);
      String command = enable ? ClimateSubsystemCapability.EnableSchedulerRequest.NAME : ClimateSubsystemCapability.DisableSchedulerRequest.NAME;
      Map<String, Object> attributes = ImmutableMap.of(EnableSchedulerRequest.ATTR_THERMOSTAT, thermostat.getRepresentation()); 
      return new SendAction(command, Functions.constant(address), attributes);
   }

   private Action generateSetAttributes(ActionContext context, Address target, ThermostatAction action) {
      Map<String, Object> attributes = new HashMap<String, Object>(5);
      attributes.put(ThermostatCapability.ATTR_HVACMODE, action.getMode());
      switch(action.getMode()) {
      case ThermostatCapability.HVACMODE_AUTO:
         Preconditions.checkNotNull(action.getHeatSetPoint());
         Preconditions.checkNotNull(action.getCoolSetPoint());
         Model model = context.getModelByAddress(target);
         Preconditions.checkNotNull(model);
         //The minimum setpoint for the thermostat, inclusive.  The heatsetpoint can't be set below this and the coolsetpoint can't be set below minsetpoint + setpointseparation.
         double minSetPoint = ThermostatModel.getMinsetpoint(model).doubleValue();
         //The maximum setpoint for the thermostat, inclusive.  The coolsetpoint can't be set above this and the heatsetpoint can't be set above maxsetpoint - setpointseparation.
         double maxSetPoint = ThermostatModel.getMaxsetpoint(model).doubleValue();
         double seperation = ThermostatModel.getSetpointseparation(model).doubleValue();
         Preconditions.checkArgument(Precision.compareTo(action.getHeatSetPoint(), minSetPoint, PRECISION) >= 0 , "The heatsetpoint can't be set below minSetPoint");
         Preconditions.checkArgument(Precision.compareTo(action.getCoolSetPoint(), minSetPoint + seperation, PRECISION) >= 0 , "The coolsetpoint can't be set below minSetPoint + setpointseparation");
         Preconditions.checkArgument(Precision.compareTo(action.getCoolSetPoint(), maxSetPoint, PRECISION) <= 0 , "The coolsetpoint can't be set above maxSetPoint");
         Preconditions.checkArgument(Precision.compareTo(action.getHeatSetPoint(), maxSetPoint - seperation, PRECISION) <= 0 , "The heatsetpoint can't be set above maxsetpoint - setpointseparation");

         attributes.put(ThermostatCapability.ATTR_HEATSETPOINT, action.getHeatSetPoint());
         attributes.put(ThermostatCapability.ATTR_COOLSETPOINT, action.getCoolSetPoint());
         break;
      case ThermostatCapability.HVACMODE_HEAT:
         Preconditions.checkNotNull(action.getHeatSetPoint());
         attributes.put(ThermostatCapability.ATTR_HEATSETPOINT, action.getHeatSetPoint());
         attributes.put(ThermostatCapability.ATTR_COOLSETPOINT, action.getHeatSetPoint() + 2);
         break;
      case ThermostatCapability.HVACMODE_COOL:
         Preconditions.checkNotNull(action.getCoolSetPoint());
         attributes.put(ThermostatCapability.ATTR_COOLSETPOINT, action.getCoolSetPoint());
         attributes.put(ThermostatCapability.ATTR_HEATSETPOINT, action.getCoolSetPoint() - 2);
         break;

      }

      if (action.getFanmode() != null)
      {
         attributes.put(ThermostatCapability.ATTR_FANMODE, action.getFanmode());
      }
      return new SendAction(Capability.CMD_SET_ATTRIBUTES, Functions.constant(target), attributes);
   }

}

