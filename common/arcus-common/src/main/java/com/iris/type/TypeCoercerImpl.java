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

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.reflect.TypeUtils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.iris.type.functional.CollectionTransformer;
import com.iris.type.functional.EnumTransformer;
import com.iris.type.functional.SupportedCollectionPredicate;
import com.iris.type.functional.SupportedEnumPredicate;
import com.iris.type.functional.SupportedTypePredicate;
import com.iris.type.functional.TypeHandlerTransformer;
import com.iris.type.functional.TypePredicate;
import com.iris.type.functional.UnsupportedTransformer;
import com.iris.type.functional.UnsupportedTypePredicate;
import com.iris.type.handler.CollectionHandler;
import com.iris.type.handler.EnumHandler;
import com.iris.type.handler.ListFactory;
import com.iris.type.handler.SetFactory;
import com.iris.util.IrisCollections;

@SuppressWarnings("serial")
public class TypeCoercerImpl implements TypeCoercer {
   private final static Map<Class<?>, TypeHandler<?>> staticHandlers 
      = IrisCollections.toUnmodifiableMap(standardHandlers, new Function<TypeHandler<?>, Class<?>>() {
         @Override
         public Class<?> apply(TypeHandler<?> input) {
            return input.getTargetType();
         }        
      });
   
   private final Map<Class<?>, TypeHandler<?>> handlers = new HashMap<>();
   private final EnumHandler enumHandler = new EnumHandler();
   private final CollectionHandler collectionHandler;
   
   public TypeCoercerImpl(TypeHandler<?>... handlers) {
      registerHandlers(handlers);
      collectionHandler = new CollectionHandler(this);
   }
   
   @Override
   public boolean isSupportedType(Class<?> clazz, Type type) {
      if (type == null) {
         return false;
      }
      if (TypeUtils.isAssignable(type, clazz)) {
         return true;
      }
      TypeHandler<?> handler = getHandler(clazz);
      if (handler != null) {
         return handler.isSupportedType(type);
      }
      if (clazz.isEnum()) {
         return enumHandler.isSupportedType(clazz, type);
      }
      return false;
   }
   
   @Override
   public boolean isSupportedCollectionType(Class<?> containedClazz, Type type) {
      return type != null ? collectionHandler.isSupportedType(containedClazz, type) : false;
   }

   @Override
   public boolean isCoercible(Class<?> clazz, Object obj) {
      if (obj == null) {
         return true;
      }
      if (clazz.isInstance(obj)) {
         return true;
      }
      TypeHandler<?> handler = getHandler(clazz);
      if (handler != null) {
         return handler.isCoercible(obj);
      }
      if (clazz.isEnum()) {
         return enumHandler.isCoercible(clazz, obj);
      }
      return false;
   }

   @Override
   public boolean isCoercibleCollection(Class<?> containedClazz, Object obj) {
      if (obj == null) {
         return true;
      }
      return collectionHandler.isCoercible(containedClazz, obj);
   }

   @SuppressWarnings("unchecked")
   @Override
   public <T> T coerce(Class<T> clazz, Object obj) {
      if (obj == null) {
         return null;
      }
      if (clazz.isInstance(obj)) {
         // If it is the same class or a subclass, just cast it.
         return (T)obj;
      }
      TypeHandler<T> handler = getHandler(clazz);
      if (handler != null) {
         return handler.coerce(obj);
      }
      if (clazz.isEnum()) {
         return enumHandler.coerce(clazz, obj);
      }
      throw new IllegalArgumentException("Object of class " + obj.getClass().getName() + " cannot be coerced to " + clazz.getName());
   }

   @Override
   public <T> List<T> coerceList(Class<T> clazz, Object obj) {
      return collectionHandler.coerce(clazz, obj, new ListFactory<T>());
   }

   @Override
   public <T> Set<T> coerceSet(Class<T> clazz, Object obj) {
      return collectionHandler.coerce(clazz, obj, new SetFactory<T>());
   }

   @Override
   public <T> T attemptCoerce(Class<T> clazz, Object obj) {
      try {
         return coerce(clazz, obj);
      }
      catch (Exception ex) {
         return null;
      }
   }

   @Override
   public <T> List<T> attemptCoerceList(Class<T> clazz, Object obj) {
      try {
         return coerceList(clazz, obj);
      }
      catch (Exception ex) {
         return null;
      }
   }

   @Override
   public <T> Set<T> attemptCoerceSet(Class<T> clazz, Object obj) {
      try {
         return coerceSet(clazz, obj);
      }
      catch (Exception ex) {
         return null;
      }
   }

   @Override
   public <T> Function<Object, T> createTransformer(Class<T> clazz) {
      return createTransformer(clazz, "to" + clazz.getSimpleName());
   }
   
   @Override
   public <T> Function<Object, T> createTransformer(Class<T> clazz, String description) {
      TypeHandler<T> handler = getHandler(clazz);
      if (handler != null) {
         return new TypeHandlerTransformer<T>(handler, clazz, description);
      }
      if (clazz.isEnum()) {
         return new EnumTransformer<T>(enumHandler, clazz, description);
      }
      return new UnsupportedTransformer<T>(clazz, description);
   }

   @Override
   public <T> Function<Object, List<T>> createListTransformer(Class<T> containedClazz) {
      return createListTransformer(containedClazz, "toListOf" + containedClazz.getSimpleName());
   }
   
   @Override
   public <T> Function<Object, List<T>> createListTransformer(Class<T> containedClazz, String description) {
      return new CollectionTransformer<T, List<T>>(collectionHandler, containedClazz, new ListFactory<T>(), description);
   }

   @Override
   public <T> Function<Object, Set<T>> createSetTransformer(Class<T> containedClazz) {
      return createSetTransformer(containedClazz, "toSetOf" + containedClazz.getSimpleName());
   }
   
   @Override
   public <T> Function<Object, Set<T>> createSetTransformer(Class<T> containedClazz, String description) {
      return new CollectionTransformer<T, Set<T>>(collectionHandler, containedClazz, new SetFactory<T>(), description);
   }

   @Override
   public <T> Predicate<Object> createPredicate(Class<T> clazz, Predicate<T> predicate) {
      return new TypePredicate<T>(createTransformer(clazz), predicate, false);
   }
   
   @Override
   public <T> Predicate<Object> createPredicate(Class<T> clazz, Predicate<T> predicate, boolean returnFalseOnException) {
      return new TypePredicate<T>(createTransformer(clazz), predicate, returnFalseOnException);
   }

   @Override
   public <T> Predicate<Object> createListPredicate(Class<T> clazz, Predicate<List<T>> predicate) {
      return new TypePredicate<List<T>>(createListTransformer(clazz), predicate, false);
   }
   
   @Override
   public <T> Predicate<Object> createListPredicate(Class<T> clazz, Predicate<List<T>> predicate, boolean returnFalseOnException) {
      return new TypePredicate<List<T>>(createListTransformer(clazz), predicate, returnFalseOnException);
   }

   @Override
   public <T> Predicate<Object> createSetPredicate(Class<T> clazz, Predicate<Set<T>> predicate) {
      return new TypePredicate<Set<T>>(createSetTransformer(clazz), predicate, false);
   }
   
   @Override
   public <T> Predicate<Object> createSetPredicate(Class<T> clazz, Predicate<Set<T>> predicate, boolean returnFalseOnException) {
      return new TypePredicate<Set<T>>(createSetTransformer(clazz), predicate, returnFalseOnException);
   }

   @Override
   public <T> Predicate<Type> createSupportedTypePredicate(Class<T> clazz) {
      TypeHandler<T> handler = getHandler(clazz);
      if (handler != null) {
         return new SupportedTypePredicate(handler);
      }
      if (clazz.isEnum()) {
         return new SupportedEnumPredicate(enumHandler, clazz);
      }
      return new UnsupportedTypePredicate(clazz);
   }

   @Override
   public <T> Predicate<Type> createSupportedCollectionPredicate(Class<T> containedClazz) {
      return new SupportedCollectionPredicate(collectionHandler, containedClazz);
   }

   @Override
   public void registerHandlers(TypeHandler<?>... newHandlers) {
      if (newHandlers != null && newHandlers.length > 0) {
         for (TypeHandler<?> handler : newHandlers) {
            handlers.put(handler.getTargetType(), handler);
         }
      }
   }
   
   @SuppressWarnings("unchecked")
   private <T> TypeHandler<T> getHandler(Class<T> clazz) {
      // Use the custom handlers first so they can override the static handlers
      TypeHandler<?> handler = handlers.get(clazz);
      return (TypeHandler<T>)(handler != null ? handler : staticHandlers.get(clazz));
   }
}

