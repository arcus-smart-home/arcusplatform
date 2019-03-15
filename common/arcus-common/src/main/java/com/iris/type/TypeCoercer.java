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
package com.iris.type;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.iris.type.handler.AddressHandler;
import com.iris.type.handler.BooleanHandler;
import com.iris.type.handler.ByteHandler;
import com.iris.type.handler.DateHandler;
import com.iris.type.handler.DoubleHandler;
import com.iris.type.handler.IntegerHandler;
import com.iris.type.handler.LongHandler;
import com.iris.type.handler.ShortHandler;
import com.iris.type.handler.StringHandler;
import com.iris.type.handler.TimeOfDayHandler;

public interface TypeCoercer extends Serializable {

   public static final TypeHandler<?>[] standardHandlers = new TypeHandler[] {
      new AddressHandler(),
      new BooleanHandler(),
      new ByteHandler(),
      new ShortHandler(),
      new IntegerHandler(),
      new LongHandler(),
      new DoubleHandler(),
      new DateHandler(),
      new StringHandler(),
      new TimeOfDayHandler()
   };

   public boolean isSupportedType(Class<?> clazz, Type type);

   public boolean isSupportedCollectionType(Class<?> containedClazz, Type type);

   public boolean isCoercible(Class<?> clazz, Object obj);

   public boolean isCoercibleCollection(Class<?> containedClazz, Object obj);

   public <T> T coerce(Class<T> clazz, Object obj);

   public <T> List<T> coerceList(Class<T> clazz, Object obj);

   public <T> Set<T> coerceSet(Class<T> clazz, Object obj);

   /**
    * Same thing as coerce only it returns null on failure instead of throwing exception.
    *
    * @param clazz target class to coerce to
    * @param obj instance to coerce
    * @return instance of coerced class
    */
   public <T> T attemptCoerce(Class<T> clazz, Object obj);

   /**
    * Same thing as coerceList only it returns null on failure instead of throwing exception.
    *
    * @param clazz target class to coerce to
    * @param obj instance to coerce
    * @return instance of coerced class
    */
   public <T> List<T> attemptCoerceList(Class<T> clazz, Object obj);

   /**
    * Same thing as coerceSet only it returns null on failure instead of throwing exception.
    *
    * @param clazz target class to coerce to
    * @param obj instance to coerce
    * @return instance of coerced class
    */
   public <T> Set<T> attemptCoerceSet(Class<T> clazz, Object obj);

   public <T> Function<Object, T> createTransformer(Class<T> clazz);

   public <T> Function<Object, T> createTransformer(Class<T> clazz, String description);

   public <T> Function<Object, List<T>> createListTransformer(Class<T> containedClazz);

   public <T> Function<Object, List<T>> createListTransformer(Class<T> containedClazz, String description);

   public <T> Function<Object, Set<T>> createSetTransformer(Class<T> containedClazz);

   public <T> Function<Object, Set<T>> createSetTransformer(Class<T> containedClazz, String description);

   public <T> Predicate<Object> createPredicate(Class<T> clazz, Predicate<T> predicate);

   public <T> Predicate<Object> createPredicate(Class<T> clazz, Predicate<T> predicate, boolean returnFalseOnException);

   public <T> Predicate<Object> createListPredicate(Class<T> clazz, Predicate<List<T>> predicate);

   public <T> Predicate<Object> createListPredicate(Class<T> clazz, Predicate<List<T>> predicate, boolean returnFalseOnException);

   public <T> Predicate<Object> createSetPredicate(Class<T> clazz, Predicate<Set<T>> predicate);

   public <T> Predicate<Object> createSetPredicate(Class<T> clazz, Predicate<Set<T>> predicate, boolean returnFalseOnException);

   public <T> Predicate<Type> createSupportedTypePredicate(Class<T> clazz);

   public <T> Predicate<Type> createSupportedCollectionPredicate(Class<T> containedClazz);

   public void registerHandlers(TypeHandler<?>... newHandlers);
}

