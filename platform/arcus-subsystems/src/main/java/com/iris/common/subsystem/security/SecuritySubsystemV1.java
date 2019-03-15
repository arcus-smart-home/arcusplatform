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
package com.iris.common.subsystem.security;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.annotation.Version;
import com.iris.common.subsystem.BaseSubsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.annotation.Subsystem;
import com.iris.common.subsystem.util.CallTree;
import com.iris.common.subsystem.util.NotificationsUtil;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.GlassCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.capability.KeyPadCapability.ArmPressedEvent;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.capability.MotorizedDoorCapability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.NotificationCapability.IvrNotificationAcknowledgedEvent;
import com.iris.messages.capability.NotificationCapability.IvrNotificationRefusedEvent;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SecurityAlarmModeCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.capability.SecuritySubsystemCapability.AcknowledgeRequest;
import com.iris.messages.capability.SecuritySubsystemCapability.ArmBypassedRequest;
import com.iris.messages.capability.SecuritySubsystemCapability.ArmBypassedResponse;
import com.iris.messages.capability.SecuritySubsystemCapability.ArmRequest;
import com.iris.messages.capability.SecuritySubsystemCapability.ArmResponse;
import com.iris.messages.capability.SecuritySubsystemCapability.DisarmRequest;
import com.iris.messages.capability.SecuritySubsystemCapability.PanicRequest;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.event.ModelAddedEvent;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.listener.annotation.OnAdded;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.messages.listener.annotation.OnRemoved;
import com.iris.messages.listener.annotation.OnScheduledEvent;
import com.iris.messages.listener.annotation.OnValueChanged;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Model;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.model.subs.SecuritySubsystemModel;

/**
 *
 */
@Singleton
@Subsystem(SecuritySubsystemModel.class)
@Version(1)
public class SecuritySubsystemV1 extends BaseSubsystem<SecuritySubsystemModel> {
   public static final String LAST_ALERT_CAUSE_PANIC = "panic";
   public static final String MODE_OFF = SecuritySubsystemCapability.ALARMMODE_OFF;
   public static final String MODE_ON = SecuritySubsystemCapability.ALARMMODE_ON;
   public static final String MODE_PARTIAL = SecuritySubsystemCapability.ALARMMODE_PARTIAL;

   public static final String ATTR_SOUNDSENABLED_PARTIAL = SecurityAlarmModeCapability.ATTR_SOUNDSENABLED + ":" + SecuritySubsystemCapability.ALARMMODE_PARTIAL;
   public static final String ATTR_SOUNDSENABLED_ON = SecurityAlarmModeCapability.ATTR_SOUNDSENABLED + ":" + SecuritySubsystemCapability.ALARMMODE_ON;
   public static final String ATTR_SILENT_PARTIAL = SecurityAlarmModeCapability.ATTR_SILENT + ":" + SecuritySubsystemCapability.ALARMMODE_PARTIAL;
   public static final String ATTR_SILENT_ON = SecurityAlarmModeCapability.ATTR_SILENT + ":" + SecuritySubsystemCapability.ALARMMODE_ON;

   public static final String QUERY_SECURITY_DEVICES =
	         "(base:caps contains '" + MotionCapability.NAMESPACE + "' OR " +
	               "base:caps contains '" + ContactCapability.NAMESPACE + "' OR " +
	               //TODO - backed out ITWO-6269 to be moved to sprint 1.12
	               //"base:caps contains '" + MotorizedDoorCapability.NAMESPACE + "' OR " + 
	               "base:caps contains '" + GlassCapability.NAMESPACE + "') AND " +
	               "!(base:caps contains '" + KeyPadCapability.NAMESPACE + "')"; 
   

   private static final String QUERY_PEOPLE =
         "base:caps contains '" + PersonCapability.NAMESPACE + "'";

   @Inject(optional = true)
   @Named("security.calltree.timeout")
   private Integer callTreeAlertTimeoutMs = 90000;
   private final SecurityStateMachine stateMachine = new SecurityStateMachine();
   private final CallTree callTree = new CallTree(SecuritySubsystemCapability.ATTR_CALLTREE);
   private final KeypadState keypadState = new KeypadState();
   //TODO - we are hard coding the product ids for now to avoid the delay to retreive the blacklisted product ids from platform-services
   private final Set<String> blacklistedProductIds = ImmutableSet.<String> of ("aeda44");
   
   

   @Override
   protected void onAdded(SubsystemContext<SecuritySubsystemModel> context) {
      super.onAdded(context);
      SecuritySubsystemUtil.initSystem(context);
   }

   // KEYPAD EVENTS
   @OnMessage(types = { KeyPadCapability.ArmPressedEvent.NAME })
   public void onKeypadArm(PlatformMessage message, SubsystemContext<SecuritySubsystemModel> context) {
      String mode = ArmPressedEvent.getMode(message.getValue());
      context.logger().debug("arming in {} : platform message {}", mode, message);
      try {
         armSecurity(message, mode, keypadState.isInAllowBypassMode(context), context);
      }
      catch(ErrorEventException e) {
         if(
               SecuritySubsystemUtil.CODE_TRIGGERED_DEVICES.equals(e.getCode()) ||
               Errors.CODE_INVALID_REQUEST.equals(e.getCode()) // can't arm from this state
         ) {
            MessageBody armUnavailble = KeyPadCapability.ArmingUnavailableRequest.instance();
            context.request(message.getSource(), armUnavailble);
         }
         else {
            context.logger().warn("Unable to arm security system from keypad", e);
            throw e;
         }
      }
   }
   
 
   
   @OnMessage(types = { KeyPadCapability.ArmingUnavailableResponse.NAME })
   public void onArmingUnavailableResponse(PlatformMessage message, SubsystemContext<SecuritySubsystemModel> context) {
	   SecuritySubsystemUtil.setMessageSource(message, context);
	   context.logger().debug("got arm unavailable response {}", message);
      keypadState.enableBypassMode(context);
   }
   
   @OnMessage(types = { KeyPadCapability.PanicPressedEvent.NAME })
   public void onKeypadPanic(PlatformMessage message, SubsystemContext<SecuritySubsystemModel> context) {
      context.logger().debug("panic platform message {}", message);
      panicSecurity(message, context);
   }

   @OnMessage(types = { KeyPadCapability.DisarmPressedEvent.NAME })
   public void onKeypadDisarm(PlatformMessage message, SubsystemContext<SecuritySubsystemModel> context) {
      context.logger().debug("disarm platform message {}", message);
      disarmSecurity(message, context);
   }

   @Override
   protected void onStarted(SubsystemContext<SecuritySubsystemModel> context) {
      super.onStarted(context);
      SecuritySubsystemUtil.initSystem(context);
      syncDevices(context);
      syncCallTreeEnabled(context);
      syncCallTree(context);
      syncSubsystemAvailable(context);
      stateMachine.init(context);
   }

   @Override
   protected void onStopped(SubsystemContext<SecuritySubsystemModel> context) {
   	super.onStopped(context);
   }
   
   /**
    * Manages {@link SecuritySubsystemCapability#ATTR_CALLTREEENABLED}. True
    * when the associated place is PREMIUM.
    * 
    * @param context
    */
   protected void syncCallTreeEnabled(SubsystemContext<SecuritySubsystemModel> context) {
      String serviceLevel = String.valueOf(context.models().getAttributeValue(Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE), PlaceCapability.ATTR_SERVICELEVEL));
      context.model().setCallTreeEnabled(ServiceLevel.isPremiumOrPromon(serviceLevel));
   }

   /**
    * Manages {@link SecuritySubsystemCapability#ATTR_CALLTREE} Ensures that
    * each person associated with the place (and no others) has a call tree
    * entry. By default the call tree will only be enabled for the account
    * owner.
    * 
    * @param context
    */
   protected void syncCallTree(SubsystemContext<SecuritySubsystemModel> context) {
      callTree.syncCallTree(context);
   }

   /**
    * Manages {@link SecuritySubsystemCapability#ATTR_SECURITYDEVICES} and
    * {@link SecurityAlarmModeCapability#ATTR_DEVICES} for both ON and PARTIAL.
    * This also triggers {@link #syncDeviceState(SubsystemContext)} and
    * {@link #syncSubsystemAvailable(SubsystemContext)}. Ensures when a new
    * security device is added that it is added to security devices and ON
    * devices. If it is not a motion sensor it will also be added to partial
    * devices.
    * 
    * @param context
    */
   protected void syncDevices(SubsystemContext<SecuritySubsystemModel> context) {
      Set<String> securityDevices = new HashSet<>();
      Set<String> blacklistedDevices = new HashSet<>();
      
      Set<String> partialDevices = new HashSet<>(SecuritySubsystemUtil.getPartialDevices(context));
      Set<String> onDevices = new HashSet<>(SecuritySubsystemUtil.getOnDevices(context));

      Set<String> existingSecurityDevices = context.model().getSecurityDevices();
      Set<String> existingBlacklistedDevices = context.model().getBlacklistedSecurityDevices();

      context.logger().debug("syncing security devices with model {}", context);
      for (Model model : context.models().getModels()){
         if (!SecuritySubsystemUtil.isSecurityDevice(model)){
            continue;
         }
         if(SecuritySubsystemUtil.isBlacklisted(model, getBlacklistedProductIds())) {
        	 blacklistedDevices.add(model.getAddress().getRepresentation());
        	 if (existingBlacklistedDevices.contains(model.getAddress().getRepresentation())){
                 // already been added to the system, don't overwrite on / partial
                 continue;
             }
        	 //Remove it from onDevices and partialDevices
        	 onDevices.remove(model.getAddress().getRepresentation());
        	 partialDevices.remove(model.getAddress().getRepresentation());
         }else {        	 
        	 securityDevices.add(model.getAddress().getRepresentation());    
        	 
        	 if (existingSecurityDevices.contains(model.getAddress().getRepresentation())){
                 // already been added to the system, don't overwrite on / partial
                 continue;
             }
             onDevices.add(model.getAddress().getRepresentation());
             if (!model.supports(MotionCapability.NAMESPACE)){
                 partialDevices.add(model.getAddress().getRepresentation());
             }            
         }
       }
      // clear out any deleted devices
      onDevices.retainAll(securityDevices);
      partialDevices.retainAll(securityDevices);

      context.model().setAttribute(SecuritySubsystemUtil.DEVICES_KEY_PARTIAL, partialDevices);
      context.model().setAttribute(SecuritySubsystemUtil.DEVICES_KEY_ON, onDevices);
      context.model().setSecurityDevices(securityDevices);
      context.model().setBlacklistedSecurityDevices(blacklistedDevices);
      
      syncMotionSensors(context, onDevices, SecuritySubsystemUtil.MOTIONSENSOR_COUNT_KEY_ON);
      syncMotionSensors(context, partialDevices, SecuritySubsystemUtil.MOTIONSENSOR_COUNT_KEY_PARTIAL);
      
      syncSubsystemAvailable(context);
      syncDeviceState(context);
      KeypadState.sync(context);
   }

   private void syncMotionSensors(SubsystemContext<SecuritySubsystemModel> context, Set<String> devices, String motionsensorCountKey) {
	  int motionSensorCount = 0;
      for(String address : devices) {
    	  Model model = context.models().getModelByAddress(Address.fromString(address));
    	  if(model != null && model.supports(MotionCapability.NAMESPACE)) {
    		  motionSensorCount++;
    	  }
      }
      context.model().setAttribute(motionsensorCountKey, motionSensorCount);	
   }

/**
    * Manages {@link SecuritySubsystemCapability#ATTR_READYDEVICES},
    * {@link SecuritySubsystemCapability#ATTR_TRIGGEREDDEVICES}, and
    * {@link SecuritySubsystemCapability#ATTR_OFFLINEDEVICES}. A security alarm
    * device must be in one of these three states, ready, offline or triggered.
    * An offline device may not accurately report its state so it can't be
    * considered triggered regardless of what the state variables report.
    * 
    * @param context
    */
   protected void syncDeviceState(SubsystemContext<SecuritySubsystemModel> context) {
      Set<String> readyDevices = new HashSet<String>();
      Set<String> triggeredDevices = new HashSet<String>();
      Set<String> offlineDevices = new HashSet<String>();
      Set<String> blacklistedDevices = new HashSet<String>();

      for (Model model : context.models().getModels()){
         if (!SecuritySubsystemUtil.isSecurityDevice(model)){
            continue;
         }
         
         if(SecuritySubsystemUtil.isBlacklisted(model, getBlacklistedProductIds())) {
        	 blacklistedDevices.add(model.getAddress().getRepresentation());
        	 continue;
         }

         if (SecuritySubsystemUtil.isDisconnected(model)){
            offlineDevices.add(model.getAddress().getRepresentation());
            continue;
         }

         if (SecuritySubsystemUtil.isTriggered(model)){
            triggeredDevices.add(model.getAddress().getRepresentation());
            continue;
         }

         readyDevices.add(model.getAddress().getRepresentation());
      }

      if (!context.model().isAlarmModeOFF()){
         readyDevices.removeAll(context.model().getArmedDevices());
         readyDevices.removeAll(context.model().getBypassedDevices());
      }
      context.model().setReadyDevices(readyDevices);
      context.model().setTriggeredDevices(triggeredDevices);
      context.model().setOfflineDevices(offlineDevices);
      context.model().setBlacklistedSecurityDevices(blacklistedDevices);
   }

   /**
    * Manages the {@link SubsystemCapability#ATTR_AVAILABLE} flag. This is set
    * to {@code true} when there are any security devices registered.
    * 
    * @param context
    */
   protected void syncSubsystemAvailable(SubsystemContext<SecuritySubsystemModel> context) {
      if (context.model().getSecurityDevices().size() > 0){
         context.model().setAvailable(true);
      }
      else{
         context.model().setAvailable(false);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.iris.common.subsystem.BaseSubsystem#setAttributes(com.iris.messages
    * .MessageBody, com.iris.common.subsystem.SubsystemContext)
    */
   @Override
   @Request(Capability.CMD_SET_ATTRIBUTES)
   public MessageBody setAttributes(
         PlatformMessage request,
         SubsystemContext<SecuritySubsystemModel> context
         ) {
      try{
         return super.setAttributes(request, context);
      }
      finally{
         Set<String> attributes = request.getValue().getAttributes().keySet();
         if (attributes.contains(SecuritySubsystemCapability.ATTR_CALLTREE)){
            syncCallTree(context);
         }
         if (
         		attributes.contains(SecuritySubsystemUtil.DEVICES_KEY_ON) ||
               attributes.contains(SecuritySubsystemUtil.DEVICES_KEY_PARTIAL)
         ){
            syncDevices(context);
         }
         else if (
               attributes.contains(ATTR_SILENT_ON) ||
               attributes.contains(ATTR_SILENT_PARTIAL) ||
               attributes.contains(ATTR_SOUNDSENABLED_ON) ||
               attributes.contains(ATTR_SOUNDSENABLED_PARTIAL)
         ) {
            KeypadState.syncSounds(context);
         }
      }
   }

   @OnScheduledEvent
   public void onScheduledEvent(ScheduledEvent event, SubsystemContext<SecuritySubsystemModel> context) {
      boolean handled = false;
      if(SubsystemUtils.isMatchingTimeout(event, context, KeypadState.KEYPAD_ALLOW_BYPASSED_TIMEOUT_KEY)) {
         keypadState.onScheduledEvent(event, context);
         handled = true;
      }
      if (SubsystemUtils.isMatchingTimeout(event, context, CallTree.CALL_TREE_TIMEOUT_KEY)) {
         callTree.onScheduledEvent(event, context);
         handled = true;
      }
      if (SubsystemUtils.isMatchingTimeout(event, context)) {
         stateMachine.timeout(context);
         handled = true;
      }
      if(!handled) {
         context.logger().warn("onScheduled event found outdated event.  Throwing away event {}", event);
      }
   }

   @Request(DisarmRequest.NAME)
   public void disarm(PlatformMessage message, SubsystemContext<SecuritySubsystemModel> context) {
      disarmSecurity(message, context);
   }

   private void disarmSecurity(PlatformMessage message, SubsystemContext<SecuritySubsystemModel> context) {
      SecuritySubsystemUtil.setMessageSource(message, context);
      try {
         stateMachine.disarm(message.getActor(), context);
         syncDevices(context);
      }
      finally {
         SecuritySubsystemUtil.clearMessageSource(context);
      }
   }

   @Request(ArmRequest.NAME)
   public MessageBody arm(PlatformMessage message, @Named(ArmRequest.ATTR_MODE) String mode, SubsystemContext<SecuritySubsystemModel> context) {
      int delaySec = armSecurity(message, mode, false, context);

      return ArmResponse
            .builder()
            .withDelaySec(delaySec)
            .build();
   }

   @Request(ArmBypassedRequest.NAME)
   public MessageBody armBypassed(@Named(ArmBypassedRequest.ATTR_MODE) String mode, SubsystemContext<SecuritySubsystemModel> context, PlatformMessage message) {
	   int delaySec = armSecurity(message, mode, true, context);
      return ArmBypassedResponse
            .builder()
            .withDelaySec(delaySec)
            .build();
   }

   private int armSecurity(PlatformMessage message, String mode, boolean bypassed, SubsystemContext<SecuritySubsystemModel> context) {
      int delay = stateMachine.arm(message.getActor(), mode, bypassed, context);
      // this has to happen after arm to ensure that it actually armed
      SecuritySubsystemUtil.setMessageSource(message, context);
      return delay;
   }
   
   @Request(AcknowledgeRequest.NAME)
   public void onAcknowledgeRequest(PlatformMessage message, SubsystemContext<SecuritySubsystemModel> context) {
	   SecuritySubsystemUtil.setMessageSource(message, context);
	   acknowledgeAlert(message, context);
   }

   @Request(PanicRequest.NAME)
   public void onPanicRequest(PlatformMessage message, SubsystemContext<SecuritySubsystemModel> context) {
	   SecuritySubsystemUtil.setMessageSource(message, context);
	   panicSecurity(message, context);
   }

   private void panicSecurity(PlatformMessage message, SubsystemContext<SecuritySubsystemModel> context) {
      stateMachine.panic(message.getActor(), context);
   }

   @OnMessage(types = { IvrNotificationAcknowledgedEvent.NAME })
   public void ivrAcknowledged(PlatformMessage message, SubsystemContext<SecuritySubsystemModel> context) {
	   SecuritySubsystemUtil.setMessageSource(message, context);
	   String msgKey = NotificationCapability.IvrNotificationAcknowledgedEvent.getMsgKey(message.getValue());
      // we are only interested in security
      if (NotificationsUtil.SECURITY_ALERT_KEY.equals(msgKey)){
         acknowledgeAlert(message, context);
      }
      else{
         context.logger().debug("Ignoring IVRAcknowledgement for msg key [{}]", msgKey);
      }

   }

   @OnMessage(types = { IvrNotificationRefusedEvent.NAME })
   public void ivrRefused(PlatformMessage message, SubsystemContext<SecuritySubsystemModel> context) {
	   SecuritySubsystemUtil.setMessageSource(message, context);
	   alertFailed(message, context);
   }

   private void alertFailed(PlatformMessage message, SubsystemContext<SecuritySubsystemModel> context) {
      context.model().setLastAcknowledgedBy(null);
      context.model().setLastAcknowledgementTime(new Date());
      context.model().setLastAcknowledgement(SecuritySubsystemCapability.LASTACKNOWLEDGEMENT_FAILED);
   }

   private void acknowledgeAlert(PlatformMessage message, SubsystemContext<SecuritySubsystemModel> context) {
      context.model().setLastAcknowledgement(SecuritySubsystemCapability.LASTACKNOWLEDGEMENT_ACKNOWLEDGED);
      context.model().setLastAcknowledgedBy(message.getActor() != null ? message.getActor().getRepresentation() : null);
      context.model().setLastAcknowledgementTime(new Date());
      callTree.cancel(context);
   }

   @OnValueChanged(attributes = {
         DeviceConnectionCapability.ATTR_STATE
   })
   public void onConnectivityStateChange(ModelChangedEvent event, SubsystemContext<SecuritySubsystemModel> context) {
      String address = event.getAddress().getRepresentation();
      if (!context.model().getSecurityDevices().contains(address)){
         context.logger().debug("Ignoring connectivity change from non-safety device {}", event);
         return;
      }

      Model model = context.models().getModelByAddress(event.getAddress());
      if (model == null){
         context.logger().warn("Unable to retrieve model for safety device {}", address);
         return;
      }
      context.logger().debug("Device connection change for device {} connection status {}", address, model.getAttribute(DeviceConnectionCapability.ATTR_STATE));
      syncDeviceState(context);
      if (context.model().getTriggeredDevices().contains(address)){
         triggerTransition(model.getAddress(), context);
      }
   }

   @OnValueChanged(attributes = {
         PlaceCapability.ATTR_SERVICELEVEL
   })
   public void onSubscriptionLevelChange(ModelChangedEvent event, SubsystemContext<SecuritySubsystemModel> context) {
      context.logger().info("Detected a subscription level change {}", event);
      syncCallTreeEnabled(context);
   }

   @OnAdded(query = QUERY_SECURITY_DEVICES)
   public void onDeviceAdded(ModelAddedEvent event, SubsystemContext<SecuritySubsystemModel> context) {
      if (addSecurityDevice(event.getAddress().getRepresentation(), context)){
         context.logger().info("A new security device was added {}", event);
      }
   }

   @OnRemoved(query = QUERY_SECURITY_DEVICES)
   public void onDeviceRemoved(ModelRemovedEvent event, SubsystemContext<SecuritySubsystemModel> context) {
      if (removeSecurityDevice(event.getModel(), context)){
         context.logger().info("A security device was removed {}", event);
      }
   }

   @OnAdded(query = QUERY_PEOPLE)
   public void onPersonAdded(ModelAddedEvent event, SubsystemContext<SecuritySubsystemModel> context) {
      Map<String, Boolean> entries = CallTree.callTreeToMap(context.model().getCallTree());
      String address = event.getAddress().getRepresentation();
      if (!entries.containsKey(address)){
         entries.put(address, false);
         context.model().setCallTree(CallTree.callTreeToList(entries));
      }
      context.logger().info("A new person was added to the call tree {}", event);
   }

   @OnRemoved(query = QUERY_PEOPLE)
   public void onPersonRemoved(ModelRemovedEvent event, SubsystemContext<SecuritySubsystemModel> context) {
      Map<String, Boolean> entries = CallTree.callTreeToMap(context.model().getCallTree());
      if (entries.remove(event.getAddress().getRepresentation()) != null){
         context.model().setCallTree(CallTree.callTreeToList(entries));
      }
      context.logger().info("A person was removed from the call tree {}", event);
   }

   protected boolean addSecurityDevice(String address, SubsystemContext<SecuritySubsystemModel> context) {
      if (context.model().getSecurityDevices().contains(address) || context.model().getBlacklistedSecurityDevices().contains(address)){
         return false;
      }

      syncDevices(context);
      return true;
   }

   protected boolean removeSecurityDevice(Model m, SubsystemContext<SecuritySubsystemModel> context) {
      String address = m.getAddress().getRepresentation();

      boolean removed = false;
      removed |= removeAddressFromSet(address, SecuritySubsystemCapability.ATTR_SECURITYDEVICES, context.model());
      removed |= removeAddressFromSet(address, SecuritySubsystemCapability.ATTR_BLACKLISTEDSECURITYDEVICES, context.model());
      removed |= removeAddressFromSet(address, SecuritySubsystemCapability.ATTR_TRIGGEREDDEVICES, context.model());
      removed |= removeAddressFromSet(address, SecuritySubsystemCapability.ATTR_READYDEVICES, context.model());
      removed |= removeAddressFromSet(address, SecuritySubsystemCapability.ATTR_BYPASSEDDEVICES, context.model());
      removed |= removeAddressFromSet(address, SecuritySubsystemCapability.ATTR_OFFLINEDEVICES, context.model());
      removed |= removeAddressFromSet(address, SecuritySubsystemUtil.DEVICES_KEY_ON, context.model());
      removed |= removeAddressFromSet(address, SecuritySubsystemUtil.DEVICES_KEY_PARTIAL, context.model());

      syncSubsystemAvailable(context);
      return removed;
   }

   @OnValueChanged(attributes = GlassCapability.ATTR_BREAK)
   public void onGlassBreak(ModelChangedEvent event, SubsystemContext<SecuritySubsystemModel> context) {
      String value = getSecurityDeviceModelEvent(event, GlassCapability.NAME, context);
      if (value == null){
         return;
      }
      switch (value) {
      case GlassCapability.BREAK_DETECTED:
         triggerDevice(event.getAddress(), context);
         triggerTransition(event.getAddress(), context);
         break;
      case GlassCapability.BREAK_SAFE:
         clearDevice(event.getAddress(), context);
         break;
      default:
         context.logger().warn("Received invalid value for glass:break [{}] on [{}]", value, event.getAddress());
      }
   }

   @OnValueChanged(attributes = ContactCapability.ATTR_CONTACT)
   public void onContactChange(ModelChangedEvent event, SubsystemContext<SecuritySubsystemModel> context) {
      String value = getSecurityDeviceModelEvent(event, ContactCapability.NAME, context);
      if (value == null){
         return;
      }
      switch (value) {
      case ContactCapability.CONTACT_OPENED:
         triggerDevice(event.getAddress(), context);
         triggerTransition(event.getAddress(), context);
         break;
      case ContactCapability.CONTACT_CLOSED:
         clearDevice(event.getAddress(), context);
         break;
      default:
         context.logger().warn("Received invalid value for glass:break [{}] on [{}]", value, event.getAddress());
      }
   }

   @OnValueChanged(attributes = MotionCapability.ATTR_MOTION)
   public void onMotionChange(ModelChangedEvent event, SubsystemContext<SecuritySubsystemModel> context) {

      String value = getSecurityDeviceModelEvent(event, MotionCapability.NAME, context);
      if (value == null){
         return;
      }
      switch (value) {
      case MotionCapability.MOTION_DETECTED:
         triggerDevice(event.getAddress(), context);
         triggerTransition(event.getAddress(), context);
         break;
      case MotionCapability.MOTION_NONE:
         clearDevice(event.getAddress(), context);
         break;
      default:
         context.logger().warn("Received invalid value for glass:break [{}] on [{}]", value, event.getAddress());
      }
   }
   
   @OnValueChanged(attributes = MotorizedDoorCapability.ATTR_DOORSTATE)
   public void onMotorizedDoorStateChange(ModelChangedEvent event, SubsystemContext<SecuritySubsystemModel> context) {
	   //TODO - check to see if the device is on the blacklist
	  if(context.model().getSecurityDevices().contains(event.getAddress().getRepresentation())) {
	      String value = getSecurityDeviceModelEvent(event, MotorizedDoorCapability.NAME, context);
	      if (value == null){
	         return;
	      }
	      if(MotorizedDoorCapability.DOORSTATE_CLOSED.equals(value)) {
	    	  clearDevice(event.getAddress(), context);
	      }else {
	    	  triggerDevice(event.getAddress(), context);
	          triggerTransition(event.getAddress(), context);
	      }
	  }
   }
   

   private void triggerTransition(Address triggerDevice, SubsystemContext<SecuritySubsystemModel> context) {
      stateMachine.triggerDevice(triggerDevice, context);
   }

   private String getSecurityDeviceModelEvent(ModelChangedEvent event, String capability, SubsystemContext<SecuritySubsystemModel> context) {
      if (!context.model().getSecurityDevices().contains(event.getAddress().getRepresentation())){
         context.logger().debug("Received an event for a security device that we are not interested in [{}] on [{}]", capability, event.getAddress());
         return null;
      }

      String value = (String) event.getAttributeValue();
      if (value == null){
         context.logger().warn("Received invalid value for {} [{}] on [{}]", capability, value, event.getAddress());
         return null;
      }
      return value;
   }

   protected void triggerDevice(Address device, SubsystemContext<SecuritySubsystemModel> context) {
      addAddressToSet(device.getRepresentation(), SecuritySubsystemCapability.ATTR_TRIGGEREDDEVICES, context.model());
      removeAddressFromSet(device.getRepresentation(), SecuritySubsystemCapability.ATTR_READYDEVICES, context.model());

      // FIXME this really belongs in the state machine since the behavior is state-dependent
      HashMap<String, Date> lastAlertTriggers = new HashMap<String, Date>(context.model().getLastAlertTriggers());
      if ((context.model().isAlarmStateARMED() || context.model().isAlarmStateSOAKING() || context.model().isAlarmStateALERT())
            && context.model().getArmedDevices().contains(device.getRepresentation())){
         lastAlertTriggers.put(device.getRepresentation(), new Date());
      }

      context.model().setLastAlertTriggers((lastAlertTriggers));
      triggerTransition(device, context);
   }

   protected void clearDevice(Address device, Set<String> readyDevices, Set<String> triggeredDevices) {
      triggeredDevices.remove(device.getRepresentation());
      readyDevices.add(device.getRepresentation());
   }

   protected void clearDevice(Address device, SubsystemContext<SecuritySubsystemModel> context) {
      Set<String> triggeredDevices = new HashSet<String>(context.model().getTriggeredDevices());
      Set<String> readyDevices = new HashSet<String>(context.model().getReadyDevices());
      clearDevice(device, readyDevices, triggeredDevices);

      context.model().setTriggeredDevices(triggeredDevices);
      context.model().setReadyDevices(readyDevices);
   }

   
   protected Set<String> getBlacklistedProductIds() {
	   return blacklistedProductIds;
   }
   
  
}

