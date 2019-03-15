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
package com.iris.driver.groovy;

import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.model.AttributeDefinition;
import com.iris.driver.DriverPredicates;
import com.iris.model.type.AttributeType;
import com.iris.model.type.AttributeTypes;
import com.iris.util.ByteSet;
import com.iris.validators.Validator;

public class DriverMatchers {

   /**
    * Converts a definition and a value to a matcher.  If the value is null or not
    * set this will return {@code null}.  If the proper matcher can't be determined
    * this will return {@code null} and add the appropriate error to {@code v}.
    * @param def
    * @param value
    * @param v
    * @return
    */
   public static Predicate<AttributeMap> toMatcher(AttributeDefinition def, Object value) {
      if(value == null) {
         return null;
      }
      AttributeKey<?> key = def.getKey();
      if(String.class.equals(key.getType())) {
         return toStringMatcher((AttributeKey<String>) key, value);
      }
      else if(Collection.class.isAssignableFrom(TypeUtils.getRawType(key.getType(), null))) {
         return toCollectionMatcher(
               // this cast should be safe since we're using it read-only
               // any class can be down-cast to Object on a get
               // sets would throw ClassCastException in most cases
               (AttributeKey<Collection<Object>>) key,
               AttributeTypes.extractContainedType(def.getAttributeType()), 
               value
         );
      }
      else if(ByteSet.class.equals(key.getType())) {
         return toByteSetMatcher((AttributeKey<ByteSet>) key, value);
      }
      else if(TypeUtils.getRawType(key.getType(), null).isAssignableFrom(value.getClass())) {
         return DriverPredicates.attributeEquals((AttributeKey) key, value);
      }
      else if(def.getAttributeType().isAssignableFrom(value.getClass())) {
         return DriverPredicates.attributeEquals((AttributeKey) key, def.getAttributeType().coerce(value));
      }
      else if(value instanceof Collection) {
         return toCollectionMatcher(key, def.getAttributeType(), value);
      }
      GroovyValidator.error("Unable to create a matcher for [" + key + "] unsupported type for matching");
      return null;
   }

   public static Predicate<AttributeMap> toStringMatcher(
         AttributeKey<String> key,
         Object value
   ) {
      if(value instanceof String) {
         return
            DriverPredicates
               .attribute(key)
               .equalTo((String) value);
      }
      if(value instanceof Pattern) {
         return
            DriverPredicates
               .attribute(key)
               .matches(Predicates.contains((Pattern) value));
      }

      GroovyValidator.error("Unable to convert [" + value + "] to a string matcher, should be a string or a regex");
      return null;
   }

   public static Predicate<AttributeMap> toCollectionMatcher(
         AttributeKey<?> key,
         AttributeType type,
         Object value
   ) {
      return
         DriverPredicates
            .attribute(key)
            .matches(DriverPredicates.isIn(AttributeTypes.convertToCoercedSet(type, (Collection<Object>)value)));
   }


   public static Predicate<AttributeMap> toCollectionMatcher(
         AttributeKey<Collection<Object>> key,
         AttributeType type,
         Object value,
         Validator v
   ) {
      // TODO deeper type inspection?
      if(value instanceof Collection) {
         return 
            DriverPredicates
               .<Collection<Object>>attribute(key)
               .matches(DriverPredicates.containsAll(AttributeTypes.convertToCoercedSet(type, (Collection<?>)value)));
      }
      else {
         return 
            DriverPredicates
               .attribute(key)
               .matches(DriverPredicates.contains(type.coerce(value)));
      }
   }

   @Nullable
   public static Predicate<AttributeMap> toByteSetMatcher(
         AttributeKey<ByteSet> key,
         Object value
   ) {
      if(value instanceof Number) {
         byte b = ((Number) value).byteValue();
         return toByteSetMatcher(key, b);
      }
      if(value instanceof String) {
         try {
            byte b = Byte.parseByte((String) value);
            return toByteSetMatcher(key, b);
         }
         catch(NumberFormatException e) {
            GroovyValidator.error("Invalid matcher for [" + key + "]: " + e.getMessage());
         }
      }
      return null;
   }

   public static Predicate<AttributeMap> toByteSetMatcher(
         AttributeKey<ByteSet> key,
         final byte b
   ) {
      return 
         DriverPredicates
            .attribute(key)
            .matches(new Predicate<ByteSet>() {
               @Override
               public boolean apply(ByteSet input) {
                  return input != null && input.contains(b);
               }
            });
   }


}

