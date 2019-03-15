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

import java.util.Calendar;

import com.iris.common.time.TimeOfDay;

public class CalendarUtil {

   private CalendarUtil() {
   }

   public static Calendar setTime(Calendar calendar, TimeOfDay tod) {
      Calendar c = (Calendar) calendar.clone();
      return tod.on(c);
   }

   public static Calendar addDays(Calendar calendar, int days) {
      Calendar c = (Calendar) calendar.clone();
      c.add(Calendar.DAY_OF_YEAR, days);
      return c;
   }

   public static Calendar midnight(Calendar calendar) {
      Calendar c = (Calendar) calendar.clone();
      c.set(Calendar.HOUR_OF_DAY, 0);
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);
      return c;
   }

   public static Calendar nextOdd(Calendar calendar) {
      Calendar c = (Calendar) calendar.clone();
      while(c.get(Calendar.DATE) % 2 == 0) {
         c.add(Calendar.DATE, 1);
      }
      return c;

   }

   public static Calendar nextEven(Calendar calendar) {
      Calendar c = (Calendar) calendar.clone();
      while(c.get(Calendar.DATE) % 2 != 0) {
         c.add(Calendar.DATE, 1);
      }
      return c;
   }
}

