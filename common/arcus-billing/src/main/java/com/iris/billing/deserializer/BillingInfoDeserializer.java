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

import com.iris.billing.client.model.BillingInfo;

class BillingInfoDeserializer extends AbstractRecurlyDeserializer<BillingInfo> {
   private BillingInfo billingInfo;

   public BillingInfoDeserializer() {
      this(null);
   }

   BillingInfoDeserializer(SAXParser saxParser) {
      super(saxParser, BillingInfo.Tags.TAG_NAME);
      billingInfo = new BillingInfo();
   }

   @Override
   protected BillingInfo getResult() {
      return billingInfo;
   }
   
   @Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {
      // No child elements
   }

   @Override
   public void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      // No-Op, no special action required on start elements.
   }

   @Override
   public void onEndElement(String uri, String localName, String qName) throws SAXException {
      switch(qName) {
         case BillingInfo.Tags.FIRST_NAME:
            billingInfo.setFirstName(getCurrentTextValue());
            break;
         case BillingInfo.Tags.LAST_NAME:
            billingInfo.setLastName(getCurrentTextValue());
            break;
         case BillingInfo.Tags.ADDRESS_ONE:
            billingInfo.setAddress1(getCurrentTextValue());
            break;
         case BillingInfo.Tags.ADDRESS_TWO:
            billingInfo.setAddress2(getCurrentTextValue());
            break;
         case BillingInfo.Tags.CITY:
            billingInfo.setCity(getCurrentTextValue());
            break;
         case BillingInfo.Tags.STATE:
            billingInfo.setState(getCurrentTextValue());
            break;
         case BillingInfo.Tags.COUNTRY:
            billingInfo.setCountry(getCurrentTextValue());
            break;
         case BillingInfo.Tags.ZIP_CODE:
            billingInfo.setZip(getCurrentTextValue());
            break;
         case BillingInfo.Tags.PHONE_NUMBER:
            billingInfo.setPhone(getCurrentTextValue());
            break;
         case BillingInfo.Tags.VAT_NUMBER:
            billingInfo.setVatNumber(getCurrentTextValue());
            break;
         case BillingInfo.Tags.IP_ADDRESS:
            billingInfo.setIpAddress(getCurrentTextValue());
            break;
         case BillingInfo.Tags.IP_ADDRESS_COUNTRY:
            billingInfo.setIpAddressCountry(getCurrentTextValue());
            break;
         case BillingInfo.Tags.FIRST_SIX:
            billingInfo.setFirstSix(getCurrentTextValue());
            break;
         case BillingInfo.Tags.LAST_FOUR:
            billingInfo.setLastFour(getCurrentTextValue());
            break;
         case BillingInfo.Tags.CARD_TYPE:
            billingInfo.setCardType(getCurrentTextValue());
            break;
         case BillingInfo.Tags.CC_EXP_MONTH:
            billingInfo.setMonth(getCurrentTextValue());
            break;
         case BillingInfo.Tags.CC_EXP_YEAR:
            billingInfo.setYear(getCurrentTextValue());
            break;
         case BillingInfo.Tags.PAYPAL_BILLING_AGREEMENT_ID:
            billingInfo.setPaypalBillingAgreementId(getCurrentTextValue());
            break;
         case BillingInfo.Tags.AMAZON_BILLING_AGREEMENT_ID:
            billingInfo.setAmazonBillingAgreementId(getCurrentTextValue());
            break;
         case BillingInfo.Tags.COMPANY_NAME:
            billingInfo.setCompanyName(getCurrentTextValue());
            break;
         default:
            // No-Op
            break;
      }
   }

}

