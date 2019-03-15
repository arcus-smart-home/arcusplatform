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


public class SunriseSunsetInfo {
   
   private final Calendar sunrise;
   private final Calendar sunset;
   
   public static SunriseSunsetInfo safeCreate(Calendar forDate, Calendar sunrise, Calendar sunset){
      sunrise=cloneAndCopy(forDate, sunrise);
      sunset=cloneAndCopy(forDate, sunset);
      return new SunriseSunsetInfo(sunrise, sunset);
   }
   
   public SunriseSunsetInfo(Calendar sunrise, Calendar sunset){
      this.sunrise=(Calendar)sunrise.clone();
      this.sunset=(Calendar)sunset;
   }
   
   public Calendar getSunset() {
      return sunset;
   }
   
   public Calendar getSunrise() {
      return sunrise;
   }
   
   private static Calendar cloneAndCopy(Calendar base, Calendar calculated){
      Calendar cal = (Calendar)base.clone();
      cal.set(Calendar.MILLISECOND, calculated.get(Calendar.MILLISECOND));
      cal.set(Calendar.SECOND, calculated.get(Calendar.SECOND));
      cal.set(Calendar.MINUTE, calculated.get(Calendar.MINUTE));
      cal.set(Calendar.HOUR_OF_DAY, calculated.get(Calendar.HOUR_OF_DAY));
      cal.set(Calendar.MONTH, calculated.get(Calendar.MONTH));
      cal.set(Calendar.DAY_OF_MONTH, calculated.get(Calendar.DAY_OF_MONTH));
      cal.set(Calendar.YEAR, calculated.get(Calendar.YEAR));
      return cal;
   }
   
   @Override
   public String toString() {
      return "SunriseSunsetResults [sunset=" + sunset + ", sunrise=" + sunrise + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((sunrise == null) ? 0 : sunrise.hashCode());
      result = prime * result + ((sunset == null) ? 0 : sunset.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      SunriseSunsetInfo other = (SunriseSunsetInfo) obj;
      if (sunrise == null) {
         if (other.sunrise != null)
            return false;
      }else if (!sunrise.equals(other.sunrise))
         return false;
      if (sunset == null) {
         if (other.sunset != null)
            return false;
      }else if (!sunset.equals(other.sunset))
         return false;
      return true;
   }
}

