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

public class PlanAddon extends RecurlyModel {
   public static class Tags {
      private Tags() {}
      public static final String URL_RESOURCE = "/add_ons";
      public static final String TAG_NAME = "add_on";

      public static final String COST_IN_CENTS = "unit_amount_in_cents";
      public static final String ADD_ON_CODE = "add_on_code";
      public static final String ADD_ON_NAME = "name";
      public static final String DEFAULT_QUANTITY = "default_quantity";
      public static final String DISPLAY_QTY_ON_HOSTED_PAGE = "display_quantity_on_hosted_page";
      public static final String CREATED_AT = "created_at";

      public static final String QUANTITY = "quantity";
   }

   private String quantity;

   private String addOnCode;
   private String name;
   private CostInCents unitAmountInCents;
   private String defaultQuantity;
   private Boolean displayQuantityOnHostedPage;
   private Date createdAt;

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
      PlanAddons planAddons = new PlanAddons();
      planAddons.add(this);
      return planAddons;
   }

   public final String getQuantity() {
      return quantity;
   }

   public final void setQuantity(String quantity) {
      this.quantity = quantity;
      mappings.put(Tags.QUANTITY, quantity);
   }

   public final String getAddOnCode() {
      return addOnCode;
   }

   public final void setAddOnCode(String addOnCode) {
      this.addOnCode = addOnCode;
      mappings.put(Tags.ADD_ON_CODE, quantity);
   }

   public final String getName() {
      return name;
   }

   public final void setName(String name) {
      this.name = name;
   }

   public final CostInCents getUnitAmountInCents() {
      return unitAmountInCents;
   }

   public final void setUnitAmountInCents(CostInCents unitAmountInCents) {
      this.unitAmountInCents = unitAmountInCents;
   }

   public final String getDefaultQuantity() {
      return defaultQuantity;
   }

   public final void setDefaultQuantity(String defaultQuantity) {
      this.defaultQuantity = defaultQuantity;
   }

   public final Boolean getDisplayQuantityOnHostedPage() {
      return displayQuantityOnHostedPage;
   }

   public final void setDisplayQuantityOnHostedPage(Boolean displayQuantityOnHostedPage) {
      this.displayQuantityOnHostedPage = displayQuantityOnHostedPage;
   }

   public final Date getCreatedAt() {
      return createdAt;
   }

   public final void setCreatedAt(Date createdAt) {
      this.createdAt = createdAt;
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("PlanAddon [addOnCode=").append(addOnCode)
      .append(", name=").append(name)
      .append(", unitAmountInCents=").append(unitAmountInCents)
      .append(", defaultQuantity=").append(defaultQuantity)
      .append(", displayQuantityOnHostedPage=")
      .append(displayQuantityOnHostedPage).append(", createdAt=")
      .append(createdAt).append("]");
      return builder.toString();
   }
}

