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
package com.iris.common.subsystem.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DateMidnight {
   private final Date midnight;
   private final TimeZone tz;

   public DateMidnight(Calendar cal) {
      this(cal.getTime(),cal.getTimeZone());
   }

   public DateMidnight(Date date,TimeZone tz) {
      Calendar calStart = new GregorianCalendar(tz);
      calStart.setTime(date);
      calStart.set(Calendar.HOUR_OF_DAY, 0);
      calStart.set(Calendar.MINUTE, 0);
      calStart.set(Calendar.SECOND, 0);
      calStart.set(Calendar.MILLISECOND, 0);
      this.midnight = calStart.getTime();
      this.tz=tz;
   }
   public Date lastMidnight(){
      return midnight;
   }
   public Date nextMidnight(){
      Calendar calStart = new GregorianCalendar(tz);
      calStart.setTime(midnight);
      calStart.add(Calendar.DAY_OF_YEAR,1);
      return calStart.getTime();
   }
   @Override
   public String toString() {
      return "DateMidnight [midnight=" + midnight + ", tz=" + tz.getID() + "]";
   }
   
   
}

