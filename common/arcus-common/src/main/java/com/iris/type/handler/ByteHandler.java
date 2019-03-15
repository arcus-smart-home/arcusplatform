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

public class ByteHandler extends TypeHandlerImpl<Byte> {

   public ByteHandler() {
      super(Byte.class, Number.class, String.class);
   }

   @Override
   protected Byte convert(Object value) {
      if(value instanceof Number) {
         // Allow numbers over Byte.MAX_VALUE since the groovy driver will send over values like 0xff as integer 255.
         return ((Number)value).byteValue();
      }
      Byte b;
      try {
         b = Byte.valueOf((String)value);
         return b;
      }
      catch (NumberFormatException nfe) {
         return Byte.decode((String)value);
      }
   }

}

