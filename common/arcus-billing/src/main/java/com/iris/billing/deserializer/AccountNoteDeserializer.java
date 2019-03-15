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

import com.iris.billing.client.model.AccountNote;

class AccountNoteDeserializer extends AbstractRecurlyDeserializer<AccountNote> {
   private AccountNote  note;

   public AccountNoteDeserializer() {
      this(null);
   }

   AccountNoteDeserializer(SAXParser saxParser) {
      super(saxParser, AccountNote.Tags.TAG_NAME);
      note = new AccountNote();
   }

   @Override
   protected AccountNote getResult() {
      return note;
   }
   
   @Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {
      // Has no child elements.
   }

   @Override
   public void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
   	// Nothing to do.
   }

   @Override
   public void onEndElement (String uri, String localName, String qName) throws SAXException {
      switch(qName) {
         case AccountNote.Tags.MESSAGE:
            note.setMessage(getCurrentTextValue());
            break;
         case AccountNote.Tags.CREATED_AT:
            note.setCreatedAt(getDateFromString(getCurrentTextValue()));
            break;
         default:
            // No-Op
            break;
      }
   }

}

