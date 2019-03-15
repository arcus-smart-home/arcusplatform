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

public class BooleanHandler extends TypeHandlerImpl<Boolean> {

   public BooleanHandler() {
      super(Boolean.class, Number.class, String.class);
   }

   @Override
   protected Boolean convert(Object value) {
      if(value instanceof Number) {
         // TODO:  should we only allow 0 or 1?
         return ((Number) value).longValue() != 0;
      }
      return Boolean.valueOf((String) value);
   }
}

