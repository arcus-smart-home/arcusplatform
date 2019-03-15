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
package com.iris.util;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.reflect.TypeUtils;

import com.google.common.base.Preconditions;

/**
 * 
 */
public abstract class TypeMarker<T> {
   private static final TypeMarker<Object> OBJECT = new TypeMarker<Object>() {};
   private static final TypeMarker<Boolean> BOOL = new TypeMarker<Boolean>() {};
   private static final TypeMarker<Integer> INTEGER = new TypeMarker<Integer>() {};
   private static final TypeMarker<Long> LONG = new TypeMarker<Long>() {};
   private static final TypeMarker<String> STRING = new TypeMarker<String>() {};
   
   public static TypeMarker<Object> object() {
      return OBJECT;
   }
   
   public static TypeMarker<Boolean> bool() {
      return BOOL;
   }

   public static TypeMarker<Integer> integer() {
      return INTEGER;
   }
   
   public static TypeMarker<Long> longNumber() {
      return LONG;
   }
   
   public static TypeMarker<String> string() {
      return STRING;
   }
   
   public static TypeMarker<?> wrap(Type type) {
      return new TypeMarker<Object>(type) {};
   }
   
   public static <T> TypeMarker<List<T>> listOf(Class<T> containedType) {
      Preconditions.checkNotNull(containedType, "containedType may not be null");
      return new TypeMarker<List<T>>(TypeUtils.parameterize(List.class, containedType)) {};
   }
   
   public static <T> TypeMarker<Set<T>> setOf(Class<T> containedType) {
      Preconditions.checkNotNull(containedType, "containedType may not be null");
      return new TypeMarker<Set<T>>(TypeUtils.parameterize(Set.class, containedType)) {};
   }
   
   public static <T> TypeMarker<Collection<T>> collectionOf(Class<T> containedType) {
      Preconditions.checkNotNull(containedType, "containedType may not be null");
      return new TypeMarker<Collection<T>>(TypeUtils.parameterize(Collection.class, containedType)) {};
   }
   
   public static <V> TypeMarker<Map<String, V>> mapOf(Class<V> valueType) {
      return mapOf(String.class, valueType);
   }
   
   public static <K, V> TypeMarker<Map<K, V>> mapOf(Class<K> keyType, Class<V> valueType) {
      Preconditions.checkNotNull(keyType, "keyType may not be null");
      Preconditions.checkNotNull(valueType, "valueType may not be null");
      return new TypeMarker<Map<K, V>>(TypeUtils.parameterize(Map.class, keyType, valueType)) {};
   }
   
   private final Type type;
   
   protected TypeMarker() {
      Map<TypeVariable<?>, Type> types = TypeUtils.getTypeArguments(this.getClass().getGenericSuperclass(), TypeMarker.class);
      if(types.isEmpty()) {
         throw new IllegalArgumentException("Improper use of TypeMarker, must specify a concrete (no ?s) type for the type variable");
      }
      this.type = types.values().iterator().next();
   }
   
   private TypeMarker(Type type) {
      this.type = type;
   }
   
   public Type getType() {
      return type;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      return result;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (!(obj instanceof TypeMarker)) return false;
      TypeMarker<?> other = (TypeMarker<?>) obj;
      if (type == null) {
         if (other.type != null) return false;
      }
      else if (!type.equals(other.type)) return false;
      return true;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "TypeMarker [type=" + type + "]";
   }

}

