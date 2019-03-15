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

import com.iris.billing.client.model.PlanAddon;
import com.iris.billing.client.model.PlanAddons;

public class PlanAddonsDeserializer extends AbstractRecurlyDeserializer<PlanAddons> {
   private PlanAddons planAddons;
   
   public PlanAddonsDeserializer() {
      this(null);
   }
   
   PlanAddonsDeserializer(SAXParser saxParser) {
      super(saxParser, PlanAddons.Tags.TAG_NAME);
      planAddons = new PlanAddons();
   }

   @Override
   protected PlanAddons getResult() {
      return planAddons;
   }

   @Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {
      if (childDeserializer instanceof PlanAddonDeserializer) {
         planAddons.add((PlanAddon)childDeserializer.getResult());
      }
   }

   @Override
   protected void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      switch(qName) {
      case PlanAddon.Tags.TAG_NAME:
         PlanAddonDeserializer planAddonDeserializer = new PlanAddonDeserializer(getParser());
         planAddonDeserializer.setParentHandler(this);
         setContentHandler(planAddonDeserializer);
         break;
      }
   }

   @Override
   protected void onEndElement(String uri, String localName, String qName) throws SAXException {
      // No-Op
   }
   
   
}

