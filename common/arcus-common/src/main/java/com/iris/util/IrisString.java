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

import org.apache.commons.lang3.StringUtils;

public class IrisString {

   /***
    * @param separator The string to put between elements in params
    * @param params String which are to be combined if not empty. If empty, string is ignored.
    * @return Non empty params separated by separator in single string.
    */
   public static String joinIfNotEmpty(String separator, String... params) {
      if (params.length <= 0) return "";

      StringBuilder builder = null;
      for (String s : params) {
         if (StringUtils.isEmpty(s)) continue;

         if (builder == null) {
            builder = new StringBuilder(s);
         } else {
            builder.append(separator);
            builder.append(s);
         }
      }

      if (builder == null) return "";

      return builder.toString();
   }

}

