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
package com.iris.platform.rule.catalog.serializer;

import java.util.ArrayList;
import java.util.List;

import com.iris.platform.rule.catalog.condition.config.ConditionConfig;
import com.iris.platform.rule.catalog.condition.config.OrConfig;
import com.iris.serializer.sax.TagProcessor;
import com.iris.validators.Validator;

public class OrConditionProcessor extends ConditionsContainerProcessor {

   public static final String TAG = "or";
   
   private List<ConditionConfig> configs = new ArrayList<>();
   
   public OrConditionProcessor(Validator validator) {
      super(validator);
   }
  
   @Override
   public OrConfig getCondition() {
      return new OrConfig(configs);
   }
   
   @Override
   public boolean isFilter() {
      return false;
   }
   
   @Override
   public void exitTag(String qName) {

      super.exitTag(qName);

      // Exiting an <or> tag
      if(TAG.equals(qName)) {
         validator.assertTrue(configs.size() > 1, "or tag must contain at least two conditions");
      }
      // Exiting a non-<or> lightweight child tag
      else {
         configs.add(super.getCondition());

         // Reset condition so that multiple lightweight child tags can be processed
         setCondition(null); 
      }
   }

   // Exiting a non-<or> heavyweight child tag
   @Override
   public void exitChildTag(String qName, TagProcessor handler) {

      // Don't call super.exitChildTag() because it would call ConditionsContainerProcessor.setCondition(), which would
      // throw an exception if there's more than one heavyweight child tag

      if (handler instanceof ConditionsContainerProcessor)
      {
         configs.add(((ConditionsContainerProcessor) handler).getCondition());
      }
      else if (handler instanceof ReceivedMessageProcessor)
      {
         configs.add(((ReceivedMessageProcessor) handler).getCondition());
      }
   }
}

