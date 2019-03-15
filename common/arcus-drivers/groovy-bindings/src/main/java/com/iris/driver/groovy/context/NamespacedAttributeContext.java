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
package com.iris.driver.groovy.context;

import java.util.Map;

import com.iris.Utils;
import com.iris.device.model.AttributeDefinition;

/**
 *
 */
public class NamespacedAttributeContext extends AttributesContext {
   private final String namespace;

   public NamespacedAttributeContext(Map<String, AttributeDefinition> definitions, String namespace) {
      super(definitions);
      this.namespace = namespace;
   }

   @Override
   public Object getProperty(String property) {
      switch(property) {
      case "class": // fall through
      case "properties":
         return super.getProperty(property);
      default:
         if(Utils.isNamespaced(property)) {
            // TODO throw MissingPropertyException?
            return getAttribute(property);
         }
         else {
            return getAttribute(Utils.namespace(namespace, property));
         }
      }
   }

   @Override
   public void setProperty(String property, Object newValue) {
      if(Utils.isNamespaced(property)) {
         setAttribute(property, newValue);
      }
      else {
         setAttribute(Utils.namespace(namespace, property), newValue);
      }
   }

}

