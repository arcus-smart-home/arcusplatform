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
package com.iris.platform.address;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.iris.util.IrisComparator;
import org.apache.commons.lang3.StringUtils;

import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Copyable;
import com.iris.messages.model.Place;

public class StreetAddress implements Cloneable, Copyable<StreetAddress> {
   private String line1;
   private String line2;
   private String city;
   private String state;
   private String zip;
   
   /**
    * @return the line1
    */
   public String getLine1() {
      return line1;
   }
   
   /**
    * @param line1 the line1 to set
    */
   public void setLine1(String line1) {
      this.line1 = line1;
   }
   
   /**
    * @return the line2
    */
   public String getLine2() {
      return line2;
   }
   
   /**
    * @param line2 the line2 to set
    */
   public void setLine2(String line2) {
      this.line2 = line2;
   }
   
   /**
    * @return the city
    */
   public String getCity() {
      return city;
   }
   
   /**
    * @param city the city to set
    */
   public void setCity(String city) {
      this.city = city;
   }
   
   /**
    * @return the state
    */
   public String getState() {
      return state;
   }
   
   /**
    * @param state the state to set
    */
   public void setState(String state) {
      this.state = state;
   }
   
   /**
    * @return the zip
    */
   public String getZip() {
      return zip;
   }
   
   /**
    * @param zip the zip to set
    */
   public void setZip(String zip) {
      this.zip = zip;
   }
   
   @Override
   public StreetAddress copy() {
      try {
         return (StreetAddress) super.clone();
      }
      catch(CloneNotSupportedException e) {
         throw new UnsupportedOperationException(e);
      }
   }
   
   @Override
   public String toString() {
      return "Address [line1=" + line1 + ", line2=" + line2 + ", city=" + city
            + ", state=" + state + ", zip=" + zip + "]";
   }

   public Map<String, Object> toTypeAttributes() {
      Map<String,Object> attrs = new HashMap<>();
      attrs.put(com.iris.messages.type.StreetAddress.ATTR_LINE1, getLine1());
      attrs.put(com.iris.messages.type.StreetAddress.ATTR_LINE2, getLine2());
      attrs.put(com.iris.messages.type.StreetAddress.ATTR_CITY, getCity());
      attrs.put(com.iris.messages.type.StreetAddress.ATTR_ZIP, getZip());
      attrs.put(com.iris.messages.type.StreetAddress.ATTR_STATE, getState());
      return attrs;
   }

   public Map<String,Object> toPlaceAttributes() {
      String zip = getZip();
      String zipPlus4 = zip;
      if(zip != null && zip.length() > 5) {
         zip = zip.substring(0, 5);
      } else {
         zipPlus4 = null;
      }
      Map<String, Object> result = new HashMap<String, Object>(6);
      if(getLine1() != null){
         result.put(PlaceCapability.ATTR_STREETADDRESS1, getLine1());
      }
      if(getLine2() != null) {
         result.put(PlaceCapability.ATTR_STREETADDRESS2, getLine2());
      }
      if(getCity() != null) {
         result.put(PlaceCapability.ATTR_CITY, getCity());
      }
      if(getState() != null) {
         result.put(PlaceCapability.ATTR_STATE, getState());
         result.put(PlaceCapability.ATTR_STATEPROV, getState());
      }
      if(zip != null) {
         result.put(PlaceCapability.ATTR_ZIPCODE, zip);
      }
      if(zipPlus4 != null) {
         result.put(PlaceCapability.ATTR_ZIPPLUS4, zipPlus4);
      }
      return result;
   }

   /** copy values from addr into this instance if this instance has no value and returns a copy */
   public StreetAddress merge(StreetAddress addr) {
      StreetAddress copy = this.copy();
      if(copy.zip == null) { copy.zip = addr.zip; }
      if(copy.city == null) { copy.city = addr.city; }
      if(copy.state == null) { copy.state = addr.state; }
      if(copy.line1 == null) { copy.line1 = addr.line1; }
      if(copy.line2 == null) { copy.line2 = addr.line2; }
      return copy;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((city == null) ? 0 : city.hashCode());
      result = prime * result + ((line1 == null) ? 0 : line1.hashCode());
      result = prime * result + ((line2 == null) ? 0 : line2.hashCode());
      result = prime * result + ((state == null) ? 0 : state.hashCode());
      result = prime * result + ((zip == null) ? 0 : zip.hashCode());
      return result;
   }
   
   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      StreetAddress other = (StreetAddress) obj;
      if (city == null) {
         if (other.city != null) return false;
      }
      else if (!city.equals(other.city)) return false;
      if (line1 == null) {
         if (other.line1 != null) return false;
      }
      else if (!line1.equals(other.line1)) return false;
      if (StringUtils.isBlank(line2)) {
         if (StringUtils.isNotBlank(other.line2)) return false;
      }
      else if (!line2.equals(other.line2)) return false;
      if (state == null) {
         if (other.state != null) return false;
      }
      else if (!state.equals(other.state)) return false;
      if (zip == null) {
         if (other.zip != null) return false;
      }
      else if (!zip.equals(other.zip)) return false;
      return true;
   }
   
	public static StreetAddress fromMap(Map<String, Object> streetAddressData) {
		StreetAddress addr = new StreetAddress();
		if(streetAddressData != null) {
         addr.setLine1((String) streetAddressData.get(com.iris.messages.type.StreetAddress.ATTR_LINE1));
         addr.setLine2((String) streetAddressData.get(com.iris.messages.type.StreetAddress.ATTR_LINE2));
         addr.setCity((String) streetAddressData.get(com.iris.messages.type.StreetAddress.ATTR_CITY));
         addr.setZip((String) streetAddressData.get(com.iris.messages.type.StreetAddress.ATTR_ZIP));
         addr.setState((String) streetAddressData.get(com.iris.messages.type.StreetAddress.ATTR_STATE));
      }
		return addr;
	}

	public static StreetAddress fromPlace(Place place) {
      StreetAddress address = new StreetAddress();
      address.setLine1(place.getStreetAddress1());
      address.setLine2(place.getStreetAddress2());
      address.setCity(place.getCity());
      address.setState(place.getState());
      if(StringUtils.isBlank(address.getState())) {
         address.setState(place.getStateProv());
      }
      address.setZip(StringUtils.isBlank(place.getZipPlus4())? place.getZipCode() : place.getZipPlus4());
      return address;
   }

   public static StreetAddress fromPlaceAttributes(Map<String, Object> placeAttrs) {
      StreetAddress address = new StreetAddress();
      if(placeAttrs != null) {
         address.setLine1((String) placeAttrs.get(PlaceCapability.ATTR_STREETADDRESS1));
         address.setLine2((String) placeAttrs.get(PlaceCapability.ATTR_STREETADDRESS2));
         address.setCity((String) placeAttrs.get(PlaceCapability.ATTR_CITY));
         address.setState((String) placeAttrs.get(PlaceCapability.ATTR_STATE));
         if(StringUtils.isBlank(address.getState())) {
            address.setState((String) placeAttrs.get(PlaceCapability.ATTR_STATEPROV));
         }
         String zip4 = (String) placeAttrs.get(PlaceCapability.ATTR_ZIPPLUS4);
         String zip = (String) placeAttrs.get(PlaceCapability.ATTR_ZIPCODE);
         address.setZip(StringUtils.isBlank(zip4) ? zip : zip4);
      }
      return address;
   }

   public static ZipBuilder zipBuilder() { return new ZipBuilder(); }
   public static class ZipBuilder {
      private static final Pattern ZIP_CODE_USA_PATTERN = Pattern.compile("^(\\d{5})(-\\d{4})?$");

      protected ZipBuilder() {}

      private String zip5;
      private String zipPlus4;

      public ZipBuilder withZip(String zip) {
         this.zip5 = zip;
         if (zip == null || zip.length() == 5) return this;

         splitUsaZipIfNeeded(zip);
         return this;
      }

      private void splitUsaZipIfNeeded(String zip) {
         Matcher matcher = ZIP_CODE_USA_PATTERN.matcher(zip);
         if (matcher.find())
         {
            this.zip5 = matcher.group(1);
            if (matcher.group(2) != null)
            {
               this.zipPlus4 = matcher.group(2).substring(1);
            }
         }
      }

      public ZipBuilder withZipPlus4(String zipPlus4) {
         this.zipPlus4 = zipPlus4;
         return this;
      }

      public String build() {
         if(StringUtils.isEmpty(zip5)) {
            return "";
         }
         if(StringUtils.isEmpty(zipPlus4)) {
            return zip5;
         }

         return zip5 + "-" + zipPlus4;
      }
   }
   
   public static Builder builder() {
      return new Builder();
   }
   public static class Builder {
      private String streetNumber;
      private String streetPreDirection;
      private String streetName;
      private String streetSuffix;
      private String streetPostDirection;
      private String suite;
      private String suiteName;
      private String suiteNumber;
      private String city;
      private String state;
      private String address2;

      private ZipBuilder zipBuilder = StreetAddress.zipBuilder();

      protected Builder() {
         
      }
      
      /**
       * @param streetNumber the streetNumber to set
       */
      public Builder withStreetNumber(String streetNumber) {
         this.streetNumber = streetNumber;
         return this;
      }

      /**
       * @param streetPreDirection the streetPreDirection to set
       */
      public Builder withStreetPreDirection(String streetPreDirection) {
         this.streetPreDirection = streetPreDirection;
         return this;
      }

      /**
       * @param streetName the streetName to set
       */
      public Builder withStreetName(String streetName) {
         this.streetName = streetName;
         return this;
      }

      /**
       * @param streetSuffix the streetSuffix to set
       */
      public Builder withStreetSuffix(String streetSuffix) {
         this.streetSuffix = streetSuffix;
         return this;
      }

      /**
       * @param streetPostDirection the streetPostDirection to set
       */
      public Builder withStreetPostDirection(String streetPostDirection) {
         this.streetPostDirection = streetPostDirection;
         return this;
      }

      /**
       * @param suite the suite to set
       */
      public Builder withSuite(String suite) {
         this.suite = suite;
         return this;
      }

      /**
       * @param suiteName the suiteName to set
       */
      public Builder withSuiteName(String suiteName) {
         this.suiteName = suiteName;
         return this;
      }

      /**
       * @param suiteNumber the suiteNumber to set
       */
      public Builder withSuiteNumber(String suiteNumber) {
         this.suiteNumber = suiteNumber;
         return this;
      }

      /**
       * @param city the city to set
       */
      public Builder withCity(String city) {
         this.city = city;
         return this;
      }

      /**
       * @param state the state to set
       */
      public Builder withState(String state) {
         this.state = state;
         return this;
      }

      /**
       * @param zip the zip to set
       */
      public Builder withZip(String zip) {
         zipBuilder.withZip(zip);
         return this;
      }

      /**
       * @param zipPlus4 the zipPlus4 to set
       */
      public Builder withZipPlus4(String zipPlus4) {
         zipBuilder.withZipPlus4(zipPlus4);
         return this;
      }

      public Builder withAddress2(String address2) {
         this.address2 = address2;
         return this;

      }

      public StreetAddress build() {
         StreetAddress address = new StreetAddress();
         address.setCity(city);
         address.setState(state);
         address.setLine1(buildAddressLine1());
         address.setLine2(buildAddressLine2());
         address.setZip(buildZip());
         return address;
      }
      
      private String buildAddressLine1() {
         StringBuilder sb = new StringBuilder();
         appendIf(streetNumber, sb);
         appendIf(streetPreDirection, sb);
         appendIf(streetName, sb);
         appendIf(streetSuffix, sb);
         appendIf(streetPostDirection, sb);   
         return sb.toString();
      }

      private String buildAddressLine2() {
         StringBuilder sb = new StringBuilder();
         appendIf(address2, sb);
         appendIf(suite, sb);
         appendIf(suiteName, sb);
         // TODO add a pound sign here?
         appendIf(suiteNumber, sb);
         return sb.toString();
      }
      
      private String buildZip() {
         return zipBuilder.build();
      }

      private void appendIf(String value, StringBuilder sb) {
         if(StringUtils.isEmpty(value)) {
            return;
         }
         if(sb.length() > 0) {
            sb.append(" ");
         }
         sb.append(value);
      }
      
   }

}

