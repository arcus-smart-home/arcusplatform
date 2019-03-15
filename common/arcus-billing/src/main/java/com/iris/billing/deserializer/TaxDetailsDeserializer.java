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
import com.iris.billing.client.model.TaxDetails;

class TaxDetailsDeserializer extends AbstractRecurlyDeserializer<TaxDetails> {
   private TaxDetails taxDetails;

   public TaxDetailsDeserializer() {
      this(null);
   }

   TaxDetailsDeserializer(SAXParser saxParser) {
      super(saxParser, TaxDetails.Tags.TAG_NAME);
      taxDetails = new TaxDetails();
   }

   @Override
   protected TaxDetails getResult() {
      return taxDetails;
   }
   
   @Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {
      if (childDeserializer instanceof TaxDetailDeserializer) {
         taxDetails.add((TaxDetail)childDeserializer.getResult());
      }
   }

   @Override
   public void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      if (TaxDetail.Tags.TAG_NAME.equals(qName)) {
         TaxDetailDeserializer taxDetailDeserializer = new TaxDetailDeserializer(getParser());
         taxDetailDeserializer.setParentHandler(this);
         setContentHandler(taxDetailDeserializer);
      }
   }

   @Override
   public void onEndElement (String uri, String localName, String qName) throws SAXException {
      // No-Op
   }
}

