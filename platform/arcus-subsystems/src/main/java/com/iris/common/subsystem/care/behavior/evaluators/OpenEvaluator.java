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
package com.iris.common.subsystem.care.behavior.evaluators;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.care.behavior.CareBehaviorTypeWrapper;
import com.iris.common.subsystem.care.behavior.SubsystemVariableKey;
import com.iris.common.subsystem.care.behavior.WeeklyTimeWindow;
import com.iris.messages.address.Address;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.ContactModel;
import com.iris.messages.model.subs.CareSubsystemModel;
import com.iris.messages.type.CareBehaviorOpen;

public class OpenEvaluator extends BaseBehaviorEvaluator {

   private final CareBehaviorOpen config;
   private final static Set<String> INTERESTED_IN_ATTRIBUTES = ImmutableSet.<String> of(ContactCapability.ATTR_CONTACT);
   public final static SubsystemVariableKey OPEN_TIMEOUT = new SubsystemVariableKey("open-timeout");

   public OpenEvaluator(Map<String, Object> config) {
      this.config = new CareBehaviorOpen(config);
   }

   @Override
   public void onAlarmCleared(SubsystemContext<CareSubsystemModel> context) {
      
      /* If an alarm is cleared but the device remains open, we will put it into an exclusion list until its closed so we re-trigger the alert.
       *The exclusion list will get cleared on window close       
      */       
      forEachBehaviorDevice(context, EvaluatorPredicates.OPEN_CONTACT, new ContextModelConsumer() {
         public void accept(SubsystemContext<CareSubsystemModel> context, Model model) {
            if (getCareBehavior().getActive()) {
               addToAlertExclusionList(model.getAddress(), context);
            }
         }
      });
   }

   // if an contacts are open on start set the first timeout based on the later
   // of the window start or last contact changed time of an open device
   @Override
   public void onWindowStart(WeeklyTimeWindow window, SubsystemContext<CareSubsystemModel> context) {
      scheduleTimeoutsForOpenDevices(context.getLocalTime().getTime(), context);
   }

   private void scheduleTimeoutsForOpenDevices(Date startTime, SubsystemContext<CareSubsystemModel> context) {
      for (String device : config.getDevices()){
         Model model = context.models().getModelByAddress(Address.fromString(device));
         if (existsInExclusionList(model.getAddress(), context)) {
            return;
         }
         if (ContactModel.getContact(model).equals(ContactCapability.CONTACT_OPENED)) {
            Date openedDate = ContactModel.getContactchanged(model);
            Date nextAlertBase = max(startTime, openedDate);
            Integer duration = config.getDurationSecs();
            if (duration == null || duration == 0) {
               addToLastTriggeredDevice(model.getAddress().getRepresentation(), context);
               scheduleMonitorAlertTimeout(0, context);
               return;
            }
            Date wakeAt = new Date(nextAlertBase.getTime() + (duration * 1000));
            SubsystemUtils.setTimeout(wakeAt, context, OPEN_TIMEOUT.create(getBehaviorId(), model.getAddress().getRepresentation()));
         }
      }
   }

   @Override
   public void onModelChange(ModelChangedEvent event, SubsystemContext<CareSubsystemModel> context) {
      String address = event.getAddress().getRepresentation();
      if (config.getDevices().contains(address) && INTERESTED_IN_ATTRIBUTES.contains(event.getAttributeName())) {
         if (ContactCapability.CONTACT_OPENED.equals(event.getAttributeValue())) {
            int duration = 0;
            if (config.getDurationSecs() != null) {
               duration = config.getDurationSecs();
            }
            Date timeout = new Date(context.getLocalTime().getTimeInMillis() + duration * 1000);
            SubsystemUtils.setTimeout(timeout, context, OPEN_TIMEOUT.create(getBehaviorId(), address));
         }else if (ContactCapability.CONTACT_CLOSED.equals(event.getAttributeValue())) {
            removeFromExclusionList(event.getAddress(), context);
            SubsystemUtils.clearTimeout(context, OPEN_TIMEOUT.create(getBehaviorId(), address));
         }
      }
   }

   @Override
   public void onTimeout(ScheduledEvent event, SubsystemContext<CareSubsystemModel> context) {
      for (String device : config.getDevices()){
         Model model = context.models().getModelByAddress(Address.fromString(device));
         if (SubsystemUtils.isMatchingTimeout(event, context, OPEN_TIMEOUT.create(getBehaviorId(), model.getAddress().getRepresentation()))) {
            addToLastTriggeredDevice(model.getAddress().getRepresentation(), context);
            scheduleMonitorAlertTimeout(0, context);
         }
      }
   }

   @Override
   public void onWindowEnd(WeeklyTimeWindow window, SubsystemContext<CareSubsystemModel> context) {
      for (String device : config.getDevices()){
         SubsystemUtils.clearTimeout(context, OPEN_TIMEOUT.create(getBehaviorId(), device));
      }
      clearExclusionList(context);
   }

   @Override
   public CareBehaviorTypeWrapper getCareBehavior() {
      return new CareBehaviorTypeWrapper(config.toMap());
   }

   @Override
   public void onRemoved(SubsystemContext<CareSubsystemModel> context) {
      clearVar(OPEN_TIMEOUT.create(getBehaviorId()), context);
      removeExclusionList(context);
   }

}

