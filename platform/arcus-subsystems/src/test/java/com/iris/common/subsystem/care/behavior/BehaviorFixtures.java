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
package com.iris.common.subsystem.care.behavior;

import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.type.CareBehavior;
import com.iris.messages.type.CareBehaviorInactivity;
import com.iris.messages.type.TimeWindow;

public class BehaviorFixtures {

      public static CareBehavior createCareBehavior(){
         CareBehavior cb = new CareBehavior();
         cb.setTimeWindows(ImmutableList.<Map<String,Object>>of(createTimeWindow().toMap()));
         cb.setAvailableDevices(ImmutableSet.<String>of());
         cb.setDevices(ImmutableSet.<String>of());
         cb.setTemplateId("1");
         cb.setName("Test Behavior");
         cb.setType(CareBehavior.TYPE_INACTIVITY);
         cb.setEnabled(true);;
         return cb;
      }
      public static TimeWindow createTimeWindow(String day, int duration, String startTime){
         TimeWindow timeWindow=new TimeWindow();
         timeWindow.setDay(day);
         timeWindow.setDurationSecs(duration);
         timeWindow.setStartTime(startTime);
         return timeWindow;
      }

      public static TimeWindow createTimeWindow(){
         return createTimeWindow("MONDAY", 300, "07:00:00");
      }
      public static CareBehaviorInactivity createInactivtyCareBehavior(){
         CareBehaviorInactivity inactivity = new CareBehaviorInactivity(createCareBehavior().toMap());
         inactivity.setDurationSecs(30);
         return inactivity;
      }

}

