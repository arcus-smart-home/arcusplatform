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
package com.iris.util;

public class Objects
{
   /*
    * There is a StringUtils.equalsAny() in commons-lang 3.5, but for now we're on 3.3.2.
    */
   @SafeVarargs
   public static <T> boolean equalsAny(T obj, T... others)
   {
      if (obj == null || others == null)
      {
         return false;
      }

      for (Object other : others)
      {
         if (obj.equals(other))
         {
            return true;
         }
      }

      return false;
   }

   private Objects() { }
}

