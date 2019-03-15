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

import com.iris.billing.client.model.CostInCents;
import com.iris.billing.client.model.Plan;
import com.iris.billing.client.model.PlanAddon;

class PlanAddonDeserializer extends AbstractRecurlyDeserializer<PlanAddon> {
   private PlanAddon addon;

   public PlanAddonDeserializer() {
      this(null);
   }

   PlanAddonDeserializer(SAXParser saxParser) {
      super(saxParser, PlanAddon.Tags.TAG_NAME);
      addon = new PlanAddon();
   }

   @Override
   protected PlanAddon getResult() {
      return addon;
   }
   
   @Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {
      if (childDeserializer instanceof CostInCentsDeserializer) {
         addon.setUnitAmountInCents((CostInCents)childDeserializer.getResult());
      }
   }

   @Override
   public void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      switch(qName) {
         case PlanAddon.Tags.COST_IN_CENTS:
            CostInCentsDeserializer unitAmountDeserializer = new CostInCentsDeserializer(getParser(), PlanAddon.Tags.COST_IN_CENTS);
            unitAmountDeserializer.setParentHandler(this);
            setContentHandler(unitAmountDeserializer);
            break;

            // HREF
         case Plan.Tags.TAG_NAME:
            break;
         default:
            // No-Op
            break;
      }
   }

   @Override
   public void onEndElement (String uri, String localName, String qName) throws SAXException {
      switch(qName) {
         case PlanAddon.Tags.ADD_ON_CODE:
            addon.setAddOnCode(getCurrentTextValue());
            break;
         case PlanAddon.Tags.ADD_ON_NAME:
            addon.setName(getCurrentTextValue());
            break;
         case PlanAddon.Tags.CREATED_AT:
            addon.setCreatedAt(getDateFromString(getCurrentTextValue()));
            break;
         case PlanAddon.Tags.DEFAULT_QUANTITY:
            addon.setDefaultQuantity(getCurrentTextValue());
            break;
         case PlanAddon.Tags.DISPLAY_QTY_ON_HOSTED_PAGE:
            addon.setDisplayQuantityOnHostedPage(Boolean.valueOf(getCurrentTextValue()));
            break;
         default:
            // No-Op
            break;
      }
   }

}

