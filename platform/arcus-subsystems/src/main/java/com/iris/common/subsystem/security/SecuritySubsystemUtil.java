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
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.util.NotificationsUtil;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.GlassCapability;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.capability.SecurityAlarmModeCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.dev.ContactModel;
import com.iris.messages.model.dev.DeviceConnectionModel;
import com.iris.messages.model.dev.GlassModel;
import com.iris.messages.model.dev.MotionModel;
import com.iris.messages.model.subs.SecurityAlarmModeModel;
import com.iris.messages.model.subs.SecuritySubsystemModel;
import com.iris.model.query.expression.ExpressionCompiler;
import com.iris.type.LooselyTypedReference;
import com.iris.util.TypeMarker;

public class SecuritySubsystemUtil {
   public static final String CODE_TRIGGERED_DEVICES = "TriggeredDevices";
   
   public static final String SECURITY_ALERT_KEY = "security.alert";
   public static final String SECURITY_METHOD_KEY = "security.method";
   public static final String SECURITY_ACTOR_KEY = "security.actor";
   public static enum SECURITY_METHOD { DEVICE, CLIENT, RULE}
   
   public static final String DEVICES_KEY_PARTIAL = SecurityAlarmModeCapability.ATTR_DEVICES + ":" + SecuritySubsystemCapability.ALARMMODE_PARTIAL;
   public static final String DEVICES_KEY_ON = SecurityAlarmModeCapability.ATTR_DEVICES + ":" + SecuritySubsystemCapability.ALARMMODE_ON;

   public static final String MOTIONSENSOR_COUNT_KEY_PARTIAL = SecurityAlarmModeCapability.ATTR_MOTIONSENSORCOUNT + ":" + SecuritySubsystemCapability.ALARMMODE_PARTIAL;
   public static final String MOTIONSENSOR_COUNT_KEY_ON = SecurityAlarmModeCapability.ATTR_MOTIONSENSORCOUNT + ":" + SecuritySubsystemCapability.ALARMMODE_ON;
     
   private static final Predicate<Model> IS_SECURITY_DEVICE = ExpressionCompiler.compile(SecuritySubsystemV1.QUERY_SECURITY_DEVICES);

   public static void initSystem(SubsystemContext<SecuritySubsystemModel> context) {
      context.model().setAttribute(
            Capability.ATTR_INSTANCES,
            ImmutableMap.<String, Object> of(
                  SecuritySubsystemCapability.ALARMMODE_ON, ImmutableSet.of(SecurityAlarmModeCapability.NAMESPACE),
                  SecuritySubsystemCapability.ALARMMODE_PARTIAL, ImmutableSet.of(SecurityAlarmModeCapability.NAMESPACE)
                  )
            );

      SubsystemUtils.setIfNull(context.model(), SecuritySubsystemCapability.ATTR_SECURITYDEVICES, ImmutableSet.<String> of());
      SubsystemUtils.setIfNull(context.model(), SecuritySubsystemCapability.ATTR_READYDEVICES, ImmutableSet.<String> of());
      SubsystemUtils.setIfNull(context.model(), SecuritySubsystemCapability.ATTR_ARMEDDEVICES, ImmutableSet.<String> of());
      SubsystemUtils.setIfNull(context.model(), SecuritySubsystemCapability.ATTR_OFFLINEDEVICES, ImmutableSet.<String> of());
      SubsystemUtils.setIfNull(context.model(), SecuritySubsystemCapability.ATTR_BYPASSEDDEVICES, ImmutableSet.<String> of());
      SubsystemUtils.setIfNull(context.model(), SecuritySubsystemCapability.ATTR_TRIGGEREDDEVICES, ImmutableSet.<String> of());
      SubsystemUtils.setIfNull(context.model(), SecuritySubsystemCapability.ATTR_LASTALERTTRIGGERS, ImmutableMap.<String, Date> of());
      SubsystemUtils.setIfNull(context.model(), SecuritySubsystemCapability.ATTR_CURRENTALERTTRIGGERS, ImmutableMap.<String, Date> of());
      SubsystemUtils.setIfNull(context.model(), SecuritySubsystemCapability.ATTR_CURRENTALERTCAUSE, SecuritySubsystemCapability.CURRENTALERTCAUSE_NONE);
      SubsystemUtils.setIfNull(context.model(), SecuritySubsystemCapability.ATTR_TRIGGEREDDEVICES, ImmutableMap.<String, Date> of());
      SubsystemUtils.setIfNull(context.model(), SecuritySubsystemCapability.ATTR_KEYPADS, ImmutableSet.<String> of());

      SubsystemUtils.setIfNull(context.model(), SecuritySubsystemCapability.ATTR_CALLTREE, ImmutableSet.<String> of());

      SubsystemUtils.setIfNull(context.model(), SecuritySubsystemCapability.ATTR_ALARMSTATE, SecuritySubsystemCapability.ALARMSTATE_DISARMED);
      SubsystemUtils.setIfNull(context.model(), SecuritySubsystemCapability.ATTR_ALARMMODE, SecuritySubsystemCapability.ALARMMODE_OFF);
      SubsystemUtils.setIfNull(context.model(), DEVICES_KEY_PARTIAL, ImmutableSet.<String> of());
      SubsystemUtils.setIfNull(context.model(), DEVICES_KEY_ON, ImmutableSet.<String> of());
      SubsystemUtils.setIfNull(context.model(), SecuritySubsystemCapability.ATTR_KEYPADARMBYPASSEDTIMEOUTSEC, 4);
      SubsystemUtils.setIfNull(context.model(), SecuritySubsystemCapability.ATTR_BLACKLISTEDSECURITYDEVICES, ImmutableSet.<String> of());
            
      initInstances(context);            
   }

   private static void initInstances(SubsystemContext<SecuritySubsystemModel> context) {
      SimpleModel model = new SimpleModel();
      SecurityAlarmModeModel samm = new SecurityAlarmModeModel(model);
      samm.setEntranceDelaySec(30);
      samm.setExitDelaySec(30);
      samm.setAlarmSensitivityDeviceCount(0);
      samm.setSilent(false);
      samm.setSoundsEnabled(true);
      samm.setMotionSensorCount(0);
      setMultiInstanceModel(context, model, SecuritySubsystemCapability.ALARMMODE_PARTIAL);
      setMultiInstanceModel(context, model, SecuritySubsystemCapability.ALARMMODE_ON);
   }

   private static void setMultiInstanceModel(SubsystemContext<SecuritySubsystemModel> context, SimpleModel model, String instance) {
      for (Entry<String, Object> entry : model.toMap().entrySet()){
      	SubsystemUtils.setIfNull(context.model(), entry.getKey() + ":" + instance, entry.getValue());
      }
   }

   public static boolean isSecurityDevice(Model m) {
      return IS_SECURITY_DEVICE.apply(m);
   }

   public static boolean isDisconnected(Model model) {
      return DeviceConnectionModel.isStateOFFLINE(model);
   }
   
   public static boolean isBlacklisted(Model model, Set<String> blacklistedProductIds) {
	   if(blacklistedProductIds != null && blacklistedProductIds.size() > 0) {
		   String curProductId = model.getAttribute(TypeMarker.string(), DeviceCapability.ATTR_PRODUCTID).or("").toLowerCase();
		   return blacklistedProductIds.contains(curProductId);
	   }else {
		   return false;
	   }
   }

   public static String getAccountOwnerAddress(SubsystemContext<SecuritySubsystemModel> context) {
      return NotificationsUtil.getAccountOwnerAddress(context);
   }
   
   /**
    * This indicates if there are triggered devices that would prevent arming fully.
    * NOTE: Motion sensors will not require the alarm to be bypassed.
    * @param mode
    * @param context
    * @return
    */
   public static boolean hasTriggeredDevicesForMode(String mode,SubsystemContext<SecuritySubsystemModel> context){
      return !getTriggeredDevicesForMode(mode, context).isEmpty();
   }
   
   /**
    * This returns the set of devices that should be bypassed in order to arm bypassed.
    * NOTE: Motion sensors will not require the alarm to be bypassed.
    * @param mode
    * @param context
    * @return
    */
   public static Set<String> getTriggeredDevicesForMode(String mode, final SubsystemContext<SecuritySubsystemModel> context){
      Set<String> devicesToArm = context.model().getAttribute(TypeMarker.setOf(String.class), SecurityAlarmModeCapability.ATTR_DEVICES + ":" + mode, ImmutableSet.<String> of());
      Set<String> triggers = Sets.newHashSet(
    		  			Iterables.filter(context.model().getTriggeredDevices(), new Predicate<String>() {
							@Override
							public boolean apply(String input) {
								if (input != null) {
									Model curModel = context.models().getModelByAddress(Address.fromString(input));
									if(curModel != null) {
										 //by pass motion sensors
										return !curModel.supports(MotionCapability.NAMESPACE);
									}
								}								
								return false;
							}
						})
    		  			
    		  );           
      triggers.retainAll(devicesToArm);
      return triggers;
   }

   public static boolean isTriggered(Model model) {
      // note this checks for actively triggered and then drops through to the
      // next
      // because we could theoretically have a motion & glass break sensor or
      // something of that nature

      if (model.supports(MotionCapability.NAMESPACE) && MotionModel.isMotionDETECTED(model)){
         return true;
      }

      if (model.supports(ContactCapability.NAMESPACE) && ContactModel.isContactOPENED(model)){
         return true;
      }

      if (model.supports(GlassCapability.NAMESPACE) && GlassModel.isBreakDETECTED(model)){
         return true;
      }

      return false;
   }

   public static Set<String> getPartialDevices(SubsystemContext<SecuritySubsystemModel> context) {
      return context.model().getAttribute(TypeMarker.setOf(String.class), DEVICES_KEY_PARTIAL, ImmutableSet.<String> of());
   }

   public static Set<String> getOnDevices(SubsystemContext<SecuritySubsystemModel> context) {
      return context.model().getAttribute(TypeMarker.setOf(String.class), DEVICES_KEY_ON, ImmutableSet.<String> of());
   }

   /**
    * Returns the set of devices which are currently active. If the alarm is
    * disarmed this is all securityDevices, if it is armed then it is the set
    * from the current mode.
    * 
    * @param context
    * @return
    */
   public static Set<String> getActiveDevices(SubsystemContext<SecuritySubsystemModel> context) {
      if (context.model().isAlarmModeOFF()){
         return context.model().getSecurityDevices();
      }
      else if (context.model().isAlarmModeON()){
         return getOnDevices(context);
      }
      else{
         return getPartialDevices(context);
      }
   }

   public static void addAlertCause(String deviceAddress) {
      // TODO Auto-generated method stub

   }

   public static boolean hasMetTriggerThreshold(SubsystemContext<SecuritySubsystemModel> context) {
      int sensitivity = context.model().getAttribute(TypeMarker.integer(), SecurityAlarmModeCapability.ATTR_ALARMSENSITIVITYDEVICECOUNT + ":" + context.model().getAlarmMode(), 1);
      Map<String,Date>lastTriggers=context.model().getLastAlertTriggers();
      if (lastTriggers.size() < sensitivity){
         return false;
      }
      return true;
   }

   public static boolean soundsEnabled(SubsystemContext<SecuritySubsystemModel> context) {
      // Get alarm sound flag, default to value for instance SecurityAlarmMode "ON"
      String mode = context.model().getAlarmMode();
      if(SecuritySubsystemCapability.ALARMMODE_OFF.equals(mode)) {
         mode = SecuritySubsystemCapability.ALARMMODE_ON;
      }
	   return context.model().getAttribute(
	         TypeMarker.bool(),
            SecurityAlarmModeCapability.ATTR_SOUNDSENABLED + ":" + mode,
            Boolean.TRUE
      );
   }  
   
   public static boolean silentAlarm(SubsystemContext<SecuritySubsystemModel> context) {
      // Get alarm sound flag, default to value for instance SecurityAlarmMode "ON"
      String mode = context.model().getAlarmMode();
      if(SecuritySubsystemCapability.ALARMMODE_OFF.equals(mode)) {
         mode = SecuritySubsystemCapability.ALARMMODE_ON;
      }
      return context.model().getAttribute(
            TypeMarker.bool(),
            SecurityAlarmModeCapability.ATTR_SILENT + ":" + mode,
            Boolean.TRUE
      );
   }
   
   public static void clearLastAlertFields(SubsystemContext<SecuritySubsystemModel> context){
      context.model().setLastAlertTime(null);
      context.model().setLastAlertCause(null);
      context.model().setLastAlertTriggers(ImmutableMap.<String, Date>of());

   }
   
   public static void clearLastAcknowledgeFields(SubsystemContext<SecuritySubsystemModel> context){
      context.model().setLastAcknowledgementTime(null);
      context.model().setLastAcknowledgedBy(null);
      context.model().setLastAcknowledgement(null);
   }
   
   public static int currentModeExitDelaySec(SubsystemContext<SecuritySubsystemModel> context){
      int exitDelaySec = context.model().getAttribute(TypeMarker.integer(), SecurityAlarmModeCapability.ATTR_EXITDELAYSEC + ":" + context.model().getAlarmMode(), 0);
      return exitDelaySec;
   }
   
   public static int currentModeEntranceDelaySec(SubsystemContext<SecuritySubsystemModel> context){
      int entranceDelaySec = context.model().getAttribute(TypeMarker.integer(), SecurityAlarmModeCapability.ATTR_ENTRANCEDELAYSEC + ":" + context.model().getAlarmMode(), 0);
      return entranceDelaySec;
   }
   
   
   public static void setMessageSource(PlatformMessage message, SubsystemContext<SecuritySubsystemModel> context) {	   
	   context.setVariable(SECURITY_METHOD_KEY, message.getSource().getRepresentation());	   
	   context.setVariable(SECURITY_ACTOR_KEY, message.getActor());
   }
   
   public static void clearMessageSource(SubsystemContext<SecuritySubsystemModel> context) {     
      context.setVariable(SECURITY_METHOD_KEY, null);     
      context.setVariable(SECURITY_ACTOR_KEY, null);
   }
   
   private static String getMessageSource(SubsystemContext<SecuritySubsystemModel> context) {
	   LooselyTypedReference val = context.getVariable(SECURITY_METHOD_KEY);
	   if(val != null) {
		   return val.as(String.class);
	   }
	   return "";  //TODO - default value?
		   
   }
   
   private static String getMethod(SubsystemContext<SecuritySubsystemModel> context)  {
	   LooselyTypedReference val = context.getVariable(SECURITY_METHOD_KEY);
	   if(val != null){
		   String sourceAddressStr = val.as(String.class);
		   if(sourceAddressStr != null && sourceAddressStr.startsWith(MessageConstants.DRIVER)) {
			   return SECURITY_METHOD.DEVICE.toString();
		   } else if (sourceAddressStr != null && sourceAddressStr.startsWith(MessageConstants.CLIENT)) {
			   return SECURITY_METHOD.CLIENT.toString();
		   }
	   }
	   return SECURITY_METHOD.RULE.toString(); 
   }
   
   private static Address setActorForEvent(SubsystemContext<SecuritySubsystemModel> context) {
	   //Address existingActor = context.get
	   LooselyTypedReference val = context.getVariable(SECURITY_ACTOR_KEY);
	   if(val != null){
		   Address curActor = val.as(Address.class);
		   if(curActor != null) {
			   context.setActor(curActor);
		   }
	   }
	   return null;
   }
   
   public static void broadcastEventOnDisarmed(SubsystemContext<SecuritySubsystemModel> context) {
	   try{
         String by = getBy(context, context.model().getLastDisarmedBy());
		   setActorForEvent(context);
		   context.broadcast(SecuritySubsystemCapability.DisarmedEvent.builder()
	      		 .withBy(by)
	      		 .withMethod(getMethod(context))
	      		 .build());
		   clearContextVariablesAfterBroadcast(context);
	   }catch(Exception e) {
		   context.logger().error("Error broadcastEventOnDisarmed" , e);
	   } 
   }
   
   public static void broadcastEventOnArmed(SubsystemContext<SecuritySubsystemModel> context) {
	   try{
	      String by = getBy(context, context.model().getLastArmedBy());
		   SecuritySubsystemModel curModel = context.model();		   		   
		   setActorForEvent(context);
		   context.broadcast(SecuritySubsystemCapability.ArmedEvent.builder()
	    		 .withAlarmMode(curModel.getAlarmMode())
	    		 .withBypassedDevices(getBypassedDevices(curModel))   
	      		 .withBy(by)
	      		 .withMethod(getMethod(context))
	      		 .withParticipatingDevices(getParticipatingDevices(curModel))	
	      		 .build());
		   clearContextVariablesAfterBroadcast(context);
	   }catch(Exception e) {
		   context.logger().error("Error broadcastEventOnArmed" , e);
	   }
   }
   
   public static void broadcastEventOnAlert(SubsystemContext<SecuritySubsystemModel> context) {	 
	   try{
		   SecuritySubsystemModel curModel = context.model();	   
		   String securityMethod = isPanic(curModel)?SecuritySubsystemCapability.AlertEvent.METHOD_PANIC:SecuritySubsystemCapability.AlertEvent.METHOD_DEVICE;
		   setActorForEvent(context);
		   context.broadcast(SecuritySubsystemCapability.AlertEvent.builder()
			   .withCause(curModel.getLastAlertCause())
			   .withTriggers(curModel.getLastAlertTriggers())
	  		   .withBy(getMessageSource(context))
	  		   .withMethod(securityMethod)  
	  		   .build());
		   clearContextVariablesAfterBroadcast(context);
	   }catch(Exception e) {
		   context.logger().error("Error broadcastEventOnAlert" , e);
	   }
	   	   
   }
   
   private static String getBy(SubsystemContext<SecuritySubsystemModel> context, String lastActor) {
      if(StringUtils.isEmpty(lastActor)) {
         return getMessageSource(context);
      }
      else {
         return lastActor;
      }
   }
   
   private static void clearContextVariablesAfterBroadcast(SubsystemContext<SecuritySubsystemModel> context) {
	   //context.setActor(null);
	   //context.setVariable(SECURITY_METHOD_KEY, null);	   
	   context.setVariable(SECURITY_ACTOR_KEY, null);
   }

   static boolean isPanic(SecuritySubsystemModel curModel) {
	   if(SecuritySubsystemCapability.CURRENTALERTCAUSE_PANIC.equals(curModel.getCurrentAlertCause())) {
		   return true;
	   }
	   else if(curModel.getLastAlertTriggers() != null && curModel.getLastAlertTriggers().size() > 0) {
		   return false;
	   }else {
		   return true;
	   }
   }
   
   private static Set<String> getParticipatingDevices (SecuritySubsystemModel curModel) {
	   if (curModel.getAlarmMode() != null && SecurityAlarmModeModel.getDevices(curModel.getAlarmMode(), curModel) != null) {
		   Set<String> retVal = new HashSet<>();
		   retVal.addAll(SecurityAlarmModeModel.getDevices(curModel.getAlarmMode(), curModel));
		   return retVal;
	   } else {
		   return null;
	   }
   }
   
   private static Set<String> getBypassedDevices(SecuritySubsystemModel curModel) {
	   if(curModel.getBypassedDevices() != null && curModel.getBypassedDevices().size() > 0) {
		   Set<String> retVal = new HashSet<>();
		   retVal.addAll(curModel.getBypassedDevices());
		   return retVal;
	   }else {
		   return null;
	   }
   }

}

