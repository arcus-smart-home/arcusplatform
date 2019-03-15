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
package com.iris.resource.manager;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isAnyEmpty;
import static org.apache.commons.lang3.StringUtils.trim;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class SimpleMapCsvParser extends BaseCsvParser<Map<String, String>>
{
   @Override
   protected Map<String, String> newResult()
   {
      return new HashMap<String, String>();
   }

   @Override
   protected int getExpectedLength()
   {
      return 2;
   }

   @Override
   protected void processLine(String[] nextLine, Map<String, String> result, int lineNumber)
   {
      String key = trim(nextLine[0]);
      String value = trim(nextLine[1]);

      if (result.containsKey(key))
      {
         throw new IllegalStateException(
            format("Duplicate key in record {key:[%s] value:[%s]} on line %d", key, value, lineNumber));
      }

      if (isAnyEmpty(key, value))
      {
         throw new IllegalStateException(
            format("Empty field(s) in record {key:[%s] value:[%s]} on line %d", key, value, lineNumber));
      }

      result.put(key, value);
   }

   @Override
   protected Map<String, String> finalizeResult(Map<String, String> result)
   {
      return ImmutableMap.copyOf(result);
   }
}

