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
package com.iris.model.type;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Date;
import java.util.Map;

/**
 *
 */
public enum AnyType implements AttributeType {
   INSTANCE;

   private static final AttributeType ANY_MAP = new MapType(INSTANCE);
   private static final AttributeType ANY_LIST = new ListType(INSTANCE);

   @Override
   public String getTypeName() {
      return "any";
   }

   @Override
   public Class<?> getJavaType() {
      return Object.class;
   }

   @Override
   public Object coerce(Object obj) {
      if(obj == null) {
         return null;
      }
      Class<?> cls = obj.getClass();
      if(
            // direct pass-through
            Boolean.class.isAssignableFrom(cls) ||
            Byte.class.isAssignableFrom(cls) ||
            Integer.class.isAssignableFrom(cls) ||
            Long.class.isAssignableFrom(cls) ||
            Double.class.isAssignableFrom(cls) ||
            String.class.isAssignableFrom(cls)
      ) {
         return obj;
      }
      if(Short.class.isAssignableFrom(cls)) {
         return ((Short) obj).intValue();
      }
      if(Number.class.isAssignableFrom(cls)) {
         return ((Number) obj).doubleValue();
      }
      if(Iterable.class.isAssignableFrom(cls)) {
         return ANY_LIST.coerce(obj);
      }
      if(Map.class.isAssignableFrom(cls)) {
         return ANY_MAP.coerce(obj);
      }
      // TODO fall back to string here?
      throw new IllegalArgumentException("Object of type " + cls + " is not a valid attribute type");
   }

   @Override
   public boolean isAssignableFrom(Type type) {
      if(type instanceof Class) {
         Class<?> cls = (Class<?>) type;
         return
               Boolean.class.isAssignableFrom(cls) ||
               Number.class.isAssignableFrom(cls) ||
               String.class.isAssignableFrom(cls) ||
               Date.class.isAssignableFrom(cls) ||
               Iterable.class.isAssignableFrom(cls) ||
               Map.class.isAssignableFrom(cls) ||
               Object.class.equals(cls);
      }
      if(type instanceof ParameterizedType) {
         ParameterizedType pType = (ParameterizedType) type;
         if(Map.class.isAssignableFrom((Class<?>) pType.getRawType())) {
            return
                  pType.getActualTypeArguments().length == 2 &&
                  String.class.equals(pType.getActualTypeArguments()[0]) &&
                  this.isAssignableFrom(pType.getActualTypeArguments()[1]);
         }
         else if(Iterable.class.isAssignableFrom((Class<?>) pType.getRawType())) {
            return
               pType.getActualTypeArguments().length == 1 &&
               this.isAssignableFrom(pType.getActualTypeArguments()[0]);
         }
      }
      return false;
   }

   @Override
   public String toString() {
      return "any";
   }

}

