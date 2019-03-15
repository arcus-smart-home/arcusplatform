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
import com.iris.billing.client.model.Adjustments;
import com.iris.billing.client.model.Invoice;
import com.iris.billing.client.model.Subscription;
import com.iris.billing.client.model.Transactions;

class InvoiceDeserializer extends AbstractRecurlyDeserializer<Invoice> {
   private Invoice  invoice;

   public InvoiceDeserializer() {
      this(null, null);
   }

   InvoiceDeserializer(SAXParser saxParser, Attributes attributes) {
      super(saxParser, Invoice.Tags.TAG_NAME);
      invoice = new Invoice();
      invoice.setUrl(attributes.getValue(Invoice.Attributes.HREF));
   }

   @Override
   protected Invoice getResult() {
      return invoice;
   }
   
   @Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {
      if (childDeserializer instanceof AddressDeserializer) {
         invoice.setAddress((Address)childDeserializer.getResult());
      }
      else if (childDeserializer instanceof AdjustmentsDeserializer) {
         invoice.setAdjustments((Adjustments)childDeserializer.getResult());
      }
      else if (childDeserializer instanceof TransactionsDeserializer) {
         invoice.setTransactions((Transactions)childDeserializer.getResult());
      }
   }

   @Override
   public void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      switch(qName) {
            // HREF's
         case Subscription.Tags.TAG_NAME:
         case Account.Tags.TAG_NAME:
         case Account.Tags.REDEMPTION:
            break;

         case Address.Tags.TAG_NAME:
         	AddressDeserializer addressDeserializer = new AddressDeserializer(getParser());
            addressDeserializer.setParentHandler(this);
            setContentHandler(addressDeserializer);
            break;
            
         case Transactions.Tags.TAG_NAME:
            TransactionsDeserializer transactionsDeserializer = new TransactionsDeserializer(getParser());
            transactionsDeserializer.setParentHandler(this);
            setContentHandler(transactionsDeserializer);
            break;

         case Invoice.Tags.LINE_ITEMS:
            AdjustmentsDeserializer adjustmentsDeserializer = new AdjustmentsDeserializer(getParser(), Invoice.Tags.LINE_ITEMS);
            adjustmentsDeserializer.setParentHandler(this);
            setContentHandler(adjustmentsDeserializer);
            break;
      }
   }

   @Override
   public void onEndElement (String uri, String localName, String qName) throws SAXException {
      switch(qName) {
         case Invoice.Tags.INVOICE_ID:
            invoice.setInvoiceID(getCurrentTextValue());
            break;
         case Invoice.Tags.STATE:
            invoice.setState(getCurrentTextValue());
            break;
         case Invoice.Tags.INVOICE_NUMBER:
            invoice.setInvoiceNumber(getCurrentTextValue());
            break;
         case Invoice.Tags.INVOICE_NUM_PREFIX:
            invoice.setInvoiceNumberPrefix(getCurrentTextValue());
            break;
         case Invoice.Tags.PO_NUM:
            invoice.setPoNumber(getCurrentTextValue());
            break;
         case Invoice.Tags.VAT_NUM:
            invoice.setVatNumber(getCurrentTextValue());
            break;
         case Invoice.Tags.SUBTOTAL_IN_CENTS:
            invoice.setSubtotalInCents(getCurrentTextValue());
            break;
         case Invoice.Tags.TAX_IN_CENTS:
            invoice.setTaxInCents(getCurrentTextValue());
            break;
         case Invoice.Tags.TOTAL_IN_CENTS:
            invoice.setTotalInCents(getCurrentTextValue());
            break;
         case Invoice.Tags.CURRENCY:
            invoice.setCurrency(getCurrentTextValue());
            break;
         case Invoice.Tags.CREATED:
            invoice.setCreatedAt(getDateFromString(getCurrentTextValue()));
            break;
         case Invoice.Tags.CLOSED:
            invoice.setClosedAt(getDateFromString(getCurrentTextValue()));
            break;
         case Invoice.Tags.TAX_TYPE:
            invoice.setTaxType(getCurrentTextValue());
            break;
         case Invoice.Tags.TAX_REGION:
            invoice.setTaxRegion(getCurrentTextValue());
            break;
         case Invoice.Tags.TAX_RATE:
            invoice.setTaxRate(getCurrentTextValue());
            break;
         case Invoice.Tags.NET_TERMS:
            invoice.setNetTerms(getCurrentTextValue());
            break;
         case Invoice.Tags.COLLECTION_METHOD:
            invoice.setCollectionMethod(getCurrentTextValue());
            break;
         default:
            // No-Op
            break;
      }
   }
}

