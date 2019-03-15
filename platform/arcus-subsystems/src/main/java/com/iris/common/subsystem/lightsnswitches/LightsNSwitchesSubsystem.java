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
package com.iris.common.subsystem.lightsnswitches;


import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.inject.Singleton;
import com.iris.annotation.Version;
import com.iris.common.subsystem.BaseSubsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.annotation.Subsystem;
import com.iris.common.subsystem.util.AddressesAttributeBinder;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DimmerCapability;
import com.iris.messages.capability.LightsNSwitchesSubsystemCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.event.ModelAddedEvent;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.listener.annotation.OnAdded;
import com.iris.messages.listener.annotation.OnRemoved;
import com.iris.messages.listener.annotation.OnValueChanged;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.LightsNSwitchesSubsystemModel;
import com.iris.model.query.expression.ExpressionCompiler;
import com.iris.util.TypeMarker;

@Singleton
@Subsystem(LightsNSwitchesSubsystemModel.class)
@Version(1)
public class LightsNSwitchesSubsystem extends BaseSubsystem<LightsNSwitchesSubsystemModel> {

   private static final String HINT_SWITCH = "switch";
   private static final String HINT_DIMMER = "dimmer";
   private static final String HINT_LIGHT = "light";
   private static final String HINT_HALO = "halo";
   
   static final private Map<String,Integer>EMTPY_COUNTS=ImmutableMap.of(HINT_LIGHT,0,HINT_DIMMER,0,HINT_SWITCH,0);
   
   static final String QUERY_SWITCH_ON = "swit:state == 'ON'";
   static final String QUERY_DIMMER_ON = "dim:brightness > 0 AND !(swit:state is supported)";
   static final String QUERY_SWITCH_DEVICES = "dev:devtypehint == 'Switch' or dev:devtypehint == 'Dimmer' or dev:devtypehint == 'Light' or dev:devtypehint == 'Halo'";
   static final Predicate<Model> IS_SWITCH = ExpressionCompiler.compile(QUERY_SWITCH_DEVICES);
   static final Predicate<Model> IS_SWITCH_ON = ExpressionCompiler.compile(QUERY_SWITCH_ON);
   static final Predicate<Model> IS_DIMMER_ON = ExpressionCompiler.compile(QUERY_DIMMER_ON);
   
   private final AddressesAttributeBinder<LightsNSwitchesSubsystemModel> switches = 
         new AddressesAttributeBinder<LightsNSwitchesSubsystemModel>(IS_SWITCH, LightsNSwitchesSubsystemCapability.ATTR_SWITCHDEVICES) {
            @Override
            protected void afterAdded(SubsystemContext<LightsNSwitchesSubsystemModel> context, Model added) {
               context.logger().info("A new switch device was added {}", added.getAddress());
               syncOnDeviceCounts(context);
            }
            
            @Override
            protected void afterRemoved(SubsystemContext<LightsNSwitchesSubsystemModel> context, Address address) {
               context.logger().info("A switch device was removed {}", address);
               syncOnDeviceCounts(context);
            }
         };
   
   @Override
   protected void onAdded(SubsystemContext<LightsNSwitchesSubsystemModel> context) {
      super.onAdded(context);
   }

   @Override
   protected void onStarted(SubsystemContext<LightsNSwitchesSubsystemModel> context) {
      super.onStarted(context);
      switches.bind(context);
      syncOnDeviceCounts(context);
   }
   
   @OnAdded(query = QUERY_SWITCH_DEVICES)
   public void onDeviceAdded(ModelAddedEvent event, SubsystemContext<LightsNSwitchesSubsystemModel> context) {
      context.logger().info("a new lightsnswitches device was added {}", event);
      syncOnDeviceCounts(context);
   }
  
   @OnRemoved(query = QUERY_SWITCH_DEVICES)
   public void onDeviceRemoved(ModelRemovedEvent event, SubsystemContext<LightsNSwitchesSubsystemModel> context) {
      context.logger().info("a new lightsnswitches device was removed {}", event);
      syncOnDeviceCounts(context);
   }
   
   @OnValueChanged(attributes = {SwitchCapability.ATTR_STATE})
   public void onSwitchStateChange(ModelChangedEvent event,SubsystemContext<LightsNSwitchesSubsystemModel> context){
      syncOnDeviceCounts(context);
   }
   @OnValueChanged(attributes = {DimmerCapability.ATTR_BRIGHTNESS})
   public void onDimmerChange(ModelChangedEvent event,SubsystemContext<LightsNSwitchesSubsystemModel> context){
      syncOnDeviceCounts(context);
   }

   protected void syncSubsystemAvailable(SubsystemContext<LightsNSwitchesSubsystemModel> context) {
      
      if (Iterables.size(context.models().getModels(IS_SWITCH)) > 0){
         context.model().setAvailable(true);
      }
      else{
         context.model().setAvailable(false);
      }
   }
   private void syncOnDeviceCounts(SubsystemContext<LightsNSwitchesSubsystemModel> context){
      Map<String, Integer>onCounts=new HashMap<String,Integer>(EMTPY_COUNTS);

      for(Model model:context.models().getModels(IS_SWITCH)){
         if(!(IS_SWITCH_ON.apply(model) || IS_DIMMER_ON.apply(model))){
            continue;
         }
         String devtype = model.getAttribute(TypeMarker.string(), DeviceCapability.ATTR_DEVTYPEHINT).or("").toLowerCase();
         switch(devtype){
            case HINT_LIGHT:
            case HINT_HALO:
               onCounts.put(HINT_LIGHT, onCounts.get(HINT_LIGHT)+1);
               break;
            case HINT_DIMMER:
               onCounts.put(HINT_DIMMER, onCounts.get(HINT_DIMMER)+1);
               break;
            case HINT_SWITCH:
               onCounts.put(HINT_SWITCH, onCounts.get(HINT_SWITCH)+1);
               break;
         }
      }
      context.model().setAttribute(LightsNSwitchesSubsystemCapability.ATTR_ONDEVICECOUNTS, onCounts);
      syncSubsystemAvailable(context);
   }
   
 

}

