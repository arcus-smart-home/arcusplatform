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
package com.iris.kafka.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Instants {
   private static final Pattern ABSOLUTE_TIME_PATTERN = Pattern.compile("(\\d{4}(-\\d{1,2})?(-\\d{1,2})?)?(T\\d{1,2}(:\\d{2})?(:\\d{2})?)?(Z\\S*)?");
   private static final Pattern RELATIVE_TIME_PATTERN = Pattern.compile("([+-])?(\\d+d)?(\\d+h)?(\\d+m)?(\\d+s)?");

   public static Instant parse(String value) {
      Matcher m = ABSOLUTE_TIME_PATTERN.matcher(value);
      if(m.matches()) {
         return parseAbsolute(m);
      }
      
      m = RELATIVE_TIME_PATTERN.matcher(value);
      if(m.matches()) {
         TemporalAmount delta = parseRelative(m);
         return Instant.now().plus(delta);
      }
      
      throw new IllegalArgumentException("Value [" + value + "] is neither an absolute or relative time.");
   }
   
   public static Instant parseAbsolute(String value) {
      Matcher m = ABSOLUTE_TIME_PATTERN.matcher(value);
      if(!m.matches()) {
         throw new IllegalArgumentException("Invalid start date: " + value + ".\nMust be 'earliest', 'latest', day as '2015-12-2', time as 'T11:12:14ZCST' or some similar combination.");
      }
      
      return parseAbsolute(m);
   }
   
   public static TemporalAmount parseRelative(String offset) {
      Matcher m = RELATIVE_TIME_PATTERN.matcher(offset);
      if(!m.matches()) {
         throw new IllegalArgumentException("Invalid offset: " + offset + ".\nMust be of the form +/-10d6h30m15s");
      }
      
      return parseRelative(m);
   }
   
   private static TemporalAmount parseRelative(Matcher m) {
      boolean neg = m.group(1) != null && m.group(1).equals("-");
      Duration duration = Duration.ZERO;
      if(m.group(2) != null) {
         Duration delta = Duration.of(parseDuration(m.group(2)), ChronoUnit.DAYS);
         duration = add(duration, delta, neg);
      }
      if(m.group(3) != null) {
         Duration delta = Duration.of(parseDuration(m.group(3)), ChronoUnit.HOURS);
         duration = add(duration, delta, neg);
      }
      if(m.group(4) != null) {
         Duration delta = Duration.of(parseDuration(m.group(4)), ChronoUnit.MINUTES);
         duration = add(duration, delta, neg);
      }
      if(m.group(5) != null) {
         Duration delta = Duration.of(parseDuration(m.group(5)), ChronoUnit.SECONDS);
         duration = add(duration, delta, neg);
      }
      return duration;
   }
   
   private static Duration add(Duration lhs, Duration delta, boolean neg) {
      return neg ? lhs.minus(delta) : lhs.plus(delta);
   }

   private static Instant parseAbsolute(Matcher m) {
      LocalDateTime ldt = LocalDateTime.now();
      if(m.group(1) != null) {
         ldt = setDate(ldt, m.group(1)); // YYYY-MM-DD
      }
      if(m.group(4) != null) {
         ldt = setTime(ldt, m.group(4).substring(1)); // HH:MM:SS
      }
      ZoneId zone;
      if(m.group(7) != null) {
         zone = getOffset(m.group(7).substring(1)); // offset
      }
      else {
         zone = ZoneId.systemDefault();
      }
      return ldt.atZone(zone).toInstant();
   }
   
   private static long parseDuration(String duration) {
      return Long.parseLong(duration.substring(0, duration.length() - 1));
   }

   private static ZoneId getOffset(String zoneId) {
      return ZoneId.of(zoneId);
   }

   private static LocalDateTime setDate(LocalDateTime ldt, String date) {
      String [] parts = date.split("\\-");
      ldt = ldt.withYear(Integer.parseInt(parts[0]));
      if(parts.length > 1) {
         ldt = ldt.withMonth(Integer.parseInt(parts[1]));
      }
      if(parts.length > 2) {
         ldt = ldt.withDayOfMonth(Integer.parseInt(parts[2]));
      }
      return ldt;
   }

   private static LocalDateTime setTime(LocalDateTime ldt, String time) {
      String [] parts = time.split("\\:");
      ldt = ldt.withHour(Integer.parseInt(parts[0]));
      if(parts.length > 1) {
         ldt = ldt.withMinute(Integer.parseInt(parts[1]));
      }
      if(parts.length > 2) {
         ldt = ldt.withSecond(Integer.parseInt(parts[2]));
      }
      return ldt;
   }

}

