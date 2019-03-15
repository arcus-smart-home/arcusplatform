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

import java.lang.reflect.Type;
import java.util.Set;

import org.apache.commons.lang3.reflect.TypeUtils;

import com.iris.type.TypeHandler;
import com.iris.util.IrisCollections;

public abstract class TypeHandlerImpl<T> implements TypeHandler<T> {
   private final Set<Class<?>> supportedTypes;
   protected final Class<T> targetType;
   
   public TypeHandlerImpl(Class<T> targetType, Class<?>... supportedTypes) {
      this.targetType = targetType;
      this.supportedTypes = IrisCollections.setOf(supportedTypes);
   }

   @Override
   public Class<T> getTargetType() {
      return targetType;
   }
   
   @Override
   public boolean isSupportedType(Type type) {
      if (type == null) {
         return false;
      }
      if (TypeUtils.isAssignable(type, targetType)) {
         return true;
      }
      for (Class<?> clazz : supportedTypes) {
         if (TypeUtils.isAssignable(type, clazz)) {
            return true;
         }
      }
      return false;
   }
   
   @Override
   public boolean isCoercible(Object value) {
      return value == null ? true : isSupportedType(value.getClass());
   }

   /**
    * The value must not be null.
    */
   @Override
   public T coerce(Object value) {
      if (isSupportedType(value.getClass())) {
         return convert(value);
      }
      throw new IllegalArgumentException("Object of class " + value.getClass().getName() + " cannot be coerced to " + targetType.getName());
   }
   
   /**
    * The value is guaranteed to be one of the supported classes.
    * 
    * @param value object to convert
    * @return the converted object
    */
   protected abstract T convert(Object value);
}

