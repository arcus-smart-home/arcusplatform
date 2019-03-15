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
package com.iris.util;

import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

/**
 * 
 */
public class TimeZones {
   private static final Set<String> timeZoneIds = ImmutableSet.copyOf(TimeZone.getAvailableIDs());;

   public static double getOffsetAsHours(int timezoneOffsetMs) {
      return timezoneOffsetMs / (double) TimeUnit.HOURS.toMillis(1);
   }
   
   public static int getOffsetAsMillis(double timezoneOffsetHours) {
      return (int) (Math.round(timezoneOffsetHours * 10) * 100 /* seconds */ * 60 /* minutes */ * 60 /* hours */);
   }
   
   public static Optional<TimeZone> getTimeZoneById(String timeZoneId) {
      if(timeZoneIds.contains(timeZoneId)) {
         return Optional.of( TimeZone.getTimeZone(timeZoneId) );
      }
      else {
         return Optional.absent();
      }
   }

   public static TimeZone guessTimezone(String timeZoneName, Double offsetHours, Boolean useDst) throws IllegalArgumentException {
      if(offsetHours == null || useDst == null) {
         Optional<TimeZone> tz = getTimeZoneById("US/" + timeZoneName);
         if(tz.isPresent()) {
            return tz.get();
         }
         else {
            throw new IllegalArgumentException("Time zone [" + timeZoneName + "] is not a valid name and no offset / dst is specified");
         }
      }
      else {
         int offsetMs = getOffsetAsMillis(offsetHours);
         TimeZone bestMatch = null;
         for(String id: TimeZone.getAvailableIDs(offsetMs)) {
            TimeZone tz = TimeZone.getTimeZone(id);
            if(tz.useDaylightTime() != useDst) {
               continue;
            }
            
            if(bestMatch == null) {
               bestMatch = tz;
               continue;
            }
            
            if(tz.getID().startsWith("US/") && !bestMatch.getID().startsWith("US/")) {
               bestMatch = tz;
               continue;
            }
            
            if(tz.getID().contains(timeZoneName) && !bestMatch.getID().contains(timeZoneName)) {
               bestMatch = tz;
               continue;
            }
            
            if(tz.getID().startsWith("Etc/") && !bestMatch.getID().startsWith("Etc/")) {
               bestMatch = tz;
               continue;
            }
         }
         if(bestMatch != null) {
            return bestMatch;
         }
      }
      throw new IllegalArgumentException("Unable to find a matching timezone for the requested offset / DST.");
   }

}

