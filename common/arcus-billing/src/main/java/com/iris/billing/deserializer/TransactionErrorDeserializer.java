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

import com.iris.billing.client.model.TransactionError;

public class TransactionErrorDeserializer extends AbstractRecurlyDeserializer<TransactionError> {
   private TransactionError transactionError;
   
   public TransactionErrorDeserializer() {
      this(null);
   }
   
   TransactionErrorDeserializer(SAXParser saxParser) {
      super(saxParser, TransactionError.Tags.TAG_NAME);
      transactionError = new TransactionError();
   }

   @Override
   protected TransactionError getResult() {
      return transactionError;
   }

   @Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {
      // There are child elements to worry about it.
   }

   @Override
   protected void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      // No-Op
   }

   @Override
   protected void onEndElement(String uri, String localName, String qName) throws SAXException {
      switch(qName) {
         case TransactionError.Tags.CODE:
            transactionError.setErrorCode(getCurrentTextValue());
            break;
         case TransactionError.Tags.CATEGORY:
            transactionError.setErrorCategory(getCurrentTextValue());
            break;
         case TransactionError.Tags.MERCHANT_MSG:
            transactionError.setMerchantMessage(getCurrentTextValue());
            break;
         case TransactionError.Tags.CUSTOMER_MSG:
            transactionError.setCustomerMessage(getCurrentTextValue());
            break;
      }
   }
   
}

