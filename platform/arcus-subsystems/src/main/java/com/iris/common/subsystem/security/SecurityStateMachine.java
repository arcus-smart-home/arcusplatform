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
package com.iris.common.subsystem.security;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.address.Address;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.model.subs.SecuritySubsystemModel;

public class SecurityStateMachine {

   public void init(SubsystemContext<SecuritySubsystemModel> context) {
      SecurityState state = state(context);
      String next = state.onStarted(context);
      transition(state, next, context);
   }
   
   public void timeout(SubsystemContext<SecuritySubsystemModel> context) {
      SecurityState state = state(context);
      String next = state.timeout(context);
      transition(state, next, context);
   }
   
   public int arm(Address armedBy, String mode, boolean bypass, SubsystemContext<SecuritySubsystemModel> context) {
      SecurityState state = state(context);
      int delay = bypass ? state.armBypassed(armedBy, mode, context) : state.arm(armedBy, mode, context);
      // if arm is successful it always transitions to ARMING
      String next = SecuritySubsystemCapability.ALARMSTATE_ARMING;
      transition(state, next, context);
      return delay;
   }
   
   public void disarm(Address disarmedBy, SubsystemContext<SecuritySubsystemModel> context) {
      SecurityState state = state(context);
      String next = state.disarm(disarmedBy, context);
      transition(state, next, context);
   }
   
   public void transitionTo(String next, SubsystemContext<SecuritySubsystemModel> context) {
      SecurityState state = state(context);
      transition(state, next, context);
   }
   
   public void triggerDevice(Address device, SubsystemContext<SecuritySubsystemModel> context) {
      SecurityState state = state(context);
      String next = state.triggerDevice(device, context);
      transition(state, next, context);
   }
   
   public void panic(Address panicBy, SubsystemContext<SecuritySubsystemModel> context) {
      SecurityState state = state(context);
      String next = state.panic(panicBy, context);
      transition(state, next, context);
   }
   
   protected SecurityState state(SubsystemContext<SecuritySubsystemModel> context) {
      return state(context.model().getAlarmState());
   }
   
   protected SecurityState state(String state) {
      switch(state) {
      case SecuritySubsystemCapability.ALARMSTATE_DISARMED :
         return SecurityState.disarmed;
      case SecuritySubsystemCapability.ALARMSTATE_ARMING:
         return SecurityState.arming;
      case SecuritySubsystemCapability.ALARMSTATE_ARMED:
         return SecurityState.armed;
      case SecuritySubsystemCapability.ALARMSTATE_SOAKING:
         return SecurityState.soaking;
      case SecuritySubsystemCapability.ALARMSTATE_ALERT:
         return SecurityState.alert;
      default:
         throw new IllegalStateException("Unrecognized state: " + state);
      }
   }
   
   protected void transition(String state, String next, SubsystemContext<SecuritySubsystemModel> context) {
      transition(state(state), next, context);
   }
   
   protected void transition(SecurityState state, String next, SubsystemContext<SecuritySubsystemModel> context) {
      if(next.equals(state.getName())) {
         return;
      }
      try {
         state.onExit(context);
      }
      catch(Exception e) {
         context.logger().warn("Error exiting state [{}]", state, e);
      }
      context.model().setAlarmState(next);
      try {
         state = state(next);
         next = state.onEnter(context);
      }
      catch(Exception e) {
         context.logger().warn("Error entering state [{}]", next, e);
      }
      transition(state, next, context);
   }
   
    

}

