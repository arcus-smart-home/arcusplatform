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
package com.iris.billing.deserializer;

import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.iris.billing.client.model.CostInCents;
import com.iris.billing.client.model.Plan;

class PlanDeserializer extends AbstractRecurlyDeserializer<Plan> {
   private Plan plan;

   public PlanDeserializer() {
      this(null);
   }

   PlanDeserializer(SAXParser saxParser) {
      super(saxParser, Plan.Tags.TAG_NAME);
      plan = new Plan();
   }
   
   @Override
   protected Plan getResult() {
      return plan;
   }
   
   @Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {
      if (Plan.Tags.PLAN_COST_IN_CENTS.equals(childDeserializer.getTagName())) {
         plan.setUnitAmountInCents((CostInCents)childDeserializer.getResult());
      }
      else if (Plan.Tags.SETUP_FEE_IN_CENTS.equals(childDeserializer.getTagName())) {
         plan.setSetupFeeInCents((CostInCents)childDeserializer.getResult());
      }
   }

   @Override
   public void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      switch(qName) {
         case Plan.Tags.PLAN_COST_IN_CENTS:
         case Plan.Tags.SETUP_FEE_IN_CENTS:
            CostInCentsDeserializer costInCentsDeserializer = new CostInCentsDeserializer(getParser(), qName);
            costInCentsDeserializer.setParentHandler(this);
            setContentHandler(costInCentsDeserializer);
            break;
         default:
            // No-Op
            break;
      }
   }

   @Override
   public void onEndElement (String uri, String localName, String qName) throws SAXException {
      switch(qName) {
         case Plan.Tags.ACCOUNTING_CODE:
            plan.setAccountingCode(getCurrentTextValue());
            break;
         case Plan.Tags.PLAN_CODE:
            plan.setPlanCode(getCurrentTextValue());
            break;
         case Plan.Tags.PLAN_NAME:
            plan.setName(getCurrentTextValue());
            break;
         case Plan.Tags.PLAN_DESCRIPTION:
            plan.setDescription(getCurrentTextValue());
            break;
         case Plan.Tags.INTERVAL_UNIT:
            plan.setPlanIntervalUnit(getCurrentTextValue());
            break;
         case Plan.Tags.INTERVAL_LENGTH:
            plan.setPlanIntervalLength(getCurrentTextValue());
            break;
         case Plan.Tags.TRIAL_INTERVAL_UNIT:
            plan.setTrialIntervalUnit(getCurrentTextValue());
            break;
         case Plan.Tags.TRIAL_INTERVAL_LENGTH:
            plan.setTrialIntervalLength(getCurrentTextValue());
            break;
         case Plan.Tags.UNIT_NAME:
            plan.setUnitName(getCurrentTextValue());
            break;
         case Plan.Tags.DISPLAY_QUANTITY:
            plan.setDisplayQuantity(getCurrentTextValue());
            break;
         case Plan.Tags.SUCCESS_URL:
            plan.setSuccessUrl(getCurrentTextValue());
            break;
         case Plan.Tags.CANCEL_URL:
            plan.setCancelUrl(getCurrentTextValue());
            break;
         case Plan.Tags.TAX_EXEMPT:
            plan.setTaxExempt(getCurrentTextValue());
            break;
         case Plan.Tags.TAX_CODE:
            plan.setTaxCode(getCurrentTextValue());
            break;
         case Plan.Tags.BILLING_CYCLES:
            plan.setTotalBillingCycles(getCurrentTextValue());
            break;
         case Plan.Tags.PAYMENT_TOS_LINK:
            plan.setPlanTOSLink(getCurrentTextValue());
            break;
         case Plan.Tags.CREATED_AT:
             plan.setCreatedAt(getDateFromString(getCurrentTextValue()));
             break;

         // Unhandled
         case Plan.Tags.BYPASS_HOSTED_CONFIRM:
         case Plan.Tags.DISPLAY_PHONE_NUMBER:
            break;

         default:
            // No-Op
            break;
      }
   }

}

