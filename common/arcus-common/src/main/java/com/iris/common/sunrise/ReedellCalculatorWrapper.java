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
package com.iris.common.sunrise;

import java.util.Calendar;

import com.google.common.base.Preconditions;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

public class ReedellCalculatorWrapper implements com.iris.common.sunrise.SunriseSunsetCalc{
   private static final ZENITH DEFAULT_ZENITH=ZENITH.OFFICAL;

   @Override
   public SunriseSunsetInfo calculateSunriseSunset(Calendar forDate, GeoLocation location, ZENITH zenith) {
      Preconditions.checkNotNull(forDate, "date may not be null");
      Preconditions.checkNotNull(location, "latitude may not be null");
      Preconditions.checkNotNull(zenith, "zenith may not be null");
      
      SunriseSunsetCalculator calc = new SunriseSunsetCalculator(new Location(location.getLatitude(), location.getLongitude()), forDate.getTimeZone());
      Calendar sunsetCalendar = calcSunsetDateForZenith(calc, forDate, zenith);
      Calendar sunriseCalendar= calcSunriseDateForZenith(calc, forDate, zenith);
      SunriseSunsetInfo results = SunriseSunsetInfo.safeCreate(forDate, sunriseCalendar, sunsetCalendar);
      
      return results;
   }

   @Override
   public SunriseSunsetInfo calculateSunriseSunset(Calendar forDay, GeoLocation location) {
      return calculateSunriseSunset(forDay,location,DEFAULT_ZENITH);
   }
   
   private Calendar calcSunriseDateForZenith(SunriseSunsetCalculator calc,Calendar date,ZENITH zenith){
      switch(zenith){
         case OFFICAL:
            return calc.getOfficialSunriseCalendarForDate(date);
         case CIVIL:
            return calc.getCivilSunriseCalendarForDate(date);
         case NAUTICAL:
            return calc.getNauticalSunriseCalendarForDate(date);
         case ASTRONOMICAL:
            return calc.getAstronomicalSunriseCalendarForDate(date);
         default:
            throw new IllegalArgumentException("unrecognized zenith " + zenith);
      }
   }
   private Calendar calcSunsetDateForZenith(SunriseSunsetCalculator calc,Calendar date,ZENITH zenith){
      switch(zenith){
         case OFFICAL:
            return calc.getOfficialSunsetCalendarForDate(date);
         case CIVIL:
            return calc.getCivilSunsetCalendarForDate(date);
         case NAUTICAL:
            return calc.getNauticalSunsetCalendarForDate(date);
         case ASTRONOMICAL:
            return calc.getAstronomicalSunsetCalendarForDate(date);
         default:
            throw new IllegalArgumentException("unrecognized zenith " + zenith);
      }
   }

}

