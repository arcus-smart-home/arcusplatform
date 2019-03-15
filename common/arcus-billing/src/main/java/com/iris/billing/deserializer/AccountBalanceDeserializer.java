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

import com.iris.billing.client.model.AccountBalance;
import com.iris.billing.client.model.CostInCents;

public class AccountBalanceDeserializer extends AbstractRecurlyDeserializer<AccountBalance> {

   private AccountBalance accountBalance;

   public AccountBalanceDeserializer() {
      this(null);
   }

   AccountBalanceDeserializer(SAXParser saxParser) {
      super(saxParser, AccountBalance.Tags.TAG_NAME);
      accountBalance = new AccountBalance();
   }

   @Override
   protected AccountBalance getResult() {
      return accountBalance;
   }

   @Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {
      if (childDeserializer instanceof CostInCentsDeserializer) {
         accountBalance.setBalanceInCents((CostInCents)childDeserializer.getResult());
      }
   }

   @Override
   public void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      switch (qName) {
         case AccountBalance.Tags.BALANCE_IN_CENTS:
            CostInCentsDeserializer balanceDeserializer = new CostInCentsDeserializer(getParser(), AccountBalance.Tags.BALANCE_IN_CENTS);
            balanceDeserializer.setParentHandler(this);
            setContentHandler(balanceDeserializer);
            break;
         default:
            // No-Op
            break;
      }
   }

   @Override
   public void onEndElement(String uri, String localName, String qName) throws SAXException {
      switch (qName) {
         case AccountBalance.Tags.PAST_DUE:
            accountBalance.setPastDue(Boolean.valueOf(getCurrentTextValue()));
            break;
         default:
            // No-Op
            break;
      }
   }
}

