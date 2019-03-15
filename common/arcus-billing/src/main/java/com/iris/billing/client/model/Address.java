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
package com.iris.billing.client.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.iris.messages.model.Copyable;

public class Address extends RecurlyModel implements Copyable<Address> {
   public static class Tags {
      private Tags() {}
      public static final String TAG_NAME = "address";

      public static final String ADDRESS_ONE = "address1";
      public static final String ADDRESS_TWO = "address2";
      public static final String CITY = "city";
      public static final String STATE = "state";
      public static final String ZIP_CODE = "zip";
      public static final String COUNTRY = "country";
      public static final String PHONE_NUMBER = "phone";
   }

   private String address1;
   private String address2;
   private String city;
   private String state;
   private String zip;
   private String country;
   private String phoneNumber;

   private Map<String, String> mappings = new HashMap<>();

   @Override
   public String getTagName() {
      return Tags.TAG_NAME;
   }
   
   @Override
   public RecurlyModels<?> createContainer() {
      // There is no address container.
      return null;
   }

   @Override
   public Map<String, Object> getXMLMappings() {
      return Collections.<String, Object>unmodifiableMap(mappings);
   }

   public final String getAddress1() {
      return address1;
   }

   public final void setAddress1(String address1) {
      this.address1 = address1;
      mappings.put(Tags.ADDRESS_ONE, address1);
   }

   public final String getAddress2() {
      return address2;
   }

   public final void setAddress2(String address2) {
      this.address2 = address2;
      mappings.put(Tags.ADDRESS_TWO, address2);
   }

   public final String getCity() {
      return city;
   }

   public final void setCity(String city) {
      this.city = city;
      mappings.put(Tags.CITY, city);
   }

   public final String getState() {
      return state;
   }

   public final void setState(String state) {
      this.state = state;
      mappings.put(Tags.STATE, state);
   }

   public final String getZip() {
      return zip;
   }

   public final void setZip(String zip) {
      this.zip = zip;
      mappings.put(Tags.ZIP_CODE, zip);
   }

   public final String getCountry() {
      return country;
   }

   public final void setCountry(String country) {
      this.country = country;
      mappings.put(Tags.COUNTRY, country);
   }

   public final String getPhoneNumber() {
      return phoneNumber;
   }

   public final void setPhoneNumber(String phoneNumber) {
      this.phoneNumber = phoneNumber;
      mappings.put(Tags.PHONE_NUMBER, phoneNumber);
   }

   @Override
   public Address copy() {
      try {
         return (Address)clone();
      } catch (CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      Address clone = new Address();
      clone.setAddress1(address1);
      clone.setAddress2(address2);
      clone.setCity(city);
      clone.setState(state);
      clone.setZip(zip);
      clone.setCountry(country);
      clone.setPhoneNumber(phoneNumber);
      return clone;
   }

   @Override
   public String toString() {
      return "Address [address1=" + address1 + ", address2=" + address2
            + ", city=" + city + ", state=" + state + ", zip=" + zip
            + ", country=" + country + ", phoneNumber=" + phoneNumber + "]";
   }
}

