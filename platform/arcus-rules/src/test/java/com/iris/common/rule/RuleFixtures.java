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
package com.iris.common.rule;

import java.util.Calendar;
import java.util.Date;

import org.junit.Assert;

import com.iris.common.rule.time.DayOfWeek;
import com.iris.common.rule.time.TimeOfDay;

/**
 * 
 */
// TODO move to common test library
public class RuleFixtures {
   
   /**
    * Helper for creating a calender at a given {@link TimeOfDay}.
    * @param hours
    * @param minutes
    * @param seconds
    * @return
    */
   public static Calendar time(int hours, int minutes, int seconds) {
      Calendar calendar = Calendar.getInstance();
      calendar.set(Calendar.HOUR_OF_DAY, hours);
      calendar.set(Calendar.MINUTE, minutes);
      calendar.set(Calendar.SECOND, seconds);
      return calendar;
   }

   /**
    * Helper for creating a calendar on a give {@link DayOfWeek}. 
    * @param dayOfWeek
    * @return
    */
   public static Calendar day(int dayOfWeek) {
      Calendar calendar = Calendar.getInstance();
      calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
      return calendar;
   }

   public static void assertDateEquals(long expected, long actual) {
      assertDateEquals(new Date(expected), new Date(actual));
   }
   
   public static void assertDateEquals(Date expected, Date actual) {
      assertDateEquals(expected, actual, 1000);
   }
   
   public static void assertDateEquals(Date expected, Date actual, long deltaMs) {
      long delta = Math.abs(actual.getTime() - expected.getTime()); 
      Assert.assertTrue("Expected " + expected + " but was " + actual + " difference of " + Math.abs(actual.getTime() - expected.getTime()) + " ms", delta <= deltaMs);
   }
   
}

