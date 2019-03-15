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

import java.util.Comparator;

/**
 * Created by wesleystueve on 4/14/17.
 */
public abstract class IrisComparator<T> implements Comparator<T> {

   protected static final Comparator<String> NULL_EQUALS_EMPTY_STRING_CASE_INSENSITIVE_COMPARATOR = Comparator.comparing(
         (String s) -> s == null ? "" : s,
         String::compareToIgnoreCase);

   protected static final int NULL_CASE_NOT_FOUND = 42;

   protected int CompareNullCasesNullFirst(Object o1, Object o2)
   {
      //take care of the null cases first. null is considered first.
      if (o1 == null && o2 == null) return 0;
      if (o1 != null && o2 == null) return -1;
      if (o1 == null /* && o2 != null <- case always true at this point */) return 1;

      return NULL_CASE_NOT_FOUND;
   }
   
   @Override
   public abstract int compare(T o1, T o2);
}

