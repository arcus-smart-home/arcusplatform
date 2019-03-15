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
package com.iris.oculus.modules.place;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.iris.client.capability.Place;

public class PlaceInfo {
   private String accountId;
   private String population;
   private String name;
   private String state;
   private String serviceLevel;
   private Set<String> serviceAddons;
   private String address1;
   private String address2;
   private String city;
   private String stateProv;
   private String zipCode;
   private String zipPlus4;
   private String country;
   private String county;
   private String countyFips;
   private String tzName;
   private int tzOffset;
   private boolean tzUsesDST;
   private boolean addrValidated;
   private String addrType;
   private String zipType;
   private double latitude;
   private double longitude;
   private String geoPrecision;
   private String rdi;


   public String getAccountId() {
      return accountId;
   }
   public void setAccountId(String accountId) {
      this.accountId = accountId;
   }
   public String getPopulation() {
      return population;
   }
   public void setPopulation(String populationId) {
      this.population = populationId;
   }
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }
   public String getState() {
      return state;
   }
   public void setState(String state) {
      this.state = state;
   }
   public String getAddress1() {
      return address1;
   }
   public void setAddress1(String address1) {
      this.address1 = address1;
   }
   public String getAddress2() {
      return address2;
   }
   public void setAddress2(String address2) {
      this.address2 = address2;
   }
   public String getCity() {
      return city;
   }
   public void setCity(String city) {
      this.city = city;
   }
   public String getStateProv() {
      return stateProv;
   }
   public void setStateProv(String stateProv) {
      this.stateProv = stateProv;
   }
   public String getZipCode() {
      return zipCode;
   }
   public void setZipCode(String zipCode) {
      this.zipCode = zipCode;
   }
   public String getServiceLevel() {
      return serviceLevel;
   }
   public void setServiceLevel(String serviceLevel) {
      this.serviceLevel = serviceLevel;
   }
   public Set<String> getServiceAddons() {
      return serviceAddons;
   }
   public void setServiceAddons(Set<String> serviceAddons) {
      this.serviceAddons = serviceAddons;
   }
   public String getZipPlus4() {
      return zipPlus4;
   }
   public void setZipPlus4(String zipPlus4) {
      this.zipPlus4 = zipPlus4;
   }
   public String getCountry() {
      return country;
   }
   public void setCountry(String country) {
      this.country = country;
   }
   public String getCounty() {
      return county;
   }
   public void setCounty(String county) {
      this.county = county;
   }
   public String getCountyFips() {
      return countyFips;
   }
   public void setCountyFips(String countyFips) {
      this.countyFips = countyFips;
   }
   public String getTzName() {
      return tzName;
   }
   public void setTzName(String tzName) {
      this.tzName = tzName;
   }
   public int getTzOffset() {
      return tzOffset;
   }
   public void setTzOffset(int tzOffset) {
      this.tzOffset = tzOffset;
   }
   public boolean isTzUsesDST() {
      return tzUsesDST;
   }
   public void setTzUsesDST(boolean tzUsesDST) {
      this.tzUsesDST = tzUsesDST;
   }
   public boolean isAddrValidated() {
      return addrValidated;
   }
   public void setAddrValidated(boolean addrValidated) {
      this.addrValidated = addrValidated;
   }
   public String getAddrType() {
      return addrType;
   }
   public void setAddrType(String addrType) {
      this.addrType = addrType;
   }
   public String getZipType() {
      return zipType;
   }
   public void setZipType(String zipType) {
      this.zipType = zipType;
   }
   public double getLatitude() {
      return latitude;
   }
   public void setLatitude(double latitude) {
      this.latitude = latitude;
   }
   public double getLongitude() {
      return longitude;
   }
   public void setLongitude(double longitude) {
      this.longitude = longitude;
   }
   public String getGeoPrecision() {
      return geoPrecision;
   }
   public void setGeoPrecision(String geoPrecision) {
      this.geoPrecision = geoPrecision;
   }
   public String getRdi() {
      return rdi;
   }
   public void setRdi(String rdi) {
      this.rdi = rdi;
   }

   public Map<String,Object> asAttributes() {
      Map<String,Object> map = new LinkedHashMap<String,Object>();
      setIf(map, Place.ATTR_NAME, getName());
      setIf(map, Place.ATTR_STATE, getState());
      setIf(map, Place.ATTR_STREETADDRESS1, getAddress1());
      setIf(map, Place.ATTR_STREETADDRESS2, getAddress2());
      setIf(map, Place.ATTR_CITY, getCity());
      setIf(map, Place.ATTR_STATEPROV, getStateProv());
      setIf(map, Place.ATTR_ZIPCODE, getZipCode());
      setIf(map, Place.ATTR_ZIPPLUS4, getZipPlus4());
      setIf(map, Place.ATTR_TZNAME, getTzName());
      setIf(map, Place.ATTR_TZOFFSET, getTzOffset());
      setIf(map, Place.ATTR_TZUSESDST, isTzUsesDST());
      setIf(map, Place.ATTR_COUNTRY, getCountry());
      setIf(map, Place.ATTR_ADDRVALIDATED, isAddrValidated());
      setIf(map, Place.ATTR_ADDRTYPE, getAddrType());
      setIf(map, Place.ATTR_ADDRZIPTYPE, getZipType());
      setIf(map, Place.ATTR_ADDRLATITUDE, getLatitude());
      setIf(map, Place.ATTR_ADDRLONGITUDE, getLongitude());
      setIf(map, Place.ATTR_ADDRGEOPRECISION, getGeoPrecision());
      setIf(map, Place.ATTR_ADDRRDI, getRdi());
      setIf(map, Place.ATTR_ADDRCOUNTY, getCounty());
      setIf(map, Place.ATTR_ADDRCOUNTYFIPS, getCountyFips());
      return map;
   }

   private void setIf(Map<String,Object> map, String key, Object value) {
      if(value != null) {
         map.put(key, value);
      }
   }
}

