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

import com.google.common.base.Strings;
import com.iris.billing.client.model.RecurlyError;

/**
 * Since the XML is able to return both a transaction error and an API error in the same go,
 * if a transaction error is present, that is the only item this response will contain
 * and can be checked using isTransactionError on the {@link RecurlyError} object.
 *  
 * If this is not a transaction error, but an API error, then the above will be false and
 * this can contain multiple errors in the resulting list.
 *
 */
class ErrorDeserializer extends AbstractRecurlyDeserializer<RecurlyError> {
   private RecurlyError recurlyError;

   public ErrorDeserializer() {
      this(null, null);
   }

   ErrorDeserializer(SAXParser saxParser, Attributes attributes) {
      super(saxParser, RecurlyError.Tags.TAG_NAME);
      recurlyError = new RecurlyError();
      if (attributes != null) {
         recurlyError.setErrorField(attributes.getValue(RecurlyError.Tags.ERROR_FIELD));
         recurlyError.setErrorSymbol(attributes.getValue(RecurlyError.Tags.ERROR_SYMBOL));
         recurlyError.setLanguage(attributes.getValue(RecurlyError.Tags.ERROR_LANGUAGE));
      }
   }

   @Override
   protected RecurlyError getResult() {
      return recurlyError;
   }
   
   @Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {
      // No child elements to worry about.  
   }

   @Override
   public void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      switch(qName) {
         case RecurlyError.Tags.ERROR_DESCRIPTION_TAG:
            recurlyError.setLanguage(attributes.getValue(RecurlyError.Tags.ERROR_LANGUAGE));
            break;
      }
   }

   @Override
   public void onEndElement(String uri, String localName, String qName) throws SAXException {
      switch(qName) {
         case RecurlyError.Tags.ERROR_FIELD:
            recurlyError.setErrorField(getCurrentTextValue());
            break;
         case RecurlyError.Tags.ERROR_SYMBOL:
            recurlyError.setErrorSymbol(getCurrentTextValue());
            break;
         case RecurlyError.Tags.ERROR_DESCRIPTION_TAG:
            recurlyError.setErrorText(getCurrentTextValue());
            break;
      }
   }

   @Override
   protected void onFinish() {
      if (Strings.isNullOrEmpty(recurlyError.getErrorText())) {
         recurlyError.setErrorText(getCurrentTextValue());
      }
      super.onFinish();
   }
}

