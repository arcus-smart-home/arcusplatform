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

import java.util.Collection;

public class Collections
{
   /*
    * There is a CollectionUtils.containsAny() in commons-collections, but varargs is very useful for readability.
    */
   @SafeVarargs
   public static <T> boolean containsAny(Collection<T> objs, T... others)
   {
      if (objs == null || others == null)
      {
         return false;
      }

      for (Object other : others)
      {
         if (objs.contains(other))
         {
            return true;
         }
      }

      return false;
   }

   /*
    * There is a CollectionUtils.containsAll() in commons-collections, but varargs is very useful for readability.
    */
   @SafeVarargs
   public static <T> boolean containsAll(Collection<T> objs, T... others)
   {
      if (objs == null || others == null)
      {
         return false;
      }

      for (Object other : others)
      {
         if (!objs.contains(other))
         {
            return false;
         }
      }

      return true;
   }

   private Collections() { }
}

