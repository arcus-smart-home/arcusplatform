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

import java.util.Map;

import com.iris.messages.model.Copyable;

public class TaxDetail extends RecurlyModel implements Copyable<TaxDetail> {
   public static class Tags {
      private Tags() {}
      public static final String TAG_NAME = "tax_detail";

      public static final String NAME = "name";
      public static final String TAX_TYPE = "type";
      public static final String TAX_RATE = "tax_rate";
      public static final String TAX_IN_CENTS = "tax_in_cents";

   }

   private String name;
   private String taxType;
   private String taxRate;
   private String taxInCents;

   public final String getName() {
      return name;
   }

   public final void setName(String name) {
      this.name = name;
   }

   public final String getTaxType() {
      return taxType;
   }

   public final void setTaxType(String taxType) {
      this.taxType = taxType;
   }

   public final String getTaxRate() {
      return taxRate;
   }

   public final void setTaxRate(String taxRate) {
      this.taxRate = taxRate;
   }

   public final String getTaxInCents() {
      return taxInCents;
   }

   public final void setTaxInCents(String taxInCents) {
      this.taxInCents = taxInCents;
   }

   @Override
   public Map<String, Object> getXMLMappings() {
      return null;
   }
   
   @Override
   public TaxDetail copy() {
      try {
         return (TaxDetail)clone();
      } catch (CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      TaxDetail clone = new TaxDetail();
      clone.setName(name);
      clone.setTaxType(taxType);
      clone.setTaxRate(taxRate);
      clone.setTaxInCents(taxInCents);
      return clone;
   }

   @Override
   public String toString() {
      return "TaxDetail [name=" + name + ", taxType=" + taxType
            + ", taxRate=" + taxRate + ", taxInCents=" + taxInCents + "]";
   }

   @Override
   public String getTagName() {
      return Tags.TAG_NAME;
   }

   @Override
   public RecurlyModels<?> createContainer() {
      TaxDetails taxDetails = new TaxDetails();
      taxDetails.add(this);
      return taxDetails;
   }
   
}

