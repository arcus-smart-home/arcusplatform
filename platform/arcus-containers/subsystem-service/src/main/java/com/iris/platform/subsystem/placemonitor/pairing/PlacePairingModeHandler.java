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
package com.iris.platform.subsystem.placemonitor.pairing;

import com.google.common.base.Predicate;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.capability.BridgeCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.PlaceMonitorSubsystemCapability;
import com.iris.messages.listener.annotation.OnValueChanged;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.model.query.expression.ExpressionCompiler;
import com.iris.platform.subsystem.placemonitor.BasePlaceMonitorHandler;

public class PlacePairingModeHandler extends BasePlaceMonitorHandler {

   public final static String QUERY_DEVICES_THAT_PAIR = String.format("base:caps contains '%s' OR base:caps contains '%s'",
                                                                     HubCapability.NAMESPACE,BridgeCapability.NAMESPACE);
   static final Predicate<Model> IS_PAIRING_DEVICE = ExpressionCompiler.compile(QUERY_DEVICES_THAT_PAIR);

   @Override
   public void onStarted(SubsystemContext<PlaceMonitorSubsystemModel> context) {
      super.onStarted(context);
      context.logger().debug("Place Pairing Model Handler inStarted");
      syncPairingState(context);
   }
   
   @OnValueChanged(attributes = { BridgeCapability.ATTR_PAIRINGSTATE})
   public void onBridgePairingMode(Model device, SubsystemContext<PlaceMonitorSubsystemModel> context){
      syncPairingState(context);   
   }
   
   @OnValueChanged(attributes = { HubCapability.ATTR_STATE})
   public void onHubPairingMode(Model device, SubsystemContext<PlaceMonitorSubsystemModel> context){
      syncPairingState(context);
   }

   
   private void syncPairingState(SubsystemContext<PlaceMonitorSubsystemModel> context){
      boolean allModelsIdle=true;
      boolean allModelsPairing=true;
      boolean allModelsUnPairing=true;

      for(Model model:context.models().getModels(IS_PAIRING_DEVICE)){
         if(!isModelIdle(model)){
            allModelsIdle=false;
         }
         if(!isModelPairing(model)){
            allModelsPairing=false;
         }
         if(!isModelUnpairing(model)){
            allModelsUnPairing=false;
         }

      }
      if(allModelsIdle){
         context.model().setAttribute(PlaceMonitorSubsystemCapability.ATTR_PAIRINGSTATE, PlaceMonitorSubsystemCapability.PAIRINGSTATE_IDLE);
      }
      else if(allModelsPairing){
         context.model().setAttribute(PlaceMonitorSubsystemCapability.ATTR_PAIRINGSTATE, PlaceMonitorSubsystemCapability.PAIRINGSTATE_PAIRING);
      }
      else if(allModelsUnPairing){
         context.model().setAttribute(PlaceMonitorSubsystemCapability.ATTR_PAIRINGSTATE, PlaceMonitorSubsystemCapability.PAIRINGSTATE_UNPAIRING);
      }
      else{
         context.model().setAttribute(PlaceMonitorSubsystemCapability.ATTR_PAIRINGSTATE, PlaceMonitorSubsystemCapability.PAIRINGSTATE_PARTIAL);
      }
      
   }
   
   private boolean isModelPairing(Model device){
      if(device.supports(HubCapability.NAMESPACE)){
         return device.getAttribute(HubCapability.ATTR_STATE).equals(HubCapability.STATE_PAIRING);
      }
      else if(device.supports(BridgeCapability.NAMESPACE)){
         return device.getAttribute(BridgeCapability.ATTR_PAIRINGSTATE).equals(BridgeCapability.PAIRINGSTATE_PAIRING);
      }
      else{
         throw new RuntimeException("Model is not a hub or bridge device");
      }
   }
   
   private boolean isModelIdle(Model device){
      if(device.supports(HubCapability.NAMESPACE)){
         return HubCapability.STATE_NORMAL.equals(device.getAttribute(HubCapability.ATTR_STATE));
      }
      else if(device.supports(BridgeCapability.NAMESPACE)){
         return BridgeCapability.PAIRINGSTATE_IDLE.equals(device.getAttribute(BridgeCapability.ATTR_PAIRINGSTATE));
      }
      else{
         throw new RuntimeException("Model is not a hub or bridge device");
      }
   }
   private boolean isModelUnpairing(Model device){
      if(device.supports(HubCapability.NAMESPACE)){
         return HubCapability.STATE_UNPAIRING.equals(device.getAttribute(HubCapability.ATTR_STATE));
      }
      else if(device.supports(BridgeCapability.NAMESPACE)){
         return BridgeCapability.PAIRINGSTATE_UNPAIRING.equals(device.getAttribute(BridgeCapability.ATTR_PAIRINGSTATE));
      }
      else{
         throw new RuntimeException("Model is not a hub or bridge device");
      }
   }
   
   

   


}

