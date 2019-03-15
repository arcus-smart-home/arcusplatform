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
package com.iris.platform.location;

import static com.iris.messages.capability.PlaceCapability.ATTR_TZID;
import static com.iris.messages.capability.PlaceCapability.ATTR_TZNAME;
import static com.iris.messages.capability.PlaceCapability.ATTR_TZOFFSET;
import static com.iris.messages.capability.PlaceCapability.ATTR_TZUSESDST;
import static com.iris.util.TimeZones.getOffsetAsHours;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.util.TimeZones;

public class TimezonesChangeHelper
{
   private static final Logger logger = getLogger(TimezonesChangeHelper.class);
   private final TimezonesManager timezonesMgr;
   
   public TimezonesChangeHelper(TimezonesManager timezonesMgr) {
      this.timezonesMgr = timezonesMgr;
   }
   
   public void processTimeZoneChanges(Map<String, Object> changes) {
      if (changes != null && changes.containsKey(ATTR_TZID)){
         String timeZoneId = (String) changes.get(ATTR_TZID);

         TimeZone tz = TimeZones.getTimeZoneById(timeZoneId).orNull();

         if (tz == null)
         {
            throw new ErrorEventException(Errors.invalidParam(ATTR_TZID));
         }
         
         com.iris.messages.type.TimeZone irisTz = timezonesMgr.getTimeZoneById(timeZoneId);
         String displayName = null;
         if(irisTz != null && StringUtils.isNotBlank(irisTz.getName())) {
            displayName = irisTz.getName();
         }else {
            displayName = tz.getID();
         }

         logger.debug("User request to update timezone to [{} ({})]", timeZoneId, displayName);
         changes.put(ATTR_TZID, tz.getID());
         changes.put(ATTR_TZNAME, displayName);
         changes.put(ATTR_TZOFFSET, getOffsetAsHours(tz.getRawOffset()));
         changes.put(ATTR_TZUSESDST, tz.observesDaylightTime());
      }
   }
}

