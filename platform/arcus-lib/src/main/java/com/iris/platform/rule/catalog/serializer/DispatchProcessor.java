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

import java.util.Map;

import org.xml.sax.Attributes;

import com.google.common.collect.ImmutableMap;
import com.iris.serializer.sax.TagProcessor;
import com.iris.validators.Validator;

/**
 * 
 */
public class DispatchProcessor extends BaseCatalogProcessor {
   private Map<String, TagProcessor> handlers;
   
   public DispatchProcessor(String tag, TagProcessor handler) {
      this(handler.getValidator(), ImmutableMap.of(tag, handler));
   }

   public DispatchProcessor(Map<String, TagProcessor> handlers) {
      this(null, handlers);
   }

   public DispatchProcessor(Validator validator, Map<String, TagProcessor> handlers) {
      super(validator);
      this.handlers = ImmutableMap.copyOf(handlers);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#getHandler(java.lang.String, org.xml.sax.Attributes)
    */
   @Override
   public TagProcessor getHandler(String qName, Attributes attributes) {
      TagProcessor handler = handlers.get(qName);
      if(handler != null) {
         return handler;
      }
      return super.getHandler(qName, attributes);
   }

   
}

