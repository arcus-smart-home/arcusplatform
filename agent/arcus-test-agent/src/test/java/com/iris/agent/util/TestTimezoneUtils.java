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
package com.iris.agent.util;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class TestTimezoneUtils {
   private static final TimeZone UTC;

   private static TestZone[] TEST_TIMEZONES = new TestZone[] {
      zone("US/Eastern", -5, -4),
      zone("US/Central", -6, -5),
      zone("US/Mountain", -7, -6),
      zone("US/Arizona", -7, -7),
      zone("US/Pacific", -8, -7),
      zone("US/Alaska", -9, -8),
      zone("US/Aleutian", -10, -9),
      zone("US/Hawaii", -10, -10),
      zone("US/Samoa", -11, -11),
      zone("Pacific/Palau", 9, 9),
   };

   private static TestDate[] TEST_DATES = new TestDate[] {
      date(Calendar.JANUARY, 1, 2016, false),
      date(Calendar.NOVEMBER, 5, 2016, true),
   };

   static {
      UTC = TimeZone.getTimeZone("UTC");
   }

   @Test
   public void testCorrectTimeConversion() {
      for (TestDate test : TEST_DATES) {
         Calendar utc = toutc(test.date);
         for (TestZone tz : TEST_TIMEZONES) {
            Calendar local = TimezoneUtils.convert(test.date, tz.id);
            if (test.expectDst) {
               assertEquals(tz.id, hour(utc) + tz.dstOffset, hour(local));
            } else {
               assertEquals(tz.id, hour(utc) + tz.stdOffset, hour(local));
            }
         }
      }
   }

   @Test
   public void testCorrectRawOffset() {
      for (TestDate test : TEST_DATES) {
         for (TestZone tz : TEST_TIMEZONES) {
            assertEquals(TimeUnit.MILLISECONDS.convert(tz.stdOffset, TimeUnit.HOURS), TimezoneUtils.getZoneOffset(test.date, tz.id));
         }
      }
   }

   @Test
   public void testCorrectTotalOffset() {
      for (TestDate test : TEST_DATES) {
         for (TestZone tz : TEST_TIMEZONES) {
            if (test.expectDst) {
               assertEquals(TimeUnit.MILLISECONDS.convert(tz.dstOffset, TimeUnit.HOURS), TimezoneUtils.getOffset(test.date, tz.id));
            } else {
               assertEquals(TimeUnit.MILLISECONDS.convert(tz.stdOffset, TimeUnit.HOURS), TimezoneUtils.getOffset(test.date, tz.id));
            }
         }
      }
   }

   private static final Calendar toutc(Date date) {
      Calendar cal = Calendar.getInstance(UTC);
      cal.setTime(date);

      return cal;
   }

   private static final int hour(Calendar cal) {
      return cal.get(Calendar.HOUR_OF_DAY);
   }

   private static final TestZone zone(String id, int stdOffset, int dstOffset) {
      return new TestZone(id, stdOffset, dstOffset);
   }

   private static final TestDate date(int month, int day, int year, boolean expectDst) {
      Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      cal.set(year, month, day, 12, 0, 0);
      cal.set(Calendar.MILLISECOND, 0);

      return new TestDate(cal.getTime(), expectDst);
   }

   private static String toString(Calendar cal) {
      return String.format("%d/%d/%04d %02d:%02d:%02d.%03d",
         cal.get(Calendar.MONTH) + 1,
         cal.get(Calendar.DAY_OF_MONTH),
         cal.get(Calendar.YEAR),
         cal.get(Calendar.HOUR_OF_DAY),
         cal.get(Calendar.MINUTE),
         cal.get(Calendar.SECOND),
         cal.get(Calendar.MILLISECOND));
   }

   private static final class TestZone {
      private final String id;
      private final int stdOffset;
      private final int dstOffset;

      private TestZone(String id, int stdOffset, int dstOffset) {
         this.id = id;
         this.stdOffset = stdOffset;
         this.dstOffset = dstOffset;
      }
   }

   private static final class TestDate {
      private final Date date;
      private final boolean expectDst;

      private TestDate(Date date, boolean expectDst) {
         this.date = date;
         this.expectDst = expectDst;
      }
   }
}

