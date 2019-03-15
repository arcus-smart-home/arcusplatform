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
package com.iris.platform.alarm.notification.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.common.alarm.AlertType;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.serv.PersonModel;
import com.iris.messages.model.serv.RuleModel;
import com.iris.messages.type.CallTreeEntry;
import com.iris.platform.alarm.history.ModelLoader;
import com.iris.platform.alarm.incident.Trigger;
import com.iris.platform.alarm.incident.Trigger.Event;
import com.iris.platform.alarm.notification.calltree.CallTreeContext;
import com.iris.platform.alarm.notification.calltree.CallTreeDAO;
import com.iris.platform.alarm.notification.calltree.CallTreeExecutor;

public abstract class BaseNotificationStrategy implements NotificationStrategy {
   private final static Logger logger = LoggerFactory.getLogger(BaseNotificationStrategy.class);
	
   private final ConcurrentMap<Address, Set<AlertType>> activeIncidents = new ConcurrentHashMap<>();
   private final Set<Address> cancelNotificationSentList = ConcurrentHashMap.newKeySet();
   
   private final NotificationStrategyConfig config;
   private final CallTreeExecutor callTreeExecutor;
   private final CallTreeDAO callTreeDao;
   private final ModelLoader modelLoader;

   protected BaseNotificationStrategy(
      NotificationStrategyConfig config,
      CallTreeExecutor callTreeExecutor,
      CallTreeDAO callTreeDao,
      ModelLoader modelLoader
   ) {
      this.config = config;
      this.callTreeExecutor = callTreeExecutor;
      this.callTreeDao = callTreeDao;
      this.modelLoader = modelLoader;
   }

   protected NotificationStrategyConfig getConfig() {
      return config;
   }

   protected CallTreeExecutor getCallTreeExecutor() {
      return callTreeExecutor;
   }

   protected CallTreeDAO getCallTreeDao() { return callTreeDao; }

   @Override
   public void execute(Address incidentAddress, UUID placeId, List<Trigger> triggers) {
      Set<AlertType> seen = activeIncidents.computeIfAbsent(incidentAddress, (addr) -> new HashSet<>());
      List<CallTreeEntry> callTree = callTreeDao.callTreeForPlace(placeId);
      List<Trigger> additionalTriggers = new ArrayList<>();
      for(Trigger trigger : triggers) {
         // TODO:  unified call tree does not handle care notifications at this time and I'm not sure about weather
         if(trigger.getAlarm() == AlertType.CARE || trigger.getAlarm() == AlertType.WEATHER) {
            continue;
         }
         if(!seen.contains(trigger.getAlarm())) {
            doNotify(callTreeContextBuilderFor(incidentAddress, placeId, trigger, callTree), trigger);
            seen.add(trigger.getAlarm());
         } else {
            additionalTriggers.add(trigger);
            if(Event.VERIFIED_ALARM.equals(trigger.getEvent())) {
            	onVerified(incidentAddress, placeId, trigger, callTree);
            }
         }
      }
      if(!additionalTriggers.isEmpty()) {
         onAdditionalTriggers(incidentAddress, placeId, additionalTriggers, callTree);
      }
   }
   

   @Override
   public boolean cancel(Address incidentAddress, UUID placeId, Address cancelledBy, List<String> alarms) {
	  boolean returnResult = false;
	  boolean activeIncident = activeIncidents.get(incidentAddress) != null? true:false;
	  boolean isSent = false;
      if(doCancel(incidentAddress)) {
         activeIncidents.remove(incidentAddress);      
         isSent = cancelNotificationSentList.remove(incidentAddress);
         returnResult = true;
      }
      if(activeIncident && !isSent) {
    	  sendCancelNotification(incidentAddress, placeId, cancelledBy, alarms);
      }
      postCancel(incidentAddress, returnResult);
      return returnResult;
   }
   
   private void sendCancelNotification(Address incidentAddress, UUID placeId, Address cancelledBy, List<String> alarms) {
	   if(cancelNotificationSentList.add(incidentAddress)) {
		   try{
	    	  //only send cancel notification when it's the first time
	    	  List<CallTreeEntry> callTree = callTreeDao.callTreeForPlace(placeId);
	          Model cancelledByPerson = modelLoader.findByAddress(cancelledBy);
	          CallTreeContext.Builder contextBuilder = CallTreeContext.builder()
	                  .withIncidentAddress(incidentAddress)
	                  .withPlaceId(placeId)
	                  .withPriority(NotificationCapability.NotifyRequest.PRIORITY_MEDIUM)
	                  .withMsgKey(getMessageKeyForCancel(alarms.get(0), incidentAddress))
	                  .addCallTreeEntries(callTree)
	                  .addParams(ImmutableMap.<String, String>of(
	                 		 NotificationConstants.CancelParams.PARAM_CANCELL_BY_FIRSTNAME, PersonModel.getFirstName(cancelledByPerson, ""),
	                 		 NotificationConstants.CancelParams.PARAM_CANCELL_BY_LASTNAME, PersonModel.getLastName(cancelledByPerson, "")));
	          getCallTreeExecutor().notifyParallel(contextBuilder.build());
	          
	          contextBuilder.withPriority(NotificationCapability.NotifyRequest.PRIORITY_LOW);
	          getCallTreeExecutor().notifyOwner(contextBuilder.build());
		   }catch( Exception e) {
			   logger.error(String.format("Fail to send cancel notifications for place id [%s], incident [%s]", placeId, incidentAddress ), e);
		   }
	   }
   }
   
   protected void onVerified(Address incidentAddress, UUID placeId, Trigger trigger, List<CallTreeEntry> callTree) {
	   // no op hook		
   }

   protected boolean doCancel(Address incidentAddress) {
      return true;
   }
   
   protected void postCancel(Address incidentAddress, boolean isCancelled) {
	   // no op hook
   }

   @Override
   public void acknowledge(Address incidentAddress, AlertType type) {
      // no op hook
   }

   protected void onAdditionalTriggers(Address incidentAddress, UUID placeId, List<Trigger> trigger, List<CallTreeEntry> callTree) {
      // no op hook
   }

   protected abstract void doNotify(CallTreeContext.Builder contextBuilder, Trigger trigger);
   
   protected String getMessageKeyForCancel(String alertType, Address incidentAddress) {
	   return String.format(NotificationConstants.KEY_TEMPLATE_FOR_CANCEL, alertType.toLowerCase());
   }
   
   protected String getMessageKeyForTrigger(AlertType alertType, Trigger.Event event) {
	   if(Trigger.Event.RULE.equals(event)) {
		   return String.format(NotificationConstants.KEY_TEMPLATE_FOR_TRIGGER_RULE, alertType.name().toLowerCase());
	   }else{
		   return String.format(NotificationConstants.KEY_TEMPLATE_FOR_TRIGGER, alertType.name().toLowerCase());
	   }
   }

   protected CallTreeContext.Builder callTreeContextBuilderFor(Address incidentAddress, UUID placeId, Trigger trigger, List<CallTreeEntry> callTree) {
      return CallTreeContext.builder()
            .withIncidentAddress(incidentAddress)
            .withSequentialDelaySecs(config.getCallTreeSequentialTimeoutSecs())
            .withPlaceId(placeId)
            .withPriority(NotificationCapability.NotifyRequest.PRIORITY_CRITICAL)
            .withMsgKey(getMessageKeyForTrigger(trigger.getAlarm(), trigger.getEvent()))
            .addCallTreeEntries(callTree)
            .addParams(triggerToNotificationParams(trigger));
   }
   

   protected Map<String, String> triggerToNotificationParams(Trigger t) {
      Model m = findModelForTrigger(t);
      return modelToNotificationParams(m, t);
   }

   protected Model findModelForTrigger(Trigger t) {
      if(t == null) {
         return null;
      }
      Model m = modelLoader.findByAddress(t.getSource());
      if(m == null) {
         return null;
      }
      // Supports device, rule, and person
      if(m.getCapabilities().contains(DeviceCapability.NAMESPACE)) {
         return m;
      }
      else if(m.getCapabilities().contains(RuleCapability.NAMESPACE)) {
         return m;
      }
      else if(m.getCapabilities().contains(PersonCapability.NAMESPACE)) {
         return m;
      }
      return null;
   }

   private Map<String,String> modelToNotificationParams(Model m, Trigger t) {
      if(m == null) {
         logger.warn("Unable to load model for trigger [{}]", t);
         return Collections.emptyMap();
      }
      switch(m.getType()) {
      case RuleCapability.NAMESPACE:
         return ImmutableMap.of(
               NotificationConstants.TriggerParams.PARAM_RULE_NAME, RuleModel.getName(m, "rule")
         );
      case PersonCapability.NAMESPACE:
         return ImmutableMap.of(
              NotificationConstants.TriggerParams.PARAM_ACTOR_FIRST_NAME, PersonModel.getFirstName(m, ""),
              NotificationConstants.TriggerParams.PARAM_ACTOR_LAST_NAME, PersonModel.getLastName(m, "")
         );
      case DeviceCapability.NAMESPACE:
         return ImmutableMap.of(
            NotificationConstants.TriggerParams.PARAM_DEVICE_NAME, DeviceModel.getName(m, "device"),
            NotificationConstants.TriggerParams.PARAM_DEVICE_TYPE, DeviceModel.getDevtypehint(m, "sensor")
         );
      default:
         logger.warn("Unable to determine name for model of type [{}]", m.getType());
         return ImmutableMap.of();
      }
   }

}

