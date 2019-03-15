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

import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.KeyPadModel;
import com.iris.messages.model.subs.SecuritySubsystemModel;
import com.iris.model.predicate.Predicates;
import com.iris.util.TypeMarker;

public class KeypadState {

   public static final String KEYPAD_ALLOW_BYPASSED_FLAG_KEY = "keypad.allowBypassed.flag";
   public static final String KEYPAD_ALLOW_BYPASSED_TIMEOUT_KEY = "keypad.allowBypassed.timeout";

   public static final Predicate<Model> isKeypad = Predicates.isA(KeyPadCapability.NAMESPACE);
   
   public static final Set<String> SOUNDS_OFF = ImmutableSet.of();
   public static final Set<String> SOUNDS_KEYPAD_ONLY = ImmutableSet.of(
         KeyPadCapability.ENABLEDSOUNDS_ARMED,
         KeyPadCapability.ENABLEDSOUNDS_ARMING,
         KeyPadCapability.ENABLEDSOUNDS_BUTTONS,
         KeyPadCapability.ENABLEDSOUNDS_DISARMED
   );
   public static final Set<String> SOUNDS_ALARM_ONLY = ImmutableSet.of(KeyPadCapability.ALARMSTATE_ALERTING);
   public static final Set<String> SOUNDS_ON = ImmutableSet.of(
         KeyPadCapability.ENABLEDSOUNDS_ARMED,
         KeyPadCapability.ENABLEDSOUNDS_ARMING,
         KeyPadCapability.ENABLEDSOUNDS_BUTTONS,
         KeyPadCapability.ENABLEDSOUNDS_DISARMED,
         KeyPadCapability.ENABLEDSOUNDS_SOAKING,
         KeyPadCapability.ENABLEDSOUNDS_ALERTING
   );

   private static final Map<String, String> ALARM_STATE_TO_KEYPAD_STATE = ImmutableMap.<String, String> of(
         SecuritySubsystemCapability.ALARMSTATE_DISARMED, KeyPadCapability.ALARMSTATE_DISARMED,
         SecuritySubsystemCapability.ALARMSTATE_ARMING, KeyPadCapability.ALARMSTATE_ARMING,
         SecuritySubsystemCapability.ALARMSTATE_ALERT, KeyPadCapability.ALARMSTATE_ALERTING,
         SecuritySubsystemCapability.ALARMSTATE_SOAKING, KeyPadCapability.ALARMSTATE_SOAKING,
         SecuritySubsystemCapability.ALARMSTATE_ARMED, KeyPadCapability.ALARMSTATE_ARMED
         );

   public void enableBypassMode(SubsystemContext<SecuritySubsystemModel> context) {
      context.logger().debug("Keypad to allow for armed bypass mode");
      context.setVariable(KEYPAD_ALLOW_BYPASSED_FLAG_KEY, true);
      SubsystemUtils.setTimeout(context.model().getKeypadArmBypassedTimeOutSec()*1000, context, KEYPAD_ALLOW_BYPASSED_TIMEOUT_KEY);
   }

   public boolean isInAllowBypassMode(SubsystemContext<SecuritySubsystemModel> context) {
      Boolean allowBypass = context.getVariable(KEYPAD_ALLOW_BYPASSED_FLAG_KEY).as(TypeMarker.bool());
      context.logger().debug("Checking allow bypass value [{}]", allowBypass);
      return Boolean.TRUE.equals(allowBypass);
   }

   public void onScheduledEvent(ScheduledEvent event, SubsystemContext<SecuritySubsystemModel> context) {
      context.logger().debug("Keypad State received an onScheduledEvent for event [{}]", event);
      clearAllowBypass(context);
   }

   private void clearAllowBypass(SubsystemContext<SecuritySubsystemModel> context) {
      context.logger().debug("Keypad state clearing allow bypass");
      context.setVariable(KEYPAD_ALLOW_BYPASSED_FLAG_KEY, false);
   }
   
   public static void sendToKeypads(SubsystemContext<SecuritySubsystemModel> context, MessageBody message) {
      SubsystemUtils.sendTo(context, isKeypad, message);
   }
   

   public static void sync(SubsystemContext<SecuritySubsystemModel> context) {
      syncSounds(context);
      syncState(context);
   }
   
   public static void syncSounds(SubsystemContext<SecuritySubsystemModel> context) {
      for(Model m: context.models().getModels(isKeypad)) {
         syncSounds(m, context);
      }
   }

   public static void syncState(SubsystemContext<SecuritySubsystemModel> context) {
      for(Model m: context.models().getModels(isKeypad)) {
         syncState(m, context);
      }
   }

   private static void syncSounds(Model m, SubsystemContext<SecuritySubsystemModel> context) {
      Set<String> sounds  = getSounds(context);
      if(!sounds.equals(KeyPadModel.getEnabledSounds(m))) {
         MessageBody setSounds = MessageBody.buildMessage(
               Capability.CMD_SET_ATTRIBUTES, 
               ImmutableMap.<String, Object>of(KeyPadCapability.ATTR_ENABLEDSOUNDS, sounds)
         );
         context.request(m.getAddress(), setSounds);    
      }
   }

   private static void syncState(Model m, SubsystemContext<SecuritySubsystemModel> context) {
      String state = ALARM_STATE_TO_KEYPAD_STATE.get(context.model().getAlarmState());
      String mode  = context.model().getAlarmMode();
      
      if(KeyPadModel.getAlarmState(m, "").equals(state) && KeyPadModel.getAlarmMode(m, "").equals(mode)) {
         return;
      }
      
      MessageBody attributes = MessageBody.buildMessage(
            Capability.CMD_SET_ATTRIBUTES, 
            ImmutableMap.<String, Object>of(
                  KeyPadCapability.ATTR_ALARMSTATE, state,
                  KeyPadCapability.ATTR_ALARMMODE, mode
            )
      );
      context.request(m.getAddress(), attributes);    
   }

   private static Set<String> getSounds(SubsystemContext<SecuritySubsystemModel> context) {
      boolean isSilent = SecuritySubsystemUtil.silentAlarm(context);
      boolean isKeyPadSounds = SecuritySubsystemUtil.soundsEnabled(context);
      if(isSilent && !isKeyPadSounds) {
         return SOUNDS_OFF;
      }
      else if(isSilent && isKeyPadSounds) {
         return SOUNDS_KEYPAD_ONLY;
      }
      else if(!isSilent && !isKeyPadSounds) {
         return SOUNDS_ALARM_ONLY;
      }
      else {
         return SOUNDS_ON;
      }
   }
}

