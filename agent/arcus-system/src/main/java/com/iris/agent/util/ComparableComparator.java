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
package com.iris.agent.util;

import java.io.Serializable;
import java.util.Comparator;

import org.eclipse.jdt.annotation.Nullable;

public class ComparableComparator<T extends Comparable<? super T>> implements Comparator<T>, Serializable {
   private static final long serialVersionUID = -196897585895245809L;

   @Override
   public int compare(@Nullable T o1, @Nullable T o2) {
      if (o1 == o2) return 0;
      if (o1 == null) return -1;
      if (o2 == null) return 1;

      return o1.compareTo(o2);
   }
}

