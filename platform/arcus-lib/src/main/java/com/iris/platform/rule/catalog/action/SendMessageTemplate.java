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
package com.iris.platform.rule.catalog.action;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.iris.platform.rule.catalog.action.config.SendActionConfig;
import com.iris.platform.rule.catalog.template.TemplatedExpression;

/**
 * 
 */
public class SendMessageTemplate extends AbstractActionTemplate {
   
   public SendMessageTemplate(Set<String> contextVariables) {
      super(contextVariables);
   }

   private TemplatedExpression to;
   private String type;
   private Map<String, TemplatedExpression> attributes;

   /**
    * @return the to
    */
   public TemplatedExpression getTo() {
      return to;
   }

   /**
    * @param to the to to set
    */
   public void setTo(TemplatedExpression to) {
      this.to = to;
   }

   /**
    * @return the type
    */
   public String getType() {
      return type;
   }

   /**
    * @param type the type to set
    */
   public void setType(String type) {
      this.type = type;
   }

   /**
    * @return the attributes
    */
   public Map<String, TemplatedExpression> getAttributes() {
      if(attributes == null) {
         attributes = new HashMap<String, TemplatedExpression>();
      }
      return attributes;
   }
   
   public void addAttribute(String name, String value) {
      addAttribute(name, new TemplatedExpression(value));
   }
   
   public void addAttribute(String name, TemplatedExpression value) {
      getAttributes().put(name, value);
   }

   @Override
   public SendActionConfig generateActionConfig(Map<String, Object> variables) {
      SendActionConfig sac = new SendActionConfig(type);
      sac.setAddress(to.bind(variables));
      sac.setAttributes(bind(variables));
      return sac;
   }
   
   private Map<String, TemplatedExpression> bind(Map<String, Object> variables){
      Map<String, TemplatedExpression> values = new HashMap<>(variables.size());
      for(Map.Entry<String, TemplatedExpression> attr: getAttributes().entrySet()){
         values.put(attr.getKey(), attr.getValue().bind(variables));
      }
      return values;
   }

}

