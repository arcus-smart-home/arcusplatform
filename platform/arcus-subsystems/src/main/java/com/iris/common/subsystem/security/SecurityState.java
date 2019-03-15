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

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.util.CallTree;
import com.iris.common.subsystem.util.NotificationsUtil;
import com.iris.common.subsystem.util.NotificationsUtil.SecurityAlarm;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HubSoundsCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.SecurityAlarmModeCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.capability.SecuritySubsystemCapability.ArmBypassedRequest;
import com.iris.messages.capability.SecuritySubsystemCapability.ArmRequest;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.subs.SecuritySubsystemModel;
import com.iris.util.TypeMarker;

public class SecurityState {
   private static final String LAST_ALERT_TRIGGER_ALARM = "ALARM";

   protected final CallTree callTree = new CallTree(SecuritySubsystemCapability.ATTR_CALLTREE);

   private static final Predicate<Model> excludeKeypad = Predicates.and(NotificationsUtil.isSiren, NotificationsUtil.isNotAKeypad);
   
   final String name;

   SecurityState(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public String onEnter(SubsystemContext<SecuritySubsystemModel> context) {
      context.logger().debug("Entering state: [{}]", getName());
      return getName();
   }

   public String onStarted(SubsystemContext<SecuritySubsystemModel> context) {
      context.logger().debug("Starting State: [{}]", getName());
      KeypadState.sync(context);
      return getName();
   }

   public void onExit(SubsystemContext<SecuritySubsystemModel> context) {
      SubsystemUtils.clearTimeout(context);
      context.logger().debug("Exiting state: [{}]", getName());
   }

   public String triggerDevice(Address device, SubsystemContext<SecuritySubsystemModel> context) {
      context.logger().debug("Device triggered [{}]", device);
      return getName();
   }
   
   public int arm(Address armedBy, String mode, SubsystemContext<SecuritySubsystemModel> context) {
      throw new ErrorEventException(Errors.CODE_INVALID_REQUEST, "The system can only be armed when in disarmed state!");
   }

   public int armBypassed(Address armedBy, String mode, SubsystemContext<SecuritySubsystemModel> context) {
      throw new ErrorEventException(Errors.CODE_INVALID_REQUEST, "The system can only be armed when in disarmed state!");
   }

   public String disarm(Address disarmedBy, SubsystemContext<SecuritySubsystemModel> context) {
      context.model().setLastDisarmedBy(disarmedBy != null ? disarmedBy.getRepresentation() : null);
      return SecuritySubsystemCapability.ALARMSTATE_DISARMED;
   }

   public String timeout(SubsystemContext<SecuritySubsystemModel> context) {
      context.logger().debug("State timeout [{}]", context.model().getAlarmState());
      return getName();
   }
   
   // FIXME expose lastPanicBy (actor) and lastPanicFrom (source)
   public String panic(Address panicBy, SubsystemContext<SecuritySubsystemModel> context) {
      Date timestamp = new Date();
      context.model().setLastAlertCause(SecuritySubsystemV1.LAST_ALERT_CAUSE_PANIC);
      if (panicBy == null){
         context.model().setLastAlertTriggers(ImmutableMap.<String, Date> of());
      }
      else{
         context.model().setLastAlertTriggers(ImmutableMap.of(panicBy.getRepresentation(), timestamp));
      }
      return SecuritySubsystemCapability.ALARMSTATE_ALERT;
   }

   protected void sendNotification(SubsystemContext<SecuritySubsystemModel> context, String key, String personId, String priority) {
      MessageBody message =
            NotificationCapability
            .NotifyRequest
                  .builder()
                  .withMsgKey(key)
                  .withPlaceId(context.getPlaceId().toString())
                  .withPersonId(Address.fromString(personId).getId().toString())
                  .withPriority(priority)
                  .build();
      context.send(Address.platformService(NotificationCapability.NAMESPACE), message);

   }

   final static SecurityState disarmed = new SecurityState(SecuritySubsystemCapability.ALARMSTATE_DISARMED) {

      @Override
      public String onEnter(SubsystemContext<SecuritySubsystemModel> context) {
         context.logger().debug("Entering state: [{}]", getName());
         
         context.model().setLastDisarmedTime(new Date());
         context.model().setAlarmMode(SecuritySubsystemCapability.ALARMMODE_OFF);
         context.model().setArmedDevices(ImmutableSet.<String> of());
         context.model().setBypassedDevices(ImmutableSet.<String> of());
         context.model().setLastAlertTriggers((ImmutableMap.<String, Date> of()));
         context.model().setCurrentAlertTriggers((ImmutableMap.<String, Date> of()));
         context.model().setCurrentAlertCause(SecuritySubsystemCapability.CURRENTALERTCAUSE_NONE);
         
         NotificationsUtil.notifyHubSounds(context, HubSoundsCapability.PlayToneRequest.TONE_NO_SOUND, 0);
         MessageBody disarmed =
               KeyPadCapability.DisarmedRequest.instance();
         KeypadState.sendToKeypads(context, disarmed);
         SecuritySubsystemUtil.broadcastEventOnDisarmed(context);
         return SecuritySubsystemCapability.ALARMSTATE_DISARMED;
      }

      @Override
      public int arm(Address armedBy, String mode, SubsystemContext<SecuritySubsystemModel> context) {
         if (SecuritySubsystemUtil.hasTriggeredDevicesForMode(mode, context)){
            throw new ErrorEventException(SecuritySubsystemUtil.CODE_TRIGGERED_DEVICES, "Some devices are preventing the alarm from being armed");
         }
         if (
               !SecuritySubsystemCapability.ALARMMODE_ON.equals(mode) && 
               !SecuritySubsystemCapability.ALARMMODE_PARTIAL.equals(mode)
         ){
            throw new ErrorEventException(Errors.invalidParam(ArmRequest.ATTR_MODE));
         }

         context.model().setBypassedDevices(ImmutableSet.<String>of());

         return doArm(armedBy, mode, context);
      }

      @Override
      public int armBypassed(Address armedBy, String mode, SubsystemContext<SecuritySubsystemModel> context) {
         if (
               !SecuritySubsystemCapability.ALARMMODE_ON.equals(mode) && 
               !SecuritySubsystemCapability.ALARMMODE_PARTIAL.equals(mode)
         ){
            throw new ErrorEventException(Errors.invalidParam(ArmBypassedRequest.ATTR_MODE));
         }
         
         // bypass
         Set<String> bypassed = SecuritySubsystemUtil.getTriggeredDevicesForMode(mode, context);
         context.model().setBypassedDevices(bypassed);
         
         return doArm(armedBy, mode, context);
      }

      @Override
      public String disarm(Address disarmedBy, SubsystemContext<SecuritySubsystemModel> context) {
         // indicates stop all the sounds
         NotificationsUtil.stopTheAlarms(context);
         return getName();
      }
      
      private int doArm(Address armedBy, String mode, SubsystemContext<SecuritySubsystemModel> context) {
         if (armedBy == null){
            context.model().setLastArmedBy(null);
         }
         else {
            context.model().setLastArmedBy(armedBy.getRepresentation());
         }
         context.model().setAlarmMode(mode);

         Set<String> devicesToArm;
         Set<String> readyDevices = new HashSet<String>(context.model().getReadyDevices());

         if (SecuritySubsystemCapability.ALARMMODE_PARTIAL.equals(mode)){
            devicesToArm = SecuritySubsystemUtil.getPartialDevices(context);
         }
         else{
            devicesToArm = SecuritySubsystemUtil.getOnDevices(context);
         }

         devicesToArm.removeAll(context.model().getBypassedDevices());
         // NOTE offline devices are considered armed
         readyDevices.removeAll(devicesToArm);
         context.model().setReadyDevices(readyDevices);
         context.model().setArmedDevices(devicesToArm);
         
         return context.model().getAttribute(TypeMarker.integer(), SecurityAlarmModeCapability.ATTR_EXITDELAYSEC + ":" + mode, 0);
      }

   };

   final static SecurityState arming = new SecurityState(SecuritySubsystemCapability.ALARMSTATE_ARMING) {

      @Override
      public String onEnter(SubsystemContext<SecuritySubsystemModel> context) {
         context.logger().debug("Entering state: [{}]", getName());
         
         context.model().setLastAcknowledgement(null);
         context.model().setLastAcknowledgedBy(null);
         context.model().setLastAcknowledgementTime(null);
         
         int armingDelaySec = SecuritySubsystemUtil.currentModeExitDelaySec(context);
         if(armingDelaySec > 0) {
            SubsystemUtils.setTimeout(TimeUnit.SECONDS.toMillis(armingDelaySec), context);
   
            if (SecuritySubsystemUtil.soundsEnabled(context)){
               NotificationsUtil.notifyHubSounds(context, HubSoundsCapability.PlayToneRequest.TONE_ARMING, armingDelaySec);
            }
            MessageBody arming =
                  KeyPadCapability.BeginArmingRequest
                     .builder()
                     .withAlarmMode(context.model().getAlarmMode())
                     .withDelayInS(armingDelaySec)
                     .build();
            KeypadState.sendToKeypads(context, arming);
   
            return SecuritySubsystemCapability.ALARMSTATE_ARMING;
         }
         else {
            return armed(context);
         }
      }

      public String timeout(SubsystemContext<SecuritySubsystemModel> context) {
         return armed(context);
      }
      
      public String armed(SubsystemContext<SecuritySubsystemModel> context) {
         // only do this on the transition from ARMING to ARMED
         SecuritySubsystemUtil.broadcastEventOnArmed(context);
         return SecuritySubsystemCapability.ALARMSTATE_ARMED;
      }
   };

   final static SecurityState armed = new SecurityState(SecuritySubsystemCapability.ALARMSTATE_ARMED) {

      public String triggerDevice(Address device, SubsystemContext<SecuritySubsystemModel> context) {
         if (context.model().getArmedDevices().contains(device.getRepresentation())){
            return SecuritySubsystemCapability.ALARMSTATE_SOAKING;
         }
         return SecuritySubsystemCapability.ALARMSTATE_ARMED;
      }

      @Override
      public String onEnter(SubsystemContext<SecuritySubsystemModel> context) {
         context.logger().debug("Entering state: [{}] mode: [{}]", getName(), context.model().getAlarmMode());
         
         SecuritySubsystemUtil.clearLastAlertFields(context);
         context.model().setLastArmedTime(new Date());
         if (SecuritySubsystemUtil.soundsEnabled(context)){
        	 /*
        	  * Sending a new play sounds request should pre-empt any existing ones executin on the hub
        	  * Unfortunately this isn't happening and the TONE_ARMED request gets ignored. Sending
        	  * The TONE_NO_SOUND request before the TONE_ARMED request fixes the issue.
        	  */
            NotificationsUtil.notifyHubSounds(context, HubSoundsCapability.PlayToneRequest.TONE_NO_SOUND, -1);         	 
            NotificationsUtil.notifyHubSounds(context, HubSoundsCapability.PlayToneRequest.TONE_ARMED, 0);
         }
         MessageBody armed =
               KeyPadCapability.ArmedRequest.builder()
                  .withAlarmMode(context.model().getAlarmMode())
                  .build();
         KeypadState.sendToKeypads(context, armed);
         
         return SecuritySubsystemCapability.ALARMSTATE_ARMED;
      }

   };

   final static SecurityState soaking = new SecurityState(SecuritySubsystemCapability.ALARMSTATE_SOAKING) {
      @Override
      public String onEnter(SubsystemContext<SecuritySubsystemModel> context) {
         context.logger().debug("Entering state: [{}] mode: [{}]", getName(), context.model().getAlarmMode());
         
         int timeoutSec = SecuritySubsystemUtil.currentModeEntranceDelaySec(context);
         if (timeoutSec > 0){
            if (SecuritySubsystemUtil.soundsEnabled(context)){
               if (!SecuritySubsystemUtil.silentAlarm(context)) {
                  NotificationsUtil.notifyHubSounds(context, HubSoundsCapability.PlayToneRequest.TONE_ARMING, timeoutSec);
               }
               MessageBody soaking = 
                     KeyPadCapability.SoakingRequest.builder()
                        .withAlarmMode(context.model().getAlarmMode())
                        .withDurationInS(timeoutSec)
                        .build();
               KeypadState.sendToKeypads(context, soaking);
            }
            SubsystemUtils.setTimeout(TimeUnit.SECONDS.toMillis(timeoutSec), context);
            return SecuritySubsystemCapability.ALARMSTATE_SOAKING;
         }
         else{
            return timeout(context);
         }
      }

      public String timeout(SubsystemContext<SecuritySubsystemModel> context) {
         if (!SecuritySubsystemUtil.hasMetTriggerThreshold(context)){
            return SecuritySubsystemCapability.ALARMSTATE_ARMED;
         }
         
         context.model().setLastAlertCause(LAST_ALERT_TRIGGER_ALARM);
         return SecuritySubsystemCapability.ALARMSTATE_ALERT;
      }
   };

   final static SecurityState alert = new SecurityState(SecuritySubsystemCapability.ALARMSTATE_ALERT) {
      @Override
      public String onEnter(SubsystemContext<SecuritySubsystemModel> context) {
         context.logger().debug("Entering state: [{}] mode: [{}]", getName(), context.model().getAlarmMode());
         context.logger().info("Security alarm triggered by: [{}] triggers: [{}]", context.model().getLastAlertCause(), context.model().getLastAlertTriggers());
         
         alert(context, true);
         return SecuritySubsystemCapability.ALARMSTATE_ALERT;
      }

      @Override
      public void onExit(SubsystemContext<SecuritySubsystemModel> context) {
         super.onExit(context);
         NotificationsUtil.stopTheAlarms(context, excludeKeypad);
         callTree.cancel(context);
      }

      @Override
      public String panic(Address panicBy, SubsystemContext<SecuritySubsystemModel> context) {
         if(context.model().getLastAlertCause().equals(SecuritySubsystemV1.LAST_ALERT_CAUSE_PANIC)) {
            context.logger().debug("Ignoring panic request because subsystem is already panicing");
            return SecuritySubsystemCapability.ALARMSTATE_ALERT;
         }
         // TODO restart the call tree?
         context.logger().info("Upgrading security alarm to panic alarm from: [{}]", panicBy);
         
         super.panic(panicBy, context);
         context.model().setAlarmMode(SecuritySubsystemCapability.ALARMMODE_OFF);
         alert(context, false);
         
         return SecuritySubsystemCapability.ALARMSTATE_ALERT;
      }
      
      private void setCurrentTriggers(SubsystemContext<SecuritySubsystemModel> context) {
         SecuritySubsystemUtil.clearLastAcknowledgeFields(context);
         context.model().setLastAlertTime(new Date());
         context.model().setCurrentAlertTriggers(context.model().getLastAlertTriggers());
         context.model().setCurrentAlertCause(context.model().getLastAlertCause());
      }
      
      private void alert(SubsystemContext<SecuritySubsystemModel> context, boolean sendAlert) {
         setCurrentTriggers(context);
         
         String mode = context.model().getAlarmMode();
         if(SecuritySubsystemCapability.ALARMMODE_OFF.equals(mode)) {
            mode = KeyPadCapability.AlertingRequest.ALARMMODE_PANIC;
         }
         MessageBody alerting = 
               KeyPadCapability.AlertingRequest.builder()
                  .withAlarmMode(mode)
                  .build();
         KeypadState.sendToKeypads(context, alerting);
         SecuritySubsystemUtil.broadcastEventOnAlert(context);
         if(sendAlert) {
            sendAlert(context);
         }
      }
      
      protected void sendAlert(SubsystemContext<SecuritySubsystemModel> context) {
         String accountOwner = SecuritySubsystemUtil.getAccountOwnerAddress(context);
         Map<String, Date> triggeredDevices = context.model().getCurrentAlertTriggers();
         Map<String, String> params = ImmutableMap.of();
         if(triggeredDevices != null && triggeredDevices.size() > 0) {
           //TODO - what to do if there are multiple triggered devices
           Model curDevice = context.models().getModelByAddress(Address.fromString( triggeredDevices.keySet().iterator().next()));
           if(curDevice != null) {
              params = ImmutableMap.<String, String> of(
                    SecurityAlarm.PARAM_DEVICE_NAME, DeviceModel.getName(curDevice, ""),
                    SecurityAlarm.PARAM_DEVICE_TYPE, DeviceModel.getDevtypehint(curDevice, "")
                    );
           }
         }
         String alertKey = SecuritySubsystemUtil.isPanic(context.model())?NotificationsUtil.SECURITY_PANIC_ALERT_KEY:NotificationsUtil.SECURITY_ALERT_KEY;
         
         if(context.model().getCallTreeEnabled()){
            callTree.notifySequential(
                  context, 
                  alertKey, 
                  params, 
                  NotificationsUtil.CALL_TREE_ALERT_TIMEOUT_MS
            );
         }
         else{
            NotificationsUtil.sendNotification(
                  context, 
                  alertKey, 
                  accountOwner, 
                  NotificationCapability.NotifyRequest.PRIORITY_CRITICAL,
                  params
            );
         }

         if (!SecuritySubsystemUtil.silentAlarm(context)){
            NotificationsUtil.soundTheAlarms(context);
         }
      }

   };

}

