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
package com.iris.common.subsystem.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.iris.capability.util.Addresses;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.AlertCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubSoundsCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.messages.type.CallTreeEntry;
import com.iris.model.predicate.Predicates;

public class NotificationsUtil {

   public static final String SECURITY_ALERT_KEY = "security.alert";
   public static final String SECURITY_PANIC_ALERT_KEY = "security.alert.panic";
   public static final String DEVICES_PARAMETER = "devices";
   
   public static final Predicate<Model> isSiren = Predicates.isA(AlertCapability.NAMESPACE);
   public static final Predicate<Model> isNotAKeypad = Predicates.isNotA(KeyPadCapability.NAMESPACE);
   private static final Predicate<Model> isHub = Predicates.isA(HubCapability.NAMESPACE);
   private static final Predicate<Model> isKeyPad = Predicates.isA(KeyPadCapability.NAMESPACE);
   
   public static final Integer CALL_TREE_ALERT_TIMEOUT_MS = 90000;

   public static void sendNotification(SubsystemContext<?> context, String key, String personId, String priority) {
	   try{
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
	   }catch(IllegalArgumentException e) {
		   checkAddressAndLogError(context, personId, key);
	   }
   }
   
   private static void checkAddressAndLogError(SubsystemContext<?> context, String addr, String msgKey) {
	   //decide if exception is due to null addr
	   String id = null;
	   boolean err = false;
	   try{
		   id = Addresses.getId(addr);
		   if(StringUtils.isBlank(id) ) {
			   err = true;
		   }else {
			   UUID.fromString(id);  //Is valid UUID
		   }
	   }catch(Exception e){
		   err = true;
	   }
	   if(err) {
		   context.logger().warn(String.format("Fail to send notification with key %s for place id %s because %s is not a valid person address", msgKey, context.getPlaceId(), addr), new IllegalStateException());		   
	   }
   }
   
   
   public static void sendNotification(SubsystemContext<?> context, String key, String personId, String priority,Map<String,String>parameters) {
      requestNotification(context, key, personId, priority,parameters,null);
   }
   public static void requestNotification(SubsystemContext<?> context, String key, String personId, String priority,Map<String,String>parameters,Integer ttl) {
	  try {
	      MessageBody message =
	            NotificationCapability
	            .NotifyRequest
	                  .builder()
	                  .withMsgKey(key)
	                  .withPlaceId(context.getPlaceId().toString())
	                  .withPersonId(Address.fromString(personId).getId().toString())
	                  .withPriority(priority)
	                  .withMsgParams(parameters)
	                  .build();
	      if(ttl!=null){
	         context.request(Address.platformService(NotificationCapability.NAMESPACE), message,ttl);
	      }else{
	         context.request(Address.platformService(NotificationCapability.NAMESPACE), message);
	      }
	  }catch(IllegalArgumentException e) {
		   checkAddressAndLogError(context, personId, key);
	  }catch(Exception e) {
		  context.logger().error(String.format("Fail to send notification with key %s to person %s with priority %s", key, personId, priority), e);
	  }
   }   
   public static String getAccountOwnerAddress(SubsystemContext<? extends SubsystemModel> context){
      String accountOwner = "SERV:" + PersonCapability.NAMESPACE + ":" + getAccountOwnerId(context);
      return accountOwner;
   }
   
   public static String getAccountOwnerId(SubsystemContext<? extends SubsystemModel> context){
      String accountId = (String) context.models().getAttributeValue(Address.platformService(context.getAccountId(), AccountCapability.NAMESPACE), AccountCapability.ATTR_OWNER);
      return accountId;
   }
   
   public static List<Map<String,Object>> fixCallTree(List<Map<String,Object>> callTree) {
      List<Map<String,Object>> fixed = new ArrayList<>(callTree.size());
      for(Map<String,Object> entry : callTree) {
         CallTreeEntry cte = new CallTreeEntry(entry);
         if(!cte.getPerson().startsWith("SERV:")) {
            cte.setPerson("SERV:" + PersonCapability.NAMESPACE + ":" + cte.getPerson());
         }
         fixed.add(cte.toMap());
      }
      return fixed;
   }
   
   public static CallTreeEntry createCallEntry(Address person, boolean enabled){
      CallTreeEntry cte = new CallTreeEntry();
      cte.setEnabled(enabled);
      cte.setPerson(person.getRepresentation());
      return cte;
   }
   
   public static boolean removeCTEByAddress(String address,List<Map<String,Object>>list){
      boolean removed = false;
      for(Map<String,Object>entry:list){
         if(entry.get(CallTreeEntry.ATTR_PERSON).equals(address)){
            list.remove(entry);
            removed=true;
            break;
         }
      }
      return removed;
   }
   
   public static Map<String, Address> notifiyDevicesForAlert(SubsystemContext<?> context,String alertCapabilityState, Predicate<Model> match){
      MessageBody alert =
            MessageBody
               .buildMessage(
                     Capability.CMD_SET_ATTRIBUTES,
                     ImmutableMap.<String, Object>of(AlertCapability.ATTR_STATE, alertCapabilityState)
               );
      return SubsystemUtils.sendTo(context, match, alert);
   }
   
   public static Map<String, Address> notifyHubSounds(SubsystemContext<?> context, String tone, int durationSec){
      MessageBody hubsound =
            HubSoundsCapability.PlayToneRequest.builder()
               .withTone(tone)
               .withDurationSec(durationSec)
               .build();
      return SubsystemUtils.sendTo(context, isHub, hubsound);
   }
   
   public static Map<String, Address> disarmKeypad(SubsystemContext<?> context){
      MessageBody disarm =
            KeyPadCapability.DisarmedRequest.instance();
      return SubsystemUtils.sendTo(context, isKeyPad, disarm);
   }   
   
   public static void soundTheAlarms(SubsystemContext<?> context) {
      NotificationsUtil.notifiyDevicesForAlert(context, AlertCapability.STATE_ALERTING, isSiren);
 	 /*
 	  * Sending a new play sounds request should preempt any existing ones executing on the hub
 	  * Unfortunately this isn't happening and the TONE_ARMED request gets ignored. Sending
 	  * The TONE_NO_SOUND request before the TONE_INTRUDER request fixes the issue.
 	  */
      NotificationsUtil.notifyHubSounds(context, HubSoundsCapability.PlayToneRequest.TONE_NO_SOUND, -1); 
      NotificationsUtil.notifyHubSounds(context, HubSoundsCapability.PlayToneRequest.TONE_INTRUDER, -1);
   }

   /*
    *  Send QUIET to all AlertCapable devices
    */
   public static void stopTheAlarms(SubsystemContext<?> context) {
      stopTheAlarms(context, isSiren);
   }
   
   
   /*
    *  Send QUIET to devices that meet the modelsThatMatch predicate criteria, exclude all others.
    *  This may be necessary for certain devices that have problems receiving QUIET prior to
    *  other messages in the same stop the alarm workflows e.g. Iris Keypads that receive
    *  disarm messages as the security subsystem exits the Alert state
    */
   public static void stopTheAlarms(SubsystemContext<?> context, Predicate<Model> modelsThatMatch) {
      NotificationsUtil.notifiyDevicesForAlert(context, AlertCapability.STATE_QUIET, modelsThatMatch);
      NotificationsUtil.notifyHubSounds(context, HubSoundsCapability.PlayToneRequest.TONE_NO_SOUND, -1);      
   }   
   
   public static class SafetyClear {
      public static final String KEY = "safety.clear";
      public static final String PARAM_CANCELL_BY_FIRSTNAME = "cancelByFirstName";
      public static final String PARAM_CANCELL_BY_LASTNAME = "cancelByLastName";
   }
   
   public static class CareAlarmCancelled {
      public static final String KEY = "care.alarm.cancelled";
      public static final String PARAM_CANCELL_BY_FIRSTNAME = "cancelByFirstName";
      public static final String PARAM_CANCELL_BY_LASTNAME = "cancelByLastName";
      public static final String PARAM_TIME = "time";
   }
   
   public static class SafetyAlertCommon {
	   public static final String PARAM_DEVICE_NAME = "deviceName";
	   public static final String PARAM_DEVICE_TYPE = "deviceType";
   }
   
   public static class CareAlarm {
      public static final String KEY_BY_RULE = "care.alarm.rule-triggered";
      public static final String KEY_BY_BEHAVIOR = "care.alarm.behavior-triggered";
      public static final String PARAM_SOURCE_NAME = "sourceName";
   } 
   
   public static class SecurityAlarm {
      public static final String KEY = "security.alert";
      public static final String PARAM_DEVICE_NAME = "deviceName";
      public static final String PARAM_DEVICE_TYPE = "deviceType";
   } 
   
   public static class ContinuousWaterUse {
      public static final String KEY = "water.use.continuous.triggered";
      public static final String PARAM_DEVICE_NAME = "deviceName";
      public static final String PARAM_CONTINUOUS_RATE = "continuousRate";
      public static final String PARAM_CONTINUOUS_DURATION = "continuousDuration";
   }
   
   public static class ExcessiveWaterUse {
      public static final String KEY = "water.use.excessive.triggered";
      public static final String PARAM_DEVICE_NAME = "deviceName";
   }
   
   public static class WaterSoftenerLowSalt {
      public static final String KEY = "water.lowsalt";
      public static final String PARAM_DEVICE_NAME = "deviceName";
      public static final String PARAM_CURRENT_SALT_LEVEL = "currentSaltLevel";
   }
}

