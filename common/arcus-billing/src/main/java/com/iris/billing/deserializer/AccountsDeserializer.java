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
import com.iris.billing.client.model.Accounts;

public class AccountsDeserializer extends AbstractRecurlyDeserializer<Accounts> {
   private Accounts accounts;
   
   public AccountsDeserializer() {
      this(null);
   }
   
   AccountsDeserializer(SAXParser saxParser) {
      super(saxParser, Accounts.Tags.TAG_NAME);
      accounts = new Accounts();
   }

   @Override
   protected Accounts getResult() {
      return accounts;
   }

   @Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {
      if (childDeserializer instanceof AccountDeserializer) {
         accounts.add((Account)childDeserializer.getResult());
      }
   }

   @Override
   protected void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      switch(qName) {
         case Account.Tags.TAG_NAME:
            AccountDeserializer accountDeserializer = new AccountDeserializer(getParser());
            accountDeserializer.setParentHandler(this);
            setContentHandler(accountDeserializer);
            break;
      }
   }

   @Override
   protected void onEndElement(String uri, String localName, String qName) throws SAXException {
      // No-Op
   }  
}

