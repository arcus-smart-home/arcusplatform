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
package com.iris.common.subsystem.alarm.smoke;

import java.util.Date;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.alarm.generic.AlertState;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.model.subs.SubsystemModel;

public class SmokeAlertState extends AlertState {

   private static final SmokeAlertState INSTANCE = new SmokeAlertState();

   public static SmokeAlertState instance() {
      return INSTANCE;
   }

   private SmokeAlertState() {
   }

   @Override
   public String onVerified(SubsystemContext<? extends SubsystemModel> context, Address actor, Date verifiedTime) {
      // this additional trigger should cause immediate dispatch if it hasn't already
      addTrigger(context, AlarmSubsystemCapability.ACTIVEALERTS_SMOKE, actor, TriggerEvent.VERIFIED_ALARM, verifiedTime);
      return AlarmCapability.ALERTSTATE_ALERT;
   }

   @Override
   public String onEnter(SubsystemContext<? extends SubsystemModel> context, String name)
   {          
      return super.onEnter(context, name);
   }   
   
   
   
}

