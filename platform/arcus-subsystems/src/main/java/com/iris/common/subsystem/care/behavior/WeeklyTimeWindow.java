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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.iris.common.time.DayOfWeek;
import com.iris.common.time.TimeOfDay;
import com.iris.messages.type.TimeWindow;


public class WeeklyTimeWindow {

   private static final long MILLIS_IN_MINUTE=60000;
   private static final long MILLIS_IN_HOUR=MILLIS_IN_MINUTE*60;
   private static final long MILLIS_IN_DAY=MILLIS_IN_HOUR*24;
   
   public WeeklyTimeWindow(DayOfWeek startDay,String startTime, int duration, TimeUnit timeUnit){
      this(startDay,TimeOfDay.fromString(startTime),duration,timeUnit);
   }   
   public WeeklyTimeWindow(DayOfWeek startDay,TimeOfDay startTime, int duration, TimeUnit timeUnit){
      this.startDay=startDay;
      this.startTime=startTime;
      this.duration=duration;
      this.durationTimeUnit=timeUnit;
   }
   
   public WeeklyTimeWindow(TimeWindow timeWindow){
      this.startDay=DayOfWeek.valueOf(timeWindow.getDay().toString());
      this.startTime=TimeOfDay.fromString(timeWindow.getStartTime());
      this.duration=timeWindow.getDurationSecs();
      this.durationTimeUnit=TimeUnit.SECONDS;
   }
   
   private final DayOfWeek startDay;
   private final TimeOfDay startTime;
   private final int duration;
   private final TimeUnit durationTimeUnit;
   
   
   public boolean isDateInTimeWindow(Date asOf,TimeZone tz){
      Calendar cal = Calendar.getInstance(tz);
      cal.setTime(asOf);
      DayOfWeek currentDay = DayOfWeek.from(cal);
      TimeOfDay currentTime = new TimeOfDay(cal);
      
      long windowStartTime = millisIntoWeek(startDay, startTime);
      long windowEndTime = windowStartTime+durationTimeUnit.toMillis(duration);
      long checkDateIntoWeek = millisIntoWeek(currentDay, currentTime);  
      return (checkDateIntoWeek >= windowStartTime) && (checkDateIntoWeek < windowEndTime);
   }
   
   public Date nextOrCurrentStartDate(Date asOf,TimeZone tz){
      Date thisWeekStart = thisWeekStartDate(asOf, tz);
      Date nextWeekStart = nextWeekStartDate(asOf, tz);
      if(isDateInTimeWindow(asOf, tz)){
         return thisWeekStart; 
      }
      return nextWeekStart;
   }

   public Date thisWeekStartDate(Date asOf,TimeZone tz){
      Calendar cal = Calendar.getInstance(tz);
      cal.setTime(asOf);
      DayOfWeek currentDay = DayOfWeek.from(cal);
      TimeOfDay currentTime = new TimeOfDay(cal);
      long windowStartTime = millisIntoWeek(startDay, startTime);
      long checkDateIntoWeek = millisIntoWeek(currentDay, currentTime);  
      long diff = windowStartTime-checkDateIntoWeek;
      return new Date(asOf.getTime()+diff);
   }
   
   public Date nextWeekStartDate(Date asOf,TimeZone tz){
      Date currentWeekStartDate = thisWeekStartDate(asOf, tz);
      if(currentWeekStartDate.before(asOf)){
         Calendar cal=Calendar.getInstance(tz);
         cal.setTime(currentWeekStartDate);
         cal.add(Calendar.DAY_OF_YEAR, 7);
         return cal.getTime();
      }
      return currentWeekStartDate;
   }
   
   private long millisIntoWeek(DayOfWeek dow,TimeOfDay tod){
      return (dow.ordinal()*MILLIS_IN_DAY)+(tod.getHours()*MILLIS_IN_HOUR)+(tod.getMinutes()*MILLIS_IN_MINUTE)+(tod.getSeconds()*1000);
   }
   public static Date calculateEndDate(Date startDate,TimeWindow timeWindow){
      Date endDate = new Date(startDate.getTime()+(timeWindow.getDurationSecs()*1000));
      return endDate;
   }
   
   public Date calculateEndDate(Date startDate){
      long durrationMillis=TimeUnit.MILLISECONDS.convert(duration, durationTimeUnit);
      Date endDate = new Date(startDate.getTime()+durrationMillis);
      return endDate;
   }
   
   public static WeeklyTimeWindow nextOrCurrentWindow(List<WeeklyTimeWindow>windows, Date asOfDate,TimeZone tz){
      if(windows.size()==0){
         return null;
      }
      List<WeeklyTimeWindow>sortedWindows=new ArrayList<WeeklyTimeWindow>(windows);
      Collections.sort(sortedWindows, new WeeklyTimeWindowComparator(asOfDate, tz));
      return sortedWindows.get(0);
   }
   public static List<WeeklyTimeWindow>sortByStartTime(List<WeeklyTimeWindow>windows,Date asOfDate,TimeZone tz){
      List<WeeklyTimeWindow>wtw=new ArrayList<>(windows);
      Collections.sort(wtw, new WeeklyTimeWindowComparator(asOfDate, tz));
      return wtw;
   }
   public static List<WeeklyTimeWindow>fromTimeWindowDataListSorted(List<Map<String,Object>>windowAttributes,Calendar cal){
      List<WeeklyTimeWindow>weeklyTimeWindows=new ArrayList<WeeklyTimeWindow>();
      for(Map<String,Object>window:windowAttributes){
         weeklyTimeWindows.add(new WeeklyTimeWindow(new TimeWindow(window)));
      }
      return sortByStartTime(weeklyTimeWindows,cal.getTime(),cal.getTimeZone());
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + duration;
      result = prime * result + ((durationTimeUnit == null) ? 0 : durationTimeUnit.hashCode());
      result = prime * result + ((startDay == null) ? 0 : startDay.hashCode());
      result = prime * result + ((startTime == null) ? 0 : startTime.hashCode());
      return result;
   }
   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      WeeklyTimeWindow other = (WeeklyTimeWindow) obj;
      if (duration != other.duration)
         return false;
      if (durationTimeUnit != other.durationTimeUnit)
         return false;
      if (startDay != other.startDay)
         return false;
      if (startTime == null) {
         if (other.startTime != null)
            return false;
      }else if (!startTime.equals(other.startTime))
         return false;
      return true;
   }
   @Override
   public String toString() {
      return "WeeklyTimeWindow [startDay=" + startDay + ", startTime=" + startTime + ", duration=" + duration + ", durationTimeUnit=" + durationTimeUnit + "]";
   }
   
   private static class WeeklyTimeWindowComparator implements Comparator<WeeklyTimeWindow>{
      private final Date asOfDate;
      private final TimeZone timeZone;
      
      public WeeklyTimeWindowComparator(Date asOfDate,TimeZone timeZone) {
         this.asOfDate = asOfDate;
         this.timeZone = timeZone;
      }

      @Override
      public int compare(WeeklyTimeWindow o1, WeeklyTimeWindow o2) {
         Date o1StartDate = o1.nextOrCurrentStartDate(asOfDate, timeZone);
         Date o2StartDate = o2.nextOrCurrentStartDate(asOfDate, timeZone);
         return o1StartDate.compareTo(o2StartDate);
      }
      
   }

   
}

