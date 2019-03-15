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

public class CostInCents extends RecurlyCurrencyFormats implements BaseRecurlyModel, Copyable<CostInCents> {
   private Map<String, String> unitCosts = new HashMap<String, String>();

   public static class Tags {
      public static final String TAG_NAME = "unit_amount_in_cents";
   }
   
   public final void addCost(String currencyFormat, String costInCents) {
      if (getCurrencyFormats().contains(currencyFormat)) {
         unitCosts.put(currencyFormat, costInCents);
      }
   }

   public final String getCostForCurrency(String currencyFormat) {
      if (!getCurrencyFormats().contains(currencyFormat)) {
         throw new IllegalArgumentException("Currency format not supported");
      }

      return unitCosts.get(currencyFormat);
   }

   public final String getCostForUSD() {
      return unitCosts.get("USD");
   }
   
   public final Map<String, String> getAllCosts() {
      return Collections.<String, String>unmodifiableMap(unitCosts);
   }

   @Override
   public Map<String, Object> getXMLMappings() {
      return null;
   }

   @Override
   public String getTagName() {
      return null;
   }

   @Override
   public CostInCents copy() {
      try {
         return (CostInCents)clone();
      } catch (CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      CostInCents clone = new CostInCents();
      clone.unitCosts = new HashMap<>();
      clone.unitCosts.putAll(unitCosts);
      return clone;
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("CostInCents [unitCosts=").append(unitCosts).append("]");
      return builder.toString();
   }
}

