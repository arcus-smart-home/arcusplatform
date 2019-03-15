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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.reflex.ReflexDevice;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.HubAlarmCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.type.IncidentTrigger;

import static com.iris.agent.alarm.AlarmEvents.*;

public class AlarmPanic extends AbstractSafetyAlarm {
   private static final Logger log = LoggerFactory.getLogger(AlarmSecurity.class);
   public static final String NAME = "panic";

   public AlarmPanic(AlarmController parent) {
      super(parent, log, NAME);
   }

   @Override
   protected boolean isTriggerInteresting(TriggerEvent event) {
      return event.getTrigger() == Trigger.PANIC;
   }

   @Override
   protected boolean isSupported(ReflexDevice device) {
      return device.getCapabilities().contains(KeyPadCapability.NAME);
   }

   @Override
   protected boolean isTriggered(ReflexDevice device) {
      // TODO: keep track of keypads that are panicing
      return false;
   }

   @Override
   protected String getIncidentAlarmType() {
      return IncidentTrigger.ALARM_PANIC;
   }

   @Override
   protected void handlePendingClearEvent(Event event) {
      if (event instanceof ClearEvent) {
         clearTriggers();
         if(isAttributeSetEmpty(activeDevices) && isAttributeSetEmpty(triggeredDevices) && isAttributeSetEmpty(offlineDevices)) {
            transitionTo(AlarmCapability.ALERTSTATE_INACTIVE);
         } else {
            transitionTo(HubAlarmCapability.PANICALERTSTATE_READY);
         }
      } else {
         super.handlePendingClearEvent(event);
      }
   }

   @Override
   protected void handleInactiveEvent(Event event) {
      if(event instanceof TriggerEvent) {
         TriggerEvent tevent = (TriggerEvent)event;
         if(isTriggerInteresting(tevent) && tevent.isTriggered()) {
            transitionTo(AlarmCapability.ALERTSTATE_ALERT);
         } else {
            super.handleInactiveEvent(event);
         }
      } else {
         super.handleInactiveEvent(event);
      }
   }

   @Override
   protected void handleReadyEvent(Event event) {
      if(event instanceof TriggerEvent) {
         TriggerEvent te = (TriggerEvent) event;
         if(te.getTrigger() == Trigger.PANIC && te.isTriggered()) {
            transitionTo(HubAlarmCapability.PANICALERTSTATE_ALERT);
            return;
         }
      }
      super.handleReadyEvent(event);
   }

   @Override
   public String getName() {
      return NAME;
   }

   @Override
   public int getPriority() {
      return 3;
   }
}

