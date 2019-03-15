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
/**
 *
 */
package com.iris.platform.rule.catalog.serializer;

import org.xml.sax.Attributes;

import com.iris.platform.rule.catalog.condition.config.ReceivedMessageConfig;
import com.iris.serializer.sax.TagProcessor;
import com.iris.validators.Validator;

/**
 *
 */
public class ReceivedMessageProcessor extends BaseCatalogProcessor {
   public static final String TAG = "received";
   
   private ReceivedMessageConfig template;
   
   /**
    * @param validator
    */
   public ReceivedMessageProcessor(Validator validator) {
      super(validator);
   }
  
   public ReceivedMessageConfig getCondition() {
      return template;
   }
   
   @Override
   public TagProcessor getHandler(String qName, Attributes attributes) {
      if (AttributeProcessor.TAG.equals(qName)) {
         return new AttributeProcessor(getValidator());
      }
      return this;
   }
   
   @Override
   public void enterTag(String qName, Attributes attributes) {
      if (TAG.equals(qName)) {
         addOnMessageTrigger(attributes);
      }
   }
   
   @Override
   public void exitTag(String qName) {
      
   }
 
   public void exitChildTag(String qName, TagProcessor handler) {
      if(qName.equals(AttributeProcessor.TAG)) {
         AttributeProcessor processor = (AttributeProcessor) handler;
         if(template.getAttributeExpressions().put(processor.getAttribute(), processor.getExpression()) != null) {
            validator.error("Re-defined value for attribute " + processor.getAttribute());
         }
      }
   }
   
   private void addOnMessageTrigger(Attributes attributes) {
      template = new ReceivedMessageConfig();
      if (attributes.getValue("from") != null)
      {
         template.setSourceExpression(getTemplatedExpression("from", attributes));
      }
      template.setMessageTypeExpression(getTemplatedExpression("message", attributes));
   }
   
}

