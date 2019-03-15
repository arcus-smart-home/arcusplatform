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

import org.apache.commons.lang3.builder.ToStringBuilder;

public class AccountBalance extends RecurlyModel {
   public static class Tags {
      private Tags() {}
      public static final String URL_RESOURCE = "/balance";

      public static final String TAG_NAME = "account_balance";
      
      public static final String PAST_DUE = "past_due";

      // Nested tags
      public static final String BALANCE_IN_CENTS = "balance_in_cents";
   }

   private Boolean pastDue;
   private CostInCents balanceInCents;

   public final Boolean getPastDue() {
      return pastDue;
   }

   public final CostInCents getBalanceInCents() {
      return balanceInCents;
   }

   public final void setPastDue(Boolean pastDue) {
      this.pastDue = pastDue;
   }

   public final void setBalanceInCents(CostInCents balanceInCents) {
      this.balanceInCents = balanceInCents;
   }

   @Override
   public String getTagName() {
      return Tags.TAG_NAME;
   }
   
   @Override
   public RecurlyModels<?> createContainer() {
      return null;
   }

   @Override
   public String toString() {
      return new ToStringBuilder(this)
         .append("pastDue",        pastDue)
         .append("balanceInCents", balanceInCents)
         .toString();
   }
}

