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

import com.iris.billing.client.model.Address;

class AddressDeserializer extends AbstractRecurlyDeserializer<Address> {
   private Address address;

   public AddressDeserializer() {
      this(null);
   }

   AddressDeserializer(SAXParser saxParser) {
      super(saxParser, Address.Tags.TAG_NAME);
      address = new Address();
   }

   @Override
   protected Address getResult() {
      return address;
   }
   
   @Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {
      // There are no child elements to worry about.
   }

   @Override
   public void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      // No-Op (No Attributes we need to capture from start tags)
   }

   @Override
   public void onEndElement (String uri, String localName, String qName) throws SAXException {
      switch(qName) {
         case Address.Tags.ADDRESS_ONE:
            address.setAddress1(getCurrentTextValue());
            break;
         case Address.Tags.ADDRESS_TWO:
            address.setAddress2(getCurrentTextValue());
            break;
         case Address.Tags.CITY:
            address.setCity(getCurrentTextValue());
            break;
         case Address.Tags.STATE:
            address.setState(getCurrentTextValue());
            break;
         case Address.Tags.ZIP_CODE:
            address.setZip(getCurrentTextValue());
            break;
         case Address.Tags.COUNTRY:
            address.setCountry(getCurrentTextValue());
            break;
         case Address.Tags.PHONE_NUMBER:
            address.setPhoneNumber(getCurrentTextValue());
            break;
         default:
            // No-Op
            break;
      }
   }
}

