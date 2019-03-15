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
package com.iris.driver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.iris.Utils;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;

/**
 *
 */
public class DriverPredicates {

   public static AttributeMapPredicateBuilder builder() {
      return new AttributeMapPredicateBuilder();
   }

   public static AttributeMatchBuilder<String, Predicate<AttributeMap>> vendor() {
      return attribute(DriverConstants.DEV_ATTR_VENDOR);
   }

   public static AttributeMatchBuilder<String, Predicate<AttributeMap>> model() {
      return attribute(DriverConstants.DEV_ATTR_MODEL);
   }

   public static AttributeMatchBuilder<String, Predicate<AttributeMap>> protocol() {
      return attribute(DriverConstants.DEVADV_ATTR_PROTOCOL);
   }

   public static AttributeMatchBuilder<String, Predicate<AttributeMap>> subprotocol() {
      return attribute(DriverConstants.DEVADV_ATTR_SUBPROTOCOL);
   }

   public static AttributeMatchBuilder<String, Predicate<AttributeMap>> protocolId() {
      return attribute(DriverConstants.DEVADV_ATTR_PROTOCOLID);
   }

   public static <T> AttributeMatchBuilder<T, Predicate<AttributeMap>> attribute(AttributeKey<T> key) {
      return new AttributeMatchBuilder<T, Predicate<AttributeMap>>(key, Functions.<Predicate<AttributeMap>>identity());
   }

   public static Predicate<AttributeMap> containsAttribute(final AttributeKey<? extends Object> key) {
      return attributeMatches(key, Predicates.<Object>notNull());
   }

   public static Predicate<AttributeMap> attributeLike(
         final AttributeKey<? extends CharSequence> key,
         final Pattern pattern
   ) {
      return attributeMatches(key, Predicates.contains(pattern));
   }

   public static <T> Predicate<AttributeMap> attributeEquals(
         final AttributeKey<T> key,
         final T value
   ) {
      return attributeMatches(key, Predicates.equalTo(value));
   }

   public static <T> Predicate<AttributeMap> attributeMatches(
         final AttributeKey<T> key,
         final Predicate<? super T> predicate
   ) {
      Utils.assertNotNull(key);
      Utils.assertNotNull(predicate);
      return new Predicate<AttributeMap>() {
         @Override
         public boolean apply(AttributeMap input) {
            if(input == null) {
               return false;
            }
            T value = input.get(key);
            if(value == null) {
               return false;
            }
            return predicate.apply(value);
         }

         @Override
         public String toString() {
            return "Predicate<AttributeMap> [" + key + " matches " + predicate + "]";
         }
      };
   }

   public static <T> Predicate<T> isIn(final Collection<T> value) {
      return new Predicate<T>() {
         @Override
         public boolean apply(T input) {
            if (input == null) {
               return false;
            }
            return value.contains(input);
         }
      };
   }
   
   public static <T> Predicate<Collection<? super T>> contains(final T value) {
      return new Predicate<Collection<? super T>>() {
         @Override
         public boolean apply(Collection<? super T> input) {
            if(input == null) {
               return false;
            }
            return input.contains(value);
         }
      };
   }

   public static <T> Predicate<Collection<? super T>> containsAll(final Collection<T> value) {
      return new Predicate<Collection<? super T>>() {
         @Override
         public boolean apply(Collection<? super T> input) {
            if(input == null) {
               return false;
            }
            return input.containsAll(value);
         }
      };
   }

   public static <T> Predicate<Collection<? super T>> containsAny(final Iterable<T> values) {
      return new Predicate<Collection<? super T>>() {
         @Override
         public boolean apply(Collection<? super T> input) {
            if(input == null) {
               return false;
            }
            for(T value: values) {
               if(input.contains(value)) {
                  return true;
               }
            }
            return false;
         }
      };
   }

   public static class AttributeMatchBuilder<T, R> {
      private final AttributeKey<T> key;
      private final Function<Predicate<AttributeMap>, R> transformer;

      protected AttributeMatchBuilder(AttributeKey<T> key, Function<Predicate<AttributeMap>, R> transformer) {
         this.key = key;
         this.transformer = transformer;
      }

      public R notNull() {
         return matches(Predicates.notNull());
      }

      public R equalTo(T value) {
         return matches(Predicates.equalTo(value));
      }

      public R matches(final Predicate<? super T> predicate) {
         Predicate<AttributeMap> p = attributeMatches(key, predicate);
         return transformer.apply(p);
      }
   }

   public static class AttributeMapPredicateBuilder {
      private List<Predicate<AttributeMap>> matchers = new ArrayList<Predicate<AttributeMap>>();

      protected AttributeMapPredicateBuilder() {

      }

      public <T> AttributeMatchBuilder<T, AttributeMapPredicateBuilder> attribute(AttributeKey<T> key) {
         return new AttributeMatchBuilder<T, DriverPredicates.AttributeMapPredicateBuilder>(
               key,
               new Function<Predicate<AttributeMap>, AttributeMapPredicateBuilder>() {
                  @Override
                  public AttributeMapPredicateBuilder apply(Predicate<AttributeMap> input) {
                     matchers.add(input);
                     return AttributeMapPredicateBuilder.this;
                  }

               }
         );
      }

      public boolean hasConditions() {
         return !matchers.isEmpty();
      }

      public Predicate<AttributeMap> create() {
         if(matchers.isEmpty()) {
            return Predicates.alwaysFalse();
         }
         if(matchers.size() == 1) {
            return matchers.get(0);
         }
         return Predicates.and(matchers);
      }

   }

}

