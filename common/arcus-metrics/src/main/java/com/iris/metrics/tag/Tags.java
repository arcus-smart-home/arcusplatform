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
package com.iris.metrics.tag;

import static java.lang.String.format;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class Tags
{
   public static Set<TagValue> of(String... namesAndValues)
   {
      if (namesAndValues.length % 2 != 0)
      {
         throw new IllegalArgumentException(format("namesAndValues length [%d] must be even", namesAndValues.length));
      }

      ImmutableSet.Builder<TagValue> tags = ImmutableSet.builder();

      for (int i = 0; i < namesAndValues.length; i += 2)
      {
         tags.add(new TagValue(namesAndValues[i], namesAndValues[i + 1]));
      }

      return tags.build();
   }

   private Tags() { }
}

