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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.iris.common.subsystem.care.behavior.WeeklyTimeWindow;
import com.iris.common.time.DayOfWeek;
import com.iris.common.time.TimeOfDay;

public class TestWeeklyTimeWindow {
   private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
   private TimeZone CST = TimeZone.getTimeZone("CST");
   private Calendar testCaseDate; 
   
   @Before
   public void init(){
      testCaseDate = Calendar.getInstance(CST);
   }
   
   @Test
   public void testFallsWithin() throws Exception{
      WeeklyTimeWindow window = new WeeklyTimeWindow(DayOfWeek.THURSDAY, "10:00:00", 60, TimeUnit.MINUTES);
      Date begin = simulateDateWithCST("2016-01-21T10:00:00"); //Thursday
      assertTrue(window.isDateInTimeWindow(begin,CST));
      Date end = simulateDateWithCST("2016-01-21T10:59:59");
      assertTrue(window.isDateInTimeWindow(end,CST));
   }
   
   @Test
   public void testFallsOutside() throws Exception{
      WeeklyTimeWindow window = new WeeklyTimeWindow(DayOfWeek.THURSDAY, "10:00:00", 60,TimeUnit.MINUTES);
      Date testDate2 = simulateDateWithCST("2016-01-21T11:00:00");
      assertFalse(window.isDateInTimeWindow(testDate2,CST));
   }

   @Test
   public void testFallsInsideMultiDay() throws Exception{
      WeeklyTimeWindow window = new WeeklyTimeWindow(DayOfWeek.THURSDAY, "10:00:00", 3,TimeUnit.DAYS);
      Date testDate2 = simulateDateWithCST("2016-01-23T10:00:00"); //Friday
      assertTrue(window.isDateInTimeWindow(testDate2,CST));
   }
   
   @Test
   public void testWindowStartDate() throws Exception{
      WeeklyTimeWindow window = new WeeklyTimeWindow(DayOfWeek.THURSDAY, "10:00:00", 3,TimeUnit.DAYS);
      Date staticCurrentDate = simulateDateWithCST("2016-01-23T10:00:00"); //Friday
      Date testDate2 = window.thisWeekStartDate(staticCurrentDate, CST);
      Calendar cal = Calendar.getInstance(CST);
      cal.setTime(testDate2);
      assertEquals("should be a thursday",Calendar.THURSDAY,cal.get(Calendar.DAY_OF_WEEK));
      assertEquals("should be the 21st",21,cal.get(Calendar.DAY_OF_MONTH));
      assertEquals("should be the 21st",10,cal.get(Calendar.HOUR));
      assertEquals("should be the 21st",00,cal.get(Calendar.MINUTE));
   }
   
   @Test
   public void testNextOrCurrentStartDateInWindow() throws Exception{
      Date staticCurrentDate = simulateDateWithCST("2016-01-21T06:00:00"); //THURSDAY 6AM
      WeeklyTimeWindow window = new WeeklyTimeWindow(DayOfWeek.THURSDAY, "5:00:00", 3,TimeUnit.HOURS);
      Date expected = simulateDateWithCST("2016-01-21T05:00:00"); //THURSDAY 6AM
      Date nextorcurrent = window.nextOrCurrentStartDate(staticCurrentDate,CST);
      assertEquals(expected, nextorcurrent);
   }  
   @Test
   public void testNextOrCurrentStartDateMissedWindow() throws Exception{
      Date staticCurrentDate = simulateDateWithCST("2016-01-21T20:00:00"); //THURSDAY 8P
      WeeklyTimeWindow window = new WeeklyTimeWindow(DayOfWeek.THURSDAY, "5:00:00", 3,TimeUnit.HOURS);
      Date expected = simulateDateWithCST("2016-01-28T05:00:00"); //NEXT THURSDAY 5AM
      Date nextorcurrent = window.nextOrCurrentStartDate(staticCurrentDate,CST);
      assertEquals(expected, nextorcurrent);
   }  
   
   @Test
   public void testNextTimeWindowFuture() throws Exception{
      Date staticCurrentDate = simulateDateWithCST("2016-01-21T06:00:00"); 
      WeeklyTimeWindow window = new WeeklyTimeWindow(DayOfWeek.WEDNESDAY, "7:00:00", 12,TimeUnit.HOURS);
      WeeklyTimeWindow window2 = new WeeklyTimeWindow(DayOfWeek.THURSDAY, "7:00:00", 12,TimeUnit.HOURS);
      assertFalse(window.isDateInTimeWindow(staticCurrentDate,CST));
      assertFalse(window2.isDateInTimeWindow(staticCurrentDate,CST));
      WeeklyTimeWindow nextWindow = WeeklyTimeWindow.nextOrCurrentWindow(ImmutableList.<WeeklyTimeWindow>of(window,window2),staticCurrentDate,CST);
      assertEquals(window2, nextWindow);
   }
   
   private Date simulateDateWithCST(String strDate){
      strDate+="-0600";
      Date testDate;
      try{
         testDate = df.parse(strDate);
         return testDate;

      }catch (ParseException e){
        throw new RuntimeException("bad data format, expecting " + df.toPattern(),e);
      }
   }
}

