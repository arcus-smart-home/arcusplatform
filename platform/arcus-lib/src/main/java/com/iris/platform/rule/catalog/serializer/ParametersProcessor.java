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

import org.xml.sax.Attributes;

import com.iris.platform.rule.catalog.action.Parameter;
import com.iris.platform.rule.catalog.action.config.ParameterConfig;
import com.iris.serializer.sax.TagProcessor;
import com.iris.validators.Validator;

public class ParametersProcessor extends BaseCatalogProcessor {
   public static final String TAG = "parameters";
   
   private final List<Parameter> parameters = new ArrayList<>();
   private final List<ParameterConfig> parameterConfigs = new ArrayList<>();
     
   public List<ParameterConfig> getParameterConfigs() {
      return parameterConfigs;
   }
   

   protected ParametersProcessor(Validator validator) {
      super(validator);
   }
   
   public List<Parameter> getParameters() {
      return parameters;
   }

   @Override
   public TagProcessor getHandler(String qName, Attributes attributes) {
      if (ParameterProcessor.TAG.equals(qName)) {
         return new ParameterProcessor(getValidator());
      }
      return super.getHandler(qName, attributes);
   }

   @Override
   public void exitChildTag(String qName, TagProcessor handler) {
      if (ParameterProcessor.TAG.equals(qName)) {
         parameters.add(((ParameterProcessor)handler).getParameter());
         parameterConfigs.add(((ParameterProcessor)handler).getParameterConfig());
         
      }
   }
   
   
}

