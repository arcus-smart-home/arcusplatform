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

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;
import groovy.lang.ReadOnlyPropertyException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.iris.Utils;
import com.iris.capability.key.NamespacedKey;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.attributes.AttributeValue;
import com.iris.device.model.AttributeDefinition;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.GroovyContextObject;

/**
 *
 */
public class AttributesContext extends GroovyObjectSupport {
   private Map<String, AttributeDefinition> supportedAttributes;

   public AttributesContext(Map<String, AttributeDefinition> supportedAttributes) {
      this.supportedAttributes = supportedAttributes;
   }

   public Map<String, Object> getAttributes(String namespace) {
      final String prefix = namespace + ":";
      return asMap(new Predicate<AttributeKey<?>>() {
         @Override
         public boolean apply(AttributeKey<?> input) {
            return input.getName().startsWith(prefix);
         }
      });
   }

   public Object getAttribute(AttributeKey<?> key) {
      DeviceDriverContext context = GroovyContextObject.getContext();
      return context.getAttributeValue(key);
   }

   public Object getAttribute(String name) {
      AttributeDefinition definition = supportedAttributes.get(name);
      // TODO throw MissingPropertyException?
      if(definition == null) {
         return null;
      }

      return getAttribute(definition.getKey());
   }

   public Object setAttribute(AttributeKey<?> key, Object value) {
      DeviceDriverContext context = GroovyContextObject.getContext();
      if(!supportedAttributes.containsKey(key.getName())) {
         throw new MissingPropertyException("The attribute [" + key + "] is not supported by this driver, please check your spelling and capability definitions");
      }
      return context.setAttributeValue(key.coerceToValue(value));
   }

   public Object setAttribute(String name, Object value) {
      NamespacedKey attribute = NamespacedKey.parse(name);
      AttributeDefinition definition = supportedAttributes.get(name);
      if(definition == null) {
         throw new MissingPropertyException("The attribute [" + name + "] is not supported by this driver, please check your spelling and capability definitions");
      }

      if(attribute.isInstanced()) {
         return setAttribute(definition.getKey().instance(attribute.getInstance()), value);
      }
      return setAttribute(definition.getKey(), value);
   }

   public Object getProperty(AttributeKey<?> key) {
      return getAttribute(key);
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
            return getAttributes(property);
         }
      }
   }

   @Override
   public void setProperty(String property, Object newValue) {
      if(Utils.isNamespaced(property)) {
         setAttribute(property, newValue);
      }
      else {
         throw new ReadOnlyPropertyException(property, getClass());
      }
   }

   public Object leftShift(AttributeMap other) {
      if(other == null || other.isEmpty()) {
         return null;
      }
      for(AttributeValue<?> attribute: other.entries()) {
         GroovyContextObject.getContext().setAttributeValue(attribute);
      }
      return this;
   }

   public Object leftShift(Map<String, Object> other) {
      if(other == null || other.isEmpty()) {
         return this;
      }
      for(Map.Entry<String, Object> entry: other.entrySet()) {
         setProperty(entry.getKey(), entry.getValue());
      }
      return this;
   }

   public Object asType(Class<?> cls) {
      if(Map.class.equals(cls)) {
         return asMap(Predicates.alwaysTrue());
      }

      // TODO throw?
      return null;
   }

   private Map<String, Object> asMap(Predicate<? super AttributeKey<?>> p) {
      DeviceDriverContext context = GroovyContextObject.getContext();
      Set<AttributeKey<?>> keys = context.getAttributeKeys();

      Map<String, Object> values = new HashMap<String, Object>(keys.size() + 1);
      for(AttributeKey<?> key: keys) {
         if(!p.apply(key)) {
            continue;
         }

         Object value = context.getAttributeValue(key);
         if(value != null) {
            values.put(key.getName(), value);
         }
      }
      return values;
   }
}

