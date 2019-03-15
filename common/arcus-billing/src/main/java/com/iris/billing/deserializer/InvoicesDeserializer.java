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

import com.iris.billing.client.model.Invoice;
import com.iris.billing.client.model.Invoices;

public class InvoicesDeserializer extends AbstractRecurlyDeserializer<Invoices> {
   private Invoices invoices;
   
   public InvoicesDeserializer() {
      this(null);
   }
   
   InvoicesDeserializer(SAXParser saxParser) {
      super(saxParser, Invoices.Tags.TAG_NAME);
      invoices = new Invoices();
   }

   @Override
   protected Invoices getResult() {
      return invoices;
   }

   @Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {
      if (childDeserializer instanceof InvoiceDeserializer) {
         invoices.add((Invoice)childDeserializer.getResult());
      }
   }

   @Override
   protected void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      switch(qName) {
         case Invoice.Tags.TAG_NAME:
            InvoiceDeserializer invoiceDeserializer = new InvoiceDeserializer(getParser(), attributes);
            invoiceDeserializer.setParentHandler(this);
            setContentHandler(invoiceDeserializer);
            break;
      }
   }

   @Override
   protected void onEndElement(String uri, String localName, String qName) throws SAXException {
      // No-Op
   }
}

