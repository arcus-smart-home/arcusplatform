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

import java.util.TimeZone;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.iris.common.sunrise.GeoLocation;
import com.iris.messages.model.Place;

public class PlaceLocation
{
   private String      code;
   private GeoLocation geoLocation;
   private String      geoPrecision;
   private String      primaryCity;
   private String      state;
   private String      county;
   private TimeZone    timeZone;

   public PlaceLocation() { }

   public PlaceLocation(String code, GeoLocation geoLocation, String primaryCity, String state, String county,
      TimeZone timeZone)
   {
      this.code         = code;
      this.geoLocation  = geoLocation;
      this.geoPrecision = Place.GEOPRECISION_ZIP5;
      this.primaryCity  = primaryCity;
      this.state        = state;
      this.county       = county;
      this.timeZone     = timeZone;
   }

   public String getCode()
   {
      return code;
   }

   public void setCode(String code)
   {
      this.code = code;
   }

   public GeoLocation getGeoLocation()
   {
      return geoLocation;
   }

   public void setGeoLocation(GeoLocation location)
   {
      this.geoLocation = location;
   }

   public String getPrimaryCity()
   {
      return primaryCity;
   }

   public void setPrimaryCity(String primaryCity)
   {
      this.primaryCity = primaryCity;
   }

   public String getState()
   {
      return state;
   }

   public void setState(String state)
   {
      this.state = state;
   }

   public String getCounty()
   {
      return county;
   }

   public void setCounty(String county)
   {
      this.county = county;
   }

   public TimeZone getTimeZone()
   {
      return timeZone;
   }

   public void setTimeZone(TimeZone timeZone)
   {
      this.timeZone = timeZone;
   }

   public String getGeoPrecision()
   {
      return geoPrecision;
   }

   public void setGeoPrecision(String geoPrecision)
   {
      this.geoPrecision = geoPrecision;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null) return false;
      if (this == obj) return true;
      if (getClass() != obj.getClass()) return false;

      PlaceLocation other = (PlaceLocation) obj;

      return new EqualsBuilder()
         .append(code,         other.code)
         .append(geoLocation,  other.geoLocation)
         .append(geoPrecision, other.geoPrecision)
         .append(primaryCity,  other.primaryCity)
         .append(state,        other.state)
         .append(county,       other.county)
         .append(timeZone,     other.timeZone)
         .isEquals();
   }

   @Override
   public int hashCode()
   {
      return new HashCodeBuilder()
         .append(code)
         .append(geoLocation)
         .append(geoPrecision)
         .append(primaryCity)
         .append(state)
         .append(county)
         .append(timeZone)
         .toHashCode();
   }

   @Override
   public String toString()
   {
      return new ToStringBuilder(this)
         .append("code",         code)
         .append("geoLocation",  geoLocation)
         .append("geoPrecision", geoPrecision)
         .append("primaryCity",  primaryCity)
         .append("state",        state)
         .append("county",       county)
         .append("timeZone",     timeZone)
         .toString();
   }
}

