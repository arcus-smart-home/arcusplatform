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
package com.iris.driver.groovy;


/**
 *
 */
public class Arguments {
   
   protected Arguments() { }
   
   public static String extractOptionalString(int index, Object [] arguments) {
      CharSequence cs = extractOptionalArgument(index, arguments, CharSequence.class);
      return cs != null ? cs.toString() : null;
   }

   public static Byte extractOptionalByte(int index, Object [] arguments) {
      Number n = extractOptionalArgument(index, arguments, Number.class);
      if(n == null) {
         return null;
      }
      if(n instanceof Byte) {
         return (Byte) n;
      }
      return n.byteValue();
   }

   public static <T> T extractOptionalArgument(int index, Object [] arguments, Class<T> cls) {
      if(arguments.length <= index) {
         return null;
      }
      Object value = arguments[index];
      if(value == null) {
         return null;
      }
      if(cls.isAssignableFrom(value.getClass())) {
         return (T) value;
      }
      return null;
   }
   

}

