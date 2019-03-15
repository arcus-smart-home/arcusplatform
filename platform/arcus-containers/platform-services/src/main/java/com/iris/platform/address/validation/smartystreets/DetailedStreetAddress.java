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
package com.iris.platform.address.validation.smartystreets;

import static com.google.common.base.Strings.emptyToNull;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.iris.platform.address.StreetAddress;

public class DetailedStreetAddress extends StreetAddress
{
   private String zipPlus4;
   private String country;
   private String addrType;
   private String addrZipType;
   private String addrRDI;
   private String addrCountyFIPS;
   private Double addrLatitude;
   private Double addrLongitude;
   private String addrGeoPrecision;

   public String getZipPlus4()
   {
      return zipPlus4;
   }

   public void setZipPlus4(String zipPlus4)
   {
      this.zipPlus4 = zipPlus4;
   }

   public String getCountry()
   {
      return country;
   }

   public void setCountry(String country)
   {
      this.country = country;
   }

   public String getAddrType()
   {
      return addrType;
   }

   public void setAddrType(String addrType)
   {
      this.addrType = addrType;
   }

   public String getAddrZipType()
   {
      return addrZipType;
   }

   public void setAddrZipType(String addrZipType)
   {
      this.addrZipType = addrZipType;
   }

   public String getAddrRDI()
   {
      return addrRDI;
   }

   public void setAddrRDI(String addrRDI)
   {
      this.addrRDI = addrRDI;
   }

   public String getAddrCountyFIPS()
   {
      return addrCountyFIPS;
   }

   public void setAddrCountyFIPS(String addrCountyFIPS)
   {
      this.addrCountyFIPS = addrCountyFIPS;
   }

   public Double getAddrLatitude()
   {
      return addrLatitude;
   }

   public void setAddrLatitude(Double addrLatitude)
   {
      this.addrLatitude = addrLatitude;
   }

   public Double getAddrLongitude()
   {
      return addrLongitude;
   }

   public void setAddrLongitude(Double addrLongitude)
   {
      this.addrLongitude = addrLongitude;
   }

   public String getAddrGeoPrecision()
   {
      return addrGeoPrecision;
   }

   public void setAddrGeoPrecision(String addrGeoPrecision)
   {
      this.addrGeoPrecision = addrGeoPrecision;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null) return false;
      if (this == obj) return true;
      if (getClass() != obj.getClass()) return false;

      DetailedStreetAddress other = (DetailedStreetAddress) obj;

      return new EqualsBuilder()
         .appendSuper(super.equals(obj))
         .append(zipPlus4,         other.zipPlus4)
         .append(country,          other.country)
         .append(addrType,         other.addrType)
         .append(addrZipType,      other.addrZipType)
         .append(addrRDI,          other.addrRDI)
         .append(addrCountyFIPS,   other.addrCountyFIPS)
         .append(addrLatitude,     other.addrLatitude)
         .append(addrLongitude,    other.addrLongitude)
         .append(addrGeoPrecision, other.addrGeoPrecision)
         .isEquals();
   }

   @Override
   public int hashCode()
   {
      return new HashCodeBuilder()
         .appendSuper(super.hashCode())
         .append(zipPlus4)
         .append(country)
         .append(addrType)
         .append(addrZipType)
         .append(addrRDI)
         .append(addrCountyFIPS)
         .append(addrLatitude)
         .append(addrLongitude)
         .append(addrGeoPrecision)
         .toHashCode();
   }

   @Override
   public String toString()
   {
      return new ToStringBuilder(this)
         .appendSuper(super.toString())
         .append("zipPlus4",         zipPlus4)
         .append("country",          country)
         .append("addrType",         addrType)
         .append("addrZipType",      addrZipType)
         .append("addrRDI",          addrRDI)
         .append("addrCountyFIPS",   addrCountyFIPS)
         .append("addrLatitude",     addrLatitude)
         .append("addrLongitude",    addrLongitude)
         .append("addrGeoPrecision", addrGeoPrecision)
         .toString();
   }
}

