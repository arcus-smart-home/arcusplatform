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
import com.iris.messages.capability.LeakH2OCapability;
import com.iris.messages.type.IncidentTrigger;

import static com.iris.agent.alarm.AlarmEvents.*;

public class AlarmWater extends AbstractSafetyAlarm {
   private static final Logger log = LoggerFactory.getLogger(AlarmSecurity.class);
   public static final String NAME = "water";

   public AlarmWater(AlarmController parent) {
      super(parent, log, NAME);
   }

   @Override
   protected boolean isTriggerInteresting(TriggerEvent event) {
      return event.getTrigger() == Trigger.WATER;
   }

   @Override
   protected boolean isSupported(ReflexDevice device) {
      return device.getCapabilities().contains(LeakH2OCapability.NAME);
   }

   @Override
   protected boolean isTriggered(ReflexDevice device) {
      return LeakH2OCapability.STATE_LEAK.equals(device.getAttribute(LeakH2OCapability.ATTR_STATE));
   }

   @Override
   protected String getIncidentAlarmType() {
      return IncidentTrigger.ALARM_WATER;
   }

   @Override
   public String getName() {
      return NAME;
   }

   @Override
   public int getPriority() {
      return 6;
   }
}

