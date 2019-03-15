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

public class SubscriptionAddon extends RecurlyModel implements Copyable<SubscriptionAddon>{
   public static class Tags {
      private Tags() {}
      public static final String URL_RESOURCE = "/addons";

      public static final String TAG_NAME = "subscription_add_on";

      public static final String ADD_ON_CODE = "add_on_code";
      public static final String UNIT_AMOUNT_IN_CENTS = "unit_amount_in_cents";
      public static final String QUANTITY = "quantity";
   }

   private String addonCode;
   private String unitAmountInCents;
   private Integer quantity;

   private Map<String, Object> mappings = new HashMap<String, Object>();

   @Override
   public Map<String, Object> getXMLMappings() {
      return Collections.unmodifiableMap(mappings);
   }
   
   @Override
   public RecurlyModels<?> createContainer() {
      SubscriptionAddons subscriptionAddons = new SubscriptionAddons();
      subscriptionAddons.add(this);
      return subscriptionAddons;
   }


   public final String getAddonCode() {
      return addonCode;
   }

   public final void setAddonCode(String addonCode) {
      this.addonCode = addonCode;
      mappings.put(Tags.ADD_ON_CODE, addonCode);
   }

   public final String getUnitAmountInCents() {
      return unitAmountInCents;
   }

   public final void setUnitAmountInCents(String unitAmountInCents) {
      this.unitAmountInCents = unitAmountInCents;
   }

   public final Integer getQuantity() {
      return quantity;
   }

   public final void setQuantity(Integer quantity) {
      this.quantity = quantity;
      mappings.put(Tags.QUANTITY, quantity);
   }

   @Override
   public String getTagName() {
      return Tags.TAG_NAME;
   }

   @Override
   public SubscriptionAddon copy() {
      try {
         return (SubscriptionAddon)clone();
      } catch (CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      SubscriptionAddon clone = new SubscriptionAddon();
      clone.setAddonCode(addonCode);
      clone.setUnitAmountInCents(unitAmountInCents);
      clone.setQuantity(quantity);
      return clone;
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("SubscriptionAddon [addonCode=").append(addonCode)
      .append(", unitAmountInCents=").append(unitAmountInCents)
      .append(", quantity=").append(quantity).append("]");
      return builder.toString();
   }
   
   public static SubscriptionAddon buildAddonWithQuantity(String addonCode, Integer quantity) {
      SubscriptionAddon addon = new SubscriptionAddon();
      addon.setAddonCode(addonCode);
      addon.setQuantity(quantity);
      return addon;
   }
}

