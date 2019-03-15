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
package com.iris.agent.alarm;

import static com.iris.agent.alarm.AlarmEvents.ArmEvent;
import static com.iris.agent.alarm.AlarmEvents.ClearEvent;
import static com.iris.agent.alarm.AlarmEvents.DisarmEvent;
import static com.iris.agent.alarm.AlarmEvents.Event;
import static com.iris.agent.alarm.AlarmEvents.TriggerEvent;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.google.common.base.Supplier;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.HubAlarmCapability;

public abstract class AbstractSafetyAlarm extends AbstractAlarm {
   protected final HubAttributesService.Attribute<Set<String>> activeDevices;

   public AbstractSafetyAlarm(AlarmController parent, Logger log, String name) {
      super(parent, log, name, HubAlarmCapability.COALERTSTATE_INACTIVE);

      this.activeDevices = HubAttributesService.computedSet(String.class, "hubalarm:" + name + "ActiveDevices", new Supplier<Set<String>>() {
         @Override
         public Set<String> get() {
            return getDevices()
               .filter((dev) -> !dev.isOffline() && !isTriggered(dev))
               .map((dev) -> dev.getAddress().getRepresentation())
               .collect(Collectors.toSet());
         }
      });

      this.activeDevices.setReportedOnConnect(false);
      this.activeDevices.setReportedOnValueChange(false);
   }

   @Override
   public void updateReportAttributes(Map<String,Object> attrs) {
      super.updateReportAttributes(attrs);
      attrs.put(activeDevices.name(), activeDevices.get());
   }

   /////////////////////////////////////////////////////////////////////////////
   // Ready State
   /////////////////////////////////////////////////////////////////////////////


   @Override
   protected boolean canHandleReadyEvent(Event event) {
      return (event instanceof TriggerEvent && !isAttributeSetEmpty(triggeredDevices)) ||
             super.canHandleReadyEvent(event);
   }

   @Override
   protected void handleReadyEvent(Event event) {
      if(event instanceof ClearEvent) {
         return;
      }
      if (!isAttributeSetEmpty(triggeredDevices)) {
         transitionTo(HubAlarmCapability.COALERTSTATE_ALERT);
      } else {
         super.handleReadyEvent(event);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Alerting State
   /////////////////////////////////////////////////////////////////////////////
   
   @Override
   protected boolean canHandleAlertEvent(Event event) {
      return (event instanceof DisarmEvent) ||
             (event instanceof ArmEvent) ||
             super.canHandleAlertEvent(event);
   }
   
   @Override
   protected void handleAlertEvent(Event event) {
      if (event instanceof DisarmEvent) {
         transitionTo(HubAlarmCapability.COALERTSTATE_PENDING_CLEAR);
      } else {
         super.handleAlertEvent(event);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // PendingClear State
   /////////////////////////////////////////////////////////////////////////////

   @Override
   protected void handlePendingClearEvent(Event event) {
      if(event instanceof ClearEvent) {
         clearTriggers();
         transitionTo(HubAlarmCapability.COALERTSTATE_CLEARING);
      } else if (event instanceof TriggerEvent && ((TriggerEvent) event).isTriggered()) {
         transitionTo(HubAlarmCapability.COALERTSTATE_ALERT);
      } else {
         super.handlePendingClearEvent(event);
      }
   }


   /////////////////////////////////////////////////////////////////////////////
   // Clearing State
   /////////////////////////////////////////////////////////////////////////////
   
   @Override
   protected void enterClearing() {
      super.enterClearing();
      if (isAttributeSetEmpty(triggeredDevices)) {
         transitionTo(HubAlarmCapability.COALERTSTATE_READY);
      }
   }
   
   @Override
   protected boolean canHandleClearingEvent(Event event) {
      return (event instanceof ClearEvent) ||
             (event instanceof TriggerEvent) ||
             (event instanceof ArmEvent) ||
             (event instanceof DisarmEvent) ||
             super.canHandleClearingEvent(event);
   }
   
   @Override
   protected void handleClearingEvent(Event event) {
      if (event instanceof TriggerEvent) {
         if (((TriggerEvent)event).isTriggered()) {
            transitionTo(HubAlarmCapability.COALERTSTATE_ALERT);
         } else if (isAttributeSetEmpty(triggeredDevices)) {
            transitionTo(HubAlarmCapability.COALERTSTATE_READY);
         }
      } else {
         super.handleClearingEvent(event);
      }
   }

   @Override
   public void afterProcessReflexDevices() {

      String state = alertState.get();
      if(HubAlarmCapability.COALERTSTATE_ALERT.equals(state) || HubAlarmCapability.COALERTSTATE_PENDING_CLEAR.equals(state)) {
         return;
      }

      boolean haveActive = !isAttributeSetEmpty(activeDevices);
      boolean haveOffline = !isAttributeSetEmpty(offlineDevices);
      boolean haveTriggered = !isAttributeSetEmpty(triggeredDevices);

      if(!haveActive && !haveOffline && !haveTriggered) {
         transitionTo(AlarmCapability.ALERTSTATE_INACTIVE);
      } else if(haveTriggered) {
         transitionTo(AlarmCapability.ALERTSTATE_ALERT);
      } else {
         transitionTo(AlarmCapability.ALERTSTATE_READY);
      }
   }
}

