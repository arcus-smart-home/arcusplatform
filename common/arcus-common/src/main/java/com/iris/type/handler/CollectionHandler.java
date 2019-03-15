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
package com.iris.type.handler;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;

import com.iris.type.TypeCoercer;

public class CollectionHandler implements Serializable {
   protected final TypeCoercer typeCoercer;
   protected enum ObjectType { STRING, ITERABLE, ARRAY }
   
   public CollectionHandler(TypeCoercer typeCoercer) {
      this.typeCoercer = typeCoercer;
   }
   
   public boolean isSupportedType(Class<?> containedType, Type type) {
      if (type == null) {
         return false;
      }
      if (String.class.equals(type)) {
         return true;
      }
      
      if (type instanceof Class) {
         Class<?> clazz = (Class<?>)type;
         if (clazz.isArray()) {
            return true;
         }
         if (Iterable.class.isAssignableFrom(clazz)) {
            return true;
         }
      }
      else if (type instanceof ParameterizedType) {
         ParameterizedType pType = (ParameterizedType) type;
         if (Iterable.class.isAssignableFrom((Class<?>) pType.getRawType())) {
            Type[] actualTypeArgs = pType.getActualTypeArguments();
            if (actualTypeArgs.length == 1) {
               return typeCoercer.isSupportedType(containedType, actualTypeArgs[0]);
            }
         }
      }
      return false;
   }

   public boolean isCoercible(Class<?> targetContainedClass, Object obj) {
      if (obj == null) {
         return true;
      }
      ObjectType sourceType = getSourceObjectType(obj);
      if (sourceType == ObjectType.STRING) {
         return typeCoercer.isCoercible(targetContainedClass, obj);
      }
      if (sourceType == ObjectType.ITERABLE) {
         Iterator<?> it = ((Iterable<?>)obj).iterator();
         if (it.hasNext()) {
            return typeCoercer.isCoercible(targetContainedClass, it.next());
         }
         else {
            // Coercing an empty collection is easy.
            return true;
         }
      }
      if (sourceType == ObjectType.ARRAY) {
         return Array.getLength(obj) > 0
               ? typeCoercer.isCoercible(targetContainedClass, Array.get(obj, 0))
               : true;
      }
      return false;
   }
   
   public <T, C extends Collection<T>> C coerce(Class<T> targetContainedClass, Object obj, CollectionFactory<T, C> factory) {
      if (obj == null) {
         return null;
      }
      
      ObjectType sourceType = getSourceObjectType(obj);
      
      if (sourceType != null) {
         C collection = factory.createCollection();
         populate(collection, targetContainedClass, obj, sourceType);
         return collection;
      }
      
      throw new IllegalArgumentException("Object of class " + obj.getClass().getName() + " cannot be coerced to List");
   }
   
   protected ObjectType getSourceObjectType(Object obj) {
      if (obj instanceof String) {
         return ObjectType.STRING;
      }
      if (obj instanceof Iterable<?>) {
         return ObjectType.ITERABLE;
      }
      if (obj.getClass().isArray()) {
         return ObjectType.ARRAY;
      }
      return null;
   }
   
   protected <T> void populate(Collection<T> collection, Class<T> targetContainedClass, Object obj, ObjectType sourceType) {
      if (sourceType == ObjectType.ARRAY) {
         for(int i = 0; i < Array.getLength(obj); i++) {
            collection.add(typeCoercer.coerce(targetContainedClass, Array.get(obj, i)));
         }
      }
      else if (sourceType == ObjectType.ITERABLE) {
         for(Object value: (Iterable<?>)obj) {
            collection.add(typeCoercer.coerce(targetContainedClass, value));
         }
      }
      else if (sourceType == ObjectType.STRING) {
         String [] values = StringUtils.split((String)obj, ", ");
         for(String value: values) {
            collection.add(typeCoercer.coerce(targetContainedClass, value));
         }
      }
   }
}

