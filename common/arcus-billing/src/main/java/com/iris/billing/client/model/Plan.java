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

import com.iris.messages.model.Copyable;

public class Plan extends RecurlyModel implements Copyable<Plan> {
   public static class Tags {
      private Tags() {}
      public static final String URL_RESOURCE = "/plans";

      public static final String TAG_NAME = "plan";

      public static final String PLAN_CODE =   "plan_code";
      public static final String PLAN_NAME =   "name";
      public static final String PLAN_DESCRIPTION =   "description";
      public static final String ACCOUNTING_CODE =   "accounting_code";
      public static final String INTERVAL_UNIT =   "plan_interval_unit";
      public static final String INTERVAL_LENGTH =   "plan_interval_length";
      public static final String TRIAL_INTERVAL_UNIT =   "trial_interval_unit";
      public static final String TRIAL_INTERVAL_LENGTH =   "trial_interval_length";
      public static final String BILLING_CYCLES =   "total_billing_cycles";
      public static final String UNIT_NAME =   "unit_name";
      public static final String DISPLAY_QUANTITY =   "display_quantity";
      public static final String SUCCESS_URL =   "success_url";
      public static final String CANCEL_URL =   "cancel_url";
      public static final String TAX_EXEMPT =   "tax_exempt";
      public static final String TAX_CODE =   "tax_code";
      public static final String PAYMENT_TOS_LINK = "payment_page_tos_link";
      public static final String BYPASS_HOSTED_CONFIRM = "bypass_hosted_confirmation";
      public static final String DISPLAY_PHONE_NUMBER = "display_phone_number";
      public static final String CREATED_AT = "created_at";

      public static final String QUANTITY = "quantity";

      // Nested Tags.
      public static final String SETUP_FEE_IN_CENTS =   "setup_fee_in_cents";
      public static final String PLAN_COST_IN_CENTS =   "unit_amount_in_cents";

      protected static final String CONTINIOUS_BILLING_CYCLES = "continuous";
   }

   private String planCode;
   private String name;
   private String description;
   private String accountingCode;
   private String planIntervalUnit;
   private String planIntervalLength;

   private String trialIntervalUnit;
   private String trialIntervalLength;
   private CostInCents setupFeeInCents;
   private CostInCents unitAmountInCents;
   private String totalBillingCycles; // null = continuous
   private String unitName;
   private String displayQuantity;
   private String taxExempt;
   private String taxCode;
   private Date createdAt;

   private String quantity;

   private String planTOSLink;
   private String successUrl;
   private String cancelUrl;

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
      Plans plans = new Plans();
      plans.add(this);
      return plans;
   }

   public final String getQuantity() {
      return quantity;
   }

   public final void setQuantity(String quantity) {
      this.quantity = quantity;
      mappings.put(Tags.QUANTITY, quantity);
   }

   public final String getPlanCode() {
      return planCode;
   }

   public final void setPlanCode(String planCode) {
      this.planCode = planCode;
      mappings.put(Tags.PLAN_CODE, planCode);
   }

   public final String getName() {
      return name;
   }

   public final void setName(String name) {
      this.name = name;
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

   public final String getPlanIntervalUnit() {
      return planIntervalUnit;
   }

   public final void setPlanIntervalUnit(String planIntervalUnit) {
      this.planIntervalUnit = planIntervalUnit;
   }

   public final String getPlanIntervalLength() {
      return planIntervalLength;
   }

   public final void setPlanIntervalLength(String planIntervalLength) {
      this.planIntervalLength = planIntervalLength;
   }

   public final String getTrialIntervalUnit() {
      return trialIntervalUnit;
   }

   public final void setTrialIntervalUnit(String trialIntervalUnit) {
      this.trialIntervalUnit = trialIntervalUnit;
   }

   public final String getTrialIntervalLength() {
      return trialIntervalLength;
   }

   public final void setTrialIntervalLength(String trialIntervalLength) {
      this.trialIntervalLength = trialIntervalLength;
   }

   public final CostInCents getSetupFeeInCents() {
      return setupFeeInCents;
   }

   public final void setSetupFeeInCents(CostInCents setupFeeInCents) {
      this.setupFeeInCents = setupFeeInCents;
   }

   public final CostInCents getUnitAmountInCents() {
      return unitAmountInCents;
   }

   public final void setUnitAmountInCents(CostInCents unitAmountInCents) {
      this.unitAmountInCents = unitAmountInCents;
   }

   public final String getTotalBillingCycles() {
      return totalBillingCycles;
   }

   public final void setTotalBillingCycles(String totalBillingCycles) {
      this.totalBillingCycles = totalBillingCycles;
   }

   public final String getUnitName() {
      return unitName;
   }

   public final void setUnitName(String unitName) {
      this.unitName = unitName;
   }

   public final String getDisplayQuantity() {
      return displayQuantity;
   }

   public final void setDisplayQuantity(String displayQuantity) {
      this.displayQuantity = displayQuantity;
   }

   public final String getSuccessUrl() {
      return successUrl;
   }

   public final void setSuccessUrl(String successUrl) {
      this.successUrl = successUrl;
   }

   public final String getCancelUrl() {
      return cancelUrl;
   }

   public final void setCancelUrl(String cancelUrl) {
      this.cancelUrl = cancelUrl;
   }

   public final String getTaxExempt() {
      return taxExempt;
   }

   public final void setTaxExempt(String taxExempt) {
      this.taxExempt = taxExempt;
      mappings.put(Tags.TAX_EXEMPT, taxExempt);
   }

   public final String getTaxCode() {
      return taxCode;
   }

   public final void setTaxCode(String taxCode) {
      this.taxCode = taxCode;
   }

   public final String getPlanTOSLink() {
      return planTOSLink;
   }

   public final void setPlanTOSLink(String planTOSLink) {
      this.planTOSLink = planTOSLink;
   }

   public Date getCreatedAt() {
      return createdAt;
   }

   public void setCreatedAt(Date createdAt) {
      this.createdAt = createdAt;
   }

   @Override
   public Plan copy() {
      try {
         return (Plan)clone();
      } catch (CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      Plan clone = new Plan();
      clone.setPlanCode(planCode);
      clone.setName(name);
      clone.setDescription(description);
      clone.setAccountingCode(accountingCode);
      clone.setPlanIntervalUnit(planIntervalUnit);
      clone.setPlanIntervalLength(planIntervalLength);
      clone.setTrialIntervalUnit(trialIntervalUnit);
      clone.setTrialIntervalLength(trialIntervalLength);
      clone.setSetupFeeInCents(setupFeeInCents.copy());
      clone.setUnitAmountInCents(unitAmountInCents.copy());
      clone.setTotalBillingCycles(totalBillingCycles);
      clone.setUnitName(unitName);
      clone.setDisplayQuantity(displayQuantity);
      clone.setTaxExempt(taxExempt);
      clone.setTaxCode(taxCode);
      clone.setCreatedAt((Date)createdAt.clone());
      clone.setQuantity(quantity);
      clone.setPlanTOSLink(planTOSLink);
      clone.setSuccessUrl(successUrl);
      clone.setCancelUrl(cancelUrl);
      return super.clone();
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("Plan [planCode=").append(planCode).append(", name=")
      .append(name).append(", description=").append(description)
      .append(", accountingCode=").append(accountingCode)
      .append(", planIntervalUnit=").append(planIntervalUnit)
      .append(", planIntervalLength=").append(planIntervalLength)
      .append(", trialIntervalUnit=").append(trialIntervalUnit)
      .append(", trialIntervalLength=").append(trialIntervalLength)
      .append(", setupFeeInCents=").append(setupFeeInCents)
      .append(", unitAmountInCents=").append(unitAmountInCents)
      .append(", totalBillingCycles=").append(totalBillingCycles)
      .append(", unitName=").append(unitName)
      .append(", displayQuantity=").append(displayQuantity)
      .append(", taxExempt=").append(taxExempt).append(", taxCode=")
      .append(taxCode).append(", createdAt=").append(createdAt)
      .append(", quantity=").append(quantity).append(", planTOSLink=")
      .append(planTOSLink).append(", successUrl=").append(successUrl)
      .append(", cancelUrl=").append(cancelUrl).append("]");
      return builder.toString();
   }
}

