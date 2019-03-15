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
package com.iris.util;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

public class TimeUtil
{
   private static final PeriodFormatter friendlyDurationFormatter = new PeriodFormatterBuilder()
      .appendWeeks()
      .appendSuffix(" week ",   " weeks ")
      .appendDays()
      .appendSuffix(" day ",    " days ")
      .appendHours()
      .appendSuffix(" hour ",   " hours ")
      .appendMinutes()
      .appendSuffix(" minute ", " minutes ")
      .appendSeconds()
      .appendSuffix(" second ", " seconds ")
      .toFormatter();

   private static final PeriodFormatter compactDurationFormatter = ISOPeriodFormat.standard();

   public static String toFriendlyDuration(long durationSecs)
   {
      return toFriendlyDuration(durationSecs, SECONDS);
   }

   public static String toFriendlyDuration(long duration, TimeUnit timeUnit)
   {
      return print(duration, timeUnit, friendlyDurationFormatter, true);
   }

   public static String toFriendlyDurationSince(Date date)
   {
      return toFriendlyDuration(getMillisSince(date), MILLISECONDS);
   }

   public static String toCompactDuration(long durationSecs)
   {
      return toCompactDuration(durationSecs, SECONDS);
   }

   public static String toCompactDuration(long duration, TimeUnit timeUnit)
   {
      return print(duration, timeUnit, compactDurationFormatter, false);
   }

   public static String toCompactDurationSince(Date date)
   {
      return toCompactDuration(getMillisSince(date), MILLISECONDS);
   }

   private static long getMillisSince(Date date)
   {
      return System.currentTimeMillis() - date.getTime();
   }

   private static String print(long duration, TimeUnit timeUnit, PeriodFormatter formatter, boolean trim)
   {
      Duration durationObject = Duration.millis(timeUnit.toMillis(duration));

      Period normalizedPeriod = durationObject.toPeriod().normalizedStandard();

      String durationString = formatter.print(normalizedPeriod);

      return trim ? durationString.trim() : durationString;
   }

   private TimeUtil() { }
}

