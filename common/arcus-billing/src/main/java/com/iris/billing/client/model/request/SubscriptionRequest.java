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
package com.iris.billing.client.model.request;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import com.iris.billing.client.model.Plan;
import com.iris.billing.client.model.RecurlyModel;
import com.iris.billing.client.model.RecurlyModels;
import com.iris.billing.client.model.Subscription;
import com.iris.billing.client.model.SubscriptionAddon;
import com.iris.billing.client.model.SubscriptionAddons;

public class SubscriptionRequest extends RecurlyModel {
   // totalBillingCycles;
   // firstRenewalDate;
   // vatReverseChargeNotes;
   // collectionMethod;
   // couponCode;
   // unitAmountInCents
   // trialEndsAt;
   // startsAt;

   private String subscriptionID;

   private String planCode;
   private SubscriptionAddons subscriptionAddons = new SubscriptionAddons();
   private String currency;
   private Integer quantity;
   private Boolean isBulk;
   private String customerNotes;
   private Date trialEndsAt;
   private SubscriptionTimeframe timeframe;
   private Integer unitAmountInCents;

   public enum SubscriptionTimeframe {
      NOW,
      RENEWAL
   }

   private Map<String, Object> mappings = new HashMap<String, Object>();

   /**
    * This constructor makes sure there always is a subscription_add_ons tag as without it the addons
    * will not be deleted from recurly, per there API.
    */
   public SubscriptionRequest(){
      mappings.put(Subscription.Tags.SUBSCRIPTION_ADD_ONS, subscriptionAddons);
   }

   public final String getPlanCode() {
      return planCode;
   }
   public final void setPlanCode(String planCode) {
      this.planCode = planCode;
      mappings.put(Plan.Tags.PLAN_CODE, planCode);
   }
   public final String getSubscriptionID() {
      return subscriptionID;
   }
   /**
    * Required if updating a subscription.
    *
    * @param subscriptionID
    */
   public final void setSubscriptionID(String subscriptionID) {
      this.subscriptionID = subscriptionID;
   }
   public final SubscriptionAddons getSubscriptionAddons() {
      return subscriptionAddons;
   }
   public final void setSubscriptionAddons(SubscriptionAddons subscriptionAddons) {
      if(subscriptionAddons == null) {
         subscriptionAddons = new SubscriptionAddons();
      }
      this.subscriptionAddons = subscriptionAddons;
      mappings.put(Subscription.Tags.SUBSCRIPTION_ADD_ONS, subscriptionAddons);
   }
   public final void addSubscriptionAddon(SubscriptionAddon subscriptionAddon) {
      this.subscriptionAddons.add(subscriptionAddon);
      mappings.put(Subscription.Tags.SUBSCRIPTION_ADD_ONS, subscriptionAddons);
   }
   public final String getCurrency() {
      return currency;
   }
   public final void setCurrency(String currency) {
      this.currency = currency;
      mappings.put(Subscription.Tags.CURRENCY, currency);
   }
   public final Integer getQuantity() {
      return quantity;
   }
   public final void setQuantity(Integer quantity) {
      this.quantity = quantity;
      mappings.put(Subscription.Tags.QUANTITY, quantity);
   }
   public final Integer getUnitAmountInCents() {
      return this.unitAmountInCents;
   }
   public final void setUnitAmountInCents(Integer amountInCents) {
      this.unitAmountInCents = amountInCents;
      mappings.put(Subscription.Tags.UNIT_AMOUNT_IN_CENTS, this.unitAmountInCents);
   }
   public final Boolean getIsBulk() {
      return isBulk;
   }
   public final void setIsBulk(Boolean isBulk) {
      this.isBulk = isBulk;
      mappings.put("bulk", isBulk);
   }
   public final String getCustomerNotes() {
      return customerNotes;
   }
   public final void setCustomerNotes(String customerNotes) {
      this.customerNotes = customerNotes;
      mappings.put(Subscription.Tags.CUSTOMER_NOTES, customerNotes);
   }
   public final SubscriptionTimeframe getTimeframe() {
      return this.timeframe;
   }
   public final void setTimeframe(SubscriptionTimeframe timeframe) {
      this.timeframe = timeframe;
      if (timeframe.equals(SubscriptionTimeframe.RENEWAL)) {
         mappings.put(Subscription.Tags.TIMEFRAME, "renewal");
      } else {
         mappings.put(Subscription.Tags.TIMEFRAME, "now");
      }
   }

   public final Date getTrialEndsAt() {
      return trialEndsAt;
   }

   public final void setTrialEndsAt(Date trialEndsAt) {
      this.trialEndsAt = trialEndsAt;
      if(trialEndsAt != null) {
         mappings.put(Subscription.Tags.TRIAL_ENDS_AT, formatDate(trialEndsAt));
      }
   }

   private String formatDate(Date d) {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-d'T'HH:mm:ss");
      sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
      return sdf.format(d);
   }

   @Override
   public Map<String, Object> getXMLMappings() {
      return Collections.unmodifiableMap(mappings);
   }

   @Override
   public String getTagName() {
      return Subscription.Tags.TAG_NAME;
   }
   
   @Override
   public RecurlyModels<?> createContainer() {
      // There is no container for Subscription Request.
      return null;
   }
   
}

