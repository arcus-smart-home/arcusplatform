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

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.iris.bootstrap.ServiceLocator;
import com.iris.capability.definition.AttributeDefinition;
import com.iris.capability.definition.AttributeType;
import com.iris.capability.definition.AttributeTypes;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.common.rule.time.DayOfWeek;
import com.iris.common.rule.time.TimeOfDay;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;
import com.iris.platform.rule.catalog.function.FunctionFactory;
import com.iris.platform.rule.catalog.template.TemplatedExpression;
import com.iris.platform.rule.catalog.template.TemplatedValue;
import com.iris.serializer.sax.BaseProcessor;
import com.iris.validators.Validator;

/**
 * 
 */
public class BaseCatalogProcessor extends BaseProcessor {
   private DefinitionRegistry registry;
   
   protected BaseCatalogProcessor(Validator validator) {
      super(validator);
   }
   
   // TODO make TagHandlers singletons and @Inject this
   public DefinitionRegistry getRegistry() {
      if(registry == null) {
         this.registry = ServiceLocator.getInstance(DefinitionRegistry.class);
      }
      return registry;
   }
   
   public void setRegistry(DefinitionRegistry registry) {
      this.registry = registry;
   }

   protected TemplatedValue<String> getTemplatedString(String name, Attributes attributes) {
      TemplatedValue<Object> template = getTemplatedValue(name, attributes);
      return TemplatedValue.transform(Functions.toStringFunction(), template);
   }
   
   protected TemplatedValue<String> getTemplatedString(String name, String defaultValue, Attributes attributes) {
      TemplatedValue<Object> template = getTemplatedValue(name, TemplatedValue.value(defaultValue), attributes);
      return TemplatedValue.transform(Functions.toStringFunction(), template);
   }

   protected TemplatedValue<Integer> getTemplatedInteger(String name, Attributes attributes) {
      TemplatedValue<Object> template = getTemplatedValue(name, attributes);
      return TemplatedValue.transform(FunctionFactory.INSTANCE.getToInteger(), template);
   }
   
   protected TemplatedValue<Boolean> getTemplatedBoolean(String name, Boolean defaultValue, Attributes attributes) {
      TemplatedValue<Object> template = getTemplatedValue(name, TemplatedValue.value(defaultValue), attributes);
      return TemplatedValue.transform(FunctionFactory.INSTANCE.getToBoolean(), template);
   }

   protected TemplatedValue<Set<DayOfWeek>> getTemplatedSetOfDays(String name, Attributes attributes) {
      TemplatedValue<Object> template = getTemplatedValue(name, attributes);
      return TemplatedValue.transform(FunctionFactory.INSTANCE.getToSetOfDays(), template);
   }

   protected TemplatedValue<Address> getTemplatedAddress(String name, Attributes attributes) {
      try {
         TemplatedValue<Object> template = getTemplatedValue(name, attributes);
         if(template == null) {
            return null;
         }
         return TemplatedValue.transform(FunctionFactory.INSTANCE.getToAddress(), template);
      }
      catch(IllegalArgumentException e) {
         validator.error("Invalid address [" + name + "=" + attributes.getValue(name) + "]: " + e.getMessage());
         return null;
      }
   }

   protected TemplatedValue<TimeOfDay> getTemplatedTimeOfDay(String name, Attributes attributes) {
      try {
         TemplatedValue<Object> template = getTemplatedValue(name, attributes);
         if(template == null) {
            return null;
         }
         return TemplatedValue.transform(FunctionFactory.INSTANCE.getToTimeOfDay(), template);
      }
      catch(IllegalArgumentException e) {
         validator.error("Invalid time of day [" + name + "=" + attributes.getValue(name) + "]: " + e.getMessage());
         return null;
      }
   }

   protected TemplatedValue<Object> getTemplatedValue(String name, Attributes attributes) {
      TemplatedValue<Object> template = getTemplatedValue(name, null, attributes);
      if(template == null) {
         validator.error("Missing required attribute [" + name + "] for tag <" + tag + ">");
         return null;
      }
      return template;
   }

   protected TemplatedValue<Object> getTemplatedValue(String name, TemplatedValue<Object> defaultValue, Attributes attributes) {
      String value = attributes.getValue(name);
      if(StringUtils.isEmpty(value)) {
         return defaultValue;
      }
      return TemplatedValue.parse(value);
   }

   protected TemplatedExpression getTemplatedExpression(String name, Attributes attributes) {
      TemplatedExpression template = getTemplatedExpression(name, null, attributes);
      if(template == null) {
         validator.error("Missing required attribute [" + name + "] for tag <" + tag + ">");
         return null;
      }
      return template;
   }

   protected TemplatedExpression getTemplatedExpression(String name, TemplatedExpression defaultValue, Attributes attributes) {
      String value = attributes.getValue(name);
      if(StringUtils.isEmpty(value)) {
         return defaultValue;
      }
      return new TemplatedExpression(value);
   }

   protected TemplatedValue<Object> getTemplatedAttribute(String name, String attributeName, Attributes attributes) {
      try {
         AttributeDefinition ad = getRegistry().getAttribute(attributeName);
         TemplatedValue<Object> template = getTemplatedValue(name, attributes);
         if(template == null) {
            return null;
         }

         if(ad == null) {
            return template;
         }
         return TemplatedValue.transform(new AttributeTypeTransform(ad.getType()), template);
      }
      catch(IllegalArgumentException e) {
         validator.error("Invalid attribute value [" + name + "=" + attributes.getValue(name) + "]: " + e.getMessage());
         return null;
      }
   }

   protected TemplatedValue<Object> getTemplatedAttribute(String name, String attributeName, TemplatedValue<Object> defaultValue, Attributes attributes) {
      try {
         AttributeDefinition ad = getRegistry().getAttribute(attributeName);
         TemplatedValue<Object> template = getTemplatedValue(name, null, attributes);
         if(template == null) {
            return defaultValue;
         }

         if(ad == null) {
            return template;
         }
         return TemplatedValue.transform(new AttributeTypeTransform(ad.getType()), template);
      }
      catch(IllegalArgumentException e) {
         validator.error("Invalid attribute value [" + name + "=" + attributes.getValue(name) + "]: " + e.getMessage());
         return null;
      }
   }

   protected TemplatedValue<Predicate<Model>> getTemplatedQuery(String name, Attributes attributes) {
      try {
         TemplatedValue<Object> template = getTemplatedValue(name, attributes);
         return TemplatedValue.transform(FunctionFactory.INSTANCE.getToModelPredicate(), template);
      }
      catch(Exception e) {
         validator.error("Invalid query value [" + name + "=" + attributes.getValue(name) + "]: " + e.getMessage());
         return null;
      }
   }
   
   protected AttributeType getAttributeType(String attributeName) {
      AttributeDefinition ad = getRegistry().getAttribute(attributeName);
      if(ad == null) {
         validator.error("Unrecognized attribute [" + attributeName + "]");
         return AttributeTypes.stringType();
      }
      return ad.getType();
   }
   
   private static final class AttributeTypeTransform implements Function<Object, Object> {
      private final AttributeType type;

      AttributeTypeTransform(AttributeType type) {
         this.type = type;
      }

      @Override
      public Object apply(Object input) {
         return type.coerce(input);
      }

   }
}

