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
package com.iris.common.subsystem.care;

import static com.iris.util.Objects.equalsAny;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import com.iris.annotation.Version;
import com.iris.common.subsystem.BaseSubsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.alarm.KeyPad;
import com.iris.common.subsystem.alarm.KeyPad.KeyPadAlertMode;
import com.iris.common.subsystem.alarm.subs.AlertState;
import com.iris.common.subsystem.annotation.Subsystem;
import com.iris.common.subsystem.care.behavior.BehaviorManager;
import com.iris.common.subsystem.care.behavior.BehaviorMonitor;
import com.iris.common.subsystem.care.behavior.BehaviorUtil;
import com.iris.common.subsystem.care.behavior.CareBehaviorTypeWrapper;
import com.iris.common.subsystem.util.AddressesAttributeBinder;
import com.iris.common.subsystem.util.CallTree;
import com.iris.common.subsystem.util.NotificationsUtil;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.CareSubsystemCapability;
import com.iris.messages.capability.CareSubsystemCapability.AcknowledgeRequest;
import com.iris.messages.capability.CareSubsystemCapability.AcknowledgeResponse;
import com.iris.messages.capability.CareSubsystemCapability.AddBehaviorRequest;
import com.iris.messages.capability.CareSubsystemCapability.AddBehaviorResponse;
import com.iris.messages.capability.CareSubsystemCapability.BehaviorAlertClearedEvent;
import com.iris.messages.capability.CareSubsystemCapability.BehaviorAlertEvent;
import com.iris.messages.capability.CareSubsystemCapability.ClearRequest;
import com.iris.messages.capability.CareSubsystemCapability.ListBehaviorTemplatesRequest;
import com.iris.messages.capability.CareSubsystemCapability.ListBehaviorTemplatesResponse;
import com.iris.messages.capability.CareSubsystemCapability.ListBehaviorsRequest;
import com.iris.messages.capability.CareSubsystemCapability.ListBehaviorsResponse;
import com.iris.messages.capability.CareSubsystemCapability.PanicRequest;
import com.iris.messages.capability.CareSubsystemCapability.RemoveBehaviorRequest;
import com.iris.messages.capability.CareSubsystemCapability.RemoveBehaviorResponse;
import com.iris.messages.capability.CareSubsystemCapability.UpdateBehaviorRequest;
import com.iris.messages.capability.CareSubsystemCapability.UpdateBehaviorResponse;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DevicePowerCapability;
import com.iris.messages.capability.HubSoundsCapability.PlayToneRequest;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.NotificationCapability.IvrNotificationAcknowledgedEvent;
import com.iris.messages.capability.NotificationCapability.NotifyRequest;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.PresenceCapability;
import com.iris.messages.capability.SafetySubsystemCapability;
import com.iris.messages.capability.SafetySubsystemCapability.ClearResponse;
import com.iris.messages.capability.SecuritySubsystemCapability.PanicResponse;
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
import com.iris.messages.model.serv.AccountModel;
import com.iris.messages.model.serv.PersonModel;
import com.iris.messages.model.serv.RuleModel;
import com.iris.messages.model.subs.CareSubsystemModel;
import com.iris.messages.service.RuleService;
import com.iris.messages.type.CallTreeEntry;
import com.iris.model.query.expression.ExpressionCompiler;
import com.iris.type.LooselyTypedReference;
import com.iris.util.IrisCollections;

@Singleton
@Subsystem(CareSubsystemModel.class)
@Version(1)
public class CareSubsystem extends BaseSubsystem<CareSubsystemModel> {

   private BehaviorManager behaviorManager = new BehaviorManager();
   private BehaviorMonitor behaviorMonitor = new BehaviorMonitor(); 

   public static final String CARE_EVENT_KEY = "care.";
   public static final String CARE_EVENT_RULE_TRIGGERRED = "alarm.rule-triggered";
   public static final String CARE_EVENT_BEHAVIOR_TRIGGERRED = "alarm.behavior-triggered";
   public static final String CLEAR_EVENT_KEY = "alarm.cancelled";
   public static final String ACTOR_KEY = "care.actor";
   private static final String SILENT_INIT_KEY = "silentInitialized";

   
   // Static to make this looks a little prettier.
   private static final String SELECT_CONTACT = "base:caps contains '" + ContactCapability.NAMESPACE + "'";
   private static final String CONTACT_OPENED = "'" + ContactCapability.CONTACT_OPENED + "'";
   private static final String CONTACT_CLOSED = "'" + ContactCapability.CONTACT_CLOSED + "'";
   private static final String CONTACT_STATE = ContactCapability.ATTR_CONTACT;

   private static final String SELECT_MOTION = "base:caps contains '" + MotionCapability.NAMESPACE + "'";
   private static final String MOTION_DETECTED = "'" + MotionCapability.MOTION_DETECTED + "'";
   private static final String MOTION_NONE = "'" + MotionCapability.MOTION_NONE + "'";
   private static final String MOTION_STATE =  MotionCapability.ATTR_MOTION;
   private static final String DEVICE_STATE = DeviceConnectionCapability.ATTR_STATE ;
   private static final String ONLINE = "'" + DeviceConnectionCapability.STATE_ONLINE + "'";
   private static final String OFFLINE = "'" + DeviceConnectionCapability.STATE_OFFLINE + "'";

   //wds - removed since only presence only is no longer considered an indicator of a care
   //capable device. see: https://eyeris.atlassian.net/browse/ITWO-11167. Was used in QUERY_CARE_CAPABLE_DEVICES.
   private static final String SELECT_PRESENCE = "base:caps contains '" + PresenceCapability.NAMESPACE + "'";

   private static final String SELECT_KEYPAD = "base:caps contains '" + KeyPadCapability.NAMESPACE + "'";

   private static final String OR = " OR ";
   private static final String AND = " AND ";
   private static final String EQUALS = " == ";

   private static final String QUERY_CARE_CAPABLE_DEVICES =           
         "(" + SELECT_CONTACT +
         OR + SELECT_MOTION + ")" +
         " AND (!"+ SELECT_KEYPAD +")";

   private static final Predicate<Model> IS_CARE_CAPABLE_DEVICE = ExpressionCompiler.compile(QUERY_CARE_CAPABLE_DEVICES);
   private final AddressesAttributeBinder<CareSubsystemModel> careCapableBinder = 
         new AddressesAttributeBinder<CareSubsystemModel>(IS_CARE_CAPABLE_DEVICE, CareSubsystemCapability.ATTR_CARECAPABLEDEVICES) {
            @Override
            protected void afterAdded(SubsystemContext<CareSubsystemModel> context, Model model) {
               onDeviceAdded(model, context);
            }

            @Override
            protected void afterRemoved(SubsystemContext<CareSubsystemModel> context, Address address) {
               onDeviceRemoved(address, context);
            }
            
         };
         
   private static final Predicate<Model> IS_PRESENCE_DEVICE = ExpressionCompiler.compile(SELECT_PRESENCE);
   private final AddressesAttributeBinder<CareSubsystemModel> presenceDeviceBinder = 
   	new AddressesAttributeBinder<CareSubsystemModel>(IS_PRESENCE_DEVICE, CareSubsystemCapability.ATTR_PRESENCEDEVICES) {
      	@Override
         protected void afterAdded(SubsystemContext<CareSubsystemModel> context, Model model) {
            updateAvailable(context);
         }
   
         @Override
         protected void afterRemoved(SubsystemContext<CareSubsystemModel> context, Address address) {
         	updateAvailable(context);
         }   	
   };

   private static final String DEVICE_IS_ONLINE = DEVICE_STATE + EQUALS + ONLINE;
   private static final String DEVICE_IS_OFFLINE = DEVICE_STATE + EQUALS + OFFLINE;
   static final String QUERY_MOTION_DETECTED = SELECT_MOTION + AND + MOTION_STATE + EQUALS + MOTION_DETECTED;
   static final String QUERY_MOTION_NONE     = SELECT_MOTION + AND + MOTION_STATE + EQUALS + MOTION_NONE;
   static final String QUERY_CONTACT_OPENED = SELECT_CONTACT + AND + CONTACT_STATE + EQUALS + CONTACT_OPENED;
   static final String QUERY_CONTACT_CLOSED = SELECT_CONTACT + AND + CONTACT_STATE + EQUALS + CONTACT_CLOSED;
   //static final String QUERY_TRIGGERED = QUERY_MOTION_DETECTED + OR + QUERY_CONTACT_OPENED;
   //static final String QUERY_INACTIVE = QUERY_MOTION_NONE + OR + QUERY_CONTACT_CLOSED;
   static final String QUERY_TRIGGERED = "(" + QUERY_MOTION_DETECTED  + AND +  DEVICE_IS_ONLINE +")" + OR + "(" + QUERY_CONTACT_OPENED + AND +  DEVICE_IS_ONLINE +")" ;
   static final String QUERY_INACTIVE =  "(" + SELECT_MOTION + AND + "(" + MOTION_STATE + EQUALS + MOTION_NONE + OR +  DEVICE_IS_OFFLINE + "))" 
		   + OR +  "(" + SELECT_CONTACT + AND + "(" + CONTACT_STATE + EQUALS + CONTACT_CLOSED +  OR  + DEVICE_IS_OFFLINE + "))";
   
   private static final Predicate<Model> IS_TRIGGERED = ExpressionCompiler.compile(QUERY_TRIGGERED);
   private static final AddressesAttributeBinder<CareSubsystemModel> isTriggeredBinder = new AddressesAttributeBinder<CareSubsystemModel>(IS_TRIGGERED, CareSubsystemCapability.ATTR_TRIGGEREDDEVICES);

   private static final Predicate<Model> IS_INACTIVE = ExpressionCompiler.compile(QUERY_INACTIVE);
   private static final AddressesAttributeBinder<CareSubsystemModel> isInactiveBinder = new AddressesAttributeBinder<CareSubsystemModel>(IS_INACTIVE, CareSubsystemCapability.ATTR_INACTIVEDEVICES);

   // Call Tree
   private static final String QUERY_PEOPLE =
         "base:caps contains '" + PersonCapability.NAMESPACE + "'";
   private CallTree callTreeHelper = new CallTree(CareSubsystemCapability.ATTR_CALLTREE);
   
   
   @Override
   public void onStarted(SubsystemContext<CareSubsystemModel> context) {
	   
      super.onStarted(context);
      careCapableBinder.bind(context);
      isTriggeredBinder.bind(context);
      isInactiveBinder.bind(context);
      presenceDeviceBinder.bind(context);

      setIfNull(context.model(), CareSubsystemCapability.ATTR_ACTIVEBEHAVIORS, ImmutableSet.<String>of());
      setIfNull(context.model(), CareSubsystemCapability.ATTR_BEHAVIORS, ImmutableSet.<String>of());
      setIfNull(context.model(), CareSubsystemCapability.ATTR_ALARMSTATE, CareSubsystemCapability.ALARMSTATE_READY);
      setIfNull(context.model(), CareSubsystemCapability.ATTR_ALARMMODE, CareSubsystemCapability.ALARMMODE_ON);
      setIfNull(context.model(), CareSubsystemCapability.ATTR_SILENT, false);
      setIfNull(context.model(), CareSubsystemCapability.ATTR_LASTALERTTRIGGERS, new HashMap<String,String>());
      setIfNull(context.model(), CareSubsystemCapability.ATTR_PRESENCEDEVICES, ImmutableSet.<String>of());
      
      behaviorManager.bind(context);
      behaviorMonitor.bind(context);
      
      // Setup the call tree
      List<Map<String,Object>> existingCallTree = context.model().getCallTree();
      List<Map<String,Object>> callTree =
            existingCallTree == null ? 
                  new ArrayList<Map<String,Object>>() : 
                  new ArrayList<Map<String,Object>>(existingCallTree);

      for(Model model: context.models().getModels()) {
         if(model.getType().equals(AccountCapability.NAMESPACE)) {
            // if the call tree is empty make sure the account owner is set
            String owner = AccountModel.getOwner(model);
            if(callTree.isEmpty() && owner != null) {
               CallTreeEntry cte = new CallTreeEntry();
               cte.setEnabled(true);
               cte.setPerson("SERV:" + PersonCapability.NAMESPACE + ":" + owner);
               callTree.add(cte.toMap());
            }
         }
      }
      callTree = NotificationsUtil.fixCallTree(callTree);
      context.model().setCallTree(callTree);
      context.model().setCallTreeEnabled(true);
      updateAvailable(context);
      
      //Data fix
      fixCareDevices(context);
   }
   
   //Care went into prod without this field being set. So we will sync careBehaviors with careCareCapableDevices.  Just once.
   protected void fixCareDevices(SubsystemContext<CareSubsystemModel> context){
      if(context.model().getCareDevicesPopulated()==null || !context.model().getCareDevicesPopulated()){
         if(context.model().getCareCapableDevices()!=null){
            context.model().setCareDevices(new HashSet<>(context.model().getCareCapableDevices()));
         }
         context.model().setCareDevicesPopulated(Boolean.TRUE);
      }
   }

   @Override
   protected void onAdded(SubsystemContext<CareSubsystemModel> context) {
      super.onAdded(context);
      CareSubsystemModel model = context.model();
      model.setCareCapableDevices(ImmutableSet.<String> of());
      model.setCareDevices(ImmutableSet.<String> of());
      model.setTriggeredDevices(ImmutableSet.<String> of());
      model.setPresenceDevices(ImmutableSet.<String> of());
      context.setVariable(SILENT_INIT_KEY, Boolean.FALSE);
      if(model.getSilent() == null) {
    	  //default silent to be true
    	  model.setSilent(true);
    	  context.setVariable(SILENT_INIT_KEY, Boolean.TRUE);
      }
      updateAvailable(context);
   }

   @Override
   @Request(Capability.CMD_SET_ATTRIBUTES)
   public MessageBody setAttributes(PlatformMessage request, SubsystemContext<CareSubsystemModel> context) {
      try{
         return super.setAttributes(request, context);
      }finally{
         Set<String> attributes = request.getValue().getAttributes().keySet();
         if (attributes.contains(SafetySubsystemCapability.ATTR_CALLTREE)){
            callTreeHelper.syncCallTree(context);
         }
      }
   }

   @OnValueChanged(attributes = { PlaceCapability.ATTR_SERVICELEVEL })
   public void onSubscriptionLevelChange(ModelChangedEvent event, SubsystemContext<CareSubsystemModel> context) {
      updateAvailable(context);
   }

   @OnValueChanged(attributes = { PlaceCapability.ATTR_TZID})
   public void onTimeZoneChange(ModelChangedEvent event, SubsystemContext<CareSubsystemModel> context) {
      SubsystemUtils.refreshTimeZoneOnContext(context);
      context.logger().debug("Time Zone changed to [{}]. Rescheudling windows",context.getLocalTime().getTimeZone().toString());
      behaviorMonitor.changeMode(context.model().getAlarmMode(), context);
   }
   
   @OnValueChanged(attributes = { CareSubsystemCapability.ATTR_ALARMMODE})
   public void onChangeMode(ModelChangedEvent event, SubsystemContext<CareSubsystemModel> context) {
      context.logger().debug("Care subsystem mode change to [{}]",context.model().getAlarmMode());
      behaviorMonitor.changeMode(context.model().getAlarmMode(), context);
   }

   @OnValueChanged(attributes = Capability.ATTR_CAPS)
   public void onCapsChanged(ModelChangedEvent event, SubsystemContext<CareSubsystemModel> context) {
      updateAvailable(context);
   }

   @OnValueChanged(attributes = {
         DeviceConnectionCapability.ATTR_STATE,
         DeviceConnectionCapability.ATTR_SIGNAL,
         DevicePowerCapability.ATTR_BATTERY
   })
   public void onConnectivityStateChange(ModelChangedEvent event, SubsystemContext<CareSubsystemModel> context) {
      updateAvailable(context);
   }
   
   public void onDeviceAdded(Model device, SubsystemContext<CareSubsystemModel> context) {
      addAddressToSet(device.getAddress().getRepresentation(), CareSubsystemCapability.ATTR_CAREDEVICES, context.model());
      updateAvailable(context);
   }

   public void onDeviceRemoved(Address deviceAddress, SubsystemContext<CareSubsystemModel> context) {
      removeAddressFromSet(deviceAddress.getRepresentation(), CareSubsystemCapability.ATTR_CAREDEVICES, context.model());
      updateAvailable(context);
   }         
   
   // Make sure there are care capable devices
   protected void updateAvailable(SubsystemContext<CareSubsystemModel> context) {
      String serviceLevel = String.valueOf(context.models().getAttributeValue(Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE), PlaceCapability.ATTR_SERVICELEVEL));
      boolean isPremium = ServiceLevel.isPremiumOrPromon(serviceLevel);
      boolean hasDevices = (context.model().getCareCapableDevices().size() > 0 || context.model().getPresenceDevices().size() > 0);
      context.model().setAvailable(hasDevices && isPremium);
      context.model().setCallTreeEnabled(isPremium);
   }

   // ALARM SUPPORT
   @OnAdded(query = QUERY_PEOPLE)
   public void onPersonAdded(ModelAddedEvent event, SubsystemContext<CareSubsystemModel> context) {
      List<Map<String,Object>>callTree=IrisCollections.copyOf(context.model().getCallTree());
      NotificationsUtil.removeCTEByAddress(event.getAddress().getRepresentation(), callTree);
      callTree.add(NotificationsUtil.createCallEntry(event.getAddress(),false).toMap());
      context.model().setCallTree(callTree);
      context.logger().info("A new person was added to the call tree {}", event);
   }

   @OnRemoved(query = QUERY_PEOPLE)
   public void onPersonRemoved(ModelRemovedEvent event, SubsystemContext<CareSubsystemModel> context) {
      List<Map<String,Object>>callTree=IrisCollections.copyOf(context.model().getCallTree());
      NotificationsUtil.removeCTEByAddress(event.getAddress().getRepresentation(), callTree);
      context.model().setCallTree(callTree);
      context.logger().info("A person was removed from the call tree {}", event);
   }

   @Request(PanicRequest.NAME) 
   public MessageBody onPanic( PlatformMessage message, SubsystemContext<CareSubsystemModel> context) {
      triggerAlert(message.getSource().getRepresentation(),context);
      return PanicResponse.instance();
   }
   
   @Request(AcknowledgeRequest.NAME) 
   public MessageBody onAcknowledge( PlatformMessage message, SubsystemContext<CareSubsystemModel> context) {
      String source = message.getSource().getRepresentation();
      acknowledgeCareAlert(context,source);
      return AcknowledgeResponse.instance();
   }
   
   private void acknowledgeCareAlert(SubsystemContext<CareSubsystemModel> context,String acknowledgedBy){
      if (context.model().isLastAcknowledgementPENDING()) {
         context.model().setLastAcknowledgedBy(acknowledgedBy);
         context.model().setLastAcknowledgement(CareSubsystemCapability.LASTACKNOWLEDGEMENT_ACKNOWLEDGED);
         context.model().setLastAcknowledgementTime(new Date());
         callTreeHelper.cancel(context);     // Stop the Call Tree from calling more people.
         context.broadcast(CareSubsystemCapability.BehaviorAlertAcknowledgedEvent.instance());
      }
   }
   
   @Request(ClearRequest.NAME) 
   public MessageBody onClear( PlatformMessage message,  SubsystemContext<CareSubsystemModel> context) {
      clear(message.getActor().getRepresentation(), context);      
      return ClearResponse.instance();
   }
   
   private void clear(String cause, SubsystemContext<CareSubsystemModel> context) {
      if (context.model().isAlarmStateALERT()) {
         context.model().setLastClearTime(new Date());
         context.model().setLastClearedBy(cause);
         context.model().setAlarmState(CareSubsystemCapability.ALARMSTATE_READY);
         context.model().setLastAlertTriggers(null);

         context.logger().debug("Clearing behavior timeout");
         behaviorMonitor.careAlarmCleared(context);

         // If Cleared while pending assume fail to ack.
         if (context.model().isLastAcknowledgementPENDING()) {
            context.model().setLastAcknowledgement(CareSubsystemCapability.LASTACKNOWLEDGEMENT_FAILED);
         }
         sendClearNotification(context);
         stopTheAlarms(context);
         sendClearedEvent(context);
      }
   }
   private void sendClearedEvent(SubsystemContext<CareSubsystemModel> context){
      context.broadcast(BehaviorAlertClearedEvent.instance());
   }
   private void stopTheAlarms(SubsystemContext<CareSubsystemModel> context) {
      NotificationsUtil.stopTheAlarms(context);      
   }

   public void sendAlertNotification(SubsystemContext<CareSubsystemModel> context) {
      String alertKey = null;
      //determine the source of the alert
      String cause = context.model().getLastAlertCause();
      String sourceName = "";
      if(StringUtils.isNotBlank(cause)) {
    	  try {
	    	  Address causeAddr = Address.fromString(cause);
	    	  Model sourceModel = context.models().getModelByAddress(causeAddr);
	    	  if(RuleService.NAMESPACE.equals(causeAddr.getNamespace())) {
	    		  alertKey = NotificationsUtil.CareAlarm.KEY_BY_RULE;    
	    		  if(sourceModel != null) {
	        		  sourceName = RuleModel.getName(sourceModel, "");
	        	  }
	    	  }
    	  }catch( Exception e) {
    		  //You will get an exception if it's behavior triggered because cause will be an UUID
    	  }    	  	  
      }
      if(alertKey == null) {
    	  alertKey = NotificationsUtil.CareAlarm.KEY_BY_BEHAVIOR;
    	  CareBehaviorTypeWrapper behavior = BehaviorUtil.getBehaviorFromContext(cause, context);
    	  if (behavior != null) {
    	     sourceName = behavior.getName();
    	  }
      }      
      callTreeHelper.notifyParallel(context, alertKey, ImmutableMap.<String, String> of(NotificationsUtil.CareAlarm.PARAM_SOURCE_NAME, sourceName));
   }

   public void sendClearNotification(SubsystemContext<CareSubsystemModel> context) {
      String ownerAddress = NotificationsUtil.getAccountOwnerAddress(context);
      String lastClearBy = context.model().getLastClearedBy();
      String lastClearByFirstName = "";
      String lastClearByLastName = "";
      if(lastClearBy != null) {
    	  Model curPerson = context.models().getModelByAddress(Address.fromString(lastClearBy));
    	  lastClearByFirstName = PersonModel.getFirstName(curPerson, "");
    	  lastClearByLastName = PersonModel.getLastName(curPerson, "");
      }
      NotificationsUtil.sendNotification(context, NotificationsUtil.CareAlarmCancelled.KEY, ownerAddress, NotifyRequest.PRIORITY_MEDIUM,
    		  ImmutableMap.<String, String> of(NotificationsUtil.CareAlarmCancelled.PARAM_CANCELL_BY_FIRSTNAME, lastClearByFirstName,
    	    		  NotificationsUtil.CareAlarmCancelled.PARAM_CANCELL_BY_LASTNAME, lastClearByLastName,
    	    		  NotificationsUtil.CareAlarmCancelled.PARAM_TIME, context.model().getLastClearTime().toString()));
   }
   
   // Check for failed acknowledgement 
   protected void checkForFailedAcknowledgement(ScheduledEvent event, SubsystemContext<CareSubsystemModel> context) {
      context.logger().debug("Failed to obtains acknowledgement for alarm");
      if (SubsystemUtils.isMatchingTimeout(event, context, CallTree.CALL_TREE_TIMEOUT_KEY )) {
         if (!callTreeHelper.hasNext(context)) {
            context.model().setLastAcknowledgement(CareSubsystemCapability.LASTACKNOWLEDGEMENT_FAILED);
         }
       }
   }
   
   @OnMessage(types = { KeyPadCapability.DisarmPressedEvent.NAME })
   public void onKeypadDisarm(PlatformMessage message, SubsystemContext<CareSubsystemModel> context) {
      context.logger().debug("Keypad disarm");
      clear ( message.getActor().getRepresentation(), context );
   }

   
   //BEHAVIOR SUPPORT
   
   @OnScheduledEvent()
   public void onScheduledEvent(ScheduledEvent event, SubsystemContext<CareSubsystemModel> context) {
      context.logger().debug("Care subsystem received an onScheduledEvent for event [{}]", event);

      checkForFailedAcknowledgement(event,context);

      behaviorMonitor.handleTimeout(event, context);
   }

   @OnMessage(types= {BehaviorAlertEvent.NAME}) 
   public void onAlert( PlatformMessage message,  SubsystemContext<CareSubsystemModel> context) {
      String id = BehaviorAlertEvent.getBehaviorId(message.getValue());
      triggerAlert(id,context);
   }

   
   private void triggerAlert(String cause, SubsystemContext<CareSubsystemModel> context){
      if (!context.model().isAlarmStateALERT()) {
         context.model().setLastAlertCause(cause);
         context.model().setLastAlertTime(new Date());
         context.model().setAlarmState(CareSubsystemCapability.ALARMSTATE_ALERT);
         context.model().setLastAcknowledgement(CareSubsystemCapability.LASTACKNOWLEDGEMENT_PENDING);
         clearLastTriggers(context);
         addLastTrigger(cause,context);
         sendAlertNotification(context);
         soundTheAlarms(cause, context);         
      } else {
         addLastTrigger(cause,context);
      }
   }

   private void clearLastTriggers(SubsystemContext<CareSubsystemModel> context ) {
      context.model().setLastAlertTriggers(new HashMap<String,Date>());      
   }
   
   private void addLastTrigger(String name,SubsystemContext<CareSubsystemModel> context ) {
      setIfNull(context.model(), CareSubsystemCapability.ATTR_LASTALERTTRIGGERS, new HashMap<String,Date>());
      Map<String,Date> allTriggers = new HashMap<>(context.model().getLastAlertTriggers());
      allTriggers.put(name, new Date());
      context.model().setLastAlertTriggers(allTriggers);      
   }
   
   private void soundTheAlarms(String source, SubsystemContext<CareSubsystemModel> context) {
      if (!context.model().getSilent()) {
         AlertState.sendAlert(context, PlayToneRequest.TONE_INTRUDER);
         MessageBody alertRequest = KeyPad.createAlertingRequest(KeyPadAlertMode.PANIC);
         SubsystemUtils.sendTo(context, KeyPad.isKeypad, alertRequest);         
      }
   }
  
   // 
   private static Address setActorForEvent(SubsystemContext<CareSubsystemModel> context) {
      //Address existingActor = context.get
      LooselyTypedReference val = context.getVariable(ACTOR_KEY);
      if(val != null){
         Address curActor = val.as(Address.class);
         if(curActor != null) {
            context.setActor(curActor);
         }
      }
      return null;
   }

   @Request(ListBehaviorsRequest.NAME)
   public MessageBody listBehaviors(PlatformMessage message, SubsystemContext<CareSubsystemModel> context) {
      List<Map<String, Object>> behaviors = BehaviorUtil.convertListOfType(behaviorManager.listCareBehaviors(context));
      MessageBody response = ListBehaviorsResponse.builder()
            .withBehaviors(behaviors)
            .build();
      return response;
   }

   @Request(UpdateBehaviorRequest.NAME)
   public MessageBody updateBehavior(PlatformMessage message, SubsystemContext<CareSubsystemModel> context) {
      Map<String, Object> behavior = UpdateBehaviorRequest.getBehavior(message.getValue());
      CareBehaviorTypeWrapper behaviorWrapper = new CareBehaviorTypeWrapper(behavior);
      if (behaviorManager.behaviorNameExists(behaviorWrapper, context)) {
         throw new ErrorEventException(CareErrors.duplicateName(behaviorWrapper.getName()));
      }
      behaviorManager.updateBehavior(behaviorWrapper, context);
      MessageBody response = UpdateBehaviorResponse.instance();
      return response;
   }

   @Request(AddBehaviorRequest.NAME)
   public MessageBody addBehavior(PlatformMessage message, SubsystemContext<CareSubsystemModel> context) {
      Map<String, Object> behavior = AddBehaviorRequest.getBehavior(message.getValue());
      CareBehaviorTypeWrapper behaviorWrapper = new CareBehaviorTypeWrapper(behavior);
      if(behaviorManager.behaviorNameExists(behaviorWrapper.getName(), context)){
         throw new ErrorEventException(CareErrors.duplicateName(behaviorWrapper.getName()));
      }
      String behaviorId = behaviorManager.addBehavior(behaviorWrapper, context);
      MessageBody response = AddBehaviorResponse.builder().withId(behaviorId).build();
      return response;
   }



   @Request(RemoveBehaviorRequest.NAME)
   public MessageBody removeBehavior(PlatformMessage message, SubsystemContext<CareSubsystemModel> context) {
      String behaviorId = RemoveBehaviorRequest.getId(message.getValue());
      if (behaviorId == null) {
         throw new ErrorEventException(Errors.invalidParam("behaviorId"));
      }
      boolean removed = behaviorManager.removeBehavior(behaviorId, context);
      MessageBody response = RemoveBehaviorResponse.builder().withRemoved(removed).build();
      return response;
   }

   @Request(ListBehaviorTemplatesRequest.NAME)
   public MessageBody listBehaviorTemplates(PlatformMessage message, SubsystemContext<CareSubsystemModel> context) {
      List<Map<String, Object>> behaviorTemplates = BehaviorUtil.convertListOfType(behaviorManager.listCareBehaviorTemplates(context));
      MessageBody response = ListBehaviorTemplatesResponse.builder()
            .withBehaviorTemplates(behaviorTemplates)
            .build();
      return response;
   }
   
   //IVR Ack Support
   @OnMessage(types = { IvrNotificationAcknowledgedEvent.NAME })
   public void ivrAcknowledged(PlatformMessage message, SubsystemContext<CareSubsystemModel> context) {
      //SecuritySubsystemUtil.setMessageSource(message, context);
      String msgKey = NotificationCapability.IvrNotificationAcknowledgedEvent.getMsgKey(message.getValue());
      Address actor = message.getActor();
      // we are only interested in care
      if (equalsAny(msgKey, NotificationsUtil.CareAlarm.KEY_BY_RULE, NotificationsUtil.CareAlarm.KEY_BY_BEHAVIOR)){
         acknowledgeCareAlert(context,actor!=null?actor.getRepresentation():null);
      }
   }
}

