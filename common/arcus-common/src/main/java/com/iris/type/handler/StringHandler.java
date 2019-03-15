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
import java.util.HashMap;
import java.util.Map;

import com.iris.type.ToStringHandler;

@SuppressWarnings("serial")
public class StringHandler extends TypeHandlerImpl<String> {
   private final Map<Class<?>, ToStringHandler<?>> toStringHandlers = new HashMap<>();

   public StringHandler() {
      this(new ToStringHandler<?>[]{});
   }
   
   public StringHandler(ToStringHandler<?>... additionalToStringHandlers) {
      super(String.class, Object.class);
      addHandler(new ToStringAddressHandler());
      if (additionalToStringHandlers != null && additionalToStringHandlers.length > 0) {
         for (ToStringHandler<?> toStringHandler : additionalToStringHandlers) {
            addHandler(toStringHandler);
         }
      }
   }
   
   @Override
   public boolean isSupportedType(Type type) {
      return type != null;
   }

   @Override
   public String coerce(Object value) {
      if (value == null) {
         return null;
      }
      if (value instanceof String) {
         return (String)value;
      }
      // Check custom handlers in case the object needs to do something besides call 'toString'
      ToStringHandler<?> toStringHandler = getHandler(value);
      if (toStringHandler != null) {
         String s = toStringHandler.apply(value);
         if (s != null) {
            return s;
         }
      }
      return String.valueOf(value);
   }

   @Override
   protected String convert(Object value) {
      // Stub, since we override coerce this is never called.
      return null;
   }
   
   private ToStringHandler<?> getHandler(Object value) {
      ToStringHandler<?> toStringHandler = toStringHandlers.get(value.getClass());
      if (toStringHandler != null) {
         return toStringHandler;
      }
      for (Class<?> handlerInputClazz : toStringHandlers.keySet()) {
         if (handlerInputClazz.isInstance(value)) {
            return toStringHandlers.get(handlerInputClazz);
         }
      }
      return null;
   }
   
   private void addHandler(ToStringHandler<?> toStringHandler) {
      toStringHandlers.put(toStringHandler.inputClass(), toStringHandler);
   }
}

