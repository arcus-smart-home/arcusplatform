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
package com.iris.model.type;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.iris.device.attributes.AttributeMap;

public class AttributeTypes {

   public static AttributeType fromJavaType(Type type) {
      if(type instanceof Class) {
         return fromClass((Class<?>) type);
      }
      if(type instanceof ParameterizedType) {
         return fromParameterizedType((ParameterizedType) type);
      }
      throw new IllegalArgumentException("Unsupported type [" + type + "]");
   }
   
   public static AttributeType extractContainedType(AttributeType type) {
      return (type instanceof CollectionType) ? ((CollectionType)type).getContainedType() : null;
   }
   
   @SuppressWarnings("rawtypes")
   public static Set<Object> convertToCoercedSet(AttributeType type, Collection values) {
      Set<Object> set = new LinkedHashSet<>();
      for (Object value : values) {
         set.add(type.coerce(value));
      }
      return set;
   }

   private static AttributeType fromClass(Class<?> type) {
      // void
      if(Void.TYPE.isAssignableFrom(type)) {
         return VoidType.INSTANCE;
      }
      
      // wildcard type
      if(Object.class.equals(type)) {
         return AnyType.INSTANCE;
      }
      
      // primitive types
      if(Boolean.class.equals(type)) {
         return BooleanType.INSTANCE;
      }
      if(Byte.class.equals(type)) {
         return ByteType.INSTANCE;
      }
      if(Short.class.equals(type) || Integer.class.equals(type)) {
         return IntType.INSTANCE;
      }
      if(LongType.class.equals(type)) {
         return LongType.INSTANCE;
      }
      if(Number.class.isAssignableFrom(type)) {
         return DoubleType.INSTANCE;
      }
      if(Date.class.isAssignableFrom(type)) {
         return TimestampType.INSTANCE;
      }
      if(String.class.isAssignableFrom(type)) {
         return StringType.INSTANCE;
      }
      
      if(Collection.class.isAssignableFrom(type)) {
         return new ListType(AnyType.INSTANCE);
      }
      // map type
      if(Map.class.isAssignableFrom(type) || AttributeMap.class.isAssignableFrom(type)) {
         return new MapType(AnyType.INSTANCE);
      }
      
      // TODO support custom Java objects
      
      throw new IllegalArgumentException("Unsupported type [" + type + "], not a valid AttributeType");
   }

   private static AttributeType fromParameterizedType(ParameterizedType type) {
      Class<?> raw = (Class<?>) type.getRawType();
      Type [] genericTypes = type.getActualTypeArguments();
      if(Map.class.isAssignableFrom(raw)) {
         if(genericTypes.length == 2 && String.class.equals(genericTypes[0])) {
            return new MapType(fromJavaType(genericTypes[1]));
         }
         else {
            throw new IllegalArgumentException("Only Map<String, T> is valid, where T is compatible with AttributeType");
         }
      }
      if(Collection.class.isAssignableFrom(raw)) {
         if(genericTypes.length == 1) {
            if (Set.class.isAssignableFrom(raw)) {
               return new SetType(fromJavaType(genericTypes[0]));
            }
            return new ListType(fromJavaType(genericTypes[0]));
         }
         else {
            throw new IllegalArgumentException("Only Collection<T> is valid, where T is compatible with AttributeType");
         }
      }

      throw new IllegalArgumentException("Unsupported type [" + type + "], not a valid AttributeType");
   }

}

