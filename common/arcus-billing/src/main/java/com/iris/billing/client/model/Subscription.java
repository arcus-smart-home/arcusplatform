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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.iris.messages.model.Copyable;
import com.iris.messages.model.ServiceLevel;
import com.iris.util.IrisCollections;

public class Subscription extends RecurlyModel implements Copyable<Subscription> {
   public static class Tags {
      private Tags() {}
      public static final String URL_RESOURCE = "/subscriptions";

      public static final String TAG_NAME = "subscription";

      public static final String PLAN = "plan";
      public static final String UUID = "uuid";
      public static final String STATE = "state";
      public static final String UNIT_AMOUNT_IN_CENTS = "unit_amount_in_cents";
      public static final String QUANTITY = "quantity";
      public static final String CURRENCY = "currency";
      public static final String ACTIVATED_AT = "activated_at";
      public static final String CANCELED_AT = "canceled_at";
      public static final String EXPIRES_AT = "expires_at";
      public static final String CURRENT_PERIOD_STARTED_AT = "current_period_started_at";
      public static final String CURRENT_PERIOD_ENDS_AT = "current_period_ends_at";
      public static final String TRIAL_STARTED_AT = "trial_started_at";
      public static final String   TRIAL_ENDS_AT = "trial_ends_at";
      public static final String TAX_IN_CENTS = "tax_in_cents";
      public static final String TAX_TYPE = "tax_type";
      public static final String TAX_REGION = "tax_region";
      public static final String TAX_RATE = "tax_rate";
      public static final String PO_NUMBER = "po_number";
      public static final String NET_TERMS = "net_terms";
      public static final String COLLECTION_METHOD = "collection_method";
      public static final String SUBSCRIPTION_ADD_ONS = "subscription_add_ons";
      public static final String PENDING_SUBSCRIPTION = "pending_subscription";
      public static final String TERMS_AND_CONDITIONS = "terms_and_conditions";
      public static final String CUSTOMER_NOTES = "customer_notes";
      public static final String BULK = "bulk";
      public static final String TIMEFRAME = "timeframe";

      public static final String A_NAME_CANCEL = "cancel";
      public static final String A_NAME_TERMINATE = "terminate";
      public static final String A_NAME_POSTPONE = "postpone";
   }

   private String subscriptionID; //uuid
   private String state;
   private int quantity;
   private String currency;
   private Date activatedAt;
   private Date canceledAt;
   private Date expiresAt;
   private Date currentPeriodStartedAt;
   private Date currentPeriodEndsAt;
   private Date trialStartedAt;
   private Date trialEndsAt;
   private String taxInCents;
   private String taxType;
   private String taxRegion;
   private String taxRate;
   private String poNumber;
   private String netTerms;
   private String collectionMethod;
   private String termsAndConditions;
   private String customerNotes;

   // To make adding/updating subscriptions easier
   private String planCode;

   // TODO: Support Customer Notes?
   // TODO: Support manual collection methods?
   // TODO: Support custom trial end dates?
   // TODO: Support these at creation/addition time?
   //   private Date startsAt;
   //   private Date firstRenewalDate;
   //   private Integer totalBillingCycles;
   //   private String vatReverseChargeNotes;

   private Subscription pendingSubscription; // Creates a sub instance of this?
   private Plan planDetails; // plan
   private List<SubscriptionAddon> subscriptionAddOns;
   private String unitAmountInCents;

   private Map<String, Object> mappings = new HashMap<>();

   public Subscription() {
      subscriptionAddOns = new ArrayList<SubscriptionAddon>();
   }

   @Override
   public Map<String, Object> getXMLMappings() {
      if (!mappings.isEmpty() && currency == null) { // Set default currency
         setCurrency(RecurlyCurrencyFormats.getDefaultCurrency());
      }

      return Collections.unmodifiableMap(mappings);
   }

   @Override
   public String getTagName() {
      return Tags.TAG_NAME;
   }
   
   @Override
   public RecurlyModels<?> createContainer() {
      Subscriptions subscriptions = new Subscriptions();
      subscriptions.add(this);
      return subscriptions;
   }

   public final String getSubscriptionID() {
      return subscriptionID;
   }

   public final void setSubscriptionID(String subscriptionID) {
      this.subscriptionID = subscriptionID;
   }

   public final String getState() {
      return state;
   }

   public final void setState(String state) {
      this.state = state;
   }

   public final int getQuantity() {
      return quantity;
   }

   public final void setQuantity(String quantity) {
      this.quantity = StringUtils.isEmpty(quantity) ? 0 : Integer.valueOf(quantity);
      mappings.put(Tags.QUANTITY, quantity);
   }

   public final String getCurrency() {
      return currency;
   }

   public final void setCurrency(String currency) {
      if (!RecurlyCurrencyFormats.isValidCurrency(currency)) {
         throw new IllegalArgumentException("Currency not supported. Found: " + currency);
      }

      this.currency = currency;
      mappings.put(Tags.CURRENCY, currency);
   }

   public final Date getActivatedAt() {
      return activatedAt;
   }

   public final void setActivatedAt(Date activatedAt) {
      this.activatedAt = activatedAt;
   }

   public final Date getCanceledAt() {
      return canceledAt;
   }

   public final void setCanceledAt(Date canceledAt) {
      this.canceledAt = canceledAt;
   }

   public final Date getExpiresAt() {
      return expiresAt;
   }

   public final void setExpiresAt(Date expiresAt) {
      this.expiresAt = expiresAt;
   }

   public final Date getCurrentPeriodStartedAt() {
      return currentPeriodStartedAt;
   }

   public final void setCurrentPeriodStartedAt(Date currentPeriodStartedAt) {
      this.currentPeriodStartedAt = currentPeriodStartedAt;
   }

   public final Date getCurrentPeriodEndsAt() {
      return currentPeriodEndsAt;
   }

   public final void setCurrentPeriodEndsAt(Date currentPeriodEndsAt) {
      this.currentPeriodEndsAt = currentPeriodEndsAt;
   }

   public final Date getTrialStartedAt() {
      return trialStartedAt;
   }

   public final void setTrialStartedAt(Date trialStartedAt) {
      this.trialStartedAt = trialStartedAt;
   }

   public final Date getTrialEndsAt() {
      return trialEndsAt;
   }

   public final void setTrialEndsAt(Date trialEndsAt) {
      this.trialEndsAt = trialEndsAt;
   }

   public final String getTaxInCents() {
      return taxInCents;
   }

   public final void setTaxInCents(String taxInCents) {
      this.taxInCents = taxInCents;
   }

   public final String getTaxType() {
      return taxType;
   }

   public final void setTaxType(String taxRype) {
      this.taxType = taxRype;
   }

   public final String getTaxRegion() {
      return taxRegion;
   }

   public final void setTaxRegion(String taxRegion) {
      this.taxRegion = taxRegion;
   }

   public final String getTaxRate() {
      return taxRate;
   }

   public final void setTaxRate(String taxRate) {
      this.taxRate = taxRate;
   }

   public final String getPoNumber() {
      return poNumber;
   }

   public final void setPoNumber(String poNumber) {
      this.poNumber = poNumber;
      mappings.put(Tags.PO_NUMBER, poNumber);
   }

   public final String getNetTerms() {
      return netTerms;
   }

   public final void setNetTerms(String netTerms) {
      this.netTerms = netTerms;
   }

   public final String getCollectionMethod() {
      return collectionMethod;
   }

   public final void setCollectionMethod(String collectionMethod) {
      this.collectionMethod = collectionMethod;
      // Note: null values indicate "automatic" collection methods.
   }

   public final String getTermsAndConditions() {
      return termsAndConditions;
   }

   public final void setTermsAndConditions(String termsAndConditions) {
      this.termsAndConditions = termsAndConditions;
      mappings.put(Tags.TERMS_AND_CONDITIONS, termsAndConditions);
   }

   public final String getCustomerNotes() {
      return customerNotes;
   }

   public final void setCustomerNotes(String customerNotes) {
      this.customerNotes = customerNotes;
   }

   public final Subscription getPendingSubscription() {
      return pendingSubscription;
   }

   public final void setPendingSubscription(Subscription pendingSubscription) {
      this.pendingSubscription = pendingSubscription;
   }

   public final Plan getPlanDetails() {
      return planDetails;
   }

   public final void setPlanDetails(Plan planDetails) {
      this.planDetails = planDetails;
      if (planDetails != null) {
         setPlanCode(planDetails.getPlanCode());
      }
   }

   public final String getPlanCode() {
      return planCode;
   }

   public final void setPlanCode(String planCode) {
      this.planCode = planCode;
      mappings.put(Plan.Tags.PLAN_CODE, planCode);
   }

   public final List<SubscriptionAddon> getSubscriptionAddOns() {
      return subscriptionAddOns;
   }

   public final void clearSubscriptionAddOns() {
      this.subscriptionAddOns.clear();
      mappings.put(Tags.SUBSCRIPTION_ADD_ONS, this.subscriptionAddOns);
   }

   public final void setSubscriptionAddOns(List<SubscriptionAddon> addonList) {
      if (addonList != null && !addonList.isEmpty()) {
         this.subscriptionAddOns.addAll(addonList);
         mappings.put(Tags.SUBSCRIPTION_ADD_ONS, this.subscriptionAddOns);
      }
   }

   public final void addSubscriptionAddon(SubscriptionAddon addon) {
      this.subscriptionAddOns.add(addon);
      mappings.put(Tags.SUBSCRIPTION_ADD_ONS, this.subscriptionAddOns);
   }

   public final String getUnitAmountInCents() {
      return unitAmountInCents;
   }

   public final void setUnitAmountInCents(String unitAmountInCents) {
      this.unitAmountInCents = unitAmountInCents;
   }

   // Helper Methods
   public ServiceLevel getServiceLevel() {
      if (planCode != null) {
         return ServiceLevel.fromString(planCode);
      }
      if (planDetails != null) {
         return planDetails.getPlanCode() != null ? ServiceLevel.fromString(planDetails.getPlanCode()) : null;
      }
      return null;
   }

   public boolean isExpired() {
      return state != null && state.contains(Constants.STATE_EXPIRED);
   }

   public boolean isCanceled() {
      return state != null && state.contains(Constants.STATE_CANCELED);
   }

   public boolean isActive() {
      return state != null && state.contains(Constants.STATE_ACTIVE);
   }

   @Override
   public Subscription copy() {
      try {
         return (Subscription)clone();
      } catch (CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      Subscription clone = new Subscription();
      clone.setSubscriptionID(subscriptionID);
      clone.setState(state);
      clone.setQuantity(String.valueOf(quantity));
      clone.setCurrency(currency);
      clone.setActivatedAt(activatedAt != null ? (Date)activatedAt.clone() : null);
      clone.setCanceledAt(canceledAt != null ? (Date)canceledAt.clone() : null);
      clone.setExpiresAt(expiresAt != null ? (Date)expiresAt.clone() : null);
      clone.setCurrentPeriodStartedAt(currentPeriodStartedAt != null ? (Date)currentPeriodStartedAt.clone(): null);
      clone.setCurrentPeriodEndsAt(currentPeriodEndsAt != null ? (Date)currentPeriodEndsAt.clone() : null);
      clone.setTrialStartedAt(trialStartedAt != null ? (Date)trialStartedAt.clone() : null);
      clone.setTrialEndsAt(trialEndsAt != null ? (Date)trialEndsAt.clone() : null);
      clone.setTaxInCents(taxInCents);
      clone.setTaxType(taxType);
      clone.setTaxRegion(taxRegion);
      clone.setTaxRate(taxRate);
      clone.setPoNumber(poNumber);
      clone.setNetTerms(netTerms);
      clone.setCollectionMethod(collectionMethod);
      clone.setTermsAndConditions(termsAndConditions);
      clone.setCustomerNotes(customerNotes);
      clone.setPlanCode(planCode);
      clone.setPendingSubscription(pendingSubscription != null ? pendingSubscription.copy() : null);
      clone.setPlanDetails(planDetails != null ? planDetails.copy() : null);
      clone.setSubscriptionAddOns(IrisCollections.deepCopyList(subscriptionAddOns));
      clone.setUnitAmountInCents(unitAmountInCents);
      return clone;
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("Subscription [subscriptionID=")
      .append(subscriptionID).append(", planCode=").append(planCode).append(", state=").append(state)
      .append(", quantity=").append(quantity).append(", currency=")
      .append(currency).append(", activatedAt=").append(activatedAt)
      .append(", canceledAt=").append(canceledAt).append(", expiresAt=")
      .append(expiresAt).append(", currentPeriodStartedAt=")
      .append(currentPeriodStartedAt).append(", currentPeriodEndsAt=")
      .append(currentPeriodEndsAt).append(", trialStartedAt=")
      .append(trialStartedAt).append(", trialEndsAt=")
      .append(trialEndsAt).append(", taxInCents=").append(taxInCents)
      .append(", taxType=").append(taxType).append(", taxRegion=")
      .append(taxRegion).append(", taxRate=").append(taxRate)
      .append(", poNumber=").append(poNumber).append(", netTerms=")
      .append(netTerms).append(", collectionMethod=")
      .append(collectionMethod).append(", termsAndConditions=")
      .append(termsAndConditions).append(", customerNotes=")
      .append(customerNotes).append(", planDetails=")
      .append(planDetails).append(", subscriptionAddOns=")
      .append(subscriptionAddOns).append(", unitAmountInCents=")
      .append(unitAmountInCents).append(", pendingSubscription=")
      .append(pendingSubscription).append("]");
      return builder.toString();
   }
}

