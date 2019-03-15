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
package com.iris.type.functional;

import java.io.Serializable;

import com.google.common.base.Function;
import com.iris.type.handler.EnumHandler;

@SuppressWarnings("serial")
public class EnumTransformer<T> extends DescribedFunction implements Function<Object, T>, Serializable {
   private final EnumHandler handler;
   private final Class<T> clazz;
   
   public EnumTransformer(EnumHandler handler, Class<T> clazz) {
      this(handler, clazz, "to" + clazz.getSimpleName());
   }
   
   public EnumTransformer(EnumHandler handler, Class<T> clazz, String description) {
      super(description);
      this.handler = handler;
      this.clazz = clazz;
   }

   @SuppressWarnings("unchecked")
   @Override
   public T apply(Object input) {
      if (input == null) {
         return null;
      }
      if (clazz.isInstance(input)) {
         return (T)input;
      }
      return handler.coerce(clazz, input);
   }
}

