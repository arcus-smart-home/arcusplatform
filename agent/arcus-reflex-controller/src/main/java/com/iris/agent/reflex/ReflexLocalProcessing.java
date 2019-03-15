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
package com.iris.agent.reflex;

import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.hal.LEDState;
import com.iris.agent.hal.Model;
import com.iris.agent.hal.SounderMode;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlertCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubAlarmCapability;
import com.iris.messages.capability.KeyPadCapability;

@Singleton
public class ReflexLocalProcessing {

   private static final Set<String> SOUNDS_OFF = ImmutableSet.of();
   private static final Set<String> SOUNDS_KEYPAD_ONLY = ImmutableSet.of(
      KeyPadCapability.ENABLEDSOUNDS_ARMED,
      KeyPadCapability.ENABLEDSOUNDS_ARMING,
      KeyPadCapability.ENABLEDSOUNDS_BUTTONS,
      KeyPadCapability.ENABLEDSOUNDS_DISARMED
   );
   private static final Set<String> SOUNDS_ALARM_ONLY = ImmutableSet.of(KeyPadCapability.ALARMSTATE_ALERTING);
   private static final Set<String> SOUNDS_ON = ImmutableSet.of(
      KeyPadCapability.ENABLEDSOUNDS_ARMED,
      KeyPadCapability.ENABLEDSOUNDS_ARMING,
      KeyPadCapability.ENABLEDSOUNDS_BUTTONS,
      KeyPadCapability.ENABLEDSOUNDS_DISARMED,
      KeyPadCapability.ENABLEDSOUNDS_SOAKING,
      KeyPadCapability.ENABLEDSOUNDS_ALERTING
   );

   private static final Logger log = LoggerFactory.getLogger(ReflexLocalProcessing.class);
   private @Nullable ReflexController reflexController;
   private @Nullable Listener listener;

   @Inject
   public ReflexLocalProcessing() {
   }

   void setReflexController(ReflexController reflexController) {
      this.reflexController = reflexController;
   }

   public void setListener(Listener listener) {
      this.listener = listener;
   }

   public Collection<? extends ReflexDevice> getDevices() {
      ReflexController reflex = reflexController;
      if (reflex == null) {
         return ImmutableList.of();
      }

      return reflex.getDevices();
   }

   public void onDisarmed() {
      ReflexController reflex = reflexController;
      if (reflex == null) {
         log.warn("reflex controller not initialized, dropping disarmed");
         return;
      }

      log.info("disarmed");
      // Clear alarm in case still sounding
      IrisHal.setSounderMode(SounderMode.NO_SOUND);
      reflex.deliver(
         MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(
            AlertCapability.ATTR_STATE, AlertCapability.STATE_QUIET
         )));
      reflex.deliver(KeyPadCapability.DisarmedRequest.instance());
   }

   public void onArming(int exitDelay, boolean silent, boolean soundsEnabled, String alertState) {
      onArming(KeyPadCapability.BeginArmingRequest.ALARMMODE_ON, exitDelay, silent, soundsEnabled, alertState);
   }

   public void onArmingPartial(int exitDelay, boolean silent, boolean soundsEnabled, String alertState) {
      onArming(KeyPadCapability.BeginArmingRequest.ALARMMODE_PARTIAL, exitDelay, silent, soundsEnabled, alertState);
   }

   private void onArming(String mode, long exitDelay, boolean silent, boolean soundsEnabled, String alertState) {
      ReflexController reflex = reflexController;
      if (reflex == null) {
         log.warn("reflex controller not initialized, dropping arming");
         return;
      }

      log.info("arming");
      if(!HubAlarmCapability.ALARMSTATE_ALERTING.equals(alertState)) {
         if(soundsEnabled) {
            if (mode == KeyPadCapability.BeginArmingRequest.ALARMMODE_PARTIAL) {
               IrisHal.setSounderMode(SounderMode.ARMING_GRACE_EXIT_PARTIAL, (int) exitDelay);
            } else {
               IrisHal.setSounderMode(SounderMode.ARMING_GRACE_EXIT, (int) exitDelay);
            }
         }
         IrisHal.setLedState(LEDState.ALARM_GRACE_EXIT,(int) exitDelay);
         final Set<String> expectedSounds = getSounds(silent, soundsEnabled);

         if(reflex.getDevices().stream().anyMatch((d) -> {
            if(!d.getCapabilities().contains(KeyPadCapability.NAME)) {
               return false;
            }
            Set<String> sounds = (Set<String>) d.getAttribute(KeyPadCapability.ATTR_ENABLEDSOUNDS);
            return !Objects.equals(sounds, expectedSounds);
         })) {
            reflex.deliver(MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(
               KeyPadCapability.ATTR_ENABLEDSOUNDS, expectedSounds
            )));
         }

         MessageBody keypadArming = KeyPadCapability.BeginArmingRequest.builder()
            .withAlarmMode(mode)
            .withDelayInS((int) exitDelay)
            .build();
         reflex.deliver(keypadArming);
      }
   }

   private Set<String> getSounds(boolean silent, boolean soundsEnabled) {
      if(soundsEnabled) {
         return silent ? SOUNDS_KEYPAD_ONLY : SOUNDS_ON;
      }
      return silent ? SOUNDS_OFF : SOUNDS_ALARM_ONLY;
   }

   public void onArmed(boolean soundsEnabled, String alertState) {
      onArmed(KeyPadCapability.ArmedRequest.ALARMMODE_ON, soundsEnabled, alertState);
   }

   public void onArmedPartial(boolean soundsEnabled, String alertState) {
      onArmed(KeyPadCapability.ArmedRequest.ALARMMODE_PARTIAL, soundsEnabled, alertState);
   }

   private void onArmed(String mode, boolean soundsEnabled, String alertState) {
      ReflexController reflex = reflexController;
      if (reflex == null) {
         log.warn("reflex controller not initialized, dropping armed {}", mode);
         return;
      }

      log.info("armed {}", mode);
      if(!HubAlarmCapability.ALARMSTATE_ALERTING.equals(alertState)) {
         if(soundsEnabled) {
        	 switch (mode) {
        	 	case KeyPadCapability.ArmedRequest.ALARMMODE_PARTIAL:
        	 		IrisHal.setSounderMode(SounderMode.SECURITY_ALARM_PARTIAL);
        	 		break;
        	 	case KeyPadCapability.ArmedRequest.ALARMMODE_ON:
        	 		IrisHal.setSounderMode(SounderMode.SECURITY_ALARM_ON);
        	 		break;
        	 	default:
        	 		// No sound 
        	 		break;
        	 }
         }
         IrisHal.setLedState(LEDState.ALARM_ON);
         MessageBody keypadArmed =
            KeyPadCapability.ArmedRequest.builder()
               .withAlarmMode(mode)
               .build();
         reflex.deliver(keypadArmed);
      }
   }

   public void onArmFailed(String alertState) {
      ReflexController reflex = reflexController;
      if (reflex == null) {
         log.warn("reflex controller not initialized, dropping arm failed");
         return;
      }

      log.info("arm failed");
      IrisHal.setLedState(LEDState.ALARM_FAILURE);
      IrisHal.setSounderMode(SounderMode.SECURITY_ALARM_FAILED);
      if(!HubAlarmCapability.ALARMSTATE_ALERTING.equals(alertState)) {
         MessageBody unableToArm =
            KeyPadCapability
               .ArmingUnavailableRequest
               .instance();
         reflex.deliver(unableToArm);
      }
   }

   public void onDisarmFailed() {
      ReflexController reflex = reflexController;
      if (reflex == null) {
         log.warn("reflex controller not initialized, dropping disarm failed");
         return;
      }

      log.info("disarm failed");
      IrisHal.setSounderMode(SounderMode.ARMED);
      IrisHal.setLedState(LEDState.ALARM_ON);
      reflex.deliver(MessageBody.buildMessage("SECURITY", ImmutableMap.of()));
   }

   public void onPrealert(String mode, int entranceDelay, boolean soundsEnabled, String alertState) {
      ReflexController reflex = reflexController;
      if (reflex == null) {
         log.warn("reflex controller not initialized, dropping prealert");
         return;
      }

      log.info("prealert");
      if(!HubAlarmCapability.ALARMSTATE_ALERTING.equals(alertState)) {
         if(soundsEnabled) {
            IrisHal.setSounderMode(SounderMode.ARMING_GRACE_ENTER, entranceDelay);
         }
         IrisHal.setLedState(LEDState.ALARM_GRACE_ENTER,entranceDelay);
         MessageBody keypadSoaking =
            KeyPadCapability.SoakingRequest.builder()
               .withAlarmMode(mode)
               .withDurationInS(entranceDelay)
               .build();
         reflex.deliver(keypadSoaking);
      }
   }

   public void onAlerting(SounderMode tone, String alarmMode, LEDState led, int duration) {
      ReflexController reflex = reflexController;
      if (reflex == null) {
         log.warn("reflex controller not initialized, dropping alerting");
         return;
      }

      log.info("alerting");
      //Have the hub start playing the appropriate sounds & lights 
      if(tone != null) {
         int soundDuration = 300; // Play for 5 minutes by default!
         // For v3 hub, override with provided duration
         if (Model.isV3(IrisHal.getModel())) {
            soundDuration = duration;
         }
         log.info("reflex control playing sound {} for {} seconds", tone, soundDuration);
         IrisHal.setSounderMode(tone, soundDuration);
         IrisHal.setLedState(led,duration);
         reflex.deliver(
            MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(
               AlertCapability.ATTR_STATE, AlertCapability.STATE_ALERTING
            )));
      }
      // MFF - would be nice to know what this was trying to fix.  This section is not there on disarmed or prealert.
      // just look at one keypad and assume the regular syncing of sounds has been successfully applied to all
      ReflexDevice kp;
      try {
         kp = reflex.getDevices().stream().filter((d) -> d.getCapabilities().contains(KeyPadCapability.NAME)).findFirst().get();
      } catch (NoSuchElementException e) {
         kp = null;
      }
      if( kp == null) {
    	 log.debug("No Keypads");
      } else {
         Object enabledSounds = kp.getAttribute(KeyPadCapability.ATTR_ENABLEDSOUNDS);
    	 Set<String> sounds;
    	 if (enabledSounds == null ) {
    	    log.warn("No sounds enabled on keypad"); 
    	    sounds = new HashSet<>();
    	 } else {
    		 log.info("Enabled sounds: {}",(Set<String>)enabledSounds);
    		 sounds = new HashSet<>((Set<String>)enabledSounds);
    	 }
         boolean soundsChanged = false;
         if(tone == null) {
            soundsChanged = sounds.remove(KeyPadCapability.ENABLEDSOUNDS_ALERTING);
         } else {
            soundsChanged = sounds.add(KeyPadCapability.ENABLEDSOUNDS_ALERTING);
         }

         if(soundsChanged) {
            reflex.deliver(MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(
                    KeyPadCapability.ATTR_ENABLEDSOUNDS, sounds)));
         }
         reflex.deliver(KeyPadCapability.AlertingRequest.builder().withAlarmMode(alarmMode == null ? KeyPadCapability.AlertingRequest.ALARMMODE_PANIC : alarmMode).build());
      }
   }

   public void onQuiet() {
      ReflexController reflex = reflexController;
      if (reflex == null) {
         log.warn("reflex controller not initialized, dropping quiet");
         return;
      }

      log.info("quiet");
      IrisHal.setSounderMode(SounderMode.NO_SOUND);
      reflex.deliver(
         MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(
            AlertCapability.ATTR_STATE, AlertCapability.STATE_QUIET
      )));
   }

   void fireDevicesUpdated() {
      if (listener != null) {
         listener.onReflexDevicesUpdated();
      }
   }

   void fireDeviceOnline(Address device) {
      if(listener != null) {
         listener.onReflexDeviceOnline(device);
      }
   }

   void fireDeviceOffline(Address device) {
      if(listener != null) {
         listener.onReflexDeviceOffline(device);
      }
   }

   public static interface Listener {
      void onReflexDevicesUpdated();
      void onReflexDeviceOnline(Address device);
      void onReflexDeviceOffline(Address device);
   }
}

