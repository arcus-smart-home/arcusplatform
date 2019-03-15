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
import com.iris.billing.client.model.Details;
import com.iris.billing.client.model.Invoice;
import com.iris.billing.client.model.Subscription;
import com.iris.billing.client.model.Transaction;
import com.iris.billing.client.model.TransactionError;

class TransactionDeserializer extends AbstractRecurlyDeserializer<Transaction> {
   private Transaction transaction;

   public TransactionDeserializer() {
      this(null);
   }

   TransactionDeserializer(SAXParser saxParser) {
      super(saxParser, Transaction.Tags.TAG_NAME);
      transaction = new Transaction();
   }

   @Override
   protected Transaction getResult() {
      return transaction;
   }
   
   @Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {
      if (childDeserializer instanceof DetailsDeserializer) {
         Details details = (Details)childDeserializer.getResult();
         transaction.setAccount((details != null && !details.isEmpty()) ? details.get(0) : null);
      }
      else if (childDeserializer instanceof AccountDeserializer) {
         transaction.setAccount((Account)childDeserializer.getResult());
      }
      else if (childDeserializer instanceof TransactionErrorDeserializer) {
         transaction.setTransactionError((TransactionError)childDeserializer.getResult());
      }
   }

   @Override
   public void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      switch (qName) {
         case TransactionError.Tags.TAG_NAME:
            TransactionErrorDeserializer transactionErrorDeserializer = new TransactionErrorDeserializer(getParser());
            transactionErrorDeserializer.setParentHandler(this);
            setContentHandler(transactionErrorDeserializer);
            break;
         case Transaction.Tags.DETAILS:
            DetailsDeserializer detailsDeserializer = new DetailsDeserializer(getParser());
            detailsDeserializer.setParentHandler(this);
            setContentHandler(detailsDeserializer);
            break;
            // HREF's
         case Account.Tags.TAG_NAME:
         case Invoice.Tags.TAG_NAME:
         case Subscription.Tags.TAG_NAME:
            break;
         case Transaction.Tags.CVV_RESULT:
            transaction.setCvvResultCode(attributes.getValue(Transaction.Tags.CVV_RESULT_CODE));
            break;
         case Transaction.Tags.AVS_RESULT:
            transaction.setAvsResultCode(attributes.getValue(Transaction.Tags.AVS_RESULT_CODE));
            break;
         default:
            // No-Op
            break;
      }
   }

   @Override
   public void onEndElement (String uri, String localName, String qName) throws SAXException {
      switch (qName) {
         case Transaction.Tags.TRANSACTION_ID:
            transaction.setTransactionID(getCurrentTextValue());
            break;
         case Transaction.Tags.ACTION:
            transaction.setAction(getCurrentTextValue());
            break;
         case Transaction.Tags.AMOUNT_IN_CENTS:
            transaction.setAmountInCents(getCurrentTextValue());
            break;
         case Transaction.Tags.TAX_IN_CENTS:
            transaction.setTaxInCents(getCurrentTextValue());
            break;
         case Transaction.Tags.CURRENCY:
            transaction.setCurrency(getCurrentTextValue());
            break;
         case Transaction.Tags.STATUS:
            transaction.setStatus(getCurrentTextValue());
            break;
         case Transaction.Tags.PAYMENT_METHOD:
            transaction.setPaymentMethod(getCurrentTextValue());
            break;
         case Transaction.Tags.REFERENCE:
            transaction.setReference(getCurrentTextValue());
            break;
         case Transaction.Tags.SOURCE:
            transaction.setSource(getCurrentTextValue());
            break;
         case Transaction.Tags.RECURRING:
            transaction.setRecurring(getCurrentTextValue());
            break;
         case Transaction.Tags.IS_TEST:
            transaction.setTestPayment(getCurrentTextValue());
            break;
         case Transaction.Tags.VOIDABLE:
            transaction.setVoidable(getCurrentTextValue());
            break;
         case Transaction.Tags.REFUNDABLE:
            transaction.setRefundable(getCurrentTextValue());
            break;
         case Transaction.Tags.CVV_RESULT:
            transaction.setCvvResult(getCurrentTextValue());
            break;
         case Transaction.Tags.AVS_RESULT:
            transaction.setAvsResult(getCurrentTextValue());
            break;
         case Transaction.Tags.AVS_RESULT_STREET:
            transaction.setAvsResultStreet(getCurrentTextValue());
            break;
         case Transaction.Tags.AVS_RESULT_POSTAL:
            transaction.setAvsResultPostal(getCurrentTextValue());
            break;
         case Transaction.Tags.CREATED_AT:
            transaction.setCreatedAt(getDateFromString(getCurrentTextValue()));
            break;
         default:
            // No-Op
            break;
      }
   }
}

