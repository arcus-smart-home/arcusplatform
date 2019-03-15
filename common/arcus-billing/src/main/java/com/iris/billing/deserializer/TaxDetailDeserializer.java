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

import com.iris.billing.client.model.TaxDetail;

public class TaxDetailDeserializer extends AbstractRecurlyDeserializer<TaxDetail> {
   private TaxDetail taxDetail;
   
   public TaxDetailDeserializer() {
      this(null);
   }
   
   TaxDetailDeserializer(SAXParser saxParser) {
      super(saxParser, TaxDetail.Tags.TAG_NAME);
      taxDetail = new TaxDetail();
   }

   @Override
   protected TaxDetail getResult() {
      return taxDetail;
   }

   @Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {
      // No-Op
   }

   @Override
   protected void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      // No-Op
   }

   @Override
   protected void onEndElement(String uri, String localName, String qName)
         throws SAXException {
      switch(qName) {
         case TaxDetail.Tags.NAME:
            taxDetail.setName(getCurrentTextValue());
            break;
         case TaxDetail.Tags.TAX_RATE:
            taxDetail.setTaxRate(getCurrentTextValue());
            break;
         case TaxDetail.Tags.TAX_TYPE:
            taxDetail.setTaxType(getCurrentTextValue());
            break;
         case TaxDetail.Tags.TAX_IN_CENTS:
            taxDetail.setTaxInCents(getCurrentTextValue());
            break;
         default:
            // No-Op
            break;
      }
   }
}

