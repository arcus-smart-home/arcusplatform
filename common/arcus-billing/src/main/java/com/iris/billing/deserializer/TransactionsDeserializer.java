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

import com.iris.billing.client.model.Transaction;
import com.iris.billing.client.model.Transactions;

public class TransactionsDeserializer extends AbstractRecurlyDeserializer<Transactions> {
   private Transactions transactions;
   
   public TransactionsDeserializer() {
      this(null);
   }
   
   TransactionsDeserializer(SAXParser saxParser) {
      super(saxParser, Transactions.Tags.TAG_NAME);
      transactions = new Transactions();
   }

   @Override
   protected Transactions getResult() {
      return transactions;
   }

   @Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {
      if (childDeserializer instanceof TransactionDeserializer) {
         transactions.add((Transaction)childDeserializer.getResult());
      }
   }

   @Override
   protected void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      switch(qName) {
         case Transaction.Tags.TAG_NAME:
            TransactionDeserializer transactionDeserializer = new TransactionDeserializer(getParser());
            transactionDeserializer.setParentHandler(this);
            setContentHandler(transactionDeserializer);
            break;
      }
   }

   @Override
   protected void onEndElement(String uri, String localName, String qName) throws SAXException {
      // No-Op
   }
}

