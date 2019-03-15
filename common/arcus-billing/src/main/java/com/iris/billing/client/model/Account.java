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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Strings;
import com.iris.messages.model.Copyable;

public class Account extends RecurlyModel implements Copyable<Account> {
   public static class Tags {
      private Tags() {}

      public static final String URL_RESOURCE = "/accounts";

      public static final String TAG_NAME = "account";
      public static final String TAG_NAME_PLURAL = "accounts";

      public static final String ADJUSTMENT = "adjustments";
      public static final String INVOICE = "invoices";
      public static final String REDEMPTION = "redemption";
      public static final String SUBSCRIPTION = "subscriptions";
      public static final String TRANSACTION = "transactions";
      public static final String REOPEN_ATTRIBUTE = "reopen";

      public static final String BILLING_INFO = "billing_info";
      public static final String ACCOUNT_CODE =   "account_code";
      public static final String USERNAME =   "username";
      public static final String EMAIL =   "email";
      public static final String FIRST_NAME =   "first_name";
      public static final String LAST_NAME =   "last_name";

      public static final String VAT_NUMBER =   "vat_number";
      public static final String TAX_EXEMPT =   "tax_exempt";
      public static final String ACCEPT_LANGUAGE =   "accept_language";
      public static final String HOSTED_LOGIN_TOKEN =   "hosted_login_token";
      public static final String CREATED_AT =   "created_at";
      public static final String VAT_LOCAION_VALID =   "vat_location_valid";
      public static final String STATE =   "state";
      public static final String ENTITY_USE_CODE = "entity_use_code";

      // I've seen listed as both...
      public static final String COMPANY =   "company";
      public static final String COMPANY_NAME =   "company_name";
   }

   private static final String sMAX_CHARS = "Max of 50 characters";

   // Creation Tags (Not sure about return values)
   private String entityUseCode;

   // Creation/"GET" Only Tags
   private String accountID;

   // Creation/Update/"GET" tag names
   private String username;
   private String email;
   private String firstName;
   private String lastName;
   private String companyName;
   private String vatNumber;
   private Boolean taxExempt;
   private String acceptLanguage;

   // Nested Tags, Creation/Update/"GET"
   private BillingInfo billingInfo;
   private Address address;

   // "Get" results only
   private String state;
   private String hostedLoginToken;
   private Date createdAt;
   private Boolean vatLocationValid;

   private Map<String, Object> mappings = new HashMap<>();

   @Override
   public Map<String, Object> getXMLMappings() {
      return Collections.unmodifiableMap(mappings);
   }

   @Override
   public String getTagName() {
      return Tags.TAG_NAME;
   }
   
   @Override
   public RecurlyModels<?> createContainer() {
      Accounts accounts = new Accounts();
      accounts.add(this);
      return accounts;
   }

   public final String getAccountID() {
      return accountID;
   }

   public final void setAccountID(String accountCode) {
      this.accountID = accountCode;
      mappings.put(Tags.ACCOUNT_CODE, accountCode);
   }

   public final String getUsername() {
      return username;
   }

   public final void setUsername(String username) {
      this.username = username;
      mappings.put(Tags.USERNAME, username);
   }

   public final String getEmail() {
      return email;
   }

   public final void setEmail(String email) {
      this.email = email;
      mappings.put(Tags.EMAIL, email);
   }

   public final String getFirstName() {
      return firstName;
   }

   public final void setFirstName(String firstName) {
      if (!Strings.isNullOrEmpty(firstName) && firstName.length() > 50) {
         throw new IllegalArgumentException(sMAX_CHARS);
      }

      this.firstName = firstName;
      mappings.put(Tags.FIRST_NAME, firstName);
   }

   public final String getLastName() {
      return lastName;
   }

   public final void setLastName(String lastName) {
      if (!Strings.isNullOrEmpty(lastName) && lastName.length() > 50) {
         throw new IllegalArgumentException(sMAX_CHARS);
      }

      this.lastName = lastName;
      mappings.put(Tags.LAST_NAME, lastName);
   }

   public final String getCompanyName() {
      return companyName;
   }

   public final void setCompanyName(String companyName) {
      if (!Strings.isNullOrEmpty(companyName) && companyName.length() > 50) {
         throw new IllegalArgumentException(sMAX_CHARS);
      }

      this.companyName = companyName;
      mappings.put(Tags.COMPANY_NAME, companyName);
   }

   public final String getVatNumber() {
      return vatNumber;
   }

   public final void setVatNumber(String vatNumber) {
      this.vatNumber = vatNumber;
      mappings.put(Tags.VAT_NUMBER, vatNumber);
   }

   public final Boolean isTaxExempt() {
      return taxExempt;
   }

   public final String getAcceptLanguage() {
      return acceptLanguage;
   }

   public final void setAcceptLanguage(String acceptLanguage) {
      this.acceptLanguage = acceptLanguage;
      mappings.put(Tags.ACCEPT_LANGUAGE, acceptLanguage);
   }

   public final BillingInfo getBillingInfo() {
      return billingInfo;
   }

   public final void setBillingInfo(BillingInfo billingInfo) {
      this.billingInfo = billingInfo;
      mappings.put(Tags.BILLING_INFO, billingInfo);
   }

   public final Address getAddress() {
      return address;
   }

   public final void setAddress(Address address) {
      this.address = address;
      mappings.put(Address.Tags.TAG_NAME, address);
   }

   public final String getState() {
      return state;
   }

   public final void setState(String state) {
      this.state = state;
   }

   public final String getHostedLoginToken() {
      return hostedLoginToken;
   }

   public final void setHostedLoginToken(String hostedLoginToken) {
      this.hostedLoginToken = hostedLoginToken;
   }

   public final Date getCreatedAt() {
      return createdAt;
   }

   public final void setCreatedAt(Date createdAt) {
      this.createdAt = createdAt;
   }

   public final boolean isVatLocationValid() {
      return vatLocationValid;
   }

   public final void setVatLocationValid(Boolean vatLocationValid) {
      this.vatLocationValid = vatLocationValid;
   }

   public String getEntityUseCode() {
      return this.entityUseCode;
   }

   public final Boolean getTaxExempt() {
      return taxExempt;
   }

   public final void setTaxExempt(Boolean taxExempt) {
      this.taxExempt = taxExempt;
      if (taxExempt != null && taxExempt) {
         mappings.put(Tags.TAX_EXEMPT, String.valueOf(taxExempt));
      }
   }

   public final void setEntityUseCode(String entityUseCode) {
      this.entityUseCode = entityUseCode;
      mappings.put(Tags.ENTITY_USE_CODE, this.entityUseCode);
   }

   @Override
   public Account copy() {
      try {
         return (Account)clone();
      } catch (CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      Account clone = new Account();
      clone.setEntityUseCode(entityUseCode);
      clone.setAccountID(accountID);
      clone.setUsername(username);
      clone.setEmail(email);
      clone.setFirstName(firstName);
      clone.setLastName(lastName);
      clone.setCompanyName(companyName);
      clone.setVatNumber(vatNumber);
      clone.setTaxExempt(taxExempt);
      clone.setAcceptLanguage(acceptLanguage);
      clone.setBillingInfo(billingInfo != null ? billingInfo.copy() : null);
      clone.setAddress(address != null ? address.copy() : null);
      clone.setState(state);
      clone.setHostedLoginToken(hostedLoginToken);
      clone.setCreatedAt(createdAt != null ? (Date)createdAt.clone() : null);
      clone.setVatLocationValid(vatLocationValid);
      return clone;
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("Account [accountCode=").append(accountID).append(", username=")
      .append(username).append(", email=").append(email)
      .append(", firstName=").append(firstName).append(", lastName=")
      .append(lastName).append(", companyName=").append(companyName)
      .append(", vatNumber=").append(vatNumber).append(", taxExempt=")
      .append(taxExempt).append(", acceptLanguage=")
      .append(acceptLanguage).append(", billingInfo=")
      .append(billingInfo).append(", state=").append(state)
      .append(", hostedLoginToken=").append(hostedLoginToken)
      .append(", createdAt=").append(createdAt)
      .append(", vatLocationValid=").append(vatLocationValid)
      .append(", address=").append(address).append("]");
      return builder.toString();
   }
}

