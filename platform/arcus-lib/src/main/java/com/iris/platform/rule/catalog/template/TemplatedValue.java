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
package com.iris.platform.rule.catalog.template;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

/**
 * 
 */
public interface TemplatedValue<V> extends Serializable {

   V apply(Map<String, Object> variables);
   
   // Can the template be resolved using the given variables?
   boolean isResolveable(Map<String, Object> variables);
   
   boolean hasContextVariables(Set<String> contextVariables);
   
   static final Pattern TEXT = Pattern.compile(".*\\$\\{(.*)\\}.*");
   static final Pattern NAMED = Pattern.compile("\\$\\{([^{}]*)\\}");
   
   public static boolean isTemplated(String value) {
      return TEXT.matcher(value).matches();
   }
   
   public static <V> TemplatedValue<V> nullValue() {
      return NullValue.instance();            
   }

   public static <V> TemplatedValue<V> value(V value) {
      return new ConstantValue<V>(value);          
   }
   
   public static TemplatedValue<Object> named(String name) {
      return named(name, Object.class);
   }
   
   public static <V> TemplatedValue<V> named(String name, Class<V> expectedType) {
      LookupValue<V> v = new LookupValue<>(expectedType);
      v.setName(name);
      return v;
   }
   
   public static <I, O> TemplatedValue<O> transform(Function<I, O> transform, TemplatedValue<I> template) {
      if(template instanceof ConstantValue) {
         return value(transform.apply(template.apply(ImmutableMap.of())));
      }
      return new TransformTemplate<I, O>(transform, template);
   }
   
   /**
    * Returns a {@link TemplatedValue} for a text string containing one
    * or more replacement values.
    * @param template
    * @return
    */
   public static TemplatedValue<String> text(String template) {
      return new StringValue(template);
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   public static TemplatedValue<Object> parse(String template) {
      if(StringUtils.isEmpty(template)) {
         return nullValue();
      }
      Matcher m = NAMED.matcher(template);
      if(m.matches()) {
         return named(m.group(1));
      }
      else if(TEXT.matcher(template).matches()) {
         return (TemplatedValue) text(template);
      }
      else {
         return value(template);
      }
   }
   
}

