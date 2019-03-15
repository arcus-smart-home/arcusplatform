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
package com.iris.common.subsystem.lawnngarden.util;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.lawnngarden.model.ZoneDuration;
import com.iris.common.subsystem.lawnngarden.model.ZoneWatering;
import com.iris.common.subsystem.lawnngarden.model.schedules.Schedule;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleMode;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleStatus;
import com.iris.common.subsystem.lawnngarden.model.schedules.Transition;
import com.iris.common.subsystem.lawnngarden.model.schedules.even.EvenSchedule;
import com.iris.common.subsystem.lawnngarden.model.schedules.even.EvenScheduleEvent;
import com.iris.common.subsystem.lawnngarden.model.schedules.interval.IntervalSchedule;
import com.iris.common.subsystem.lawnngarden.model.schedules.interval.IntervalScheduleEvent;
import com.iris.common.subsystem.lawnngarden.model.schedules.odd.OddSchedule;
import com.iris.common.subsystem.lawnngarden.model.schedules.odd.OddScheduleEvent;
import com.iris.common.subsystem.lawnngarden.model.schedules.weekly.WeeklySchedule;
import com.iris.common.subsystem.lawnngarden.model.schedules.weekly.WeeklyScheduleEvent;
import com.iris.common.subsystem.lawnngarden.model.state.IrrigationScheduleState;
import com.iris.common.time.TimeOfDay;
import com.iris.messages.address.Address;
import com.iris.messages.type.IrrigationSchedule;
import com.iris.type.TypeCoercerImpl;

@SuppressWarnings("serial")
public final class LawnNGardenTypeUtil extends TypeCoercerImpl {
   public final static LawnNGardenTypeUtil INSTANCE = new LawnNGardenTypeUtil();

   private LawnNGardenTypeUtil() {
      super(
            Transition.typeHandler(),
            EvenSchedule.typeHandler(),
            EvenScheduleEvent.typeHandler(),
            IntervalSchedule.typeHandler(),
            IntervalScheduleEvent.typeHandler(),
            OddSchedule.typeHandler(),
            OddScheduleEvent.typeHandler(),
            WeeklyScheduleEvent.typeHandler(),
            WeeklySchedule.typeHandler(),
            ZoneDuration.typeHandler(),
            ScheduleStatus.typeHandler(),
            ZoneWatering.typeHandler(),
            IrrigationScheduleState.typeHandler()
      );
   }

   public static Schedule<?, ?> schedule(Map<String,Object> map) {
      if(map == null) {
         return null;
      }

      ScheduleMode type = INSTANCE.coerce(ScheduleMode.class, map.get(IrrigationSchedule.ATTR_TYPE));
      switch(type) {
      case EVEN: return INSTANCE.coerce(EvenSchedule.class, map);
      case ODD: return INSTANCE.coerce(OddSchedule.class, map);
      case WEEKLY: return INSTANCE.coerce(WeeklySchedule.class, map);
      case INTERVAL: return INSTANCE.coerce(IntervalSchedule.class, map);
      default: return null;
      }
   }

   public static Address address(Object o) {
      return INSTANCE.coerce(Address.class, o);
   }

   public static ScheduleMode scheduleMode(Object o) {
      return INSTANCE.coerce(ScheduleMode.class, o);
   }

   public static List<ZoneDuration> zoneDurations(Object o) {
      return INSTANCE.coerceList(ZoneDuration.class, o);
   }

   public static Integer integer(Object o) {
      return INSTANCE.coerce(Integer.class, o);
   }

   public static Integer integer(Object o, int def) {
      if(o == null) {
         return def;
      }
      return integer(o);
   }

   public static String string(Object o) {
      return INSTANCE.coerce(String.class, o);
   }

   public static TimeOfDay timeOfDay(Object o) {
      return INSTANCE.coerce(TimeOfDay.class, o);
   }

   public static Boolean bool(Object o) {
      return INSTANCE.coerce(Boolean.class, o);
   }

   public static Boolean bool(Object o, boolean def) {
      if(o == null) {
         return def;
      }
      return bool(o);
   }

   public static Date date(Object o) {
      return INSTANCE.coerce(Date.class, o);
   }

   public static Transition transition(Object o) {
      return INSTANCE.coerce(Transition.class, o);
   }

   public static Map<Address,ScheduleStatus> scheduleStatus(Map<String,Map<String,Object>> scheduleStatuses) {
      ImmutableMap.Builder<Address,ScheduleStatus> builder = ImmutableMap.builder();
      for(Map.Entry<String,Map<String,Object>> entry : scheduleStatuses.entrySet()) {
         builder.put(
               INSTANCE.coerce(Address.class, entry.getKey()),
               INSTANCE.coerce(ScheduleStatus.class, entry.getValue()));
      }
      return builder.build();
   }
}

