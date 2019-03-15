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

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isAnyEmpty;
import static org.apache.commons.lang3.StringUtils.trim;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.iris.common.sunrise.GeoLocation;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Place;
import com.iris.resource.manager.BaseCsvParser;
import com.iris.util.TimeZones;

public class ZipCodesCsvParser extends BaseCsvParser<Map<String, PlaceLocation>>
{
   @Override
   protected Map<String, PlaceLocation> newResult()
   {
      // Initialized to prevent resizing, with default load factor (0.75), estimated max of ~46,000 elements
      return new HashMap<String, PlaceLocation>(61_400);
   }

   @Override
   protected int getExpectedLength()
   {
      return 7;
   }

   @Override
   protected void processLine(String[] nextLine, Map<String, PlaceLocation> result, int lineNumber)
   {
      String zipCode =     trim(nextLine[0]);
      String latitude =    trim(nextLine[1]);
      String longitude =   trim(nextLine[2]);
      String primaryCity = trim(nextLine[3]);
      String state =       trim(nextLine[4]);
      String county =      trim(nextLine[5]);
      String timeZoneId =  trim(nextLine[6]);

      if (result.containsKey(zipCode))
      {
         throw new IllegalStateException(
            format("Duplicate zipcode in record {zipcode:[%s] latitude:[%s] longitude:[%s] tzid:[%s]} on line %d",
               zipCode, latitude, longitude, timeZoneId, lineNumber));
      }

      if (isAnyEmpty(zipCode, latitude, longitude, timeZoneId))
      {
         throw new IllegalStateException(
            format("Empty field(s) in record {zipcode:[%s] latitude:[%s] longitude:[%s] tzid:[%s]} on line %d",
               zipCode, latitude, longitude, timeZoneId, lineNumber));
      }

      Optional<TimeZone> timeZone = TimeZones.getTimeZoneById(timeZoneId);

      if (!timeZone.isPresent())
      {
         throw new IllegalStateException(
            format("Unknown tzid in record {zipcode:[%s] latitude:[%s] longitude:[%s] tzid:[%s]} on line %d",
               zipCode, latitude, longitude, timeZoneId, lineNumber));
      }

      PlaceLocation placeLocation = new PlaceLocation(zipCode, GeoLocation.fromCoordinates(latitude, longitude),
         primaryCity, state, county, timeZone.get());
      placeLocation.setGeoPrecision(Place.GEOPRECISION_ZIP5);

      result.put(zipCode, placeLocation);
   }

   @Override
   protected Map<String, PlaceLocation> finalizeResult(Map<String, PlaceLocation> result)
   {
      return ImmutableMap.copyOf(result);
   }
}

