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

import com.iris.billing.client.model.RecurlyError;
import com.iris.billing.client.model.RecurlyErrors;
import com.iris.billing.client.model.Transaction;
import com.iris.billing.client.model.TransactionError;

public class ErrorsDeserializer extends AbstractRecurlyDeserializer<RecurlyErrors> {
   private RecurlyErrors recurlyErrors;
   
   public ErrorsDeserializer() {
      this(null);
   }
   
   ErrorsDeserializer(SAXParser saxParser) {
      super(saxParser, RecurlyErrors.Tags.TAG_NAME);
      recurlyErrors = new RecurlyErrors();
   }

   @Override
   protected RecurlyErrors getResult() {
      return recurlyErrors;
   }

   @Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {
      if (childDeserializer instanceof ErrorDeserializer) {
         recurlyErrors.add((RecurlyError)childDeserializer.getResult());
      }
      else if (childDeserializer instanceof TransactionErrorDeserializer) {
         recurlyErrors.addTransactionError((TransactionError)childDeserializer.getResult());
      }
      else if (childDeserializer instanceof TransactionDeserializer) {
         recurlyErrors.addTransaction((Transaction)childDeserializer.getResult());
      }
   }

   @Override
   protected void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      switch(qName) {
         case RecurlyError.Tags.TAG_NAME:
            ErrorDeserializer errorDeserializer = new ErrorDeserializer(getParser(), attributes);
            errorDeserializer.setParentHandler(this);
            setContentHandler(errorDeserializer);
            break;
         case TransactionError.Tags.TAG_NAME:
            TransactionErrorDeserializer transactionErrorDeserializer = new TransactionErrorDeserializer(getParser());
            transactionErrorDeserializer.setParentHandler(this);
            setContentHandler(transactionErrorDeserializer);
            break;
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

