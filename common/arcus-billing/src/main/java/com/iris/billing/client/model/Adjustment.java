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

import java.util.Date;
import java.util.List;

import com.iris.messages.model.Copyable;
import com.iris.util.IrisCollections;

public class Adjustment extends RecurlyModel implements Copyable<Adjustment> {
   public static class Tags {
      private Tags() {}
      public static final String URL_RESOURCE = "/adjustments";

      public static final String TAG_NAME = "adjustment";
      public static final String TAG_NAME_PLURAL = "adjustments";

      public static final String ID = "uuid";
      public static final String ORIGINAL_ID = "original_adjustment_uuid";
      public static final String STATE = "state";
      public static final String DESCRIPTION = "description";
      public static final String ACCOUNTING_CODE = "accounting_code";
      public static final String PRODUCT_CODE = "product_code";
      public static final String ORIGIN = "origin";
      public static final String AMOUNT_IN_CENTS = "unit_amount_in_cents";
      public static final String QTY = "quantity";
      public static final String DISCOUNT_IN_CENTS = "discount_in_cents";
      public static final String TAX_IN_CENTS = "tax_in_cents";
      public static final String TOTAL_IN_CENTS = "total_in_cents";
      public static final String CURRENCY_TYPE = "currency";
      public static final String TAXABLE = "taxable";
      public static final String TAX_TYPE = "tax_type";
      public static final String TAX_REGION = "tax_region";
      public static final String TAX_RATE = "tax_rate";
      public static final String TAX_EXEMPT = "tax_exempt";
      public static final String TAX_CODE = "tax_code";
      public static final String TAX_DETAILS = "tax_details"; //(array)
      public static final String START_DATE = "start_date";
      public static final String END_DATE = "end_date";
      public static final String CREATED_AT = "created_at";

      public static final String LINE_ITEM_PLURAL = "line_items";
   }

   public enum AdjustmentType {
      CHARGE("charge"),
      CREDIT("credit");

      private final String type;

      private AdjustmentType(final String type) {
         this.type = type;
      }

      public String getType() {
         return type;
      }
   }

   public enum AdjustmentState {
      PENDING("pending"),
      INVOICED("invoiced");

      private final String state;

      private AdjustmentState(final String state) {
         this.state = state;
      }

      public String getState() {
         return state;
      }
   }

   private String adjustmentType;
   private String adjustmentID;
   private String originalAdjustmentUuid;
   private String state;
   private String description;
   private String accountingCode;
   private String productCode;
   private String origin;
   private String unitAmountInCents;
   private String quantity;
   private String discountInCents;
   private String taxInCents;
   private String totalInCents;
   private String currency;
   private String taxable;
   private String taxType;
   private String taxExempt;
   private String taxRegion;
   private String taxRate;
   private List<TaxDetail> taxDetails;
   private String taxCode;
   private Date startDate;
   private Date endDate;
   private Date createdAt;

   @Override
   public String getTagName() {
      return Tags.TAG_NAME;
   }
  
   @Override
   public RecurlyModels<?> createContainer() {
      Adjustments adjustments = new Adjustments();
      adjustments.add(this);
      return adjustments;
   }

   public final String getAdjustmentType() {
      return adjustmentType;
   }

   public final void setAdjustmentType(String adjustmentType) {
      this.adjustmentType = adjustmentType;
   }

   public final String getAdjustmentID() {
      return adjustmentID;
   }

   public final void setAdjustmentID(String adjustmentID) {
      this.adjustmentID = adjustmentID;
   }

   public final String getOriginalAdjustmentUuid() {
      return originalAdjustmentUuid;
   }

   public final void setOriginalAdjustmentUuid(String originalAdjustmentUuid) {
      this.originalAdjustmentUuid = originalAdjustmentUuid;
   }

   public final String getState() {
      return state;
   }

   public final void setState(String state) {
      this.state = state;
   }

   public final String getDescription() {
      return description;
   }

   public final void setDescription(String description) {
      this.description = description;
   }

   public final String getAccountingCode() {
      return accountingCode;
   }

   public final void setAccountingCode(String accountingCode) {
      this.accountingCode = accountingCode;
   }

   public final String getProductCode() {
      return productCode;
   }

   public final void setProductCode(String productCode) {
      this.productCode = productCode;
   }

   public final String getOrigin() {
      return origin;
   }

   public final void setOrigin(String origin) {
      this.origin = origin;
   }

   public final String getUnitAmountInCents() {
      return unitAmountInCents;
   }

   public final void setUnitAmountInCents(String unitAmountInCents) {
      this.unitAmountInCents = unitAmountInCents;
   }

   public final String getQuantity() {
      return quantity;
   }

   public final void setQuantity(String quantity) {
      this.quantity = quantity;
   }

   public final String getDiscountInCents() {
      return discountInCents;
   }

   public final void setDiscountInCents(String discountInCents) {
      this.discountInCents = discountInCents;
   }

   public final String getTaxInCents() {
      return taxInCents;
   }

   public final void setTaxInCents(String taxInCents) {
      this.taxInCents = taxInCents;
   }

   public final String getTotalInCents() {
      return totalInCents;
   }

   public final void setTotalInCents(String totalInCents) {
      this.totalInCents = totalInCents;
   }

   public final String getCurrency() {
      return currency;
   }

   public final void setCurrency(String currency) {
      this.currency = currency;
   }

   public final String getTaxable() {
      return taxable;
   }

   public final void setTaxable(String taxable) {
      this.taxable = taxable;
   }

   public final String getTaxType() {
      return taxType;
   }

   public final void setTaxType(String taxType) {
      this.taxType = taxType;
   }

   public final String getTaxExempt() {
      return taxExempt;
   }

   public final void setTaxExempt(String taxExempt) {
      this.taxExempt = taxExempt;
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

   public final List<TaxDetail> getTaxDetails() {
      return taxDetails;
   }

   public final void setTaxDetails(List<TaxDetail> taxDetails) {
      this.taxDetails = taxDetails;
   }

   public final String getTaxCode() {
      return taxCode;
   }

   public final void setTaxCode(String taxCode) {
      this.taxCode = taxCode;
   }

   public final Date getStartDate() {
      return startDate;
   }

   public final void setStartDate(Date startDate) {
      this.startDate = startDate;
   }

   public final Date getEndDate() {
      return endDate;
   }

   public final void setEndDate(Date endDate) {
      this.endDate = endDate;
   }

   public final Date getCreatedAt() {
      return createdAt;
   }

   public final void setCreatedAt(Date createdAt) {
      this.createdAt = createdAt;
   }

   @Override
   public Adjustment copy() {
      try {
         return (Adjustment)clone();
      } catch (CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      Adjustment clone = new Adjustment();
      clone.setAdjustmentType(adjustmentType);
      clone.setAdjustmentID(adjustmentID);
      clone.setOriginalAdjustmentUuid(originalAdjustmentUuid);
      clone.setState(state);
      clone.setDescription(description);
      clone.setAccountingCode(accountingCode);
      clone.setProductCode(productCode);
      clone.setOrigin(origin);
      clone.setUnitAmountInCents(unitAmountInCents);
      clone.setQuantity(quantity);
      clone.setDiscountInCents(discountInCents);
      clone.setTaxInCents(taxInCents);
      clone.setTotalInCents(totalInCents);
      clone.setCurrency(currency);
      clone.setTaxable(taxable);
      clone.setTaxType(taxType);
      clone.setTaxExempt(taxExempt);
      clone.setTaxRegion(taxRegion);
      clone.setTaxRate(taxRate);
      clone.setTaxDetails(IrisCollections.deepCopyList(taxDetails));
      clone.setTaxCode(taxCode);
      clone.setStartDate((Date)startDate.clone());
      clone.setEndDate((Date)endDate.clone());
      clone.setCreatedAt((Date)endDate.clone());
      return clone;
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("Adjustment [adjustmentType=").append(adjustmentType)
            .append(", adjustmentID=").append(adjustmentID)
            .append(", originalAdjustmentUuid=").append(originalAdjustmentUuid)
            .append(", state=")
            .append(state).append(", description=").append(description)
            .append(", accountingCode=").append(accountingCode)
            .append(", productCode=").append(productCode).append(", origin=")
            .append(origin).append(", unitAmountInCents=")
            .append(unitAmountInCents).append(", quantity=").append(quantity)
            .append(", discountInCents=").append(discountInCents)
            .append(", taxInCents=").append(taxInCents)
            .append(", totalInCents=").append(totalInCents)
            .append(", currency=").append(currency).append(", taxable=")
            .append(taxable).append(", taxType=").append(taxType)
            .append(", taxExempt=").append(taxExempt).append(", taxRegion=")
            .append(taxRegion).append(", taxRate=").append(taxRate)
            .append(", taxDetails=").append(taxDetails).append(", taxCode=")
            .append(taxCode).append(", startDate=").append(startDate)
            .append(", endDate=").append(endDate).append(", createdAt=")
            .append(createdAt).append("]");
      return builder.toString();
   }
}

