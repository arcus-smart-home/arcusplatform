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
import java.util.TimeZone;
/*
 * Supports the calculation of Sunrise and Sunset times given a geographical location.
 *
 * http://williams.best.vwh.net/sunrise_sunset_algorithm.htm
 * zenith:  Sun's zenith for sunrise/sunset
 *    offical      = 90 degrees 50'
 *    civil        = 96 degrees
 *    nautical     = 102 degrees
 *    astronomical = 108 degrees
 */
public interface SunriseSunsetCalc {
   
   public enum ZENITH{OFFICAL,CIVIL,NAUTICAL,ASTRONOMICAL};
   
   /**
    * @param forDay The Day and TimeZone you are requesting the SunriseSunsetCalculation for
    * @param location The Latititude/Longitiude you are requesting the times for
    * @param zenith The position in sky you 
    * @return The sunrise and sunset info
    */
   SunriseSunsetInfo calculateSunriseSunset(Calendar forDay, GeoLocation location, ZENITH zenith);
   
   /**
    * Will use the OFFICIAL ZENITH
    * @return
    */
   SunriseSunsetInfo calculateSunriseSunset(Calendar forDay, GeoLocation location);
}

