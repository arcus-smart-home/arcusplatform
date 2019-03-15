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

import com.iris.billing.client.model.Account;
import com.iris.billing.client.model.Adjustment;
import com.iris.billing.client.model.Invoice;
import com.iris.billing.client.model.Subscription;
import com.iris.billing.client.model.TaxDetails;

class AdjustmentDeserializer extends AbstractRecurlyDeserializer<Adjustment> {
   private Adjustment adjustment;
   private TaxDetailsDeserializer taxDetailsDeserializer;

   public AdjustmentDeserializer() {
      this(null, null);
   }

   AdjustmentDeserializer(SAXParser reader, Attributes attributes) {
      super(reader, Adjustment.Tags.TAG_NAME);
      adjustment = new Adjustment();
      if (attributes != null) {
         adjustment.setAdjustmentType(attributes.getValue("type"));
      }
   }

   @Override
   protected Adjustment getResult() {
      return adjustment;
   }
   
   @Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {
      if (childDeserializer instanceof TaxDetailsDeserializer) {
         adjustment.setTaxDetails((TaxDetails)childDeserializer.getResult());
      }
   }

   @Override
   public void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      switch(qName) {
            // HREF's
         case Account.Tags.TAG_NAME:
         case Invoice.Tags.TAG_NAME:
         case Subscription.Tags.TAG_NAME:
            break;

         case TaxDetails.Tags.TAG_NAME:
            TaxDetailsDeserializer taxDetailsDeserializer = new TaxDetailsDeserializer(getParser());
            taxDetailsDeserializer.setParentHandler(this);
            setContentHandler(taxDetailsDeserializer);
            break;
         default:
            // No-Op
            break;
      }
   }

   @Override
   public void onEndElement (String uri, String localName, String qName) throws SAXException {
      switch(qName) {
         case Adjustment.Tags.ID:
            adjustment.setAdjustmentID(getCurrentTextValue());
            break;
         case Adjustment.Tags.STATE:
            adjustment.setState(getCurrentTextValue());
            break;
         case Adjustment.Tags.DESCRIPTION:
            adjustment.setDescription(getCurrentTextValue());
            break;
         case Adjustment.Tags.ACCOUNTING_CODE:
            adjustment.setAccountingCode(getCurrentTextValue());
            break;
         case Adjustment.Tags.PRODUCT_CODE:
            adjustment.setProductCode(getCurrentTextValue());
            break;
         case Adjustment.Tags.ORIGIN:
            adjustment.setOrigin(getCurrentTextValue());
            break;
         case Adjustment.Tags.AMOUNT_IN_CENTS:
            adjustment.setUnitAmountInCents(getCurrentTextValue());
            break;
         case Adjustment.Tags.QTY:
            adjustment.setQuantity(getCurrentTextValue());
            break;
         case Adjustment.Tags.ORIGINAL_ID:
            adjustment.setOriginalAdjustmentUuid(getCurrentTextValue());
            break;
         case Adjustment.Tags.DISCOUNT_IN_CENTS:
            adjustment.setDiscountInCents(getCurrentTextValue());
            break;
         case Adjustment.Tags.TAX_IN_CENTS:
            adjustment.setTaxInCents(getCurrentTextValue());
            break;
         case Adjustment.Tags.TOTAL_IN_CENTS:
            adjustment.setTotalInCents(getCurrentTextValue());
            break;
         case Adjustment.Tags.CURRENCY_TYPE:
            adjustment.setCurrency(getCurrentTextValue());
            break;
         case Adjustment.Tags.TAXABLE:
            adjustment.setTaxable(getCurrentTextValue());
            break;
         case Adjustment.Tags.TAX_EXEMPT:
            adjustment.setTaxExempt(getCurrentTextValue());
            break;
         case Adjustment.Tags.TAX_CODE:
            adjustment.setTaxCode(getCurrentTextValue());
            break;
         case Adjustment.Tags.START_DATE:
            adjustment.setStartDate(getDateFromString(getCurrentTextValue()));
            break;
         case Adjustment.Tags.END_DATE:
            adjustment.setEndDate(getDateFromString(getCurrentTextValue()));
            break;
         case Adjustment.Tags.CREATED_AT:
            adjustment.setCreatedAt(getDateFromString(getCurrentTextValue()));
            break;
         case Adjustment.Tags.TAX_TYPE:
            adjustment.setTaxType(getCurrentTextValue());
            break;
         case Adjustment.Tags.TAX_REGION:
            adjustment.setTaxRegion(getCurrentTextValue());
            break;
         case Adjustment.Tags.TAX_RATE:
            adjustment.setTaxRate(getCurrentTextValue());
            break;
         case Adjustment.Tags.TAX_DETAILS:
            if (taxDetailsDeserializer != null) {
               adjustment.setTaxDetails(taxDetailsDeserializer.getResult());
            }
            break;
         default:
            // No-Op
            break;
      }
   }
}

