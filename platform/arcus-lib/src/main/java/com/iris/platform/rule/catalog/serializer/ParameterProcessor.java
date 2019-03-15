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

import org.xml.sax.Attributes;

import com.iris.common.rule.type.RuleTypeUtil;
import com.iris.platform.rule.catalog.action.Parameter;
import com.iris.platform.rule.catalog.action.ParameterValueAttribute;
import com.iris.platform.rule.catalog.action.ParameterValueConstant;
import com.iris.platform.rule.catalog.action.ParameterValueTime;
import com.iris.platform.rule.catalog.action.config.ParameterConfig;
import com.iris.platform.rule.catalog.action.config.ParameterConfig.ParameterType;
import com.iris.serializer.sax.TagProcessor;
import com.iris.validators.Validator;

public class ParameterProcessor extends BaseCatalogProcessor {
   public static final String TAG = "parameter";
   private static final String TAG_ATTRIBUTE_VALUE = "attribute-value";
   private static final String TAG_DATETIME_VALUE = "datetime-value";
   private static final String TAG_CONSTANT_VALUE = "constant-value";
   
   private Parameter parameter;
   private ParameterConfig parameterConfig;
   
   protected ParameterProcessor(Validator validator) {
      super(validator);
   }
   
   public Parameter getParameter() {
      return parameter;
   }
   
   public ParameterConfig getParameterConfig() {
      return parameterConfig;
   }
   
   @Override
   public TagProcessor getHandler(String qName, Attributes attributes) {
      return this;
   }

   @Override
   public void enterTag(String qName, Attributes attributes) {
      super.enterTag(qName,  attributes);
      if (TAG.equals(qName)) {
         parseParameter(attributes);
      }
      else if (TAG_ATTRIBUTE_VALUE.equals(qName)) {
         parseAttributeValue(attributes);
         parameterConfig.setType(ParameterType.ATTRIBUTEVALUE);
      }
      else if (TAG_DATETIME_VALUE.equals(qName)) {
         parseDatetimeValue(attributes);
         parameterConfig.setType(ParameterType.DATETIME);

      }
      else if (TAG_CONSTANT_VALUE.equals(qName)) {
         parseConstantValue(attributes);
         parameterConfig.setType(ParameterType.CONSTANT);
      }
   }
   
   private void parseParameter(Attributes attributes) {
      parameter = new Parameter();
      parameterConfig = new ParameterConfig();
      parameter.setName(attributes.getValue("name"));
      parameterConfig.setName(attributes.getValue("name"));
   }
   
   private void parseConstantValue(Attributes attributes) {
      ParameterValueConstant value = new ParameterValueConstant();
      value.setValue(getTemplatedString("value", attributes));
      parameter.setValue(value);
      parameterConfig.setValue(getValue("value", attributes));
   }

   private void parseDatetimeValue(Attributes attributes) {
      ParameterValueTime value = new ParameterValueTime();
      value.setType(RuleTypeUtil.INSTANCE.coerce(ParameterValueTime.Type.class, attributes.getValue("type")));      
      parameter.setValue(value);
      //parameterConfig.setValue(getValue("value", attributes));
   }
   
   private void parseAttributeValue(Attributes attributes) {
      ParameterValueAttribute value = new ParameterValueAttribute();
      value.setAddress(getTemplatedAddress("address", attributes));
      value.setAttribute(attributes.getValue("attribute"));
      parameter.setValue(value);  
      parameterConfig.setAddress(getValue("address", attributes));
      parameterConfig.setAttributeName(getValue("attribute", attributes));
   }
  
   
}

