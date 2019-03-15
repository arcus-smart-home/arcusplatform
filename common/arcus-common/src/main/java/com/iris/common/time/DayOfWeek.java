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
package com.iris.common.time;

import java.util.Calendar;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;

/**
 *
 */
public enum DayOfWeek {
   MONDAY,
   TUESDAY,
   WEDNESDAY,
   THURSDAY,
   FRIDAY,
   SATURDAY,
   SUNDAY;

   public static DayOfWeek from(Calendar calendar) {
      Preconditions.checkNotNull(calendar, "calendar may not be null");
      int dayId = calendar.get(Calendar.DAY_OF_WEEK);
      switch(dayId) {
      case Calendar.MONDAY:     return MONDAY;
      case Calendar.TUESDAY:    return TUESDAY;
      case Calendar.WEDNESDAY:  return WEDNESDAY;
      case Calendar.THURSDAY:   return THURSDAY;
      case Calendar.FRIDAY:     return FRIDAY;
      case Calendar.SATURDAY:   return SATURDAY;
      case Calendar.SUNDAY:     return SUNDAY;
      default: throw new IllegalArgumentException("Unrecognized day of week: " + dayId);
      }
   }

   public static DayOfWeek fromAbbr(String abbv) {
      Preconditions.checkNotNull(abbv, "abbv may not be null");
      for(DayOfWeek dow : DayOfWeek.values()) {
         if(StringUtils.startsWithIgnoreCase(dow.name(), abbv)) {
            return dow;
         }
      }
      return null;
   }

   public static int toCalendar(DayOfWeek day) {
      Preconditions.checkNotNull(day, "day may not be null");
      switch(day) {
      case MONDAY: return Calendar.MONDAY;
      case TUESDAY: return Calendar.TUESDAY;
      case WEDNESDAY: return Calendar.WEDNESDAY;
      case THURSDAY: return Calendar.THURSDAY;
      case FRIDAY: return Calendar.FRIDAY;
      case SATURDAY: return Calendar.SATURDAY;
      case SUNDAY: return Calendar.SUNDAY;
      default: throw new IllegalArgumentException("Unrecognized day of week: " + day);
      }
   }
}

