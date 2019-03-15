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

// Not sure if this is needed, but this could be the start if/when we need it.
public class Coupon extends RecurlyModel {
   public static class Tags {
      private Tags() {}
      private static final String TAG_NAME = "coupon";
   }

   //   couponCode;
   //   name;
   //   hostedDescription;
   //   invoiceDescription;
   //   redeemByDate;
   //   singleUse;
   //   appliesForMonths;
   //   maxRedemptions;
   //   appliesToAllPlans;
   //   discountType;
   //   discountPercent;
   //   discountInCents;
   //   planCodes;

   @Override
   public String getTagName() {
      return Tags.TAG_NAME;
   }

   @Override
   public RecurlyModels<?> createContainer() {
      // This is no container model for Coupon.
      return null;
   }
}

