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

public class BillingInfo extends RecurlyModel implements Copyable<BillingInfo> {
   public static class Tags {
      private Tags() {}
      public static final String URL_RESOURCE = "/billing_info";

      public static final String TAG_NAME = "billing_info";
      public static final String FIRST_NAME = "first_name";
      public static final String LAST_NAME = "last_name";
      public static final String ADDRESS_ONE = "address1";
      public static final String ADDRESS_TWO = "address2";
      public static final String CITY = "city";
      public static final String STATE = "state";
      public static final String ZIP_CODE = "zip";
      public static final String COUNTRY = "country";
      public static final String PHONE_NUMBER = "phone";
      public static final String VAT_NUMBER = "vat_number";
      public static final String IP_ADDRESS = "ip_address";
      public static final String CC_EXP_YEAR = "year";
      public static final String CC_EXP_MONTH = "month";
      public static final String PAYPAL_BILLING_AGREEMENT_ID = "paypal_billing_agreement_id";
      public static final String AMAZON_BILLING_AGREEMENT_ID = "amazon_billing_agreement_id";
      public static final String COMPANY_NAME = "company_name";
      public static final String TOKEN_ID = "token_id";

      public static final String IP_ADDRESS_COUNTRY = "ip_address_country";
      public static final String CARD_TYPE = "card_type";
      public static final String LAST_FOUR = "last_four";
      public static final String FIRST_SIX = "first_six";
   }

   // Create/Update/"GET" Parameters
   private String firstName;  // Required
   private String lastName;   // Required
   private String address1;
   private String address2;
   private String city;
   private String state;
   private String country;
   private String zip;
   private String phone;
   private String vatNumber;
   private String ipAddress;
   private String year;
   private String month;
   private String paypalBillingAgreementId;
   private String amazonBillingAgreementId;

   private String tokenID;

   // "GET" Only
   private String ipAddressCountry;
   private String cardType;
   private String firstSix;
   private String lastFour;
   private String companyName; // Is returned if set on the Account it seems

   private Map<String, Object> mappings = new HashMap<String, Object>();

   @Override
   public Map<String, Object> getXMLMappings() {
      return Collections.<String, Object>unmodifiableMap(mappings);
   }

   @Override
   public String getTagName() {
      return Tags.TAG_NAME;
   }
   
   @Override
   public RecurlyModels<?> createContainer() {
      // There is no container model for BillingInfo
      return null;
   }

   public final String getTokenID() {
      return tokenID;
   }

   public final void setTokenID(String tokenID) {
      this.tokenID = tokenID;
      mappings.put(Tags.TOKEN_ID, tokenID);
   }

   public final String getFirstName() {
      return firstName;
   }

   public final void setFirstName(String firstName) {
      this.firstName = firstName;
      mappings.put(Tags.FIRST_NAME, firstName);
   }

   public final String getLastName() {
      return lastName;
   }

   public final void setLastName(String lastName) {
      this.lastName = lastName;
      mappings.put(Tags.LAST_NAME, lastName);
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

   public final String getCountry() {
      return country;
   }

   public final void setCountry(String country) {
      this.country = country;
   }

   public final String getZip() {
      return zip;
   }

   public final void setZip(String zip) {
      this.zip = zip;
      mappings.put(Tags.ZIP_CODE, zip);
   }

   public final String getPhone() {
      return phone;
   }

   public final void setPhone(String phone) {
      this.phone = phone;
   }

   public final String getVatNumber() {
      return vatNumber;
   }

   public final void setVatNumber(String vatNumber) {
      this.vatNumber = vatNumber;
      mappings.put(Tags.VAT_NUMBER, vatNumber);
   }

   public final String getIpAddress() {
      return ipAddress;
   }

   public final void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
      mappings.put(Tags.IP_ADDRESS, ipAddress);
   }

   public final String getYear() {
      return year;
   }

   public final void setYear(String year) {
      this.year = year;
   }

   public final String getMonth() {
      return month;
   }

   public final void setMonth(String month) {
      this.month = month;
   }

   public final String getPaypalBillingAgreementId() {
      return paypalBillingAgreementId;
   }

   public final void setPaypalBillingAgreementId(String paypalBillingAgreementId) {
      this.paypalBillingAgreementId = paypalBillingAgreementId;
   }

   public final String getAmazonBillingAgreementId() {
      return amazonBillingAgreementId;
   }

   public final void setAmazonBillingAgreementId(String amazonBillingAgreementId) {
      this.amazonBillingAgreementId = amazonBillingAgreementId;
   }

   public final String getIpAddressCountry() {
      return ipAddressCountry;
   }

   public final void setIpAddressCountry(String ipAddressCountry) {
      this.ipAddressCountry = ipAddressCountry;
   }

   public final String getCardType() {
      return cardType;
   }

   public final void setCardType(String cardType) {
      this.cardType = cardType;
   }

   public final String getFirstSix() {
      return firstSix;
   }

   public final void setFirstSix(String firstSix) {
      this.firstSix = firstSix;
   }

   public final String getLastFour() {
      return lastFour;
   }

   public final void setLastFour(String lastFour) {
      this.lastFour = lastFour;
   }

   public final String getCompanyName() {
      return companyName;
   }

   public final void setCompanyName(String companyName) {
      this.companyName = companyName;
   }

   @Override
   public BillingInfo copy() {
      try {
         return (BillingInfo)clone();
      } catch (CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      BillingInfo clone = new BillingInfo();
      clone.setFirstName(firstName);
      clone.setLastName(lastName);
      clone.setAddress1(address1);
      clone.setAddress2(address2);
      clone.setCity(city);
      clone.setState(state);
      clone.setCountry(country);
      clone.setZip(zip);
      clone.setPhone(phone);
      clone.setVatNumber(vatNumber);
      clone.setIpAddress(ipAddress);
      clone.setYear(year);
      clone.setMonth(month);
      clone.setPaypalBillingAgreementId(paypalBillingAgreementId);
      clone.setAmazonBillingAgreementId(amazonBillingAgreementId);
      clone.setTokenID(tokenID);
      
      clone.setIpAddressCountry(ipAddressCountry);
      clone.setCardType(cardType);
      clone.setFirstSix(firstSix);
      clone.setLastFour(lastFour);
      clone.setCompanyName(companyName);
      return clone;
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("BillingInfo [firstName=").append(firstName).append(", lastName=")
      .append(lastName).append(", address1=").append(address1)
      .append(", address2=").append(address2).append(", city=")
      .append(city).append(", state=").append(state).append(", country=")
      .append(country).append(", zip=").append(zip).append(", phone=")
      .append(phone).append(", vatNumber=").append(vatNumber)
      .append(", ipAddress=").append(ipAddress).append(", year=")
      .append(year).append(", month=").append(month)
      .append(", paypalBillingAgreementId=")
      .append(paypalBillingAgreementId)
      .append(", amazonBillingAgreementId=")
      .append(amazonBillingAgreementId).append(", ipAddressCountry=")
      .append(ipAddressCountry).append(", cardType=").append(cardType)
      .append(", lastFour=").append(lastFour)
      .append(", companyName=").append(companyName).append("]");
      return builder.toString();
   }
}

