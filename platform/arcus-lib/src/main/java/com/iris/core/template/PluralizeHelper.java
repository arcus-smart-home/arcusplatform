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
package com.iris.core.template;

import java.io.IOException;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

public class PluralizeHelper implements Helper<Number> {

   public static final String NAME = "pluralize";
   public static final Helper<Number> INSTANCE = new PluralizeHelper();

   private PluralizeHelper() {
   }

   @Override
   public CharSequence apply(Number context, Options options) throws IOException {
      if(context == null) {
        throw new IllegalArgumentException("a number must be provided as the first argument");
      }
      if(options.params == null || options.params.length < 2) {
         throw new IllegalArgumentException("two strings must be provided as the second and third arguments");
      }

      String singular = String.valueOf(options.params[0]);
      String plural = String.valueOf(options.params[1]);

      // 0 is plural in English
      if(context.intValue() == 1) {
         return singular;
      }
      return plural;
   }
}

