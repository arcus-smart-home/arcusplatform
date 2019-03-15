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
import com.iris.billing.client.model.Address;
import com.iris.billing.client.model.BillingInfo;

class AccountDeserializer extends AbstractRecurlyDeserializer<Account> {
   private Account account;
   
   public AccountDeserializer() {
      this(null);
   }

   AccountDeserializer(SAXParser saxParser) {
      super(saxParser, Account.Tags.TAG_NAME);
      account = new Account();
   }

   @Override
   protected Account getResult() {
      return account;
   }
   
   @Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {
      if (childDeserializer instanceof AddressDeserializer) {
         account.setAddress((Address)childDeserializer.getResult());
      }
      else if (childDeserializer instanceof BillingInfoDeserializer) {
         account.setBillingInfo((BillingInfo)childDeserializer.getResult());
      }
   }

   @Override
   public void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      switch(qName) {
         case Account.Tags.TAG_NAME:
            account = new Account();
            break;
         case Address.Tags.TAG_NAME:
            AddressDeserializer addressDeserializer = new AddressDeserializer(getParser());
            addressDeserializer.setParentHandler(this);
            setContentHandler(addressDeserializer);
            break;
         case Account.Tags.BILLING_INFO:
            BillingInfoDeserializer billingInfoDeserializer = new BillingInfoDeserializer(getParser());
            billingInfoDeserializer.setParentHandler(this);
            setContentHandler(billingInfoDeserializer);
            break;
            // HREF's
         case Account.Tags.ADJUSTMENT:
         case Account.Tags.INVOICE:
         case Account.Tags.REDEMPTION:
         case Account.Tags.TRANSACTION:
         case Account.Tags.SUBSCRIPTION:
            break;
         default:
            // No-Op
            break;
      }
   }

   @Override
   public void onEndElement (String uri, String localName, String qName) throws SAXException {
      switch(qName) {
         case Account.Tags.STATE:
            account.setState(getCurrentTextValue());
            break;
         case Account.Tags.ACCEPT_LANGUAGE:
            account.setAcceptLanguage(getCurrentTextValue());
            break;
         case Account.Tags.ACCOUNT_CODE:
            account.setAccountID(getCurrentTextValue());
            break;
         case Account.Tags.USERNAME:
            account.setUsername(getCurrentTextValue());
            break;
         case Account.Tags.EMAIL:
            account.setEmail(getCurrentTextValue());
            break;
         case Account.Tags.FIRST_NAME:
            account.setFirstName(getCurrentTextValue());
            break;
         case Account.Tags.LAST_NAME:
            account.setLastName(getCurrentTextValue());
            break;
         case Account.Tags.COMPANY:
         case Account.Tags.COMPANY_NAME:
            account.setCompanyName(getCurrentTextValue());
            break;
         case Account.Tags.VAT_NUMBER:
            account.setVatNumber(getCurrentTextValue());
            break;
         case Account.Tags.TAX_EXEMPT:
            account.setTaxExempt(Boolean.valueOf(getCurrentTextValue()));
            break;
         case Account.Tags.HOSTED_LOGIN_TOKEN:
            account.setHostedLoginToken(getCurrentTextValue());
            break;
         case Account.Tags.ENTITY_USE_CODE:
            account.setEntityUseCode(getCurrentTextValue());
            break;
         case Account.Tags.CREATED_AT:
            account.setCreatedAt(getDateFromString(getCurrentTextValue()));
            break;
         case Account.Tags.VAT_LOCAION_VALID:
            account.setVatLocationValid(Boolean.parseBoolean(getCurrentTextValue()));
            break;
         default:
            // No-Op
            break;
      }
   }
}

