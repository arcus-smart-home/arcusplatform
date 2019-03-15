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
package com.iris.platform.rule.catalog.function;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Calendar;

import com.google.common.base.Function;
import com.iris.common.rule.Context;

@SuppressWarnings("serial")
public class GetCurrentTimeFormatted implements Function<Context, String>, Serializable {
   private final DateFormat df;
   
   public GetCurrentTimeFormatted(DateFormat df) {
      this.df = df;
   }

   @Override
   public String apply(Context input) {
      Calendar cal = input.getLocalTime();
      df.setCalendar(cal);
      return df.format(cal.getTime());
   }

   @Override
   public String toString() {
      return "current time";
   }
}

