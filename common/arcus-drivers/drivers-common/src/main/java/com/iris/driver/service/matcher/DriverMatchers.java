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
package com.iris.driver.service.matcher;

import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.commons.lang3.reflect.TypeUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.model.AttributeDefinition;
import com.iris.driver.DriverPredicates;
import com.iris.driver.DriverPredicates.AttributeMapPredicateBuilder;
import com.iris.model.type.AttributeType;
import com.iris.model.type.AttributeTypes;
import com.iris.util.ByteSet;
import com.iris.validators.Validator;

public class DriverMatchers {

   public static boolean addMatcherIf(AttributeMapPredicateBuilder builder, AttributeDefinition def, Object value, Validator v) {
      if(value == null) {
         return false;
      }
      AttributeKey<?> key = def.getKey();
      if(String.class.equals(key.getType())) {
         return addStringMatcher(builder, (AttributeKey<String>) key, value, v);
      }
      else if(Collection.class.isAssignableFrom(TypeUtils.getRawType(key.getType(), null))) {
         return addCollectionMatcher(builder,
               (AttributeKey<Collection<?>>) key,
               AttributeTypes.extractContainedType(def.getAttributeType()), value, v);
      }
      else if(ByteSet.class.equals(key.getType())) {
         return addByteSetMatcher(builder, (AttributeKey<ByteSet>) key, value, v);
      }
      else if(TypeUtils.getRawType(key.getType(), null).isAssignableFrom(value.getClass())) {
         builder.attribute((AttributeKey) key).equalTo(value);
         return true;
      }
      else if(def.getAttributeType().isAssignableFrom(value.getClass())) {
         builder.attribute((AttributeKey) key).equalTo(def.getAttributeType().coerce(value));
         return true;
      }
      else if(value instanceof Collection) {
         return addInCollectionMatcher(builder, key, def.getAttributeType(), value);
      }
      v.error("Unable to create a matcher for [" + key + "] unsupported type for matching");
      return false;
   }

   public static boolean addStringMatcher(
         AttributeMapPredicateBuilder builder,
         AttributeKey<String> key,
         Object value,
         Validator v
   ) {
      if(value instanceof String) {
         builder
            .attribute(key)
            .equalTo((String) value);
         return true;
      }
      if(value instanceof Pattern) {
         builder
            .attribute(key)
            .matches(Predicates.contains((Pattern) value));
         return true;
      }

      v.error("Unable to convert [" + value + "] to a string matcher, should be a string or a regex");
      return false;
   }

   public static boolean addInCollectionMatcher(
         AttributeMapPredicateBuilder builder,
         AttributeKey<?> key,
         AttributeType type,
         Object value) {
      builder
         .attribute(key)
         .matches(DriverPredicates.isIn(AttributeTypes.convertToCoercedSet(type, (Collection)value)));
      return true;
   }

   public static boolean addCollectionMatcher(
         AttributeMapPredicateBuilder builder,
         AttributeKey<Collection<?>> key,
         AttributeType type,
         Object value,
         Validator v
   ) {
      // TODO deeper type inspection?
      if(value instanceof Collection) {
         builder
            .attribute((AttributeKey) key)
            .matches(DriverPredicates.containsAll(AttributeTypes.convertToCoercedSet(type, (Collection)value)));
         return true;
      }
      else {
         builder
            .attribute((AttributeKey) key)
            .matches(DriverPredicates.contains(type.coerce(value)));
         return true;
      }
   }

   public static boolean addByteSetMatcher(
         AttributeMapPredicateBuilder builder,
         AttributeKey<ByteSet> key,
         Object value,
         Validator v
   ) {
      if(value instanceof Number) {
         byte b = ((Number) value).byteValue();
         addByteSetMatcher(builder, key, b);
      }
      if(value instanceof String) {
         try {
            byte b = Byte.parseByte((String) value);
            addByteSetMatcher(builder, key, b);
         }
         catch(NumberFormatException e) {
            v.error("Invalid matcher for [" + key + "]: " + e.getMessage());
         }
      }
      return false;
   }

   public static void addByteSetMatcher(
         AttributeMapPredicateBuilder builder,
         AttributeKey<ByteSet> key,
         final byte b
   ) {
      builder
         .attribute(key)
         .matches(new Predicate<ByteSet>() {
            @Override
            public boolean apply(ByteSet input) {
               return input != null && input.contains(b);
            }
         });
   }


}

