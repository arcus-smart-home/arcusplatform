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

import com.google.common.base.Preconditions;

public class GeoLocation {
   
   private final double latitude;
   private final double longitude;
   
   public static GeoLocation fromCoordinates(double latitude,double longitude){
      return new GeoLocation(latitude, longitude);
   }
   public static GeoLocation fromCoordinates(String latitude,String longitude){
      Preconditions.checkNotNull(latitude);
      Preconditions.checkNotNull(longitude);

      double lat=Double.parseDouble(latitude);
      double longi=Double.parseDouble(longitude);
      return new GeoLocation(lat, longi);
   }
   
   public GeoLocation(double latitude, double longitude){
      this.latitude=latitude;
      this.longitude=longitude;
   }

   public double getLatitude() {
      return latitude;
   }

   public double getLongitude() {
      return longitude;
   }
   
   @Override
   public String toString() {
      return "GeoLocation [latitude=" + latitude + ", longitude=" + longitude + "]";
   }
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      long temp;
      temp = Double.doubleToLongBits(latitude);
      result = prime * result + (int) (temp ^ (temp >>> 32));
      temp = Double.doubleToLongBits(longitude);
      result = prime * result + (int) (temp ^ (temp >>> 32));
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
      GeoLocation other = (GeoLocation) obj;
      if (Double.doubleToLongBits(latitude) != Double.doubleToLongBits(other.latitude))
         return false;
      if (Double.doubleToLongBits(longitude) != Double.doubleToLongBits(other.longitude))
         return false;
      return true;
   }
   
}

