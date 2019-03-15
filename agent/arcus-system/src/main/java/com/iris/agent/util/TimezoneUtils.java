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

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class TimezoneUtils {
   private static final Logger log = LoggerFactory.getLogger(TimezoneUtils.class);
   private static final Map<String,TimezoneInfo> timezones = loadTimezones();

   private TimezoneUtils() {
   }

   public static Calendar convert(Date date, String timeZoneId) {
      Calendar cal = getCalendar(timeZoneId);
      cal.setTime(date);
      return cal;
   }

   public static int getOffset(Date date, String timeZoneId) {
      Calendar cal = convert(date, timeZoneId);
      return cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);
   }

   public static int getZoneOffset(Date date, String timeZoneId) {
      Calendar cal = convert(date, timeZoneId);
      return cal.get(Calendar.ZONE_OFFSET);
   }

   public static int getDstOffset(Date date, String timeZoneId) {
      Calendar cal = convert(date, timeZoneId);
      return cal.get(Calendar.DST_OFFSET);
   }

   private static Calendar getCalendar(String timeZoneId) {
      return Calendar.getInstance(getTimezone(timeZoneId));
   }

   private static TimeZone getTimezone(String timeZoneId) {
      TimezoneInfo info = timezones.get(timeZoneId);
      return TimeZone.getTimeZone((info != null) ? info.id : timeZoneId);
   }

   private static final Map<String,TimezoneInfo> loadTimezones() {
      Map<String,TimezoneInfo> result = new HashMap<>();
      try {
         String tzinfo = IOUtils.toString(TimezoneUtils.class.getResource("/timezones.json"));
         JsonArray tzdata = new JsonParser().parse(tzinfo).getAsJsonArray();
         for (JsonElement tzelem : tzdata) {
            JsonObject tz = tzelem.getAsJsonObject();

            String id = tz.get("id").getAsString();
            String name = tz.get("name").getAsString();
            long offset = tz.get("offset").getAsLong();
            boolean usesDst = tz.get("usesDST").getAsBoolean();

            result.put(id, new TimezoneInfo(id,name,offset,usesDst));
         }
      } catch (IOException ex) {
         log.error("could not load timezone information:", ex);
      }

      return result;
   }

   private static final class TimezoneInfo {
      private final String id;
      private final String name;
      private final long offset;
      private final boolean usesDst;

      private TimezoneInfo(String id, String name, long offset, boolean usesDst) {
         this.id = id;
         this.name = name;
         this.offset = offset;
         this.usesDst = usesDst;
      }
   }
}

/*
[
{"id":"US/Eastern",    "usesDST":true,  "offset":-5.0,  "name":"Eastern"},
{"id":"US/Central",    "usesDST":true,  "offset":-6.0,  "name":"Central"},
{"id":"US/Mountain",   "usesDST":true,  "offset":-7.0,  "name":"Mountain"},
{"id":"US/Arizona",    "usesDST":false, "offset":-7.0,  "name":"Arizona"},
{"id":"US/Pacific",    "usesDST":true,  "offset":-8.0,  "name":"Pacific"},
{"id":"US/Alaska",     "usesDST":true,  "offset":-9.0,  "name":"Alaska"},
{"id":"US/Aleutian",   "usesDST":true,  "offset":-10.0, "name":"Aleutian"},
{"id":"US/Hawaii",     "usesDST":false, "offset":-10.0, "name":"Hawaii"},
{"id":"US/Samoa",      "usesDST":false, "offset":-11.0, "name":"Samoa"},
{"id":"Pacific/Palau", "usesDST":false, "offset":9.0,   "name":"Palau"}
]
*/

